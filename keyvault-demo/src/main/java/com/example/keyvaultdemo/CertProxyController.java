package com.example.keyvaultdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@RestController
public class CertProxyController {

    private final RestTemplate restTemplate;

    @Value("${app.my.apim.colors.url}")
    private String apimColorsUrl;

    @Value("${app.my.apim.debugtoken}")
    private String debugToken;

    public CertProxyController(RestTemplate restTemplate) {
       this.restTemplate = restTemplate;
    }

    @GetMapping("/call-secure-endpoint")
    public ResponseEntity<String> callSecureService() {
        String targetUrl = apimColorsUrl + "/random";
        // use exchange to capture raw response
        ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, null, String.class);
        // return status, headers and body exactly as received
        return ResponseEntity
            .status(response.getStatusCode())
            .headers(response.getHeaders())
            .body(response.getBody());
    }
}
