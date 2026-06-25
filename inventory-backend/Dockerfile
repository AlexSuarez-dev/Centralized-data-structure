# --- ETAPA 1: Compilación (Build Stage) ---
# Usamos una imagen oficial que ya incluye Maven y Java 17 para compilar tu código
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copiamos los archivos de configuración de Maven y tu código fuente
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto y generamos el .jar dentro de la imagen de Docker
RUN mvn clean package -DskipTests

# --- ETAPA 2: Ejecución (Run Stage) ---
# Usamos una imagen de Java ultra ligera y optimizada para producción
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el .jar que se generó en la Etapa 1 hacia esta nueva imagen limpia
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación de forma ininterrumpida
ENTRYPOINT ["java", "-jar", "app.jar"]