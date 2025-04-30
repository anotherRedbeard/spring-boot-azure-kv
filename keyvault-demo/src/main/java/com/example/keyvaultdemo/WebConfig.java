package com.example.keyvaultdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class WebConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true); // Log client address and session ID
        loggingFilter.setIncludeQueryString(true); // Log the query string
        loggingFilter.setIncludePayload(true); // Log the request body (use with caution in prod!)
        loggingFilter.setMaxPayloadLength(10000); // Limit payload logging size
        loggingFilter.setIncludeHeaders(false); // Log request headers (set to true if needed)
        loggingFilter.setAfterMessagePrefix("REQUEST DATA : "); // Prefix for the log message
        // loggingFilter.setBeforeMessagePrefix("BEFORE REQUEST : "); // Uncomment for before message
        return loggingFilter;
    }
}
