package org.faang.urlshorteningservice.controller;

import org.faang.urlshorteningservice.domain.Url;
import org.faang.urlshorteningservice.domain.dto.UrlDto;
import org.faang.urlshorteningservice.service.UrlService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(controllers = UrlController.class)
@Import(TestUrlServiceConfig.class)
class UrlControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UrlService urlService; // уже бин из TestConfig

    @Test
    void testCreateShortUrl() {
        UrlDto dto = new UrlDto();
        dto.setUrl("https://example.com");

        Url url = new Url();
        url.setShortUrl("abc123");
        url.setUrl(dto.getUrl());

        Mockito.when(urlService.shortenUrl(Mockito.any()))
                .thenReturn(Mono.just(url));

        webTestClient.post()
                .uri("/api/shorter/make-short")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.shortUrl").isEqualTo("abc123");
    }
}
