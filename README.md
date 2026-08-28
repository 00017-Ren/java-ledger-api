# Java Ledger API

A portfolio-grade financial ledger REST API built with Java, Spring Boot, PostgreSQL, Flyway, Spring Data JPA/Hibernate, and JWT authentication.

The project models a simple banking-style ledger where users can register, create accounts, receive deposits, transfer funds, and view transaction history. The main learning goal is to demonstrate backend fundamentals relevant to Java/Spring fintech and banking roles: relational modeling, transactional consistency, authentication, authorization, validation, testing, containerized local development, and a public hosted API.

## Live Demo

This is a **portfolio demo**, not a bank. Data is disposable. Do not use real passwords or personal information.

- API: https://java-ledger-api.onrender.com
- Swagger UI: https://java-ledger-api.onrender.com/swagger-ui.html
- Health: https://java-ledger-api.onrender.com/actuator/health

Register and log in via Swagger to obtain a JWT, then use **Authorize**. The API stays on Render Starter (always-on). Neon Free Postgres may sleep after idle; the first request after that can take a few seconds.

## Project Status

Phase 13 (Deploy): **COMPLETED**

Current phase: none. The planned build is shipped.

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
- OpenAPI documentation with endpoint descriptions, response schemas, examples,
  and JWT bearer authentication support in Swagger UI
- OpenAPI contract integration tests covering the generated API document
- Optional development-only admin bootstrap, guarded by the `dev` profile and an
  explicit opt-in property
- Removal of the invalid placeholder admin migration seed
- Transaction create responses populated with their persisted `createdAt` value
- Full-stack Docker Compose (`app` + `db`), multi-stage image, and file-mounted secrets
- Public Actuator health endpoint (`/actuator/health` only)
- Hosted deployment on Render (Docker) with Neon PostgreSQL

Upcoming work:

- None currently planned.

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
- Spring Boot Actuator
- Render
- Neon PostgreSQL
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
- Optional development admin provisioning for local testing
- Transfers between accounts with ownership checks for access
- Transaction history with pagination
- Optimistic-lock conflict handling with a `409` response

Remaining features:

- None currently planned.

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

### Optional Development Admin

The application does not insert a default administrator into the database. For
local development, an administrator can be provisioned at startup by enabling
the `dev`-profile bootstrap and supplying credentials:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--ledger.dev-admin.enabled=true,--ledger.dev-admin.email=admin@example.com,--ledger.dev-admin.password=use-a-local-password"
```

The bootstrap is disabled by default and only runs with the `dev` profile. If
the configured email already belongs to an `ADMIN`, startup continues without
changing that user. If it belongs to a normal user, startup fails rather than
silently escalating privileges. Do not commit real credentials or use this
development configuration in production.

### Authorizing in Swagger UI

1. Open `http://localhost:8080/swagger-ui.html`.
2. Call `POST /api/v1/auth/register` (once) and `POST /api/v1/auth/login` to
   obtain a JWT from the response body.
3. Click **Authorize**, paste the raw token value (Swagger UI adds the
   `Bearer ` prefix for you), and close the dialog.
4. Every protected request from Swagger UI now includes the token
   automatically. `POST /api/v1/auth/register` and `POST /api/v1/auth/login`
   stay callable without it.

Never publish a development admin password, a JWT, or a JWT signing secret in
documentation, screenshots, or commits.

## Known Limitations

The application has the following limitations by design choice:

- No FX conversion, transfers are only available between same-currency accounts.
- Rejected transactions are not persisted as audit rows in PostgreSQL.
- Clients must retry after a 409 optimistic-lock conflict.
- No refresh-token flow, token revocation, or logout endpoint. Only short-lived
  access tokens are issued.
- No standalone withdrawal endpoint. Funds only move via deposit (admin-only,
  credit) and transfer (between accounts).
- Account creation does not yet handle a database-level account-number
  collision: on the rare occurrence of one, the request fails with a generic
  `500` response instead of the API's standard error format.
- The public demo has no rate limiting. Treat hosted data as disposable.

## Local Development

Prerequisites:

- Java 25
- Docker
- Docker Compose

### Secrets

Create two local files (gitignored, not committed):

- `secrets/jwt.secret` - at least 32 characters
- `secrets/spring.datasource.password` - non-empty

Compose mounts these as files under `/run/secrets/`. Do not put passwords or JWT
keys in `docker-compose.yml` or in environment variables.

`POSTGRES_*` values and the password file apply only while the `pgdata` volume
is empty. Changing credentials later requires `docker compose down -v`, which
deletes local database data.

### Full stack (app + database)

```bash
docker compose up --build
```

Services: `app` (API) and `db` (PostgreSQL). Inside the `app` container the
database hostname is `db`, not `localhost`. PostgreSQL port `5432` is not
published on the host.

Application URL: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui.html`

Stop containers and keep data:

```bash
docker compose down
```

Stop and delete the named volume (`pgdata`):

```bash
docker compose down -v
```

### Tests

Run tests on the host. Do not run the Testcontainers suite inside the image
build.

```bash
./mvnw clean test
```

### Run the API on the host

Compose does not publish PostgreSQL `5432`, so host-run cannot use the Compose
`db` container. You need Postgres at `localhost:5432` matching
`application-dev.yml`, then:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile is no longer a packaged default. Without it, host-run will not
load localhost JDBC settings. For an optional development admin, add the
arguments in [Optional Development Admin](#optional-development-admin).

### Hosted deployment (Render + Neon)

The live service is a Docker image on Render Starter with Neon Free PostgreSQL.
Secrets are **not** in git or in the image. Set them in the Render dashboard:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://<neon-direct-host>/<db>?sslmode=require`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32 characters; a new production key, not the local file)

Use Neon’s **direct** hostname (starts with `ep-`, no `-pooler`). Do not put
`user:password@` in the JDBC URL. Do not enable `ledger.dev-admin` on Render.
Health check path: `/actuator/health`. The process listens on `$PORT` (Render
default 10000) or 8080 locally.

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
