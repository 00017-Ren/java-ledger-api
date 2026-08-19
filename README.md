# Java Ledger API

A portfolio-grade financial ledger REST API built with Java, Spring Boot, PostgreSQL, Flyway, Spring Data JPA/Hibernate, and JWT authentication.

The project models a simple banking-style ledger where users can register, create accounts, receive deposits, transfer funds, and view transaction history. The main learning goal is to demonstrate backend fundamentals relevant to Java/Spring fintech and banking roles: relational modeling, transactional consistency, authentication, authorization, validation, testing, and containerized local development.

## Project Status

In progress. Current phase: Phase 11, API documentation improvements.

Completed:

- Spring Boot project setup
- PostgreSQL Docker Compose setup
- Flyway database migrations
- Initial database schema for users, accounts, and transactions
- Base enums for roles and transaction states
- JPA entity mappings for users, accounts, and transactions
- Repository layer (Spring Data JPA)
- Repository slice tests using `@DataJpaTest` with Testcontainers (real PostgreSQL)
- Integration tests using real PostgreSQL Testcontainers for deferred-constraint
  rollback, concurrent optimistic locking, JWT filter-chain behavior, and
  pageable maximum-size/default-page behavior
- Request/response DTOs with Jakarta Bean Validation
- Global exception handling (`@RestControllerAdvice`) with unit tests
- User registration (`AuthService`/`AuthController`) with BCrypt password
  hashing and duplicate-email prevention (app-level pre-check plus a
  DB-constraint race backstop), unit tested with Mockito
- Minimal Spring Security configuration (public auth endpoints, authenticated
  by default elsewhere)
- Login endpoint and JWT-based authentication
- Account service and secured account endpoints for creating, listing, viewing,
  and retrieving balances for the authenticated user's accounts
- Account service and controller tests, including ownership checks and invalid
  account identifiers
- Transaction service for deposits and transfers
- REST controllers for deposits, transfers, and transaction history
- Transaction service and controller tests, including validation failure paths
  and optimistic-lock propagation

Upcoming work:
- API documentation improvements
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
- Testcontainers

## Core Features

Implemented features:

- User registration and login
- JWT-based authentication
- Account creation
- Account listing, ownership-protected lookup, and balance retrieval
- Global error handling
- Automated repository, service, controller, security, and DTO tests
- Admin-only deposits
- Transfers between accounts with ownership checks for access
- Transaction history with pagination
- Optimistic-lock conflict handling with a `409` response

Remaining features:
- OpenAPI descriptions and examples

## Domain Model

The API is based on three main tables:

- `users`: registered users with email, password hash, and role
- `accounts`: user-owned accounts with account number, balance, currency, and version field
- `transactions`: ledger records for deposits, withdrawals, and transfers

Money values use `DECIMAL(19,4)` in PostgreSQL and should be represented with `BigDecimal` in Java.

## API Design

Endpoints:

| Method | Endpoint                             | Description                              | Status      |
|--------|--------------------------------------|------------------------------------------|-------------|
| POST   | `/api/v1/auth/register`              | Register a new user                      | Implemented |
| POST   | `/api/v1/auth/login`                 | Log in and receive JWT                   | Implemented |
| GET    | `/api/v1/users/me`                   | Get the authenticated user's profile     | Implemented |
| POST   | `/api/v1/accounts`                   | Create an account                        | Implemented |
| GET    | `/api/v1/accounts`                   | List accounts for the authenticated user | Implemented |
| GET    | `/api/v1/accounts/{id}`              | Get account details                      | Implemented |
| GET    | `/api/v1/accounts/{id}/balance`      | Get account balance                      | Implemented |
| GET    | `/api/v1/accounts/{id}/transactions` | View paginated transaction history       | Implemented |
| POST   | `/api/v1/transactions/deposit`       | Admin deposit into an account            | Implemented |
| POST   | `/api/v1/transactions/transfer`      | Transfer money between accounts          | Implemented |
| GET    | `/api/v1/transactions/{id}`          | View a single transaction                | Implemented |

## API Workflow

A minimal end-to-end flow using `curl` in a POSIX shell (e.g. Git Bash on
Windows, or any Linux/macOS terminal). Requires the app running locally (see
[Local Development](#local-development)). Token/id extraction uses plain
`grep`/`cut` so the flow has no extra dependencies.

```bash
# 1. Register a new user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane.doe@example.com","password":"Str0ngPassw0rd!"}'

# 2. Log in and capture the JWT
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane.doe@example.com","password":"Str0ngPassw0rd!"}')
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 3. Create an account
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currency":"USD"}')
echo "$CREATE_RESPONSE"
ACCOUNT_ID=$(echo "$CREATE_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# 4. List your accounts
curl -s http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer $TOKEN"

# 5. View that account's transaction history
curl -s "http://localhost:8080/api/v1/accounts/$ACCOUNT_ID/transactions" \
  -H "Authorization: Bearer $TOKEN"
```

Deposits (`POST /api/v1/transactions/deposit`) additionally require an `ADMIN`
role and are omitted here since a freshly registered user is a `USER`.

### Authorizing in Swagger UI

1. Open `http://localhost:8080/swagger-ui.html`.
2. Call `POST /api/v1/auth/register` (once) and `POST /api/v1/auth/login` to
   obtain a JWT from the response body.
3. Click **Authorize**, paste the raw token value (Swagger UI adds the
   `Bearer ` prefix for you), and close the dialog.
4. Every protected request from Swagger UI now includes the token
   automatically. `POST /api/v1/auth/register` and `POST /api/v1/auth/login`
   stay callable without it.

Never publish the seed-admin password, a JWT, or the development JWT signing
secret from `application-dev.yml` in documentation, screenshots, or commits.

## Known Limitations

The application has the following limitations by design choice:

- No FX conversion, transfers are only available between same-currency accounts.
- Rejected transactions are not persisted as audit rows in PostgreSQL.
- Clients must retry after a 409 optimistic-lock conflict.
- No refresh-token flow, token revocation, or logout endpoint. A refresh-token
  expiry setting exists in `application-dev.yml` but has no corresponding
  implementation; it does not enable refresh-token support.
- No standalone withdrawal endpoint. Funds only move via deposit (admin-only,
  credit) and transfer (between accounts).
- Account creation does not yet handle a database-level account-number
  collision: on the rare occurrence of one, the request fails with a generic
  `500` response instead of the API's standard error format.

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

