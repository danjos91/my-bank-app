# MyBank App - Microservices Banking Application

A comprehensive microservices-based banking application built with Spring Boot, featuring user management, account operations, money transfers, and real-time notifications.

## 🏗️ Architecture

This application follows a microservices architecture pattern with the following components:

### Core Services
- **Gateway Service** (Port 8080) - API Gateway with routing and load balancing
- **Accounts Service** (Port 8081) - User and account management
- **Cash Service** (Port 8082) - Deposit and withdrawal operations
- **Transfer Service** (Port 8083) - Money transfers between accounts
- **Notifications Service** (Port 8084) - Real-time notifications
- **Auth Server** (Port 8085) - OAuth2 authentication and authorization
- **Front UI** (Port 8086) - Web-based user interface

### Infrastructure Services
- **Eureka Server** (Port 8761) - Service discovery and registration
- **Config Server** (Port 8888) - Centralized configuration management
- **PostgreSQL** (Port 5432) - Primary database
- **Redis** (Port 6379) - Caching and session storage

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** and **Docker Compose**
- **Git**

### Option 1: Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd my-bank-app
   ```

2. **Start all services**
   ```bash
   make up
   # or
   docker-compose up -d
   ```

3. **Check service health**
   ```bash
   make health
   ```

4. **Access the application**
   - Frontend: http://localhost:8086
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - Config Server: http://localhost:8888

### Option 2: Local Development

1. **Start infrastructure services**
   ```bash
   make dev-up
   ```

2. **Build and run services locally**
   ```bash
   # Build all services
   mvn clean install -DskipTests
   
   # Run individual services (in separate terminals)
   cd accounts-service && mvn spring-boot:run
   cd cash-service && mvn spring-boot:run
   cd transfer-service && mvn spring-boot:run
   cd notifications-service && mvn spring-boot:run
   cd auth-server && mvn spring-boot:run
   cd gateway && mvn spring-boot:run
   cd front-ui && mvn spring-boot:run
   ```

## 🧪 Testing

### Run All Tests
```bash
make test
# or
mvn clean test
```

### Run Specific Test Types
```bash
# Unit tests only
make test-unit

# Integration tests only
make test-integration

# Contract tests only
make test-contracts
```

### Test Coverage
The application includes comprehensive test coverage:
- **Unit Tests**: JUnit 5 + Mockito for all service layers
- **Integration Tests**: Testcontainers for database integration
- **Contract Tests**: Spring Cloud Contract for API compatibility
- **End-to-End Tests**: Full application testing with Docker Compose

## 🐳 Docker Commands

### Production Commands
```bash
# Build all images
make build

# Start all services
make up

# Stop all services
make down

# View logs
make logs

# Clean up resources
make clean
```

### Development Commands
```bash
# Build development images
make dev-build

# Start development infrastructure
make dev-up

# Stop development services
make dev-down
```

### Service-Specific Commands
```bash
# Build specific service
make build-service SERVICE=accounts-service

# Start specific service
make up-service SERVICE=accounts-service

# View logs for specific service
make logs-service SERVICE=accounts-service
```

## 📊 API Documentation

### Authentication
All API endpoints require authentication via OAuth2. Obtain a token from the auth server:

```bash
curl -X POST http://localhost:8085/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user&password=password&client_id=mybank&client_secret=secret"
```

### Core Endpoints

#### Accounts Service
- `GET /api/accounts/users` - Get all users
- `POST /api/accounts/users/register` - Register new user
- `GET /api/accounts/users/{id}` - Get user by ID
- `GET /api/accounts/users/{id}/accounts` - Get user's accounts
- `POST /api/accounts/users/{id}/accounts` - Create new account
- `GET /api/accounts/{id}/balance` - Get account balance
- `PUT /api/accounts/{id}/balance` - Update account balance

#### Cash Service
- `POST /api/cash/deposit` - Make a deposit
- `POST /api/cash/withdraw` - Make a withdrawal
- `GET /api/cash/account/{id}/transactions` - Get transaction history

#### Transfer Service
- `POST /api/transfers` - Create money transfer
- `GET /api/transfers/{id}` - Get transfer details
- `GET /api/transfers/account/{id}` - Get transfers for account

#### Notifications Service
- `POST /api/notifications` - Create notification
- `GET /api/notifications/user/{id}` - Get user notifications
- `PUT /api/notifications/{id}/read` - Mark notification as read

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `docker` |
| `POSTGRES_DB` | Database name | `mybank` |
| `POSTGRES_USER` | Database user | `mybank` |
| `POSTGRES_PASSWORD` | Database password | `mybank123` |
| `REDIS_URL` | Redis connection URL | `redis://localhost:6379` |

### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Gateway | 8080 | API Gateway |
| Accounts | 8081 | User & Account Management |
| Cash | 8082 | Cash Operations |
| Transfer | 8083 | Money Transfers |
| Notifications | 8084 | Notifications |
| Auth Server | 8085 | Authentication |
| Front UI | 8086 | Web Interface |
| Eureka | 8761 | Service Discovery |
| Config | 8888 | Configuration Server |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |

## 🏗️ Development

### Project Structure
```
my-bank-app/
├── accounts-service/          # User and account management
├── auth-server/              # OAuth2 authentication
├── cash-service/             # Cash operations
├── config-server/            # Configuration management
├── eureka-server/            # Service discovery
├── front-ui/                 # Web interface
├── gateway/                  # API Gateway
├── notifications-service/    # Notifications
├── transfer-service/         # Money transfers
├── database/                 # Database schemas
├── docker-compose.yml        # Production Docker setup
├── docker-compose.dev.yml    # Development Docker setup
└── Makefile                  # Build automation
```

### Adding New Services

1. **Create service module**
   ```bash
   mkdir new-service
   # Add pom.xml, src/, Dockerfile
   ```

2. **Update parent POM**
   ```xml
   <modules>
       <module>new-service</module>
   </modules>
   ```

3. **Add to Docker Compose**
   ```yaml
   new-service:
     build:
       context: .
       dockerfile: new-service/Dockerfile
     ports:
       - "8087:8087"
   ```

4. **Update Gateway routes**
   ```yaml
   spring:
     cloud:
       gateway:
         routes:
           - id: new-service
             uri: lb://new-service
             predicates:
               - Path=/api/new/**
   ```

### Code Quality

The project follows these quality standards:
- **Lombok** for reducing boilerplate code
- **Spring Boot Actuator** for monitoring and health checks
- **Spring Security** for authentication and authorization
- **JPA/Hibernate** for data persistence
- **Testcontainers** for integration testing
- **Spring Cloud Contract** for API testing

## 🚀 Deployment

### Production Deployment

1. **Build production images**
   ```bash
   make build
   ```

2. **Deploy with Docker Compose**
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

3. **Verify deployment**
   ```bash
   make health
   ```

### Kubernetes Deployment

For Kubernetes deployment, use the provided Helm charts or convert Docker Compose to Kubernetes manifests.

### Environment-Specific Configuration

- **Development**: Use `docker-compose.dev.yml`
- **Production**: Use `docker-compose.yml`
- **Testing**: Use Testcontainers for isolated testing

## 📈 Monitoring

### Health Checks
All services expose health endpoints at `/actuator/health`:
- http://localhost:8080/actuator/health (Gateway)
- http://localhost:8081/actuator/health (Accounts)
- http://localhost:8082/actuator/health (Cash)
- http://localhost:8083/actuator/health (Transfer)
- http://localhost:8084/actuator/health (Notifications)
- http://localhost:8085/actuator/health (Auth)
- http://localhost:8086/actuator/health (Front UI)

### Metrics
Access metrics at `/actuator/metrics` for each service.

### Logs
View logs using Docker Compose:
```bash
docker-compose logs -f [service-name]
```

## 🔒 Security

- **OAuth2** for authentication
- **JWT tokens** for stateless authentication
- **HTTPS** support (configure in production)
- **Input validation** using Bean Validation
- **SQL injection** prevention via JPA
- **CORS** configuration for cross-origin requests

## 🐛 Troubleshooting

### Common Issues

1. **Services not starting**
   ```bash
   # Check logs
   docker-compose logs [service-name]
   
   # Check health
   make health
   ```

2. **Database connection issues**
   ```bash
   # Check PostgreSQL status
   docker-compose logs postgres
   
   # Verify database is accessible
   docker exec -it mybank-postgres psql -U mybank -d mybank
   ```

3. **Service discovery issues**
   ```bash
   # Check Eureka dashboard
   open http://localhost:8761
   
   # Verify service registration
   curl http://localhost:8761/eureka/apps
   ```

### Performance Tuning

- **JVM Settings**: Adjust heap size in Dockerfiles
- **Database**: Configure connection pooling
- **Caching**: Optimize Redis configuration
- **Load Balancing**: Configure gateway routing

## 📝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the API documentation

---

**Happy Banking! 🏦**