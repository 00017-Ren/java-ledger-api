# Interview Revision

A running revision sheet of interview-relevant topics encountered while building
this project. Grouped by area. Keep entries concise: **topic — why it matters —
takeaway**.

## JPA / Hibernate

### Entity ID generation strategies (`@GeneratedValue`)
- **Why it matters:** Classic JPA interview question. Interviewers probe whether
  you understand who generates the key (DB vs app) and the performance impact.
- **Takeaway:** Strategies are `AUTO`, `IDENTITY`, `SEQUENCE`, `UUID`.
  - `IDENTITY` relies on an auto-increment column; the DB assigns the ID only on
    insert, which **disables JDBC batch inserts** (Hibernate must insert row by
    row to read each generated key).
  - `SEQUENCE` uses a DB sequence Hibernate can pre-fetch, so it **supports
    batching** — generally preferred for performance on Postgres/Oracle.
  - For a DB column with `DEFAULT uuid_generate_v4()`, the database produces the
    UUID. Map the field as `java.util.UUID`.

### `Enum<E>` vs the concrete enum type
- **Why it matters:** A subtle Java generics trap that also breaks JPA mapping.
- **Takeaway:** `Enum<Role>` refers to the abstract base class all enums extend
  (like using `Number` instead of `Integer`). To hold one `Role` constant the
  field type must be the concrete enum: `private Role role;`. `@Enumerated`
  expects a concrete enum type and won't map `Enum<...>` correctly.

### Persisting enums as strings (`@Enumerated`)
- **Why it matters:** A common "gotcha" question and a real production bug source.
- **Takeaway:** Default JPA enum mapping is `EnumType.ORDINAL` — it stores the
  enum's integer position. Reordering or inserting enum constants then silently
  corrupts existing data. Always use `@Enumerated(EnumType.STRING)` so the name
  is stored, matching a `VARCHAR` column.

### No-arg constructor requirement
- **Why it matters:** "Why does a JPA entity need a no-arg constructor?" is a
  frequent question.
- **Takeaway:** JPA/Hibernate instantiates entities via **reflection** when
  loading rows from the database, and reflection needs an accessible no-arg
  constructor to create the object before populating its fields. Spec requires
  at least a non-private no-arg constructor.

### `@ManyToOne` fetch type: EAGER default and the N+1 problem
- **Why it matters:** One of the most common JPA performance interview questions.
- **Takeaway:** `@ManyToOne` and `@OneToOne` default to `FetchType.EAGER` —
  loading the entity always loads its association too. Prefer
  `fetch = FetchType.LAZY` so the association loads only when accessed.
  `@OneToMany`/`@ManyToMany` already default to LAZY. The owning side of a
  relationship is the side holding the foreign key (`@JoinColumn`). EAGER fetch
  in loops is a classic cause of the **N+1 select problem** (1 query for the
  parents + N queries for each child); fix with `JOIN FETCH` or entity graphs.

### Optimistic locking with `@Version`
- **Why it matters:** Demonstrates understanding of concurrency / lost updates,
  highly relevant for anything touching money.
- **Takeaway:** A `@Version` field (int/long/timestamp) is auto-managed by
  Hibernate: it increments on every update and, if two transactions update the
  same row concurrently, the second commit fails with `OptimisticLockException`.
  No DB row locks are held (unlike pessimistic locking), so it scales better for
  low-contention workloads. Never write a setter for the version field.

### `ddl-auto: validate`
- **Why it matters:** Shows you understand schema ownership in a real project.
- **Takeaway:** With `validate`, Hibernate does **not** create or alter tables; it
  only checks that entity mappings match the existing schema and fails fast on
  startup if they don't. Schema creation is owned by Flyway migrations instead —
  safer and version-controlled, unlike `ddl-auto: update` which should never be
  used in production.

## Testing

### Watch a test fail before you trust it passing
- **Why it matters:** Distinguishes engineers who write *meaningful* tests from
  those who write tests that always pass. A strong interview talking point.
- **Takeaway:** A green test proves nothing unless you've seen it go red for the
  right reason. Temporarily break the input (e.g. query a non-existent value) and
  confirm the assertion fails, then revert. Catches no-op assertions and
  mis-wired tests.

