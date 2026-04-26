# Use a lightweight Java 21 image
FROM eclipse-temurin:21-jdk-alpine

# Set the folder inside the container
WORKDIR /app

# Copy the compiled Java JAR file into the container
COPY target/*.jar app.jar

# Run the API
ENTRYPOINT ["java", "-jar", "app.jar"]