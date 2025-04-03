package org.faang.urlshorteningservice.service;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.faang.urlshorteningservice.domain.Url;
import org.faang.urlshorteningservice.domain.dto.UrlDto;
import org.faang.urlshorteningservice.repository.UrlRepository;
import org.faang.urlshorteningservice.utils.Base62Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UrlService {


    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_URL_LENGTH = 6;
    private static final String REDIS_PREFIX = "url:";
    private static final int CACHE_TTL = 24 * 60 * 60;



    @Autowired
    private UrlRepository repository;

    @Autowired
    private final KafkaTemplate<String , Url> kafkaTemplate;

    @Autowired
    private RedisTemplate<String , String> redisTemplate;

    @Autowired
    private MeterRegistry registry;

    public UrlService(KafkaTemplate<String, Url> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % 62);
            result.append(BASE62.charAt(remainder));
            value /= 62;
        }
        return result.reverse().toString();
    }



    private String shortUrlGenerator() {
        String shortUrl = Base62Utils.uuidToBase62().substring(0, 8);
        log.info("Generated Base62 short URL: {}", shortUrl);
        registry.counter("url.shorten.counter").increment();
        return shortUrl;
    }





    public Mono<Url> shortenUrl(UrlDto dto) {
        String shortUrl = shortUrlGenerator();
        registry.counter("url.shorten.counter").increment();
        log.info("Shortening URL: " + shortUrl);
        Url url = Url.builder()
                .url(dto.getUrl())
                .shortUrl(shortUrl)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        log.info("Shortened URL: " + url);
        registry.counter("url.shorten.counter").increment();

        Timer.Sample sample = Timer.start(registry);

        return repository.save(url)
                .doOnSuccess(savedUrl -> {
                    sample.stop(
                            Timer.builder("url.shorten.latency")
                                    .description("Time to shorten a URL")
                                    .register(registry)
                    );

                    String redisKey = REDIS_PREFIX + savedUrl.getShortUrl();
                    registry.counter("url.shorten.counter").increment();
                    redisTemplate.opsForValue().set(redisKey , savedUrl.getShortUrl() , CACHE_TTL , TimeUnit.SECONDS);

                    registry.counter("url.shorten.counter").increment();

                    kafkaTemplate.send("shorten-url", savedUrl.getShortUrl(), savedUrl)
                            .thenAccept(result -> log.info("Url shortened: " + url.getShortUrl()))
                            .exceptionally(ex -> {
                                log.error(ex.getMessage(), ex);
                                return null;
                            });

                });

    }


    public Mono<Url> getStats(String shortUrl) {
        return repository.findByShortUrl(shortUrl)
                .switchIfEmpty(Mono.error(new RuntimeException("Short URL not found")));

    }

    public Mono<Url> getShortUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isEmpty()) {
            return Mono.empty();
        }

        String redisKey = REDIS_PREFIX + shortUrl;

        log.info("Get shortUrl: " + shortUrl);

        String cachedurl = redisTemplate.opsForValue().get(redisKey);

        registry.counter("url.shorten.counter").increment();

        log.info("Get shortUrl: " + cachedurl);

        if (cachedurl != null) {
            log.info("Get shortUrl: " + shortUrl);
            registry.counter("url.shorten.counter").increment();
            return repository.findByShortUrl(shortUrl)
                    .flatMap(url -> {
                        registry.counter("url.shorten.counter").increment();
                        url.setAccessCount(url.getAccessCount() + 1);
                        return repository.save(url);
                    });
        }

        log.info("Get shortUrl: " + shortUrl);
        return repository.findByShortUrl(shortUrl)
                .flatMap(url ->{
                    registry.counter("url.shorten.counter").increment();
                    url.setAccessCount(url.getAccessCount() + 1);
                    redisTemplate.opsForValue().set(redisKey , url.getShortUrl() , CACHE_TTL , TimeUnit.SECONDS);
                    return repository.save(url);
                });
    }



    public Mono<Url> deleteShortUrl(String shortUrl) {
        if (shortUrl == null || shortUrl.isEmpty()) {
            return Mono.empty();
        }
        log.info("Delete shortUrl: " + shortUrl);
        registry.counter("url.shorten.counter").increment();
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
                    registry.counter("url.shorten.counter").increment();
                    String newShortUrl = shortUrlGenerator();
                    log.info("Updating shortUrl: " + newShortUrl);
                    existingUrl.setUrl(dto.getUrl());
                    existingUrl.setShortUrl(newShortUrl);
                    registry.counter("url.shorten.counter").increment();
                    existingUrl.setUpdatedAt(LocalDateTime.now());
                    log.info("Url updated: " + existingUrl.getShortUrl());
                    registry.counter("url.shorten.counter").increment();
                    return repository.save(existingUrl);
                });
    }





}
