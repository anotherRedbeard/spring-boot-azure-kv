package com.example.keyvaultdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Hello World - Service is healthy");
    }

    @GetMapping("/liveness")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("Hello World - Service is alive");
    }
}
