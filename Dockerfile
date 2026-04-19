# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create directories for persistent data
RUN mkdir logs data

# Copy the jar from build stage
COPY --from=build /app/target/boardgame-meeting-organizer-*.jar app.jar

# Standard Spring Boot port
EXPOSE 8080

# Run with SQLite location and Log path passed as properties
ENTRYPOINT ["java", "-jar", "app.jar", \
            "--spring.datasource.url=jdbc:sqlite:/app/data/bgmo.db"]