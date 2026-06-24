# Use an official lightweight Java image (adjust version 17 to the one you use)
FROM eclipse-temurin:17-jre-alpine

# Create a working directory inside the container
WORKDIR /app

# Copy the compiled .jar file from your target folder into the container
# NOTE: Change "tu-proyecto.jar" to the actual name of your file in the target/ folder
COPY target/*.jar app.jar

# Expose the internal port (Spring Boot uses 8080 by default)
EXPOSE 8080

# Command that will run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]