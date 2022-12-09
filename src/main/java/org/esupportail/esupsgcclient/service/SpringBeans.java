package org.esupportail.esupsgcclient.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SpringBeans {

    @Bean
    public HttpComponentsClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory () {
        HttpComponentsClientHttpRequestFactory httpRequestFactory;
        httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(300000);
        httpRequestFactory.setConnectTimeout(300000);
        httpRequestFactory.setReadTimeout(300000);
        return httpRequestFactory;
    }


    @Bean
    public RestTemplate getRestTemplate(HttpComponentsClientHttpRequestFactory httpRequestFactory) {
        return new RestTemplate(httpRequestFactory);
    }

}
