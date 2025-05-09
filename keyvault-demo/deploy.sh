#!/bin/bash

# --- Configuration ---
# Replace placeholders with your actual values or set them as environment variables

# Service Principal / App Registration Credentials (Needed for local execution and potentially initial infra setup)
AZURE_TENANT_ID="<YOUR_TENANT_ID>"
AZURE_CLIENT_ID="<YOUR_CLIENT_ID>"
AZURE_CLIENT_SECRET="<YOUR_CLIENT_SECRET>" # Consider using environment variables or other secure methods

# API Details (replace APIM)
API_SUBSCRIPTION_KEY="<YOUR_API_SUBSCRIPTION_KEY>" # Consider using environment variables
API_ENDPOINT_URL="<YOUR_API_ENDPOINT_URL>" # e.g., https://your-api-instance.azure-api.net/colors/colors
API_DEBUG_TOKEN="<YOUR_API_DEBUG_TOKEN_IF_NEEDED>" # Optional: For API tracing

# Azure Resource Naming and Location
AZURE_SUBSCRIPTION_ID="<YOUR_AZURE_SUBSCRIPTION_ID>"
RESOURCE_GROUP="<YOUR_RESOURCE_GROUP_NAME>" # e.g., my-springboot-app-rg
CONTAINER_APP_NAME="<YOUR_CONTAINER_APP_NAME>" # e.g., my-springboot-kvdemo
LOCATION="<YOUR_AZURE_REGION>" # e.g., centralus, eastus
ACR_NAME="<YOUR_ACR_NAME>" # e.g., myappacr (must be globally unique)
LOG_ANALYTICS_WORKSPACE="<YOUR_LOG_ANALYTICS_WORKSPACE_NAME>" # e.g., myapp-logs
KEYVAULT_NAME="<YOUR_KEYVAULT_NAME>"
KV_RESOURCE_GROUP_NAME="<YOUR_KEYVAULT_RESOURCE_GROUP_NAME>" # Resource group where Key Vault exists

# Docker Image Details
IMAGE_TAG_NAME="sb-kvdemo" # Or your preferred image name
PLACEHOLDER_IMAGE="mcr.microsoft.com/k8se/quickstart:latest" # Used for initial ACA creation

# --- Script Logic ---

# Generate a unique tag using the current timestamp for image versioning
IMAGE_TAG=$(date +%Y%m%d%H%M%S)

# Function to display usage
usage() {
    echo "Usage: deploy.sh all|infra|app|allow-public|set-local-env|run-local-container"
    echo "Ensure placeholders in the script are replaced or corresponding environment variables are set."
}

