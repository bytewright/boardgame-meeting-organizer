# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create directories for persistent data[cite: 1]
RUN mkdir logs data

# Copy the jar from build stage[cite: 1]
# Uses wildcards to handle the version: 0.0.1-SNAPSHOT[cite: 2]
COPY --from=build /app/target/boardgame-meeting-organizer-*.jar app.jar

# Standard Spring Boot port[cite: 1]
EXPOSE 8080

# Run without hardcoded SQLite string to allow Environment Variable overrides[cite: 4]
ENTRYPOINT ["java", "-jar", "app.jar"]