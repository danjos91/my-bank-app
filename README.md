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

A comprehensive microservices-based banking application built with Spring Boot, featuring user management, account operations, money transfers, and real-time notifications.



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

## 🗄️ Database Access

### Credentials

**Production (docker-compose.yml):**
- Database: `bank_app_db`
- User: `bank_app_user`
- Password: `bank_app_password`
- Port: `5432`

**Development (docker-compose.dev.yml):**
- Database: `mybank`
- User: `mybank`
- Password: `mybank123`
- Port: `5432`

### Access Methods

#### Option 1: Using Docker Exec (Recommended)

If your database is running in Docker, you can access it directly:

```bash
# Connect to PostgreSQL container
docker exec -it mybank-postgres psql -U bank_app_user -d bank_app_db

# Or for development environment
docker exec -it mybank-postgres-dev psql -U mybank -d mybank
```

Once connected, you can query the users table:

```sql
-- Set the schema
SET search_path TO accounts_schema;

-- View all users
SELECT id, username, first_name, last_name, email, birth_date, created_at 
FROM users;

-- View users with their account balances
SELECT 
    u.id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    COALESCE(SUM(a.balance), 0) as total_balance
FROM users u
LEFT JOIN accounts a ON u.id = a.user_id
GROUP BY u.id, u.username, u.first_name, u.last_name, u.email
ORDER BY u.id;

-- Count total users
SELECT COUNT(*) FROM users;

-- Exit psql
\q
```

#### Option 2: Using psql from Host Machine

If you have PostgreSQL client installed locally:

```bash
# Production
psql -h localhost -p 5432 -U bank_app_user -d bank_app_db

# Development
psql -h localhost -p 5432 -U mybank -d mybank
```

#### Option 3: Using GUI Tools

You can use graphical tools like **pgAdmin**, **DBeaver**, or **DataGrip**:

**Connection Settings:**
- Host: `localhost`
- Port: `5432`
- Database: `bank_app_db` (production) or `mybank` (development)
- Username: `bank_app_user` (production) or `mybank` (development)
- Password: `bank_app_password` (production) or `mybank123` (development)

**Important:** The users table is in the `accounts_schema` schema, so make sure to:
1. Set the search path: `SET search_path TO accounts_schema;`
2. Or use fully qualified names: `accounts_schema.users`

### Quick Query Examples

```sql
-- View all users (basic info)
SELECT * FROM accounts_schema.users;

-- View users with account information
SELECT 
    u.username,
    u.first_name || ' ' || u.last_name as full_name,
    u.email,
    a.balance
FROM accounts_schema.users u
LEFT JOIN accounts_schema.accounts a ON u.id = a.user_id;

-- Find user by username
SELECT * FROM accounts_schema.users WHERE username = 'admin';

-- View table structure
\d accounts_schema.users
```

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

Look for data flow diagram at the end of this readme.

## 📊 API Documentation

### Authentication
All API endpoints require authentication via OAuth2. Obtain a token from the auth server:

```bash
curl -X POST http://localhost:8085/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user&password=password&client_id=mybank&client_secret=secret"
```

## 🔧 Configuration


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
    │                              └─→ Accounts Service
    ├─→ Transfer Service (8083) ─→ PostgreSQL (transfer_schema)
    │                              └─→ Accounts Service
    └─→ Notifications Service (8084) ─→ PostgreSQL (notifications_schema)

All services discover each other via Eureka (8761)
All services get config from Config Server (8888)
Auth Server (8085) validates tokens
```

---