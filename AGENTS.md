# AGENTS.md

## Commands
- Use the Maven wrapper: `./mvnw ...` (wrapper config uses Maven 3.9.15; Java target is 25).
- Standard verification is `./mvnw clean test`; run one test with `./mvnw -Dtest=JavaLedgerApiApplicationTests test` or `./mvnw -Dtest=ClassName#methodName test`.
- Run the app with `./mvnw spring-boot:run`; default server port is `8080`, Swagger UI is expected at `/swagger-ui.html` once the app starts.
- Current stack is Spring Boot `4.0.6`, Java `25`, SpringDoc OpenAPI `3.0.3`, Flyway core plus `flyway-database-postgresql`, and PostgreSQL runtime driver.

## Runtime Setup
- The active Spring profile is always `dev` from `src/main/resources/application.yml` unless overridden.
- `dev` expects Postgres at `localhost:5432/ledger` with `ledger_user` / `ledger_pass`; start it with `docker compose up -d` from the repo root.
- `application-dev.yml` uses `spring.jpa.hibernate.ddl-auto: validate`; Flyway owns schema creation and Hibernate only checks mappings.
- `application-dev.yml` includes a development JWT secret; do not treat it as production-safe.

## Schema And Domain
- Flyway migrations live in `src/main/resources/db/migration/` and must stay append-only once applied; current tables are `users`, `accounts`, and `transactions`.
- Match JPA entities to the SQL exactly: UUID primary keys default to `uuid_generate_v4()`, money fields are `DECIMAL(19,4)` / `BigDecimal`, and `Account.version` is for optimistic locking.
- Persist enums as strings matching `Role`, `TransactionType`, and `TransactionStatus` under `com.hendrik.javaledgerapi.model.enums`.
- `V2__insert_admin.sql` contains a placeholder BCrypt-looking admin hash; do not treat it as a real production credential.

## Project Shape
- Root package and Spring Boot entrypoint: `com.hendrik.javaledgerapi.JavaLedgerApiApplication`.
- Git remote `origin` is `https://github.com/00017-Ren/java-ledger-api.git`.
- There is no repo-local CI, formatter, lint, or OpenCode config; prefer Maven and executable Spring/Flyway config over generated prose in `HELP.md` or `GETTING_STARTED.md` when they disagree.
