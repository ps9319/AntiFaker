package org.example.antifaker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:8000")
            .defaultHeader("Content-Type", "multipart/form-data")
            .build();
    }
}
