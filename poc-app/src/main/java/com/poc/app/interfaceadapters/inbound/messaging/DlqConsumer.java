package com.poc.app.interfaceadapters.inbound.messaging;

import com.poc.app.contracts.usecases.ManageDlqUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final ManageDlqUseCase manageDlqUseCase;

    public DlqConsumer(ManageDlqUseCase manageDlqUseCase) {
        this.manageDlqUseCase = manageDlqUseCase;
    }

    @KafkaListener(
            topicPattern = "${topics.dlq.pattern}",
            containerFactory = "dlqListenerContainerFactory"
    )
    public void onMessage(List<ConsumerRecord<String, String>> records) {
        log.debug("Received {} DLQ messages in batch", records.size());

        for (ConsumerRecord<String, String> record : records) {
            log.debug("Processing DLQ message: topic={}, key={}, partition={}, offset={}",
                    record.topic(), record.key(), record.partition(), record.offset());

            manageDlqUseCase.addMessage(
                    record.topic(),
                    record.key(),
                    record.value()
            );
        }
    }
}
