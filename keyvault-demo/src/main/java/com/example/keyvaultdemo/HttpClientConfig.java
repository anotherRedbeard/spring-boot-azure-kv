package com.example.keyvaultdemo;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Value("${app.my.apim.subscriptionkey}")
    private String subscriptionKey;

    @Value("${app.my.apim.debugtoken}")
    private String debugToken;

    private ClientHttpRequestInterceptor subscriptionKeyInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("Ocp-Apim-Subscription-Key", subscriptionKey);
            request.getHeaders().add("Apim-Debug-Authorization", debugToken);
            return execution.execute(request, body);
        };
    }
    
    // When demo.useSecureRestTemplate is true (or absent), build the secure RestTemplate
    @Bean
    @ConditionalOnProperty(name = "demo.useSecureRestTemplate", havingValue = "true", matchIfMissing = true)
    public RestTemplate secureRestTemplate(RestTemplateBuilder restTemplateBuilder, SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle("tlsClientBundle");

        // Log the SslBundle details for debugging without using getSSLContext()
        System.out.println("SslBundle details: " + sslBundle.toString());

        return restTemplateBuilder
                .sslBundle(sslBundle)
                .additionalInterceptors(subscriptionKeyInterceptor())
                .build();
    }

    // When demo.useSecureRestTemplate is false, build a plain RestTemplate
    @Bean
    @ConditionalOnProperty(name = "demo.useSecureRestTemplate", havingValue = "false")
    public RestTemplate plainRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
            .additionalInterceptors(subscriptionKeyInterceptor())
            .build();
    }
}
