package com.poc.worker.frameworks.messaging;

import com.poc.common.context.CorrelationConstants;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaCorrelationInterceptor implements ProducerInterceptor<String, String>,
        ConsumerInterceptor<String, String> {

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        String correlationId = MDC.get(CorrelationConstants.MDC_KEY);
        if (correlationId != null) {
            record.headers().add(
                    new RecordHeader(CorrelationConstants.HEADER_NAME,
                            correlationId.getBytes(StandardCharsets.UTF_8)));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        records.forEach(record -> {
            Header header = record.headers().lastHeader(CorrelationConstants.HEADER_NAME);
            if (header != null) {
                String correlationId = new String(header.value(), StandardCharsets.UTF_8);
                MDC.put(CorrelationConstants.MDC_KEY, correlationId);
            }
        });
        return records;
    }

    @Override
    public void onCommit(Map<org.apache.kafka.common.TopicPartition, OffsetAndMetadata> offsets) {
    }

    @Override
    public void close() {
        MDC.remove(CorrelationConstants.MDC_KEY);
    }
}
