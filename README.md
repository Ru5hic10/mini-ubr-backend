# Mini Uber — Docker setup guide

## Prerequisites

- Docker Desktop installed
- Java 17+ (for local development)
- Maven 3.6+ (for local development)
- Git

## Project structure

```
mini-uber/
├── docker-compose.yml
└── backend/
    ├── eureka-server/
    ├── api-gateway/
    ├── core-service/
    ├── trip-service/
    └── payment-service/
```

> This repository contains the `backend` services. The top-level `docker-compose.yml` orchestrates all services.

---

## Step-by-step setup

1. Create project directory

```bash
mkdir mini-uber
cd mini-uber
```

2. Add backend services

For each service (for example `eureka-server`, `api-gateway`, `core-service`, `trip-service`, `payment-service`):

- Generate a Spring Boot project via https://start.spring.io/ and extract to `backend/{service-name}`.
- Add a `Dockerfile` (example below).
- Provide an `application.yml` that references environment variables for secrets.

Example `Dockerfile` (build + runtime multi-stage):

```dockerfile
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
```

3. Start all services

```bash
# From project root
docker-compose up -d

# View logs
docker-compose logs -f

# View a specific service logs
docker-compose logs -f api-gateway
```

4. Stop all services

```bash
docker-compose down

# Remove volumes (clean databases)
docker-compose down -v
```

5. Rebuild services after code changes

```bash
# Rebuild a single service
docker-compose build payment-service
docker-compose up -d payment-service

# Rebuild all
docker-compose build
docker-compose up -d
```

---

## Common service ports (defaults)

| Service | Port | URL |
|---|---:|---|
| Eureka Server | 8761 | http://localhost:8761 |
| API Gateway | 8080 | http://localhost:8080 |
| Core Service | 8081 | http://localhost:8081 |
| Trip Service | 8082 | http://localhost:8082 |
| Payment Service | 8083 | http://localhost:8083 |
| PostgreSQL (user) | 5432 | jdbc:postgresql://localhost:5432/user_db |
| Redis | 6379 | redis://localhost:6379 |
| RabbitMQ | 5672 | amqp://localhost:5672 |

Adjust ports in `docker-compose.yml` and each service `application.yml` as needed.

---

## Testing examples

1. Check service registration in Eureka: `http://localhost:8761`

2. Register a user (example):

```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "phone": "+1234567890"
  }'
```

3. Login (example):

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

---

## Troubleshooting

- Check logs: `docker-compose logs {service-name}`
- Check containers: `docker ps -a`
- Restart service: `docker-compose restart {service-name}`
- Database: `docker exec -it {db-container} psql -U postgres -d {db_name}`

If services do not register in Eureka, wait 30–60 seconds and verify `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` values.

---

## Development workflow

- Make code changes in your IDE.
- Rebuild the service you changed: `docker-compose build {service-name}`.
- Restart that service: `docker-compose up -d {service-name}`.
- Check logs: `docker-compose logs -f {service-name}`.

---

If you want, I can also add placeholder environment files for each service (example `.env` templates) so you can set secrets via environment variables instead of committing them into source files.

