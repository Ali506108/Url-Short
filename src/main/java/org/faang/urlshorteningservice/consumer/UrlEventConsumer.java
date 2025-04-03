package org.faang.urlshorteningservice.consumer;


import lombok.extern.slf4j.Slf4j;
import org.faang.urlshorteningservice.domain.Url;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UrlEventConsumer {

    @KafkaListener(
            topics = "url-event-topic",
            groupId = "url-shortener-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Url url, Acknowledgment acknowledgment) {
        try {
            log.info("Consuming URL event: {}", url);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error while consuming URL event", e);
        }
    }



    @KafkaListener(topics = "dlq-url-topic" ,groupId = "dlq-consumer")
    public void handleDLQ(Url url) {
        log.warn("Received DLQ event: {}", url);
    }
}
