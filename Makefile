# MyBank App - Docker Management Makefile

.PHONY: help build up down logs clean test dev-build dev-up dev-down

# Default target
help:
	@echo "MyBank App - Available Commands:"
	@echo ""
	@echo "Production Commands:"
	@echo "  build          - Build all Docker images"
	@echo "  up             - Start all services in production mode"
	@echo "  down           - Stop all services"
	@echo "  logs           - Show logs for all services"
	@echo "  clean          - Remove all containers, networks, and volumes"
	@echo ""
	@echo "Development Commands:"
	@echo "  dev-build      - Build images for development"
	@echo "  dev-up         - Start infrastructure services for development"
	@echo "  dev-down       - Stop development services"
	@echo ""
	@echo "Testing Commands:"
	@echo "  test           - Run all tests"
	@echo "  test-unit      - Run unit tests only"
	@echo "  test-integration - Run integration tests only"
	@echo "  test-contracts - Run contract tests only"

# Production commands
build:
	@echo "Building all Docker images..."
	docker-compose build

up:
	@echo "Starting all services..."
	docker-compose up -d

down:
	@echo "Stopping all services..."
	docker-compose down

logs:
	@echo "Showing logs for all services..."
	docker-compose logs -f

clean:
	@echo "Cleaning up Docker resources..."
	docker-compose down -v --remove-orphans
	docker system prune -f

# Development commands
dev-build:
	@echo "Building development images..."
	docker-compose -f docker-compose.dev.yml build

dev-up:
	@echo "Starting development infrastructure..."
	docker-compose -f docker-compose.dev.yml up -d

dev-down:
	@echo "Stopping development services..."
	docker-compose -f docker-compose.dev.yml down

# Testing commands
test:
	@echo "Running all tests..."
	mvn clean test

test-unit:
	@echo "Running unit tests..."
	mvn test -Dtest="**/*Test"

test-integration:
	@echo "Running integration tests..."
	mvn test -Dtest="**/*IntegrationTest"

test-contracts:
	@echo "Running contract tests..."
	mvn test -Dtest="**/*ContractVerifierTest"

# Service-specific commands
build-service:
	@echo "Building specific service: $(SERVICE)"
	docker-compose build $(SERVICE)

up-service:
	@echo "Starting specific service: $(SERVICE)"
	docker-compose up -d $(SERVICE)

logs-service:
	@echo "Showing logs for service: $(SERVICE)"
	docker-compose logs -f $(SERVICE)

# Health check
health:
	@echo "Checking service health..."
	@echo "Gateway: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/actuator/health || echo 'DOWN')"
	@echo "Accounts: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/actuator/health || echo 'DOWN')"
	@echo "Cash: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/actuator/health || echo 'DOWN')"
	@echo "Transfer: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/actuator/health || echo 'DOWN')"
	@echo "Notifications: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8084/actuator/health || echo 'DOWN')"
	@echo "Auth: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8085/actuator/health || echo 'DOWN')"
	@echo "Front UI: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8086/actuator/health || echo 'DOWN')"
	@echo "Eureka: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8761/actuator/health || echo 'DOWN')"
	@echo "Config: $$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8888/actuator/health || echo 'DOWN')"
