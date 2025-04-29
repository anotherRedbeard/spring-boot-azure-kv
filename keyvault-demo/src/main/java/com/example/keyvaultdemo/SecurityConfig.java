package com.example.keyvaultdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public class SecurityConfig {

   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       http
           // Disable CSRF for testing (remove or adjust in production)
           .csrf(csrf -> csrf.disable())
           .authorizeHttpRequests(authz -> authz
               // Permit the secure endpoint explicitly
               .requestMatchers("/call-secure-endpoint").permitAll()
               .anyRequest().authenticated()
           )
           // Enable HTTP Basic and form login as defaults
           .httpBasic(Customizer.withDefaults())
           .formLogin(Customizer.withDefaults());
       return http.build();
   }
}