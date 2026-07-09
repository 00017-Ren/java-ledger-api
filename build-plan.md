# Java Ledger API Build Plan

This plan is for building `java-ledger-api` as a learning-focused portfolio project. The goal is to understand each Spring Boot, Hibernate, database, and security concept instead of copying finished code.

## 6-Week Timeline

Target: complete the project in 6 weeks. The 13 phases are grouped by week below. Weeks 3 and 4 are the hardest (Security/JWT and money-movement/transactions) and have deliberately been given more room. If something slips, expect it to slip there. Do not let Week 5 (tests) get squeezed.

| Week | Theme | Phases |
| ---- | ----- | ------ |
| 1 | Data layer: foundation, entities, repositories | 1 (done), 2 (done), 3 (done) |
| 2 | API contract: DTOs/validation and exception handling | 4 (done), 5 |
| 3 | Security: auth basics then Spring Security + JWT (hardest week) | 6, 7 |
| 4 | Domain logic: accounts then transactions (portfolio centrepiece) | 8, 9 |
| 5 | Quality: tests then API documentation | 10, 11 |
| 6 | Ship it: Dockerize then deploy | 12, 13 |

## Git Workflow (team-style)

Mimic a professional team using a feature-branch + Pull Request workflow:

1. `master` always stays stable and working. No work-in-progress goes directly to it.
2. Each phase (or a meaningful slice of one) gets its own branch.
3. Commit in small, logical steps with clear messages.
4. Open a Pull Request on GitHub, review your own diff, then merge.
5. Documentation/planning changes (like this file) may be committed straight to `master` as `docs:`/`chore:` commits.

Branch naming convention used below: `feature/...`, `fix/...`, `chore/...`, `docs/...`.

Each phase section lists a suggested branch and example commit messages. Treat the commit messages as guidance, not a script — commit when a piece genuinely works.

## Current Project State

Completed foundation:

- Maven-based Spring Boot project
- Java 25 configuration
- PostgreSQL Docker Compose setup
- `dev` Spring profile
- Flyway migrations for `users`, `accounts`, and `transactions`
- Base enums for roles, transaction types, and transaction statuses
- SpringDoc/OpenAPI dependency
- JWT library dependencies
- JPA entities for `User`, `Account`, and `Transaction` (Phase 2 complete)
- Repository layer with Testcontainers-backed slice tests (Phase 3 complete)
- Request/response DTOs with bean validation, tested via standalone `Validator`
  unit tests (Phase 4 complete)

Not built yet:

- Service layer
- Controllers
- Security configuration
- JWT authentication flow
- Global exception handling
- Real tests beyond the context load test and DTO/repository unit tests
- Dockerfile and full app containerization
- Deployment

## Week 1: Data Layer

## Phase 1: Stabilize The Foundation

Goal: make sure the existing base is correct before adding business logic.

Git:

- Branch: none needed — this is verification only. If you tweak config, use `chore/stabilize-foundation`.
- Example commits: `chore: confirm flyway schema and app startup`.

Tasks:

1. Start Postgres with `docker compose up -d`.
2. Run `./mvnw clean test`.
3. Run the app with `./mvnw spring-boot:run`.
4. Confirm Flyway creates the schema.
5. Open Swagger UI at `http://localhost:8080/swagger-ui.html`.

Learn:

- What Spring Boot auto-configuration does.
- What `application.yml` and profiles do.
- Why Flyway manages schema instead of Hibernate.

Checkpoint:

- App starts.
- Database schema exists.
- Tests run.

## Phase 2: Build JPA Entities [COMPLETED]

Goal: map Java classes to the existing SQL tables.

Create entities conceptually for:

- `User`
- `Account`
- `Transaction`

Focus on:

- `@Entity`
- `@Table`
- `@Id`
- UUID primary keys
- `BigDecimal` for money
- enum storage as strings
- `@ManyToOne`
- `@OneToMany`
- `@Version` on `Account`

Learn:

- Entity lifecycle.
- Lazy loading.
- Owning side of relationships.
- Difference between database constraints and Java validation.

Git:

- Branch: `feature/jpa-entities`
- Example commits:
  - `feat: add User JPA entity`
  - `feat: add Account JPA entity with optimistic locking`
  - `feat: add Transaction JPA entity and relationships`
- Open a PR titled "Add JPA entities", review your own diff, then merge into `master`.

Checkpoint:

- App starts with `ddl-auto: validate`. [done]
- Hibernate confirms entities match the Flyway schema. [done]

