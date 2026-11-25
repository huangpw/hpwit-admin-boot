package com.hpwit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @Author: AlbertHPW
 * @Date: 2025/12/28 16:10
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ocrWebClient() {
        return WebClient.builder()
                .baseUrl("http://127.0.0.1:1224")
                .build();
    }
}
