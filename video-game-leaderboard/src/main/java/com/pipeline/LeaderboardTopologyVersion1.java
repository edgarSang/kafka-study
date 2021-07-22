package com.pipeline;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Printed;

import com.pipeline.model.Player;
import com.pipeline.model.Product;
import com.pipeline.model.ScoreEvent;

class LeaderboardTopologyVersion1 {
    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<byte[], ScoreEvent> scoreEvents =
                builder.stream("score-events", Consumed.with(Serdes.ByteArray(), JsonSerdes.ScoreEvent()));

        KTable<String, Player> players =
                builder.table("players", Consumed.with(Serdes.String(), JsonSerdes.Player()));

        GlobalKTable<String, Product> products =
                builder.globalTable("products", Consumed.with(Serdes.String(), JsonSerdes.Product()));

        scoreEvents.print(Printed.<byte[], ScoreEvent>toSysOut().withLabel("score-events"));
        players.toStream().print(Printed.<String, Player>toSysOut().withLabel("players"));

        return builder.build();
    }
}
