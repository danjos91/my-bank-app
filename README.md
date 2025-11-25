# MyBank App - Microservices Banking Application

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring WebFlux](https://img.shields.io/badge/Spring_WebFlux-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth2-4285F4?style=for-the-badge&logo=oauth&logoColor=white)
![Eureka](https://img.shields.io/badge/Eureka-4285F4?style=for-the-badge&logo=eureka&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)

A comprehensive microservices-based banking application built with Spring Boot, featuring user management, multi-currency account operations, money transfers with currency conversion, real-time notifications, and exchange rate management.



## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.9+**
- **Docker** and **Docker Compose**
- **Git**

### Option 1: Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/danjos91/my-bank-app.git
   cd my-bank-app
   ```

2. **Start all services**
   ```bash
   docker compose up -d
   ```

4. **Access the application**
   - Frontend: http://localhost:8086
   - API Gateway: http://localhost:8080
   - Eureka Dashboard: http://localhost:8761
   - Config Server: http://localhost:8888

### Option 2: Local Development

1. **Start infrastructure services**
      docker compose -f docker-compose.dev.yml up -d

2. **Build and run services locally**
   ```bash

   
   # Run individual services (in separate terminals)
   cd auth-server; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd accounts-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd notifications-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd cash-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd transfer-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd exchange-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd exchange-generator-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd blocker-service; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd gateway; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   cd front-ui; mvn spring-boot:run -D spring-boot.run.arguments=--spring.profiles.active=local
   ```

## 🧪 Testing

### Run All Tests
```bash

mvn clean test
```


## 🐳 Docker Commands

```bash
# Build all images
docker compose build

# Start all services
docker compose up -d

# Stop all services
docker compose down

# View logs
docker compose logs -f

# Clean up resources
docker compose down -v --remove-orphans
```

## 👤 Test Users

The following test users are available for testing the application functionality:

| Username | Password | Initial Balance | Full Name | Email |
|----------|----------|-----------------|-----------|-------|
| `admin` | `password123` | $10,000.00 | Admin User | admin@bank.com |
| `john` | `password123` | $5,000.00 | John Doe | john.doe@example.com |
| `jane` | `password123` | $7,500.00 | Jane Smith | jane.smith@example.com |
| `bob` | `password123` | $3,000.00 | Bob Wilson | bob.wilson@example.com |

> **Note:** All test users share the same password: `password123`. These users are automatically created when you initialize the database.

## 🏗️ Architecture

This application follows a microservices architecture pattern with the following components:

### Core Services
- **Gateway Service** (Port 8080) - API Gateway with routing and load balancing
- **Accounts Service** (Port 8081) - User and multi-currency account management
- **Cash Service** (Port 8082) - Deposit and withdrawal operations with currency support
- **Transfer Service** (Port 8083) - Money transfers between accounts with currency conversion
- **Notifications Service** (Port 8084) - Real-time notifications
- **Exchange Service** (Port 8087) - Currency exchange rates and conversion
- **Exchange Generator Service** (Port 8088) - Automated exchange rate generation
- **Blocker Service** (Port 8089) - Suspicious transaction detection and blocking
- **Auth Server** (Port 8085) - OAuth2 authentication and authorization
- **Front UI** (Port 8086) - Web-based user interface with multi-currency support

### Infrastructure Services
- **Eureka Server** (Port 8761) - Service discovery and registration
- **Config Server** (Port 8888) - Centralized configuration management
- **PostgreSQL** (Port 5432) - Primary database
- **Redis** (Port 6379) - Caching and session storage

Look for data flow diagram at the end of this readme.

## 🔧 Configuration


### Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Gateway | 8080 | API Gateway |
| Accounts | 8081 | User & Multi-Currency Account Management |
| Cash | 8082 | Cash Operations (Deposit/Withdraw) |
| Transfer | 8083 | Money Transfers with Currency Conversion |
| Notifications | 8084 | Notifications |
| Auth Server | 8085 | Authentication |
| Front UI | 8086 | Web Interface |
| Exchange | 8087 | Currency Exchange Rates & Conversion |
| Exchange Generator | 8088 | Automated Exchange Rate Generation |
| Blocker | 8089 | Suspicious Transaction Detection |
| Eureka | 8761 | Service Discovery |
| Config | 8888 | Configuration Server |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |

## 📊 **DATA FLOW DIAGRAM**

```
Client Request
    ↓
Front UI (8086)
    ↓
Gateway (8080) [Routes + Security + Circuit Breaker]
    ↓
    ├─→ Accounts Service (8081) ──→ PostgreSQL (accounts_schema)
    ├─→ Cash Service (8082) ──→ PostgreSQL (cash_schema)
    │                              ├─→ Accounts Service
    │                              └─→ Blocker Service (8089)
    ├─→ Transfer Service (8083) ─→ PostgreSQL (transfer_schema)
    │                              ├─→ Accounts Service
    │                              ├─→ Exchange Service (8087) [Currency Conversion]
    │                              └─→ Blocker Service (8089)
    ├─→ Exchange Service (8087) ─→ PostgreSQL (exchange_schema)
    │                              └─→ Exchange Generator Service (8088) [Updates rates]
    └─→ Notifications Service (8084) ─→ PostgreSQL (notifications_schema)

All services discover each other via Eureka (8761)
All services get config from Config Server (8888)
Auth Server (8085) validates tokens
```

## 💱 **Multi-Currency Features**

The application supports multi-currency operations with the following features:

### Supported Currencies
- **RUB** (Russian Ruble) - Base currency
- **USD** (US Dollar)
- **CNY** (Chinese Yuan)

### Key Features
- **Multi-Currency Accounts**: Users can create accounts in different currencies (one account per currency)
- **Currency Conversion**: Automatic currency conversion for transfers between accounts with different currencies
- **Exchange Rates**: Real-time exchange rates displayed on the frontend, updated every second
- **Conversion Logic**: All conversions go through RUB as the base currency (e.g., USD → RUB → CNY)
- **Suspicious Transaction Detection**: Blocker service monitors transactions and blocks suspicious operations based on amount thresholds

---