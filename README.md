# Azure Key Vault Integration with Spring Boot

This project demonstrates a secure integration between Spring Boot applications and Azure Key Vault for secret management. It shows multiple approaches to access secrets and certificates stored in Azure Key Vault.

## Features

- **Secret Management**: Access secrets securely from Azure Key Vault
- **Certificate Handling**: Retrieve and use certificates for secure communications
- **Multiple Access Patterns**:
  - Direct access via Azure SDK's `SecretClient`
  - Property injection through Spring's configuration system
- **Secure Communication**: Configures SSL/TLS with certificates from Key Vault
- **Mutual TLS (mTLS) Support**: Uses Azure Key Vault JCA Provider to retrieve client certificates for mutual TLS authentication
- **API Endpoint Examples**: Demonstrates how to use the retrieved secrets in an API

## Architecture

The application:
1. Connects to Azure Key Vault during startup
2. Loads secrets and certificates using client credentials
3. Injects values into application properties
4. Configures JCA provider to retrieve client certificates for mTLS
5. Provides a REST API to demonstrate functionality
6. Uses Azure API Management as a backend service to validate the mTLS setup

## Implementation Details

This demo application showcases:

- **Mutual TLS with Azure API Management**: The application uses client certificates from Key Vault to authenticate with Azure API Management, demonstrating a complete end-to-end mTLS implementation.
  
- **API Tracing**: The API_* environment variables (API_SUBSCRIPTION_KEY, API_DEBUG_TOKEN, API_ENDPOINT_URL) enable the request tracing feature in API Management, allowing you to observe certificate validation in action.

- **Certificate Verification**: When calling the `/call-secure-endpoint` API, the application performs a full mTLS handshake with API Management, which validates the client certificate before processing the request.

You can substitute any API that supports mTLS, but Azure API Management provides excellent visibility into the certificate validation process through its tracing capabilities.

## Security Note

**⚠️ DEMONSTRATION PURPOSES ONLY ⚠️**

This application intentionally contains endpoints that expose secrets and certificates to demonstrate the integration patterns. In a production environment, never expose secrets through API responses.

## Prerequisites

- Java 17 or later
- Maven 3.8+
- Azure subscription with Key Vault instance
- Required environment variables:
  - `AZURE_CLIENT_ID`
  - `AZURE_CLIENT_SECRET`
  - `AZURE_TENANT_ID`
  - `AZURE_KEYVAULT_URL`
  - `KEYVAULT_NAME`
  - `API_ENDPOINT_URL`
  - `API_DEBUG_TOKEN`
  - `API_SUBSCRIPTION_KEY`

## Running Locally

1. Clone the repository
2. Set environment variables
3. Build the application: `mvn clean package`
4. Run: `java -jar target/keyvault-demo-0.0.1-SNAPSHOT.jar`

## Deployment

### Current Deployment Method

The application currently deploys to Azure Container Apps using the `deploy.sh` script, which:
- Builds the application into a container
- Pushes to Azure Container Registry
- Deploys to Azure Container Apps with appropriate environment variables

### Future Deployment Pipeline

We're working on implementing a GitHub Actions deployment pipeline using Bicep for infrastructure as code, which will:
- Automate the build and deployment process
- Provision and configure all necessary Azure resources
- Set up proper service connections and managed identities
- Enable continuous deployment from the main branch

## API Endpoints

- `GET /debug-injected-secret`: Demonstrates both direct and injected secret access
- `GET /debug-injected-certificate`: Shows certificate retrieval
- `GET /call-secure-endpoint`: Uses mTLS with client certificates from Key Vault to call another service that requires mutual authentication

## Code Structure

- `KeyvaultDemoApplication.java`: Main application with Azure Key Vault setup
- `CertProxyController.java`: REST controller with example endpoints
- `application.yaml`: Configuration for Spring Boot, Azure Key Vault integration, and JCA provider setup for mTLS

## Technical Highlights

- Uses Azure Key Vault JCA Provider to integrate with Java's built-in cryptography architecture
- Configures Spring's SSL bundle to use Key Vault certificates
- Automatically rotates and refreshes certificates for mTLS authentication
- Supports client-side TLS certificate authentication for service-to-service calls

## Learning Resources

- [Azure Key Vault Documentation](https://docs.microsoft.com/en-us/azure/key-vault/)
- [Spring Boot Azure Integration](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/)
- [Azure Container Apps Documentation](https://docs.microsoft.com/en-us/azure/container-apps/)
- [Azure Key Vault JCA Provider](https://docs.microsoft.com/en-us/java/api/overview/azure/security-keyvault-jca-readme)
- [Mutual TLS Authentication in Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.ssl)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
