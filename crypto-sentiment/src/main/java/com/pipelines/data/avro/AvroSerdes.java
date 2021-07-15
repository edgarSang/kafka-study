package com.pipelines.data.avro;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.common.serialization.Serde;

import com.pipelines.model.EntitySentiment;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

public class AvroSerdes {
    public static Serde<EntitySentiment> EntitySentiment(String url, boolean isKey) {
        Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", url);
        Serde<EntitySentiment> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig, isKey);
        return serde;
    }
}
