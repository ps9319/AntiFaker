package org.example.antifaker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            // 파이썬 서버 주소
            .baseUrl("http://localhost:8080")
            .build();
    }
}