### No-op assertions with AssertJ
- **Why it matters:** A subtle, common bug that silently disables a test.
- **Takeaway:** `assertThat(x.isPresent());` asserts **nothing** — `assertThat(...)`
  only *builds* an assertion object; you must chain a check
  (`.isTrue()`, `.isPresent()`, `.isEqualTo(...)`). Prefer AssertJ's first-class
  `Optional` support: `assertThat(optional).isPresent()` /
  `.contains(value)` — clearer failure messages than asserting the raw boolean.

### `@DataJpaTest` + Testcontainers: use the real DB
- **Why it matters:** Shows you know slice tests and high-fidelity DB testing.
- **Takeaway:** `@DataJpaTest` loads only the JPA slice and wraps each test in a
  transaction that **rolls back** after, keeping tests isolated. By default it
  swaps in an embedded DB — add `@AutoConfigureTestDatabase(replace = NONE)` to
  keep your real DataSource. With `@Testcontainers` + a `static @Container` +
  `@ServiceConnection`, Spring Boot 3.1+/4 auto-wires the container's JDBC
  connection (no manual `@DynamicPropertySource`). Static container = one per
  class (fast); non-static = one per method (slow). When Flyway is on the
  classpath it runs against the container, so migrations (including seed data like
  an admin insert) build the schema — real Postgres, not H2.

## Spring Data JPA

### The Repository pattern
- **Why it matters:** Common design-pattern question; explains *why* Spring's data
  layer is easy to test and swap.
- **Takeaway:** The repository abstracts data access behind a collection-like
  interface, so the rest of the app doesn't know or care whether data comes from
  Postgres, an in-memory store, or a mock. You define the interface (the
  contract); Spring Data generates the implementation at runtime.

### Derived query methods
- **Why it matters:** The signature Spring Data feature; interviewers check you
  know queries can come from method *names*.
- **Takeaway:** Spring parses the method name and builds the query, e.g.
  `findByEmail(String)` → `WHERE email = ?`. Property traversal works too:
  `findByUserId(UUID)` means "the `id` of the `user` association" (no need to load
  a full `User`). Extending `JpaRepository<T, ID>` provides `save`, `findById`,
  `findAll`, `delete`, etc. for free.

### `@Query` (JPQL) vs derived names vs native SQL
- **Why it matters:** "JPQL vs SQL?" is a frequent question.
- **Takeaway:** When a derived name gets long/unreadable or the logic is complex
  (e.g. "source OR destination account"), prefer an explicit `@Query`. JPQL
  queries the **object model** (entities/fields like `t.sourceAccount.id`), is
  database-agnostic, and is translated to SQL by Hibernate. Native SQL
  (`nativeQuery = true`) queries tables/columns directly and ties you to a
  specific DB. Bind params with `@Param`.

### `Optional<T>` return types
- **Why it matters:** "Why return `Optional` instead of null?" is common.
- **Takeaway:** `Optional` encodes "may be absent" in the type system, forcing the
  caller to handle the empty case instead of risking a `NullPointerException`.
  Idiomatic for finders that return 0-or-1 result (`findByEmail`). Use `List` for
  0-or-many.

### Pagination: `Page` and `Pageable`
- **Why it matters:** Any real API needs it; strong portfolio talking point.
- **Takeaway:** Adding a `Pageable` parameter tells Spring Data to page/sort the
  query. `Page<T>` returns the rows **plus** metadata (total elements, total
  pages, current page). Avoids loading huge result sets into memory. `Slice<T>` is
  a lighter alternative that skips the expensive total-count query.

## Architecture / System Design

### Package by layer vs package by feature
- **Why it matters:** A recurring architecture/DDD discussion; shows you think
  about maintainability, not just "does it work".
- **Takeaway:** *By layer* (`model/`, `repository/`, `service/`, `controller/`)
  optimises for "show me all the X" and is the familiar Spring-tutorial default.
  *By feature* (`user/`, `account/`, `transaction/` — each containing its own
  entity, repo, service, controller) optimises for "show me everything about Y",
  which is how you actually change code. By-feature also enables **package-private**
  access to hide internals and enforce boundaries — impossible with by-layer.
  Rule of thumb: switch to by-feature when a layer package holds ~7+ classes or
  you have 3+ distinct domains. For a small portfolio project, by-layer is fine —
  but knowing *why* and *when* to switch is the real interview signal.

## Maven / Build

### `<properties>` version vs `<dependencyManagement>` vs BOM import
- **Why it matters:** Explains *why* Spring Boot lets you omit `<version>` and how
  version management actually works — common build-tooling question.