## Phase 3: Add Repositories [COMPLETED]

Goal: create the database access layer.

Repositories needed:

- `UserRepository`
- `AccountRepository`
- `TransactionRepository`

Add only simple query methods first:

- Find user by email.
- Find accounts by user.
- Find account by account number.
- Find paginated transactions by source or destination account.

Learn:

- Spring Data JPA method names.
- `Optional`.
- `Page` and `Pageable`.
- When custom queries are needed.

Git:

- Branch: `feature/repositories`
- Example commits:
  - `feat: add UserRepository with findByEmail`
  - `feat: add AccountRepository queries`
  - `feat: add TransactionRepository with pagination`
  - `test: add repository slice tests`
- PR title: "Add repository layer".

Checkpoint:

- Repository tests can save and load basic records. [done]

## Week 2: API Contract

## Phase 4: Add DTOs And Validation [COMPLETED]

Goal: keep API input/output separate from database entities.

Request DTOs:

- Register request
- Login request
- Create account request
- Deposit request
- Transfer request

Response DTOs:

- User response
- Account response
- Transaction response
- Auth/JWT response
- Error response

Learn:

- Why controllers should not expose entities directly.
- Bean validation.
- Request vs response models.

Git:

- Branch: `feature/dtos-and-validation`
- Example commits:
  - `feat: add request DTOs with bean validation`
  - `feat: add response DTOs`
- PR title: "Add DTOs and request validation".

Checkpoint:

