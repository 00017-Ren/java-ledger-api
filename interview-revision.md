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
