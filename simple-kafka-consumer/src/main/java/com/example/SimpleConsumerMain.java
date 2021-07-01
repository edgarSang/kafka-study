package com.example;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleConsumerMain {
    private final static Logger logger = LoggerFactory.getLogger(SimpleConsumerMain.class);
    private static Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap();

    private final static String TOPIC_NAME = "order_join";
    private final static String BOOTSTRAP_SERVERS = "broker01:9092";
    private final static String GROUP_ID = "stream-gogo";
    private static KafkaConsumer<String, String> consumer = null;

    public static void main(String[] args) {
        Properties configs = new Properties();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        //Auto commit false and have to commit clearly
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(configs);
        consumer.subscribe(Arrays.asList(TOPIC_NAME));
        // 리밸런스시에 중복되는 데이터 처리를 방지하기 위해
//        consumer.subscribe(Arrays.asList(TOPIC_NAME), new ReBalanceListener());

//        consumeCommitSync(consumer);
        consumeBasic(consumer);
//        consumeCommitAsyncAndCallback(consumer);
//        preventDuplicateBecauseReBalance(consumer);
    }


    private static void preventDuplicateBecauseReBalance(KafkaConsumer<String, String> consumer) {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<String, String> record : records) {
                logger.info("{}", record);
                currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1, null));
                consumer.commitSync(currentOffsets);
            }
        }
    }

    private static void consumeCommitAsyncAndCallback(KafkaConsumer<String, String> consumer) {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> logger.info("{}", record));
            consumer.commitAsync(((offsets, exception) -> {
                if(exception != null) {
                    logger.error("Commit Failed, offsets {}", offsets, exception);
                } else {
                    logger.info("Commit Success");
                }
            }));
        }
    }

    private static void consumeCommitSync(KafkaConsumer<String, String> consumer) {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> logger.info("{}", record));
            consumer.commitSync();
        }
    }

    private static void consumeBasic(KafkaConsumer<String, String> consumer) {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(record -> logger.info("{}", record));
        }

    }

    static class ReBalanceListener implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            // 리밸런스 시작전에 콜백
            logger.warn("partition are revoked");
            consumer.commitSync(currentOffsets);

        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            logger.warn("partition are assigned");
            // 파티션 할당완료
        }
    }

}


