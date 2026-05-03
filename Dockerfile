# Stage 1: Build
FROM ghcr.io/jqlang/jq:latest AS jq-stage
FROM eclipse-temurin:21-jdk-jammy AS build
COPY --from=jq-stage /jq /usr/bin/jq
# Test that jq works after copying
RUN jq --version

ENV HOME=/app
RUN mkdir -p $HOME
WORKDIR $HOME
COPY . $HOME


# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create directories for persistent data
RUN mkdir logs data

RUN ./mvnw clean package -DskipTests

# Copy the jar from build stage
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/target/*.jar app.jar

# Standard Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]