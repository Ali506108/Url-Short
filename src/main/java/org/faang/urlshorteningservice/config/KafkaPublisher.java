package org.faang.urlshorteningservice.config;

import lombok.extern.slf4j.Slf4j;
import org.faang.urlshorteningservice.domain.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaPublisher {

    private final KafkaTemplate<String, Url> kafkaTemplate;

    @Autowired
    public KafkaPublisher(KafkaTemplate<String, Url> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUrlEvent(Url url) {
        kafkaTemplate.send("url-event-topic", url.getShortUrl(), url)
                .thenAccept(result -> log.info("Produced URL event: {}", url.getShortUrl()))
                .exceptionally(ex -> {
                    log.error("Error while producing URL event", ex);
                    return null;
                });
    }
}
