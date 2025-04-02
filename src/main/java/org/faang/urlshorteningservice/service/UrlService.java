package org.faang.urlshorteningservice.service;


import lombok.extern.slf4j.Slf4j;
import org.faang.urlshorteningservice.domain.Url;
import org.faang.urlshorteningservice.domain.dto.UrlDto;
import org.faang.urlshorteningservice.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
public class UrlService {


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_URL_LENGTH = 6;

    @Autowired
    private UrlRepository repository;

    @Autowired
    private final KafkaTemplate<String , Url> kafkaTemplate;

    public UrlService(KafkaTemplate<String, Url> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    private String generateShortUrl() {
        StringBuilder shortUrl = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < SHORT_URL_LENGTH; i++) {
            shortUrl.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return shortUrl.toString();
    }


    private String shortUrlGenerator() {
        String generatedUrl = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789AserQwerggfdSpring";
        StringBuilder shortUrl = new StringBuilder();
        Random random = new Random();
        int length = 6;

        log.info("Generated url: " + generatedUrl);
        for (int i = 0; i < length; i++) {
            shortUrl.append(generatedUrl.charAt(random.nextInt(generatedUrl.length())));
        }
        log.info("Generated url: " + shortUrl.toString());
        return shortUrl.toString();

    }


    public Mono<Url> shortenUrl(UrlDto dto) {
        String shortUrl = shortUrlGenerator();

        log.info("Shortening URL: " + shortUrl);
        Url url = Url.builder()
                .url(dto.getUrl())
                .shortUrl(shortUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        log.info("Shortened URL: " + url);

        return repository.save(url)
                .doOnSuccess(savedUrl ->
                        kafkaTemplate.send("shorten-url", savedUrl.getShortUrl() , savedUrl)
                                .thenAccept(result -> log.info("Url shortened: " + url.getShortUrl()))
                                .exceptionally(ex -> {
                                    log.error(ex.getMessage(), ex);
                                    return null;
                                })

                );

    }


    public Mono<Url> getStats(String shortUrl) {
        return repository.findByShortUrl(shortUrl)
                .switchIfEmpty(Mono.error(new RuntimeException("Short URL not found")));

    }

    public Mono<Url> getShortUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isEmpty()) {
            return Mono.empty();
        }
        log.info("Get shortUrl: " + shortUrl);
        return repository.findByShortUrl(shortUrl)
                .flatMap(url ->{
                    url.setAccessCount(url.getAccessCount() + 1);
                    return repository.save(url);
                });
    }



    public Mono<Url> deleteShortUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isEmpty()) {
            return Mono.empty();
        }
        log.info("Delete shortUrl: " + shortUrl);
        return repository.findByShortUrl(shortUrl)
                .switchIfEmpty(Mono.error( new RuntimeException("Short URL not found")))
                .flatMap(
                foudnUrl -> repository.delete(foudnUrl).thenReturn(foudnUrl)
        );
    }


    public Flux<Url> findAll() {
        return repository.findAll().cache();
    }


    public Mono<Url> updateShortUrl(String shortUrl, UrlDto dto) {
        return repository.findByShortUrl(shortUrl)
                .flatMap(existingUrl -> {
                    String newShortUrl = shortUrlGenerator();
                    log.info("Updating shortUrl: " + newShortUrl);
                    existingUrl.setUrl(dto.getUrl());
                    existingUrl.setShortUrl(newShortUrl);
                    existingUrl.setUpdatedAt(LocalDateTime.now());
                    log.info("Url updated: " + existingUrl.getShortUrl());
                    return repository.save(existingUrl);
                });
    }





}
