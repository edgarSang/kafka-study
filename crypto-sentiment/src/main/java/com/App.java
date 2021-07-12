package com;

import com.pipelines.CryptoTopology;
import java.util.Properties;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public class App {
  public static void main(String[] args) {
    Topology topology = CryptoTopology.build();

    Properties config = new Properties();
    config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev");
    config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "broker01:9092");

    KafkaStreams streams = new KafkaStreams(topology, config);

    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    System.out.println("Starting Twitter streams");

    streams.start();
  }
}
