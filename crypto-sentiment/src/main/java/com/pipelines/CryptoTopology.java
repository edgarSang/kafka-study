package com.pipelines;

import java.util.Arrays;
import java.util.List;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Printed;

import com.pipelines.data.Tweet;
import com.pipelines.data.TweetSerdes;
import com.pipelines.language.GcpClient;
import com.pipelines.language.LanguageClient;
import com.pipelines.model.EntitySentiment;

public class CryptoTopology {
    private static final List<String> currencies = Arrays.asList("bitcoin", "ethereum");

    public static Topology buildBasic() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], byte[]> stream = builder.stream("tweets");
        stream.print(Printed.<byte[], byte[]>toSysOut().withLabel("tweets-stream"));

        return builder.build();
    }

    public static Topology build() {
        LanguageClient languageClient = new GcpClient();
        StreamsBuilder builder = new StreamsBuilder();
        KStream<byte[], Tweet> stream =
                builder.stream("tweets", Consumed.with(Serdes.ByteArray(), new TweetSerdes()));
        stream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));

        KStream<byte[], Tweet> filtered = stream.filterNot((key, tweet) -> tweet.isRetweet());

        Predicate<byte[], Tweet> englishTweets = (key, tweet) -> tweet.getLang().equals("en");
        Predicate<byte[], Tweet> nonEnglishTweets = (key, tweet) -> !tweet.getLang().equals("en");

        KStream<byte[], Tweet>[] branches = filtered.branch(englishTweets, nonEnglishTweets);
        KStream<byte[], Tweet> englishStream = branches[0];
        KStream<byte[], Tweet> nonEnglishStream = branches[1];

        KStream<byte[], Tweet> translateStream = nonEnglishStream.mapValues(
                (tweet) -> {
                    return languageClient.translate(tweet, "en");
                });

        KStream<byte[], Tweet> merged = englishStream.merge(translateStream);

        KStream<byte[], EntitySentiment> enriched =
                merged.flatMapValues(
                        (tweet) -> {
                            List<EntitySentiment> results = languageClient.getEntitySentiment(tweet);
                            results.removeIf(
                                    entitySentiment -> !currencies.contains(
                                            entitySentiment.getEntity()
                                    ));
                            return results;
                        });

        return builder.build();
    }
}
