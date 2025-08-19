# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create user
RUN addgroup -g 1000 angel && \
    adduser -D -u 1000 -G angel angel

# Install health check tools
RUN apk add --no-cache curl

# Copy JAR
COPY --from=builder --chown=angel:angel /app/target/*.jar app.jar

# Create directories
RUN mkdir -p /data /logs /cache && \
    chown -R angel:angel /data /logs /cache

USER angel

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
