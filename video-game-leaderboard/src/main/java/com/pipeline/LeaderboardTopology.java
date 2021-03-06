package com.pipeline;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.apache.kafka.streams.state.KeyValueStore;

import com.pipeline.model.Player;
import com.pipeline.model.Product;
import com.pipeline.model.ScoreEvent;
import com.pipeline.model.join.Enriched;
import com.pipeline.model.join.ScoreWithPlayer;
import com.pipeline.serialization.json.JsonSerdes;

class LeaderboardTopology {

    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        // score-event에서 데이터를 읽어옴, 조인을 위해 rekey를 하기위해 플레이어 아이디로 리키를함.
        KStream<String, ScoreEvent> scoreEvents =
                builder
                        .stream("score-events", Consumed.with(Serdes.ByteArray(), JsonSerdes.ScoreEvent()))
                        .selectKey((k, v) -> v.getPlayerId().toString());

        KTable<String, Player> players =
                builder.table("players", Consumed.with(Serdes.String(), JsonSerdes.Player()));

        GlobalKTable<String, Product> products =
                builder.globalTable("products", Consumed.with(Serdes.String(), JsonSerdes.Product()));

        // join scoreEvents -> players
        ValueJoiner<ScoreEvent, Player, ScoreWithPlayer> scorePlayerJoiner =
                (score, player) -> new ScoreWithPlayer(score, player);

        // join params for scoreEvents -> players join
        Joined<String, ScoreEvent, Player> playerJoinParams =
                //조인 매개변수는 조인 레코드의 키와 값을 직렬화하는 방법을 정의합니다.
                Joined.with(Serdes.String(), JsonSerdes.ScoreEvent(), JsonSerdes.Player());

        KStream<String, ScoreWithPlayer> withPlayers =
                //inner join을 수행
                scoreEvents.join(players,
                                 scorePlayerJoiner, //  두 개의 조인 레코드에서 새 ScoreWithPlayer 값이 생성됩니다.
                                 playerJoinParams);

        // join the withPlayers stream to the product global ktable
        ValueJoiner<ScoreWithPlayer, Product, Enriched> productJoiner =
                (scoreWithPlayer, product) -> new Enriched(scoreWithPlayer, product);

        // KStream-GlobalKTable 조인을 수행하려면 KStream 레코드를 Global KTable 레코드에 매핑하는 방법을 지정하는 것이 목적인 Key ValueMapper라는 것을 생성해야 합니다.
        // ScoreWithPlayer 값에서 제품ID를 간단히 추출가능
        KeyValueMapper<String, ScoreWithPlayer, String> keyMapper =
                (leftKey, scoreWithPlayer) -> {
                    return String.valueOf(scoreWithPlayer.getScoreEvent().getProductId());
                };

        KStream<String, Enriched> withProducts = withPlayers.join(products, keyMapper, productJoiner);

        withProducts.print(Printed.<String, Enriched>toSysOut().withLabel("with-products"));

        // groupBy를 사용하는 것은 selectKey를 사용하여 스트림을 다시 입력하는 프로세스와 유사합니다.
        // 그러나 레코드를 rekey 필요가 없는 경우 groupByKey 를 사용하는것이 더좋다.
        KGroupedStream<String, Enriched> grouped =
                withProducts.groupBy(
                        (key, value) -> value.getProductId().toString(),
                        Grouped.with(Serdes.String(), JsonSerdes.Enriched()));

        Initializer<HighScores> highScoresInitializer = HighScores::new;

        Aggregator<String, Enriched, HighScores> highScoresAdder =
                (key, value, aggregate) -> aggregate.add(value);

        // key-value 저장소인 Materialized를 이용하여 대화형 쿼리를 사용하여 저장소에 쿼리를함
        KTable<String, HighScores> highScores =
                grouped.aggregate(
                        highScoresInitializer,
                        highScoresAdder,
                        // 키, 밸류, 상태 저장소의 유형<Bytes, byte[]>
                        Materialized.<String, HighScores, KeyValueStore<Bytes, byte[]>>as("leader-boards")// queries를 위한 저장소의 이름
                                                                                                          .withKeySerde(Serdes.String())
                                                                                                          .withValueSerde(JsonSerdes.HighScores()));

        highScores.toStream().to("high-scores");

        return builder.build();
    }
}
