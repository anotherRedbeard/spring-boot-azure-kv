package com.example.keyvaultdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory

@RestController
public class CertProxyController {

    private static final Logger log = LoggerFactory.getLogger(CertProxyController.class); // Add logger

    private final RestTemplate restTemplate;

    @Value("${app.my.api.endpoint.url}")
    private String apiEndpointUrl; // Renamed field

    @Value("${app.my.api.debugtoken}") 
    private String debugToken; // Kept field name, value source changed

    public CertProxyController(RestTemplate restTemplate) {
       this.restTemplate = restTemplate;
    }

    @GetMapping("/call-secure-endpoint")
    public ResponseEntity<String> callSecureService() {
        String targetUrl = apiEndpointUrl; // Use renamed field
        log.info("Calling secure endpoint: {}", targetUrl); // Log the target URL
        ResponseEntity<String> response;

        // use exchange to capture raw response
        response = restTemplate.exchange(targetUrl, HttpMethod.GET, null, String.class);
        log.info("Received response status: {}", response.getStatusCode()); // Log the response status
        log.debug("Received response body: {}", response.getBody()); // Log the response body at DEBUG level
        // return status, headers and body exactly as received
        return ResponseEntity
            .status(response.getStatusCode())
            .headers(response.getHeaders())
            .body(response.getBody());
    }
}
