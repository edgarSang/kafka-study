package com.example;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerExactPartition {
    private final static String TOPIC_NAME = "test";
    private final static int PARTITION_NUMBER = 0;
    private final static String BOOTSTRAP_SERVERS = "broker01:9092";
    private final static String GROUP_ID = "test-group";

    private final static Logger logger = LoggerFactory.getLogger(ConsumerExactPartition.class);

    public static void main(String[] args) {
        Properties configs = new Properties();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        //Auto commit false and have to commit clearly
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(configs);
//        exactPartition(consumer);
        partitionCheck(consumer);
    }


    private static void exactPartition(KafkaConsumer<String, String> consumer) {
        // 이전에 사용하던 subscribe 대신 assign 메소드를 사용하면서 test의 0번 파티션을 할당하여 레코드를 가져온다.
        // subscribe와 다르게 직접 컨슈머가 특정 토픽, 특정파티션에 할당되므로 리밸런싱 과정이없다
        // 리밸런싱: 컨슈머그룹에서 컨슈머가 할당,삭제되면서 partition을 할당하게되는것
        consumer.assign(Collections.singleton(new TopicPartition(TOPIC_NAME, PARTITION_NUMBER)));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> logger.info("{}", record));
        }
    }

    private static void partitionCheck(KafkaConsumer<String, String> consumer) {
        consumer.subscribe(Arrays.asList(TOPIC_NAME));
        Set<TopicPartition> assignedTopicPartition = consumer.assignment();

        assignedTopicPartition.forEach(topicPartition -> logger.info(topicPartition.toString()));


        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> logger.info("{}", record));
        }
    }
}
