# Use an official OpenJDK runtime as a parent image, specifying the platform
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/keyvault-demo-0.0.1-SNAPSHOT.jar sb-kvdemo.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variable for Azure logging
ENV AZURE_LOG_LEVEL=verbose

# Run the application
ENTRYPOINT ["java", "-jar", "sb-kvdemo.jar"]