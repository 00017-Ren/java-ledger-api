# java-ledger-api — Getting Started Guide

> Auto-generated guide for Hendrik Zwiegelaar. Follow each task in order. Do not skip ahead.

---

## Phase 0: Critical Fixes

### Task 1: Fix `pom.xml` and Reload Maven

1. Change Spring Boot version from `4.0.6` to `3.3.5`:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>
```

2. Replace ALL fake test dependencies (`spring-boot-starter-*-test`) with this single real one:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

3. Add JWT libraries inside `<dependencies>`:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**After editing:** Open the Maven tool window in IntelliJ (right side panel) and click the reload button. Resolve any red lines before moving on.

---

## Phase 1: Project Structure & Docker

### Task 2: Create Folder Structure
Create the following empty folders in `src/main/java/com/hendrik/javaledgerapi/`:
- `config/`
- `controller/`
- `dto/request/`
- `dto/response/`
- `exception/`
- `model/enums/`
- `repository/`
- `security/`
- `service/`
- `mapper/`

Create `src/main/resources/db/migration/`.

### Task 3: Create `docker-compose.yml`
Create in project root:
```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ledger
      POSTGRES_USER: ledger_user
      POSTGRES_PASSWORD: ledger_pass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Run: `docker compose up -d`
Verify: `docker ps`

---

## Phase 2: Configuration

### Task 4: Create `application.yml` Files

**Delete** `src/main/resources/application.properties`.

Create `src/main/resources/application.yml`:
```yaml
spring:
  profiles:
    active: dev
  application:
    name: java-ledger-api

server:
  port: 8080
```

Create `src/main/resources/application-dev.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ledger
    username: ledger_user
    password: ledger_pass
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: my-256-bit-secret-my-256-bit-secret-my-256-bit-secret
  access-token-expiration-ms: 900000        # 15 minutes
  refresh-token-expiration-ms: 604800000    # 7 days
```

> Best Practice: `ddl-auto: validate` means Hibernate checks that Java entities match the DB schema but never creates/drops tables. Flyway owns the schema.

---

## Phase 3: Database Schema

### Task 5: Write Flyway Migration `V1__init_schema.sql`

Create: `src/main/resources/db/migration/V1__init_schema.sql`

Requirements:
- Enable UUID extension at top: `CREATE EXTENSION IF NOT EXISTS "uuid-ossp";`
- `users` table: `id UUID PRIMARY KEY DEFAULT uuid_generate_v4()`, `email VARCHAR(255) UNIQUE NOT NULL`, `password_hash VARCHAR(255) NOT NULL`, `role VARCHAR(20) NOT NULL`, `created_at TIMESTAMP`, `updated_at TIMESTAMP`
- `accounts` table: `id UUID PRIMARY KEY DEFAULT uuid_generate_v4()`, `user_id UUID REFERENCES users(id)`, `account_number VARCHAR(12) UNIQUE NOT NULL`, `balance DECIMAL(19,4) NOT NULL DEFAULT 0.00`, `currency VARCHAR(3) DEFAULT 'ZAR'`, `version INT DEFAULT 0`, `created_at TIMESTAMP`, `updated_at TIMESTAMP`, `CHECK (balance >= 0)`
- `transactions` table: `id UUID PRIMARY KEY DEFAULT uuid_generate_v4()`, `source_account_id UUID REFERENCES accounts(id)`, `destination_account_id UUID REFERENCES accounts(id)`, `amount DECIMAL(19,4) NOT NULL`, `type VARCHAR(20) NOT NULL`, `status VARCHAR(20) NOT NULL`, `description VARCHAR(255)`, `created_at TIMESTAMP`
- Indexes on: `transactions.source_account_id`, `transactions.destination_account_id`, `transactions.created_at`

### Task 6: Write Flyway Migration `V2__insert_admin.sql`

Create: `src/main/resources/db/migration/V2__insert_admin.sql`

Insert one admin user. Use a placeholder password hash for now; we will generate a real BCrypt hash when we build the auth service.

---

## Phase 4: Domain Layer

### Task 7: Create Enums

In `com.hendrik.javaledgerapi.model.enums`:

**Role.java**
```java
package com.hendrik.javaledgerapi.model.enums;

public enum Role {
    USER,
    ADMIN
}
```

**TransactionType.java**
```java
package com.hendrik.javaledgerapi.model.enums;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
```

**TransactionStatus.java**
```java
package com.hendrik.javaledgerapi.model.enums;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
```

---

## Phase 5: JPA Entities

### Task 8: Create JPA Entities

In `com.hendrik.javaledgerapi.model`, create three entity classes.

**Guidelines for every entity:**
- Annotate class with `@Entity` and `@Table(name = "...")`
- Use `@Id @GeneratedValue(strategy = GenerationType.UUID)` for ID
- Use `@Enumerated(EnumType.STRING)` for enum fields
- Use `@Version` on the `version` field in `Account` ONLY
- Use `@CreationTimestamp` and `@UpdateTimestamp` from Hibernate for timestamps
- Use `java.math.BigDecimal` for `balance` and `amount` — NEVER `double` for money
- Add Lombok annotations: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`

**Relationships to map:**
- `User` → `@OneToMany(mappedBy = "user")` to `Account`
- `Account` → `@ManyToOne @JoinColumn(name = "user_id")` to `User`
- `Transaction` → `@ManyToOne @JoinColumn(name = "source_account_id")` to `Account`
- `Transaction` → `@ManyToOne @JoinColumn(name = "destination_account_id")` to `Account`

> **IMPORTANT:** If you use `@Builder`, you MUST still provide `@NoArgsConstructor` for JPA.

---

## Verification Checklist

After completing all tasks, run:
```bash
./mvnw spring-boot:run
```

You should see:
1. Flyway migrate the schema (logs say `Migrating schema "public" to version "1"` and `"2"`)
2. Hibernate validate the schema against entities
3. App starts without errors on port 8080
4. Swagger UI loads at `http://localhost:8080/swagger-ui.html`

If you get `PersistenceException` or `SchemaManagementException`, your entities do not match your SQL. Compare column names carefully.

---

## Next Phases (Coming Soon)

- Phase 6: Repositories
- Phase 7: Security & JWT
- Phase 8: Services (Auth, Account, Transaction)
- Phase 9: Controllers & DTOs
- Phase 10: Exception Handling
- Phase 11: Testing
- Phase 12: Dockerize & Deploy

---

*Generated on 2026-05-21 for Project 1: Financial Transaction API.*
