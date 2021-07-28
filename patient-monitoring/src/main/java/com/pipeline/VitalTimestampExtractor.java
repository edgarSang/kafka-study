package com.pipeline;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import com.pipeline.model.Vital;

public class VitalTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Vital measurement = (Vital) record.value();
        if (measurement != null && measurement.getTimestamp() != null) {
            String timestamp = measurement.getTimestamp();
            // System.out.println("Extracting timestamp: " + timestamp);
            return Instant.parse(timestamp).toEpochMilli();
        }
        // fallback to stream time
        return partitionTime;
    }
}
