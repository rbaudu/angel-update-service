# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Test
```bash
# Clean build (skipping tests for faster builds)
mvn clean package -DskipTests

# Run all tests
mvn test

# Run integration tests
mvn verify

# Start application locally (development mode)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Development
```bash
# Start full local environment (PostgreSQL + Redis + App)
docker-compose -f docker/docker-compose.local.yml up

# Build and run development container only
docker build -f docker/Dockerfile.dev -t angel-update-service:dev .
```

### Kubernetes Development
```bash
# Build and deploy to Minikube
./scripts/build-and-deploy.sh

# Port forward to access locally
./scripts/port-forward.sh
```

## Architecture Overview

This is a Spring Boot microservice for managing content updates for the Angel Virtual Assistant. The service provides differential ZIP packages for content updates.

### Core Components

**Controllers** (`src/main/java/com/angel/update/controller/`):
- `UpdateController`: Main API endpoint for checking updates and downloading packages
- `AdminController`: Administrative interface for managing content and monitoring

**Services** (`src/main/java/com/angel/update/service/`):
- `UpdateService`: Core business logic for update checking and package generation
- `ContentManagerService`: Manages content versioning and change tracking
- `CollectorService`: Orchestrates data collection from external APIs

**Collectors** (`src/main/java/com/angel/update/collector/`):
- `BaseCollector`: Abstract base for all data collectors
- External API collectors for news, weather, recipes, and other content types

### Data Flow

1. **External Data Collection**: Scheduled collectors fetch data from external APIs (news, weather, etc.)
2. **Content Processing**: New content is processed, versioned, and stored in PostgreSQL
3. **Caching Strategy**: Two-level caching with Caffeine (L1) and Redis (L2)
4. **Update Generation**: When updates are requested, differential packages are created as ZIP files
5. **Content Delivery**: Clients receive optimized packages containing only changed content

### Configuration Profiles

- `dev`: Development with debug logging, SQL tracing, and mock data
- `test`: Testing with H2 in-memory database and Testcontainers
- `prod`: Production with optimized settings and external monitoring

### Key Technologies

- **Framework**: Spring Boot 3.2 with Java 17
- **Database**: PostgreSQL with Flyway migrations
- **Caching**: Caffeine (L1) + Redis (L2)
- **Resilience**: Circuit breakers and rate limiting via Resilience4j
- **Monitoring**: Prometheus metrics and structured logging
- **Documentation**: OpenAPI/Swagger UI at `/swagger-ui.html`

### Regional and Content Structure

The service supports multi-regional content with the following structure:
- Country-specific content (e.g., `FR`, `US`, `DE`)
- Region-specific content within countries (e.g., `IDF` for Île-de-France)
- Content types: news, weather, recipes, discoveries, stories
- Language support configured through `Accept-Language` headers

### Testing Strategy

- **Unit Tests**: Service layer logic and utilities
- **Integration Tests**: Controller endpoints with Testcontainers for database
- **Load Testing**: K6 scripts for performance validation
- Test databases use H2 in-memory for fast execution

### Deployment

The service is designed for Kubernetes deployment with:
- Horizontal pod autoscaling based on CPU/memory
- Health checks via Spring Actuator endpoints
- ConfigMaps for environment-specific configuration
- Secrets for sensitive data (database passwords, API keys)

### Important File Locations

- Main application: `src/main/java/com/angel/update/AngelUpdateServiceApplication.java`
- API endpoints: `src/main/java/com/angel/update/controller/UpdateController.java:29` (check updates), `src/main/java/com/angel/update/controller/UpdateController.java:48` (download)
- Admin interface: `src/main/resources/static/index.html` - Web administration interface
- Core service logic: `src/main/java/com/angel/update/service/UpdateService.java:32` (update checking)
- Data collectors: `src/main/java/com/angel/update/collector/` (NewsCollector, WeatherCollector)
- WebSocket handler: `src/main/java/com/angel/update/websocket/AdminWebSocketHandler.java` (real-time monitoring)
- Configuration: `src/main/resources/application-{profile}.yml`
- Kubernetes manifests: `k8s-dev/`
- Build scripts: `scripts/build-and-deploy.sh`

### Project Status

The project is now **fully functional** with:
- ✅ Complete Spring Boot microservice architecture
- ✅ JPA repositories for content management
- ✅ Multi-level caching (Caffeine + Redis)
- ✅ Scheduled data collectors (News, Weather)
- ✅ ZIP package generation for updates
- ✅ Web administration interface with real-time monitoring
- ✅ WebSocket support for live updates
- ✅ Unit and integration tests
- ✅ Docker and Kubernetes deployment ready

### Quick Start Commands

```bash
# Start the application (requires PostgreSQL and Redis)
mvn spring-boot:run

# Run with Docker Compose (includes all dependencies)
docker-compose -f docker/docker-compose.local.yml up

# Build production package
mvn clean package

# Run tests
mvn test
```

### Web Interface

Access the administration interface at `http://localhost:8080` when the application is running. This provides real-time monitoring of collectors, content management, cache statistics, and system logs.