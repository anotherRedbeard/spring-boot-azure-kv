package com.example.keyvaultdemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;

@SpringBootApplication
public class KeyvaultDemoApplication {

	@Value("${spring.cloud.azure.keyvault.jca.vaults.red-scus-javafuncdemo-kv.endpoint}")
    private String vaultUrl;

    @Value("${spring.cloud.azure.keyvault.jca.vaults.red-scus-javafuncdemo-kv.credential.client-id}")
    private String clientId;

    @Value("${spring.cloud.azure.keyvault.jca.vaults.red-scus-javafuncdemo-kv.credential.client-secret}")
    private String clientSecret;

    @Value("${spring.cloud.azure.keyvault.jca.vaults.red-scus-javafuncdemo-kv.profile.tenant-id}")
    private String tenantId;

	public static void main(String[] args) {
		SpringApplication.run(KeyvaultDemoApplication.class, args);
	}

	@Bean
    public CommandLineRunner testKeyVaultConnectivity() {
        return args -> {
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

            CertificateClient certificateClient = new CertificateClientBuilder()
                .vaultUrl(vaultUrl)
                .credential(credential)
                .buildClient();

            try {
                var certificate = certificateClient.getCertificate("self-signed-cert");
                System.out.println("Certificate retrieved: " + certificate.getName());
            } catch (Exception e) {
                System.err.println("Failed to retrieve certificate: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

}
