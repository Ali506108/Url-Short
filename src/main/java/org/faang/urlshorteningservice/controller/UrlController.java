package org.faang.urlshorteningservice.controller;


import lombok.RequiredArgsConstructor;
import org.faang.urlshorteningservice.domain.Url;
import org.faang.urlshorteningservice.domain.dto.UrlDto;
import org.faang.urlshorteningservice.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shorter")
public class UrlController {


    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;


    @GetMapping("/get/{shortUrl}")
    public Mono<Url> getShortUrl(@PathVariable String shortUrl) {
        return urlService.getShortUrl(shortUrl);
    }


    @GetMapping("/findAll")
    public Flux<Url> findAll() {
        return urlService.findAll();
    }


    @GetMapping("/stats/{shortUrl}")
    public Mono<ResponseEntity<Url>> getUrlStats(@PathVariable String shortUrl) {
        return urlService.getShortUrl(shortUrl)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));

    }


    @PostMapping("/make-short")
    public Mono<ResponseEntity<Url>> shortenUrl(@RequestBody UrlDto dto) {
        return urlService.shortenUrl(dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PutMapping("/update/{shortUrl}")
    public Mono<ResponseEntity<Url>> updateShortUrl(@PathVariable String shortUrl, @RequestBody UrlDto dto) {
        return urlService.updateShortUrl(shortUrl, dto)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/delete/{shortUrl}")
    public Mono<ResponseEntity<Url>> deleteShortUrl(@PathVariable String shortUrl) {
        return urlService.deleteShortUrl(shortUrl)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }



}
