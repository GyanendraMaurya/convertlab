package com.convertlab.convertlab_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024) // 5MB
                )
                .build();
    }


    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient() {
        return WebClient.builder()
                .baseUrl(openAiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(5 * 1024 * 1024)
                )
                .build();
    }
}
