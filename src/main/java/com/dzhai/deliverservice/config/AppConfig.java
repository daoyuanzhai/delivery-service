package com.dzhai.deliverservice.config;

import lombok.val;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
@EnableCaching
public class AppConfig {
    @Bean
    public RestTemplate getRestTemplate() {
        val requestFactory = new HttpComponentsClientHttpRequestFactory();
        val restClient = new RestTemplate(new BufferingClientHttpRequestFactory(requestFactory));
        return restClient;
    }
}
