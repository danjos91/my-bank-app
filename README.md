# MyBank App - Microservices Banking Application (v2.0)

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Kubernetes](https://img.shields.io/badge/kubernetes-%23326ce5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?style=for-the-badge&logo=helm&logoColor=white)
![Jenkins](https://img.shields.io/badge/jenkins-%232C5263.svg?style=for-the-badge&logo=jenkins&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

A comprehensive microservices-based banking application built with Spring Boot, user management, multi-currency account operations, money transfers with currency conversion, real-time notifications and exchange rate management, with **Kubernetes** deployment using **Helm Charts** and **Jenkins CI/CD**.

**Features:**
- ☁️ **Kubernetes Native**: No more Eureka/Config Server. Uses K8s Services, ConfigMaps, and Secrets.
- 📦 **Helm Charts**: Dedicated charts for each microservice and an Umbrella chart for full deployment.
- 💾 **StatefulSets**: Databases deployed as StatefulSets with persistent storage.
- 🚀 **CI/CD**: Full Jenkins integration with pipelines for each service.
- 🔐 **OAuth2**: Auth Server running in Kubernetes.
- 🌐 **Ingress**: Front UI exposed via Ingress Controller.
- 💱 **Multi-Currency**: Full support for RUB, USD, and CNY with automatic conversion.

## 💱 Multi-Currency & Key Features

This application supports complex banking operations including:

- **Multi-Currency Accounts**: Users can create accounts in **RUB**, **USD**, and **CNY**.
- **Currency Conversion**: Automatic real-time conversion for transfers between different currencies (e.g., USD → RUB → CNY).
- **Exchange Service**: dedicated microservice for managing exchange rates.
- **Exchange Generator**: Simulates market fluctuations by updating rates every second.
- **Blocker Service**: Security microservice that monitors transactions and blocks suspicious activity based on thresholds (e.g., transactions > 10,000 RUB).
- **Notifications**: Real-time alerts for all account activities.

## 🚀 Quick Start (Kubernetes)

### Prerequisites
- **Minikube** (or Kind/Colima)
- **Kubectl**
- **Helm 3+**
- **Docker**
- **Java 21** or higher
- **Maven 3.9+**

### 1. Start Minikube
```bash
minikube start --cpus 4 --memory 8192
minikube addons enable ingress
```

### 2. Deploy with Helm (Umbrella Chart)
This will deploy ALL microservices and databases at once.

```bash
# Go to the helm directory
cd helm/

# Install the umbrella chart
helm dependency update my-bank-app
helm install my-bank-app ./my-bank-app
```

### 3. Access the Application
Get the URL for the Front UI:
```bash
# If using Minikube Tunnel (requires root):
minikube tunnel
# Access at http://bank.local (add to /etc/hosts: 127.0.0.1 bank.local)

# OR simply port-forward:
kubectl port-forward svc/front-ui 8086:8086
# Access at http://localhost:8086
```

## 🏗️ Architecture

The architecture has been migrated from a Spring Cloud stack to a Kubernetes-native approach:

| Component | v1.0 (Legacy) | v2.0 (Kubernetes) | Description |
|-----------|---------------|-------------------|-------------|
| **Service Discovery** | Netflix Eureka | Kubernetes DNS (Services) | Services find each other by K8s Service names (e.g., `http://accounts-service`) |
| **Config Management** | Spring Cloud Config | ConfigMaps & Secrets | Configuration injected as env vars or files |
| **Gateway** | Spring Cloud Gateway | Kubernetes Ingress / Gateway API | External access routing |
| **Database** | Docker Compose Service | Kubernetes StatefulSet | Persistent data storage |
| **Deployment** | Docker Compose | Helm Charts | Infrastructure as Code |

### 📂 Project Structure

```
my-bank-app/
├── accounts-service/           # User accounts & balances
├── auth-server/                # OAuth2 Authorization Server
├── blocker-service/            # Suspicious transaction blocker
├── cash-service/               # Deposits & Withdrawals
├── exchange-generator-service/ # Generates random exchange rates
├── exchange-service/           # Handles currency conversion
├── front-ui/                   # Web Interface (Thymeleaf)
├── notifications-service/      # User notifications
├── transfer-service/           # Money transfers logic
├── helm/                       # Helm Charts
│   ├── my-bank-app/            # ☂️ Umbrella Chart
│   ├── accounts-service/       # Individual Charts...
│   ├── auth-server/
│   ├── blocker-service/
│   ├── cash-service/
│   ├── exchange-generator-service/
│   ├── exchange-service/
│   ├── front-ui/
│   ├── notifications-service/
│   └── transfer-service/
├── Jenkinsfile                 # 🔄 Master CI/CD Pipeline
└── README.md                   # Documentation
```

## 🔧 Service Ports

Even though in Kubernetes services communicate via internal cluster IPs, the internal container ports remain the same:

| Service | Port | Description |
|---------|------|-------------|
| **Gateway / Ingress** | 80/443 | Entry point (Front UI exposed here) |
| **Accounts** | 8081 | User & Multi-Currency Account Management |
| **Cash** | 8082 | Cash Operations (Deposit/Withdraw) |
| **Transfer** | 8083 | Money Transfers with Currency Conversion |
| **Notifications** | 8084 | Notifications |
| **Auth Server** | 8085 | OAuth2 Authentication |
| **Front UI** | 8086 | Web Interface |
| **Exchange** | 8087 | Currency Exchange Rates & Conversion |
| **Exchange Generator** | 8088 | Automated Exchange Rate Generation |
| **Blocker** | 8089 | Suspicious Transaction Detection |
| **PostgreSQL** | 5432 | Database (Internal) |

## 🛠️ CI/CD with Jenkins

This project includes `Jenkinsfile` for each microservice and a master `Jenkinsfile` for the whole project.

### Setting up Jenkins in Minikube

1. **Install Jenkins via Helm:**
   ```bash
   helm repo add jenkins https://charts.jenkins.io
   helm repo update
   kubectl create namespace jenkins
   helm install jenkins jenkins/jenkins --namespace jenkins --set controller.serviceType=NodePort
   ```

2. **Get Admin Password:**
   ```bash
   kubectl exec --namespace jenkins -it svc/jenkins -c jenkins -- /bin/cat /run/secrets/additional/chart-admin-password && echo
   ```

3. **Access Jenkins:**
   ```bash
   minikube service jenkins -n jenkins --url
   ```

4. **Create Pipeline:**
   - New Item -> Pipeline -> Name: `my-bank-app`
   - Definition: Pipeline script from SCM -> Git
   - Repository URL: (Your Git Repo URL)
   - Script Path: `Jenkinsfile` (for the umbrella project) or `accounts-service/Jenkinsfile` (for individual services).

## 👤 Test Users

The following test users are available for testing the application functionality. They are automatically created in the database on startup.

| Username | Password | Role | Initial Balance |
|----------|----------|------|-----------------|
| `admin` | `password123` | User | 200,000 RUB |
| `john` | `password123` | User | 5,000 RUB |
| `jane` | `password123` | User | 7,500 RUB |
| `bob` | `password123` | User | 3,000 RUB |
