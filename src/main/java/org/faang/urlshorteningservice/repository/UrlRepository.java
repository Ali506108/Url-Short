package org.faang.urlshorteningservice.repository;

import org.faang.urlshorteningservice.domain.Url;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UrlRepository extends ReactiveMongoRepository<Url, String> {

    Mono<Url> findByUrl(String shortUrl);

    Mono<Url> findByShortUrl(String shortUrl);

    boolean existsByShortUrl(String shortUrl);
}
