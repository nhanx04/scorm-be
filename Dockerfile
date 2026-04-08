# syntax=docker/dockerfile:1

# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build jar
RUN mvn -U clean package -DskipTests

# ===== Run stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/target/scorm-package-generator-1.0.0.jar app.jar

# App listens on 8080 by default (can still be overridden by SERVER_PORT env)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