- **Takeaway:** A `<properties>` entry (e.g. `testcontainers.version`) just names a
  version string; it does **not** supply a version to a dependency on its own.
  `<dependencyManagement>` is what actually supplies versions to version-less
  `<dependency>` entries. A **BOM import** (`<type>pom</type>`,
  `<scope>import</scope>`) pulls in a whole family of managed versions at once.
  The Spring Boot parent manages *its own* artifacts (so
  `spring-boot-testcontainers` needs no version) and defines the
  `testcontainers.version` property, but you must still import the
  `testcontainers-bom` yourself to manage the individual `org.testcontainers:*`
  modules. Reference the property (`${testcontainers.version}`) in the import so
  the version stays in lockstep with Spring Boot on upgrade.

### Verify artifact coordinates after a major version bump
- **Why it matters:** Shows you don't blindly trust stale tutorials — a real
  engineering-hygiene signal.
- **Takeaway:** Testcontainers **2.x** changed three things at once vs the 1.x
  tutorials found everywhere online:
  - **Artifact names** gained a `testcontainers-` prefix
    (`postgresql` → `testcontainers-postgresql`,
    `junit-jupiter` → `testcontainers-junit-jupiter`). Old names give a
    "version is missing" POM *model-building* error (raised before BOMs resolve,
    so it looks like a version problem rather than a rename).
  - **Import packages** moved
    (`org.testcontainers.containers.PostgreSQLContainer` →
    `org.testcontainers.postgresql.PostgreSQLContainer`).
  - **Generics removed** — `PostgreSQLContainer` was `PostgreSQLContainer<SELF>`
    in 1.x (used as `<?>`); in 2.x it is non-generic, so `new PostgreSQLContainer(...)`
    with no `<>` (using `<>` gives "does not take parameters").
  General lesson: a major version bump can invalidate artifact names, packages,
  AND API shape — verify against the real BOM/Javadoc, not tutorials.

### Spring Boot 4 modularization: test slice annotations moved
- **Why it matters:** A current, concrete "I keep up with framework changes" point.
- **Takeaway:** Spring Boot 4 split the monolith into fine-grained modules. Test
  slice annotations left `spring-boot-starter-test`:
  - `@DataJpaTest` now needs the `spring-boot-starter-data-jpa-test` dependency and
    lives in `org.springframework.boot.data.jpa.test.autoconfigure` (the old
    Boot 3 package `...test.autoconfigure.orm.jpa` no longer exists).
  So "Cannot resolve symbol `@DataJpaTest`" on Boot 4 = missing the new test
  starter, not an IDE glitch.

## Java

### Money: use `BigDecimal`, never `double`/`float`
- **Why it matters:** Using floating point for currency is an instant code-review
  red flag and a frequent interview trap.
- **Takeaway:** `double`/`float` are binary floating point and cannot represent
  most decimal fractions exactly (`0.1 + 0.2 != 0.3`), causing rounding errors in
  money. Use `BigDecimal` with explicit `precision`/`scale` (here `DECIMAL(19,4)`),
  and always specify a `RoundingMode` when dividing.

### Identifiers as `String`, not numeric types
- **Why it matters:** Tests judgement about modelling data correctly.
- **Takeaway:** Account numbers, phone numbers, postal codes are identifiers, not
  quantities — you never do arithmetic on them and leading zeros are significant.
  Store them as `String`. Bonus reasoning: a 12-digit value overflows a Java
  `int` (max ~2.1 billion, 10 digits), so `int` would be wrong on size alone.

## Git

### Feature-branch + Pull Request workflow
- **Why it matters:** Demonstrates you can work like a team member, not just
  commit to `master`. Visible PR history is a strong portfolio signal.
- **Takeaway:** Keep `master` always stable. Do each unit of work on a
  `feature/...` branch, commit in small logical steps, then merge via a reviewed
  Pull Request. Conventional branch prefixes: `feature/`, `fix/`, `chore/`,
  `docs/`. Documentation-only changes can go straight to `master`.

### Unstaging without losing work
- **Why it matters:** Shows command of the staging area (index vs working tree).
- **Takeaway:** `git restore --staged <file>` (Git 2.23+) moves a file out of the
  staging area back to "modified/untracked" **without deleting your changes**.
  The older equivalent is `git reset HEAD <file>`. Neither touches your actual
  file contents.
