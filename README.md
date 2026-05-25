Mini Uber - Docker Setup Guide
Prerequisites

Docker Desktop installed
Java 17+ (for local development)
Maven 3.6+ (for local development)
Git

Project Structure
mini-uber/
├── docker-compose.yml
└── backend/
    ├── eureka-server/
    ├── api-gateway/
    ├── user-service/
    ├── driver-service/
    ├── ride-service/
    ├── location-service/
    └── payment-service/
Step-by-Step Setup
1. Create Project Directory
bashmkdir mini-uber
cd mini-uber
2. Create Backend Services
For each service (eureka-server, api-gateway, user-service, driver-service):

Create via Spring Initializr:

Go to https://start.spring.io/
Configure as per the guide
Download and extract to backend/{service-name}/


Add Dockerfile (same for all services):

dockerfileFROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE {PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]

Add application.yml (as provided in each service)
Add Java source files (entities, repositories, services, controllers)

3. Start All Services
bash# From project root directory
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service
4. Stop All Services
bashdocker-compose down

# Stop and remove volumes (clean database)
docker-compose down -v
5. Rebuild Services After Code Changes
bash# Rebuild specific service
docker-compose build user-service
docker-compose up -d user-service

# Rebuild all services
docker-compose build
docker-compose up -d
Service Ports
ServicePortURLEureka Server8761http://localhost:8761API Gateway8080http://localhost:8080User Service8081http://localhost:8081Driver Service8082http://localhost:8082Ride Service8083http://localhost:8083Location Service8084http://localhost:8084Payment Service8085http://localhost:8085PostgreSQL (User)5432jdbc:postgresql://localhost:5432/user_dbPostgreSQL (Driver)5433jdbc:postgresql://localhost:5433/driver_dbPostgreSQL (Ride)5434jdbc:postgresql://localhost:5434/ride_dbPostgreSQL (Payment)5435jdbc:postgresql://localhost:5435/payment_dbRedis6379redis://localhost:6379RabbitMQ5672amqp://localhost:5672RabbitMQ Management15672http://localhost:15672
Testing the Services
1. Check Service Registration
Open Eureka Dashboard: http://localhost:8761
2. Register a User
bashcurl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "phone": "+1234567890"
  }'
3. Register a Driver
bashcurl -X POST http://localhost:8080/api/drivers/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Driver",
    "email": "jane@example.com",
    "password": "password123",
    "phone": "+1234567891",
    "licenseNumber": "DL123456",
    "vehicleType": "Sedan",
    "vehicleNumber": "ABC-1234",
    "vehicleModel": "Toyota Camry"
  }'
4. Login User
bashcurl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
Troubleshooting
Services Not Starting
bash# Check logs
docker-compose logs {service-name}

# Check container status
docker ps -a

# Restart specific service
docker-compose restart {service-name}
Database Connection Issues
bash# Check if PostgreSQL is running
docker-compose ps postgres-user

# Connect to database
docker exec -it postgres-user psql -U postgres -d user_db
Eureka Registration Issues

Wait 30-60 seconds after starting services
Check EUREKA_CLIENT_SERVICEURL_DEFAULTZONE is correct
Verify network connectivity: docker network inspect mini-uber_mini-uber-network

Development Workflow

Make code changes in your IDE
Rebuild service: docker-compose build {service-name}
Restart service: docker-compose up -d {service-name}
Check logs: docker-compose logs -f {service-name}