# Function to deploy infrastructure
deploy_infra() {
    echo "--- Deploying Infrastructure ---"

    # Set Azure Subscription Context (Optional but recommended)
    # echo "Setting Azure subscription to $AZURE_SUBSCRIPTION_ID..."
    # az account set --subscription $AZURE_SUBSCRIPTION_ID

    # Create Resource Group
    echo "Creating Resource Group '$RESOURCE_GROUP' in '$LOCATION'..."
    az group create --name $RESOURCE_GROUP --location $LOCATION

    # Create Log Analytics Workspace
    echo "Creating Log Analytics Workspace '$LOG_ANALYTICS_WORKSPACE'..."
    az monitor log-analytics workspace create \
        --resource-group $RESOURCE_GROUP \
        --workspace-name $LOG_ANALYTICS_WORKSPACE \
        --location $LOCATION

    # Get Log Analytics Workspace ID
    LOG_ANALYTICS_WORKSPACE_ID=$(az monitor log-analytics workspace show \
        --resource-group $RESOURCE_GROUP \
        --workspace-name $LOG_ANALYTICS_WORKSPACE \
        --query customerId -o tsv)

    # Create Azure Container Registry
    echo "Creating Azure Container Registry '$ACR_NAME'..."
    az acr create --resource-group $RESOURCE_GROUP --location $LOCATION --name $ACR_NAME --sku Basic --admin-enabled false # Basic SKU, admin user disabled

    # Enable diagnostics for ACR (Optional)
    echo "Enabling diagnostics for Azure Container Registry..."
    ACR_RESOURCE_ID=$(az acr show --resource-group $RESOURCE_GROUP --name $ACR_NAME --query id -o tsv)
    az monitor diagnostic-settings create \
        --resource $ACR_RESOURCE_ID \
        --resource-group $RESOURCE_GROUP \
        --workspace $LOG_ANALYTICS_WORKSPACE \
        --name "ACRDiagnostics" \
        --logs '[{"category": "ContainerRegistryLoginEvents", "enabled": true}, {"category": "ContainerRegistryRepositoryEvents", "enabled": true}]' \
        >/dev/null # Suppress verbose output

    # Build and Push Initial Image (using placeholder or actual app)
    echo "Building and pushing initial/placeholder image..."
    package_app # Assumes Dockerfile exists and builds

    # Create Azure Container App Environment
    echo "Creating Azure Container App Environment '$CONTAINER_APP_NAME-env'..."
    az containerapp env create \
        --name $CONTAINER_APP_NAME-env \
        --resource-group $RESOURCE_GROUP \
        --location $LOCATION \
        --logs-workspace-id $LOG_ANALYTICS_WORKSPACE_ID \
        --enable-workload-profiles false # Use Consumption-only profile

    # Create Container App with System-Assigned Managed Identity
    echo "Creating Azure Container App '$CONTAINER_APP_NAME' with system-assigned managed identity..."
    az containerapp create \
        --name $CONTAINER_APP_NAME \
        --resource-group $RESOURCE_GROUP \
        --environment $CONTAINER_APP_NAME-env \
        --ingress external \
        --target-port 8080 \
        --image "$PLACEHOLDER_IMAGE" \ # Use the pushed image
        --registry-server "$ACR_NAME.azurecr.io" \
        --system-assigned

    # Get the Managed Identity Principal ID
    echo "Retrieving Managed Identity Principal ID..."
    MANAGED_IDENTITY_PRINCIPAL_ID=$(az containerapp show \
        --name $CONTAINER_APP_NAME \
        --resource-group $RESOURCE_GROUP \
        --query identity.principalId -o tsv)

    if [ -z "$MANAGED_IDENTITY_PRINCIPAL_ID" ]; then
        echo "Error: Could not retrieve Managed Identity Principal ID for Container App."
        exit 1
    fi
    echo "Managed Identity Principal ID: $MANAGED_IDENTITY_PRINCIPAL_ID"

    # Assign ACR Pull Role to Managed Identity (Run in background)
    echo "Assigning 'AcrPull' role to Managed Identity for ACR '$ACR_NAME'..."
    {
        az role assignment create --assignee $MANAGED_IDENTITY_PRINCIPAL_ID --role "AcrPull" --scope $ACR_RESOURCE_ID
        echo "ACR Pull role assignment initiated."
    } &

    # Assign Key Vault Roles to Managed Identity (Run in background)
    echo "Assigning Key Vault roles to Managed Identity for Key Vault '$KEYVAULT_NAME'..."
    KEYVAULT_ID=$(az keyvault show --name $KEYVAULT_NAME --resource-group $KV_RESOURCE_GROUP_NAME --query id -o tsv 2>/dev/null)
    if [ -z "$KEYVAULT_ID" ]; then
        echo "Warning: Could not find Key Vault '$KEYVAULT_NAME' in resource group '$KV_RESOURCE_GROUP_NAME'. Skipping role assignments."
    else
        {
            az role assignment create --assignee $MANAGED_IDENTITY_PRINCIPAL_ID --role "Key Vault Secrets User" --scope $KEYVAULT_ID
            echo "Key Vault Secrets User role assignment initiated."
        } &
        {
            az role assignment create --assignee $MANAGED_IDENTITY_PRINCIPAL_ID --role "Key Vault Certificate User" --scope $KEYVAULT_ID
            echo "Key Vault Certificate User role assignment initiated."
        } &
    fi

    # Wait for all background role assignment processes to complete
    echo "Waiting for role assignments to complete..."
    wait
    echo "Role assignments completed."

    echo "--- Infrastructure Deployment Finished ---"
    # Note: deploy_app is not called here automatically, run separately if needed after infra.
}

# Function to build and push app image to ACR
package_app() {
    echo "--- Packaging Application ---"
    # Ensure you are logged into Azure CLI

    # Login to ACR using Azure CLI credentials
    echo "Logging in to Azure Container Registry '$ACR_NAME'..."
    az acr login --name $ACR_NAME --resource-group $RESOURCE_GROUP

    # Build Docker image (ensure Dockerfile exists and mvn build is done if needed)
    echo "Building Docker image '$IMAGE_TAG_NAME'..."
    # Assuming Dockerfile uses amd64 platform if building on ARM
    docker build --platform linux/amd64 -t $IMAGE_TAG_NAME .

    # Tag Docker image for ACR
    echo "Tagging Docker image as '$ACR_NAME.azurecr.io/$IMAGE_TAG_NAME:$IMAGE_TAG'..."
    docker tag $IMAGE_TAG_NAME "$ACR_NAME.azurecr.io/$IMAGE_TAG_NAME:$IMAGE_TAG"

    # Push Docker image to ACR
    echo "Pushing Docker image to Azure Container Registry..."
    docker push "$ACR_NAME.azurecr.io/$IMAGE_TAG_NAME:$IMAGE_TAG"
    echo "--- Application Packaging Finished ---"
}

