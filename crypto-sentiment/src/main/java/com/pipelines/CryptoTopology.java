package com.pipelines;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

import com.pipelines.data.Tweet;
import com.pipelines.data.TweetSerdes;

public class CryptoTopology {

    public static Topology buildBasic() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], byte[]> stream = builder.stream("tweets");
        stream.print(Printed.<byte[], byte[]>toSysOut().withLabel("tweets-stream"));

        return builder.build();
    }

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<byte[], Tweet> stream =
                builder.stream("tweets", Consumed.with(Serdes.ByteArray(), new TweetSerdes()));
        stream.print(Printed.<byte[], Tweet>toSysOut().withLabel("tweets-stream"));
        return builder.build();
    }
}