- Invalid requests can be rejected cleanly before service logic runs. [done —
  verified via standalone `Validator` unit tests per request DTO, since the
  service/controller layer doesn't exist yet]

## Phase 5: Add Exception Handling

Goal: return clean API errors instead of stack traces.

Create a global exception handling approach for:

- Resource not found
- Duplicate email or account
- Insufficient funds
- Invalid transfer
- Unauthorized access
- Optimistic locking conflict
- Validation errors

Learn:

- `@RestControllerAdvice`.
- HTTP status codes.
- Consistent error responses.

Git:

- Branch: `feature/exception-handling`
- Example commits:
  - `feat: add custom domain exceptions`
  - `feat: add global RestControllerAdvice handler`
- PR title: "Add global exception handling".

Checkpoint:

- Bad requests return readable JSON errors.

## Week 3: Security

## Phase 6: Build Auth Without JWT First

Goal: understand registration and password hashing before tokens.

Implement:

- Register user.
- Hash password using BCrypt.
- Prevent duplicate emails.
- Store default role as `USER`.

Learn:

- Password hashing.
- Why passwords are never stored directly.
- Service layer responsibilities.

Git:

- Branch: `feature/registration`
- Example commits:
  - `feat: add BCrypt password encoder config`
  - `feat: add user registration service`
  - `feat: add registration controller endpoint`
- PR title: "Add user registration".

Checkpoint:

- A user can register.
- Password hash is stored.
- Duplicate email is rejected.

## Phase 7: Add Spring Security And JWT

Goal: secure the API properly.

Implement:

- Login endpoint.
- JWT generation.
- JWT validation filter.
- Security configuration.
- Public auth endpoints.
- Protected account and transaction endpoints.
- Role checks for admin-only actions.

Learn:

- Security filter chain.
- Authentication vs authorization.
- `SecurityContext`.
- Stateless API security.

Git:

- Branch: `feature/security-jwt`
- This is a large phase — commit in logical slices so the history stays readable:
  - `feat: add JWT generation and parsing utility`
  - `feat: add JWT authentication filter`
  - `feat: add security filter chain config`
  - `feat: add login endpoint issuing JWT`
  - `feat: protect account and transaction endpoints`
- PR title: "Add Spring Security and JWT authentication".

Checkpoint:

- Public endpoints work without token.
- Protected endpoints require token.
- Admin-only deposit rejects normal users.

## Week 4: Domain Logic

## Phase 8: Build Account Logic

Goal: allow users to create and view accounts.

Implement:

- Create account for authenticated user.
- Generate unique 12-digit account number.
- List current user's accounts.
- Get single account.
- Get balance.

Learn:

- Getting the authenticated user from the security context.
- Ownership checks.
- Service-layer authorization.

Git:

- Branch: `feature/account-logic`
- Example commits:
  - `feat: add account creation with unique 12-digit number`
  - `feat: add list and get account endpoints`
  - `feat: add ownership checks in account service`
- PR title: "Add account management".

Checkpoint:

- A logged-in user can manage only their own accounts.

## Phase 9: Build Transaction Logic

Goal: implement the most important portfolio feature.

Implement:

- Admin deposit.
- Transfer between accounts.
- Transaction history.
- Single transaction lookup.
- Pagination.

Focus carefully on:

- `@Transactional`
- Balance updates
- Insufficient funds checks
- Source and destination account validation
- Recording transaction history
- Optimistic locking conflict behavior

Learn:

- Transaction boundaries.
- Atomicity.
- Why money movement must succeed or fail as one unit.
- Race conditions.
- Optimistic locking.

Git:

- Branch: `feature/transaction-logic`
- The most important phase — keep commits granular so reviewers can follow the money logic:
  - `feat: add admin deposit with @Transactional`
  - `feat: add transfer with insufficient-funds check`
  - `feat: add transaction history with pagination`
  - `feat: handle optimistic locking conflicts on transfer`
- PR title: "Add transaction and transfer logic".

Checkpoint:

- Transfer subtracts from the source account and adds to the destination account.
- Failed transfer does not partially update balances.
- Transaction record is saved only for valid operations.

## Week 5: Quality

## Phase 10: Add Tests Properly

Goal: prove the important behavior works.

Start with service tests:

- Register user.
- Reject duplicate user.
- Create account.
- Deposit funds.
- Transfer funds.
- Reject insufficient funds.
- Reject transfer to same account.
- Reject unauthorized account access.

Then add API/controller tests:

- Unauthorized request returns `401`.
- Invalid request returns `400`.
- User cannot access another user's account.
- Admin can deposit.

Learn:

- Unit tests vs integration tests.
- Mocking repositories and services.
- Spring Boot test slices.
- Testcontainers later if you want stronger PostgreSQL integration.

Git:

- Branch: `test/service-and-controller-tests`
- Example commits:
  - `test: add registration and account service tests`
  - `test: add transfer and insufficient-funds tests`
  - `test: add controller auth and validation tests`
- PR title: "Add service and controller tests".

Checkpoint:

- `./mvnw clean test` passes consistently.

## Phase 11: Improve API Documentation

Goal: make the project easy for employers to understand.

Add:

- Swagger descriptions.
- Example request and response bodies.
- Clear endpoint names.
- README screenshots or sample calls.
- Known limitations section.

Learn:

- OpenAPI.
- Developer experience.
- How recruiters and senior developers scan a project.

Git:

- Branch: `docs/api-documentation`
- Example commits:
  - `docs: add OpenAPI annotations and examples`
  - `docs: update README with sample calls and limitations`
- PR title: "Improve API documentation".

Checkpoint:

- Someone can open Swagger and understand the API without asking you.

## Week 6: Ship It

## Phase 12: Dockerize The Full App

Goal: run the app and database together.

Add later:

- Dockerfile
- App service in `docker-compose.yml`
- Environment-variable based config
- Production-safe JWT secret handling

Learn:

- Difference between local development config and deployment config.
- Container networking.
- Environment variables.

Git:

- Branch: `feature/dockerize`
- Example commits:
  - `feat: add Dockerfile for app`
  - `feat: add app service to docker-compose with env config`
- PR title: "Dockerize application".

Checkpoint:

- One command starts both the database and app.

## Phase 13: Deploy

Goal: make the project portfolio-visible.

Good options:

- Render
- Railway
- Fly.io
- AWS Elastic Beanstalk later

Deploy:

- App
- PostgreSQL database
- Environment variables
- Swagger URL

Git:

- Branch: `chore/deploy`
- Example commits:
  - `chore: add deployment config for hosting platform`
  - `docs: add live API URL to README`
- PR title: "Deploy to hosting platform".

Checkpoint:

- README has a live API URL.
- GitHub repo description is updated.
- LinkedIn and OfferZen can point to the deployed project.

## Recommended Learning Order

1. Spring Boot basics
2. JPA entities and repositories
3. DTOs and validation
4. Service layer and transactions
5. Spring Security
6. JWT
7. Testing
8. Docker and deployment

Do not start with JWT. Build domain model and database confidence first. Security is easier once the basic API shape is working.

## Portfolio Notes

Keep commits small and meaningful. A good commit history for this project might look like:

- Set up database migrations
- Add JPA entities
- Add repository layer
- Add account service
- Add auth registration
- Add JWT login
- Add transfer service
- Add transaction controller
- Add service tests
- Add Dockerfile
- Update README with deployment details

This makes the repository easier to review and shows that you built the project in logical stages.
