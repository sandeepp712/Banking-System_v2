# 🏦 Multithreaded Banking API
[![main](https://github.com/sandeepp712/Banking-System_v2/actions/workflows/pr-check.yml/badge.svg)](https://github.com/sandeepp712/Banking-System_v2/actions/workflows/pr-check.yml)
[![Docker Build & Publish](https://github.com/sandeepp712/Banking-System_v2/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/sandeepp712/Banking-System_v2/actions/workflows/docker-publish.yml)

**A production, concurrent banking system built with Spring Boot.**

This project demonstrates a secure, resilient, and highly concurrent banking core. It handles money transfers with strict idempotency, pessimistic locking (PostgreSQL `FOR UPDATE`), comprehensive audit logging, and a fully automated CI/CD pipeline.

## 📖 Table of Contents
- [✨ Key Features](#-key-features)
- [🛠️ Tech Stack](#️-tech-stack)
- [🏗️ Architecture Overview](#️-architecture-overview)
- [🚀 Getting Started](#-getting-started)
    - [Prerequisites](#prerequisites)
    - [Local Setup (Without Docker)](#local-setup-without-docker)
    - [Docker Setup](#docker-setup)
- [🔐 Environment Variables](#-environment-variables)
- [🧪 Testing](#-testing)
- [🤖 CI/CD Pipeline](#-cicd-pipeline)
- [📊 API Endpoints (Examples)](#-api-endpoints-examples)

---

## ✨ Key Features
- **JWT Authentication & RBAC**: Secure login/register using RSA 256 asymmetric keys. Supports `ROLE_USER` and `ROLE_ADMIN`.
- **Idempotent Transfers**: Prevents double debiting using unique `idempotency_key` constraints at the database level (PostgreSQL `UNIQUE`).
- **Concurrency Safety**: Implements Pessimistic Locking (`SELECT ... FOR UPDATE`) with a strict **Total Lock Ordering** strategy to prevent deadlocks on concurrent transfers.
- **Audit Logging**: Automatically captures "before" and "after" states of every financial transaction using Spring AOP and stores them as JSONB.
- **Database Versioning**: Uses **Flyway** for seamless, repeatable, and version-controlled database schema migrations.
- **Containerization**: Multi-stage Dockerfile builds a slim, production-ready JRE image (< 150MB).
- **Observability**: Exposes custom metrics (transfer success/failure, idempotency hits) via Micrometer + Prometheus.
- **Comprehensive Testing**: Unit tests (Mockito) and Integration tests (Testcontainers with PostgreSQL) with > 80% coverage on core services.

## 🛠️ Tech Stack
| Layer | Technology |
| :--- | :--- |
| **Core** | Java 21, Spring Boot 3.x, Spring Security |
| **Database** | PostgreSQL 16 (via Docker or local) |
| **Migrations** | Flyway |
| **Security** | JWT (RSA 256), BCrypt |
| **Container** | Docker, Docker Compose |
| **CI/CD** | GitHub Actions (PR Checks + Docker Push) |
| **Testing** | JUnit 5, Mockito, Testcontainers |
| **Monitoring** | Spring Boot Actuator, Micrometer, Prometheus |

## 🏗️ Architecture Overview
The system follows a strict layered architecture to separate concerns:

1.  **API Layer** (`@RestController`): Exposes REST endpoints and validates input DTOs.
2.  **Service Layer** (`@Service`): Orchestrates business logic (Transfers, Deposits). Enforces idempotency and security.
3.  **Domain Layer** (`@Entity` / POJOs): Encapsulates core business rules (e.g., `Account.debit()` balance checks using `ReentrantLock`).
4.  **Persistence Layer** (`JdbcTemplate`): Handles raw SQL execution with row-level locking.
5.  **Audit Layer** (`@Aspect`): Cross-cutting concern that automatically logs all state-changing operations.
6.  **Infrastructure**: Docker Compose for local development, GitHub Actions for automated builds.

## 🚀 Getting Started

### Prerequisites
- **JDK 21**
- **Maven**
- **PostgreSQL 16** (if running locally)
- **Docker & Docker Compose** (for containerized setup)


## 🚀 Docker Setup (Recommended for Production Simulation)

### Build and run the entire stack (App + PostgreSQL):
```bash
docker-compose up --build
```

### Verify the containers are running 
```bash
docker ps
```

### The API will be aviable at: http:localhost:8080


## 🔐 Environment Variables
The project uses a .env file to manage secrets. Never commit this file to Git.

| Variable | Description | Example |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL hostname | `postgres` (Docker) or `localhost` |
| `DB_NAME` | Database name | `banking_db` |
| `DB_USER` | Database username | `bank_user` |
| `DB_PASSWORD` | Database password | `SecurePass123` |
| `JWT_PUBLIC_KEY` | RSA Public Key (Base64) | `MIIBIjAN...` |
| `JWT_PRIVATE_KEY` | RSA Private Key (Base64) | `MIIEvwIB...` |

| 💡 A template is available at .env.example. Simply copy it and fill in your values.

## 🧪 Testing

The project includes both unit and integration tests.

* **Unit Tests:** Built with **Mockito**, focusing on business logic.
* **Integration Tests:** Powered by **Testcontainers** to spin up a real PostgreSQL container.

### Running Tests

Run all tests:
```bash
mvn clean test
```

Run a specific test class:
```bash
mvn test -Dtest=TransferServiceIntegrationTest
```


## 🤖 CI/CD Pipeline

The project uses **GitHub Actions** for continuous integration and deployment.

### 🔀 PR Pipeline (`pr-checks.yml`)
* **Trigger:** Runs on every Pull Request.
* **Database Setup:** Spins up a temporary PostgreSQL container.
* **Verification:** Executes `mvn clean test` (Unit + Integration tests).
* **Quality Gate:** Blocks merging if any test fails.

### 🚀 Main Pipeline (`docker-publish.yml`)
* **Trigger:** Triggers when code is merged into `main`.
* **Safety Gate:** Re-runs tests as a pre-build double-check.
* **Build:** Builds the Docker image using a multi-stage `Dockerfile`.
* **Tagging:** Tags the image with the Git commit SHA (e.g., `f3a4b2c`) and `latest`.
* **Publishing:** Pushes the image to **GitHub Container Registry (GHCR)**.

## 📊 API Endpoints (Examples)

### Authentication Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Authenticate and receive JWT |

**Register Request:**
```bash
{
  "username": "john",
  "password": "SecurePass123!",
  "role": "ROLE_USER"
}
```

**Login Response:**
```bash
{
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer"
}
```

### Protected Resources (Requires Bearer Token)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/accounts/{id}/deposit` | Deposit money |
| `POST` | `/api/accounts/{id}/withdraw` | Withdraw money |
| `POST` | `/api/transfers` | Transfer between accounts (idempotent) |


## 🏗️ Project Structure

```text
src/
├── main/
│   ├── java/com/bank/banking_api/
│   │   ├── aspect/         # AuditAspect (AOP)
│   │   ├── config/         # Security, RSA Config, JWT Filter
│   │   ├── controller/     # REST Controllers
│   │   ├── domain/         # Entities (Account, Transaction, Money)
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Custom Exceptions & Global Handler
│   │   ├── persistence/    # JDBC Repositories
│   │   └── service/        # Business Logic (Transfer, Account, Auth)
│   └── resources/
│       ├── db/migration/   # Flyway SQL scripts (V1__...)
│       └── application.properties
├── test/                   # Unit & Integration tests
├── .github/workflows/      # CI/CD YAML files
├── Dockerfile              # Multi-stage Docker build
├── docker-compose.yml      # Local development stack
└── .env.example            # Environment variable template