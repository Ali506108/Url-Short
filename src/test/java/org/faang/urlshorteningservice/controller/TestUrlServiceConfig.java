package org.faang.urlshorteningservice.controller;

import org.faang.urlshorteningservice.service.UrlService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestUrlServiceConfig {

    @Bean
    public UrlService urlService() {
        return Mockito.mock(UrlService.class);
    }
}
