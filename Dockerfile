# Stage 1: Build
FROM ghcr.io/jqlang/jq:latest AS jq-stage

FROM eclipse-temurin:21-jdk-jammy AS build
COPY --from=jq-stage /jq /usr/bin/jq
# Test that jq works after copying
RUN jq --version

WORKDIR /app
# Copy dependency descriptors first — layer is cached until pom.xml changes
COPY .mvn/ .mvn/
# includes vaadin files
COPY mvnw pom.xml lombok.config package.json package-lock.json tsconfig.json types.d.ts ./
RUN ./mvnw dependency:go-offline -q

# Now copy source and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
# Don't run as root
RUN groupadd -r appgroup && \
    useradd -r -g appgroup -s /bin/false appuser
USER appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create directories for persistent data
RUN mkdir logs data

# Standard Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]