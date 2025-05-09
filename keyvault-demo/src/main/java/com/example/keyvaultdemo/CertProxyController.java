package com.example.keyvaultdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.azure.security.keyvault.secrets.SecretClient;

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
    private String apiEndpointUrl;

    @Value("${app.my.api.debugtoken}") 
    private String debugToken;

    @Value("${app.my.secret}")
    private String injectedSecret;

    @Value("${app.my.certificate}")
    private String injectedCertificate;

    private final SecretClient secretClient;

    public CertProxyController(RestTemplate restTemplate, SecretClient secretClient) {
        this.secretClient = secretClient;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/debug-injected-secret")
    public String debugInjectedSecret() {
        try {
            // Use SecretClient to fetch the secret and use the injected secret value
            // SECURITY WARNING: This method is for demonstration purposes only.
            // Exposing secrets in API responses is a security risk.
            // In a real application, you would use the injected secret value internally
            // and never return it directly to the caller.
            return "[DEMO ONLY] Resolved secret from SecretClient: " + secretClient.getSecret("test-secret").getValue() + 
                   " and injected secret: " + injectedSecret + 
                   " (WARNING: Never expose secrets in production APIs)";
        } catch (Exception e) {
            return "Failed to resolve secret from Spring Environment: " + e.getMessage();
        }
    }

    @GetMapping("/debug-injected-certificate")
    public String debugInjectedCertificate() {
        try {
            // Use SecretClient to fetch the certificate and use the injected certificate value
            // SECURITY WARNING: This method is for demonstration purposes only.
            // Exposing certificates in API responses is a security risk.
            // In a real application, you would use the certificate internally
            // and never return it directly to the caller.
            return "[DEMO ONLY] Injected certificate value: " + secretClient.getSecret("test-certificate").getValue() + 
                   " and injected certificate: " + injectedCertificate +
                   " (WARNING: Never expose certificates in production APIs)";
        } catch (Exception e) {
            return "Failed to fetch injected certificate: " + e.getMessage();
        }
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