# Function to update the Container App with the latest image and environment variables
deploy_app() {
    echo "--- Deploying Application Update ---"
    # Ensure the image has been pushed via package_app first

    # NOTE: The Spring Cloud Azure Starter Key Vault JCA currently only supports client credentials so we can't use
    # managed identity yet, which is why they are passed as env vars.
    echo "Updating Azure Container App '$CONTAINER_APP_NAME'..."
    az containerapp update \
        --name $CONTAINER_APP_NAME \
        --resource-group $RESOURCE_GROUP \
        --image "$ACR_NAME.azurecr.io/$IMAGE_TAG_NAME:$IMAGE_TAG" \
        --set-env-vars AZURE_KEYVAULT_URL=https://$KEYVAULT_NAME.vault.azure.net/ \
                       AZURE_TENANT_ID=$AZURE_TENANT_ID \
                       AZURE_CLIENT_ID=$AZURE_CLIENT_ID \
                       AZURE_CLIENT_SECRET=$AZURE_CLIENT_SECRET \
                       API_SUBSCRIPTION_KEY=$API_SUBSCRIPTION_KEY \
                       API_ENDPOINT_URL=$API_ENDPOINT_URL \
                       API_DEBUG_TOKEN="$API_DEBUG_TOKEN"

    # Optional: Show Container App FQDN
    APP_FQDN=$(az containerapp show --name $CONTAINER_APP_NAME --resource-group $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv)
    echo "Application deployed. Access at: https://$APP_FQDN"
    echo "--- Application Deployment Finished ---"
}

# Function to configure Key Vault network rules (Example: Allow all)
allow_public() {
    echo "--- Configuring Key Vault Network Access ---"
    echo "WARNING: Setting Key Vault '$KEYVAULT_NAME' to allow public network access."
    az keyvault update --name $KEYVAULT_NAME --resource-group $KV_RESOURCE_GROUP_NAME --public-network-access Enabled
    echo "--- Key Vault Network Access Configured ---"
}

# Function to export environment variables for local execution
set_local_env() {
    echo "--- Setting Local Environment Variables ---"
    echo "Exporting variables for local testing (using Service Principal credentials)..."
    export AZURE_KEYVAULT_URL="https://$KEYVAULT_NAME.vault.azure.net/"
    export AZURE_TENANT_ID="$AZURE_TENANT_ID"
    export AZURE_CLIENT_ID="$AZURE_CLIENT_ID"
    export AZURE_CLIENT_SECRET="$AZURE_CLIENT_SECRET" # Ensure this is set securely if used
    export API_SUBSCRIPTION_KEY="$API_SUBSCRIPTION_KEY"
    export API_ENDPOINT_URL="$API_ENDPOINT_URL"
    export API_DEBUG_TOKEN="$API_DEBUG_TOKEN"
    export KEYVAULT_NAME="$KEYVAULT_NAME"
    echo "Run 'source ./deploy.sh set-local-env' to apply these in your current shell."
    echo "--- Local Environment Variables Set ---"
}

# Function to build and run the application locally in a Docker container
run_local_container() {
    echo "--- Running Local Container ---"
    # Ensure local environment variables are set (e.g., by sourcing set_local_env)

    # Build the Java application JAR
    echo "Building Java app (mvn clean package)..."
    if [ -f "./mvnw" ]; then
        ./mvnw clean package -DskipTests
    elif command -v mvn &> /dev/null; then
        mvn clean package -DskipTests
    else
        echo "Error: Maven wrapper (mvnw) or mvn command not found. Cannot build JAR."
        exit 1
    fi

    # Build Docker image locally
    echo "Building Docker image '$IMAGE_TAG_NAME:latest'..."
    docker build -t $IMAGE_TAG_NAME:latest .

    # Run Docker container with local environment variables
    echo "Running Docker container '$IMAGE_TAG_NAME:latest'..."
    docker run --rm -p 8080:8080 \
            -e AZURE_KEYVAULT_URL="https://$KEYVAULT_NAME.vault.azure.net/" \
            -e AZURE_TENANT_ID="$AZURE_TENANT_ID" \
            -e AZURE_CLIENT_ID="$AZURE_CLIENT_ID" \
            -e AZURE_CLIENT_SECRET="$AZURE_CLIENT_SECRET" \
            -e API_SUBSCRIPTION_KEY="$API_SUBSCRIPTION_KEY" \
            -e API_ENDPOINT_URL="$API_ENDPOINT_URL" \
            -e API_DEBUG_TOKEN="$API_DEBUG_TOKEN" \
            $IMAGE_TAG_NAME:latest
    echo "--- Local Container Execution Finished ---"
}

# --- Main Script Execution ---

if [ -z "$1" ]; then
    usage
    exit 1
fi

case "$1" in
    all)
        echo "Deploying Infrastructure AND Application..."
        deploy_infra
        # deploy_app is called within deploy_infra in this version, review if separate step needed
        echo "Deployment 'all' finished."
        ;;
    infra)
        deploy_infra
        ;;
    app)
        package_app
        deploy_app
        ;;
    allow-public)
        allow_public
        ;;
    set-local-env)
        set_local_env
        ;;
    run-local-container)
        run_local_container
        ;;
    *)
        usage
        ;;
esac

echo "Script finished."
