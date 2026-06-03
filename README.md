# Java Ledger API

A portfolio-grade financial ledger REST API built with Java, Spring Boot, PostgreSQL, Flyway, Spring Data JPA/Hibernate, and JWT authentication.

The project models a simple banking-style ledger where users can register, create accounts, receive deposits, transfer funds, and view transaction history. The main learning goal is to demonstrate backend fundamentals relevant to Java/Spring fintech and banking roles: relational modeling, transactional consistency, authentication, authorization, validation, testing, and containerized local development.

## Project Status

In progress.

Current foundation completed:

- Spring Boot project setup
- PostgreSQL Docker Compose setup
- Flyway database migrations
- Initial database schema for users, accounts, and transactions
- Base enums for roles and transaction states

Upcoming work:

- JPA entity mappings
- Repository layer
- Authentication and JWT security
- Account and transaction services
- REST controllers
- Validation and exception handling
- Unit and integration tests
- Deployment

## Tech Stack

- Java 25
- Spring Boot 4
- Maven
- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway
- Spring Security
- JWT
- SpringDoc OpenAPI / Swagger UI
- Docker Compose
- JUnit 5

## Core Features

Planned features:

- User registration and login
- JWT-based authentication
- Role-based access control with `USER` and `ADMIN`
- Account creation
- Account balance tracking
- Admin-only deposits
- Transfers between accounts
- Transaction history with pagination
- Optimistic locking for safer concurrent balance updates
- Global error handling
- Swagger/OpenAPI documentation
- Automated tests for service and API behavior

## Domain Model

The API is based on three main tables:

- `users`: registered users with email, password hash, and role
- `accounts`: user-owned accounts with account number, balance, currency, and version field
- `transactions`: ledger records for deposits, withdrawals, and transfers

Money values use `DECIMAL(19,4)` in PostgreSQL and should be represented with `BigDecimal` in Java.

## API Design

Planned endpoints:

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Log in and receive JWT |
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts` | List accounts for the authenticated user |
| GET | `/api/v1/accounts/{id}` | Get account details |
| GET | `/api/v1/accounts/{id}/balance` | Get account balance |
| POST | `/api/v1/transactions/deposit` | Admin deposit into an account |
| POST | `/api/v1/transactions/transfer` | Transfer money between accounts |
| GET | `/api/v1/transactions` | View paginated transaction history |
| GET | `/api/v1/transactions/{id}` | View a single transaction |

## Local Development

Prerequisites:

- Java 25
- Docker
- Docker Compose

Start PostgreSQL:

```bash
docker compose up -d
```

Run tests:

```bash
./mvnw clean test
```

Run the application:

```bash
./mvnw spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Database Migrations

Flyway owns the database schema.

The application uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate checks that the Java entities match the database schema, but it does not create or modify tables automatically.

Migration files live in:

```text
src/main/resources/db/migration/
```

## Learning Goals

This project is designed to show practical backend competence in:

- Spring Boot application structure
- Dependency injection
- REST API design
- DTOs and validation
- JPA entity mapping
- Repository patterns
- Hibernate relationships
- Database migrations with Flyway
- Transaction boundaries with `@Transactional`
- Optimistic locking with `@Version`
- JWT authentication
- Role-based authorization
- Global exception handling
- Unit and integration testing
- Dockerized development

