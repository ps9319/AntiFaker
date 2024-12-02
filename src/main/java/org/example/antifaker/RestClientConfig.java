package org.example.antifaker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
            .baseUrl("http://localhost:8000")
            .defaultHeader("Content-Type", "multipart/form-data")
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .messageConverters(converters -> {
                converters.add(new FormHttpMessageConverter());
                converters.add(new MappingJackson2HttpMessageConverter());
            })
            .build();
    }
}
