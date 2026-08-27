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
- **Takeaway:** A `@Version` field is auto-managed by Hibernate: it increments
  on every update and detects a concurrent version conflict. In this codebase,
  Spring exposes that conflict as `ObjectOptimisticLockingFailureException`,
  `GlobalExceptionHandler` returns `409 Conflict`, and the client can retry.
  Integration coverage now exercises the conflict with concurrent PostgreSQL
  transactions. No DB row locks are held, so this suits low-contention
  workloads. Never write a setter for the version field.

### `ddl-auto: validate`
- **Why it matters:** Shows you understand schema ownership in a real project.
- **Takeaway:** With `validate`, Hibernate does **not** create or alter tables; it
  only checks that entity mappings match the existing schema and fails fast on
  startup if they don't. Schema creation is owned by Flyway migrations instead —
  safer and version-controlled, unlike `ddl-auto: update` which should never be
  used in production.

### Persistence context, flush, and transaction commit
- **Why it matters:** Distinguishes `save()` from actually executing SQL and is a
  common JPA transaction-boundary interview question.
- **Takeaway:** `save()` makes an entity managed or schedules it for persistence
  in the persistence context; it does not necessarily send SQL immediately.
  **Flush** synchronizes that context with the database by sending pending SQL,
  and generated fields may become available at that point. **Commit** completes
  the transaction, making the changes durable and visible to other transactions
  (subject to the database's isolation rules). A flush is not a commit: a later
  rollback can still undo the flushed SQL.

## Database Migrations

### Append-only migrations and invalid seed cleanup
- **Why it matters:** Flyway records a checksum for every applied versioned
  migration. Editing an applied script breaks validation and makes environments
  diverge.
- **Takeaway:** Correct an invalid historical seed with a new, idempotent
  migration. Match the known placeholder email, hash, and role so the cleanup
  cannot delete a legitimate user.

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

### Argument-order bugs: a test can target the wrong field
- **Why it matters:** Another way a test can pass while proving nothing —
  distinct from a no-op assertion, easy to introduce with multi-arg
  constructors/records.
- **Takeaway:** Passing a value into the wrong constructor position (e.g.
  `new RegisterRequest(invalidPassword, "validPassword")` when the signature is
  `(email, password)`) silently tests the *other* field's constraints instead.
  The test can still pass — just for the wrong reason. Same fix as always:
  temporarily remove the constraint you claim to be testing and confirm the
  test goes red; if it doesn't, you're not testing what you think you are.

### Testing Bean Validation constraints without a Spring context
- **Why it matters:** There's no test-slice annotation for Bean Validation
  alone — this is the standard way to unit test a DTO's constraints in
  isolation, useful when the service/controller layer isn't built yet.
- **Takeaway:** Build a `Validator` directly and reuse it via `@BeforeAll`:
  ```java
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
      validator = Validation.buildDefaultValidatorFactory().getValidator();
  }
  ```
  Then `Set<ConstraintViolation<T>> violations = validator.validate(dto);` and
  assert on the set (`.isEmpty()` for valid input, `.isNotEmpty()` — or better,
  `.hasSize(n)` — for invalid input). No Spring context required.

### Combining null and invalid values in one `@ParameterizedTest`
- **Why it matters:** `@ValueSource` alone can't express this — knowing the
  alternatives is the difference between fighting the framework and using it
  well.
- **Takeaway:** `@ValueSource` only accepts primitives/`String`/`Class` — no
  `null`, no arbitrary types like `BigDecimal`. Two ways to combine null with
  other invalid values in a single test:
  - **`@NullSource` + `@ValueSource`** stacked on the same method (supported
    since JUnit 5.8) — parse/convert inside the test body if the target type
    isn't a primitive/String.
  - **`@MethodSource`** returning a `Stream<T>` (or `Stream<Arguments>`) that
    includes `null` directly — keeps the real type end-to-end, no parsing;
    generally the cleaner option for non-primitive types like `BigDecimal`.
  Either way, no need for a separate `@Test` just to cover the null case.

### Testcontainers database tests: slices vs full contexts
- **Why it matters:** Shows you understand high-fidelity database tests and the
  lifecycle interaction between Testcontainers and Spring's cached test context.
- **Takeaway:** `@DataJpaTest` loads only the JPA slice and wraps each test in a
  transaction that **rolls back** after. Add
  `@AutoConfigureTestDatabase(replace = NONE)` to retain the configured real
  datasource. `@ServiceConnection` wires a PostgreSQL container into JDBC and
  Flyway without `@DynamicPropertySource`. For multiple `@SpringBootTest`
  classes, prefer a `@TestConfiguration` with an `@Bean @ServiceConnection`
  container: Spring starts it before dependent beans and stops it when the
  cached context closes. A JUnit static `@Container` stops after each class, so
  a reused context can retain a datasource pointing to a stopped container.

### End-to-end privileged workflow tests
- **Why it matters:** Tests that manually insert an `ADMIN` can miss bootstrap,
  password encoding, login, JWT filtering, and authorization regressions.
- **Takeaway:** Use a PostgreSQL Testcontainers test that starts with the
  bootstrap enabled, logs in as the created admin, performs the privileged API
  call, then logs in as a normal user for the follow-up workflow. This proves
  the real security and persistence path instead of a hand-built shortcut.

### Test-managed versus application-managed transactions
- **Why it matters:** A test transaction can hide whether the application's
  transaction boundary truly rolls back money movement after a late failure.
- **Takeaway:** Spring's test transaction rolls back the test method at the end;
  it is not evidence that an application service's `@Transactional` proxy works.
  For rollback integration tests, call the real proxied service without wrapping
  the test in `@Transactional`, then query persisted state after the failure.

### Deferred constraints and transaction rollback
- **Why it matters:** Tests whether you understand that database constraints can
  be checked at commit time, which is important when proving money movement is
  atomic.
- **Takeaway:** A PostgreSQL deferred constraint may allow intermediate invalid
  state inside a transaction but fail at commit; integration tests must verify
  the resulting rollback against a real PostgreSQL database, not only mocked
  repository calls.

### Testing concurrent optimistic locking
- **Why it matters:** A concurrency test demonstrates that `@Version` protects
  against lost updates under real database timing, rather than only testing
  exception translation.
- **Takeaway:** Run competing transactions against the same versioned row and
  assert that one update loses with an optimistic-lock conflict; the application
  can translate that conflict into `409 Conflict`.

### Testing pageable bounds and defaults
- **Why it matters:** Unbounded or client-controlled page sizes can waste
  resources; default behavior is part of the API contract.
- **Takeaway:** Integration-test both omitted paging parameters and oversized
  requests: this API defaults to 20 rows and caps requested pages at 100 rows,
  so clients cannot force unbounded responses.

### Targeted integration-test fixture cleanup
- **Why it matters:** Integration tests commit real rows, so careless cleanup
  causes cross-test contamination or deletes migration seed data.
- **Takeaway:** Track test-created user/account IDs, discover related
  transactions, and delete in foreign-key order: transactions, accounts, then
  users. Avoid broad `deleteAll()` calls and test-level transaction rollback
  when the test must observe application transaction behavior.

### Asserting on `Page` results
- **Why it matters:** Pagination is everywhere; interviewers check you test the
  window *and* the totals, not just "some rows came back".
- **Takeaway:** A `Page<T>` exposes two different things worth asserting:
  the **page window** (`getContent()` / `getContent().size()` = rows in *this*
  page) and the **full result metadata** (`getTotalElements()` = total across all
  pages, `getTotalPages()`, `hasNext()`). Testing a `PageRequest.of(0, 2)` over 3
  matching rows should assert `getContent().size() == 2`,
  `getTotalElements() == 3`, and `hasNext() == true`. Use `Pageable.unpaged()`
  when you only care about the full set, not paging.

### Test an OR query with a negative case
- **Why it matters:** Proves a query is correct by showing it doesn't *over*-match,
  not just that it returns something.
- **Takeaway:** For a "source OR destination account" query, arrange data that
  includes a row involving *neither* target account (e.g. a deposit to a different
  account) and assert it is **excluded** from the result. A test that only checks
  matching rows can pass even if the WHERE clause is too broad; the negative case
  is what actually validates the boundary.

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

### Cost-aware model routing for coding agents
- **Why it matters:** The same quality/cost/latency tradeoff appears in system
  design beyond AI. It shows deliberate engineering rather than applying the
  most expensive resource to every task.
- **Takeaway:** Route bounded, low-risk work such as repository exploration,
  source research, and documentation maintenance to a fast low-cost model.
  Reserve high-reasoning, long-context models for design, debugging, and money
  movement; use an independent model for code review to reduce correlated
  blind spots. Measure actual token usage and task outcomes, not only list
  price, then adjust the routing.

## Spring / Dependency Injection & Service Layer

### Constructor injection vs. field injection
- **Why it matters:** Almost guaranteed to come up if you mention Spring in an
  interview — "why not just `@Autowired` the field?"
- **Takeaway:** Constructor injection makes dependencies `private final`
  (immutable, can't be reassigned) and makes it impossible to construct the
  object in a broken/half-wired state — the compiler enforces every dependency
  is supplied. It also makes unit testing trivial: `new AuthService(mockRepo,
  mockEncoder)` needs no Spring context or reflection. Field injection
  (`@Autowired` directly on a field) allows a class to exist with `null`
  dependencies until Spring populates them, and requires a testing framework
  or reflection to inject mocks. With exactly one constructor, Spring
  auto-detects and uses it — no `@Autowired` annotation needed at all;
  it's only required to disambiguate when a class has multiple constructors.

### `@Service` vs `@Component` vs `@Repository`
- **Why it matters:** Shows you understand Spring's stereotype annotations
  aren't just decoration.
- **Takeaway:** All three are meta-annotated with `@Component`, so functionally
  they all register a bean the same way. `@Service` is a semantic marker for
  "business logic lives here" — self-documenting architecture. `@Repository`
  additionally enables Spring's exception translation (wrapping
  JDBC/Hibernate exceptions into Spring's unchecked `DataAccessException`
  hierarchy). `@Controller`/`@RestController` marks the web layer. Using the
  specific one over generic `@Component` costs nothing and documents intent.

## Spring / Web Layer & Validation

### `@RestController` vs `@Controller`
- **Why it matters:** Same composition pattern as `@RestControllerAdvice`,
  worth being able to state precisely.
- **Takeaway:** `@RestController` = `@Controller` + `@ResponseBody`. Method
  return values are serialized straight into the HTTP response body (JSON for
  a REST API), instead of being resolved as a view template name the way a
  traditional MVC `@Controller` would. `@RestControllerAdvice` follows the
  identical pattern for exception handlers (`@ControllerAdvice` +
  `@ResponseBody`) — same underlying idea applied to two different places.

### The `@Valid` + `@RequestBody` validation pipeline
- **Why it matters:** A complete, connected story interviewers like — shows
  you understand a request's full journey, not just isolated annotations.
- **Takeaway:** `@RequestBody` deserializes incoming JSON into a DTO.
  `@Valid` then runs Jakarta Bean Validation against that DTO's constraint
  annotations (`@Email`, `@NotBlank`, `@Size`, etc.) — **before** the
  controller method body executes at all. If any constraint fails, Spring
  throws `MethodArgumentNotValidException` and the method body never runs.
  That exception is caught centrally by a `@RestControllerAdvice`
  (`@ExceptionHandler(MethodArgumentNotValidException.class)`), turned into a
  clean 400 response, with zero validation logic written in the controller
  itself. One annotation (`@Valid`) wires together: DTO constraints → request
  parsing → automatic rejection → centralised error handling.

### API money precision must match the database column
- **Why it matters:** A money request with more fractional digits than the
  database column supports can be rounded or rejected only after it reaches
  persistence, which is unsafe and surprising for a ledger API.
- **Takeaway:** For `DECIMAL(19,4)`, use `@Digits(integer = 15, fraction = 4)`
  on request amounts alongside `@DecimalMin("0.01")`. The constraints cover
  different rules: `@Digits` enforces representable precision and
  `@DecimalMin` enforces a positive business minimum.

### OpenAPI as an executable API contract
- **Why it matters:** Distinguishes documentation that's actually trustworthy
  from a stale doc or Postman collection nobody maintains — a natural
  follow-up to "how do you document an API for other developers?"
- **Takeaway:** springdoc-openapi generates the OpenAPI document from the
  running application's own annotations (`@Operation`, `@ApiResponse`,
  `@Schema`), not from hand-written prose kept in a separate file. Because
  it's generated from the same code that serves requests, a MockMvc test can
  assert directly against `/v3/api-docs` (bearer requirements, status codes,
  schema references) and fail the build the moment code and documented
  contract diverge — documentation becomes something enforced, not just
  written and hoped-for.

### Reusable security schemes vs. per-operation requirements
- **Why it matters:** A common point of confusion — declaring a security
  scheme once doesn't secure anything by itself.
- **Takeaway:** `components.securitySchemes` (e.g. one `bearerAuth`
  HTTP-bearer/JWT scheme, declared once) is only a *definition*. Each
  operation must separately carry a `security` requirement referencing that
  scheme name before Swagger UI's Authorize control — or any client reading
  the spec — treats it as protected. Applying the requirement globally is a
  shortcut that silently documents public endpoints (registration, login) as
  needing auth they don't; apply it per-controller or per-operation instead,
  matching the real `SecurityFilterChain` rules.

### Operation-level auth metadata isn't derived automatically
- **Why it matters:** Shows you understand that OpenAPI generation reflects
  your *annotations*, not your actual Spring Security configuration — the
  two can silently drift apart.
- **Takeaway:** springdoc cannot inspect a `SecurityFilterChain`'s
  `.requestMatchers(...).permitAll()`/`.authenticated()` rules and infer
  which operations need bearer auth; each controller or operation needs an
  explicit `@SecurityRequirement`. That annotation is a claim, not a
  guarantee — it can fall out of sync with `SecurityConfig` if one changes
  without the other. Same discipline applies to `@AuthenticationPrincipal`
  parameters: springdoc doesn't know they're resolved from the security
  context rather than the request body, so they must be explicitly hidden
  (a `ParameterCustomizer`, or `@Parameter(hidden = true)`) or they leak into
  the generated schema as a bogus request parameter.

### Verify generated docs by breaking them, not just reading them
- **Why it matters:** The same "watch it fail" discipline used for regular
  tests, applied to a documentation contract test — proves it's actually
  load-bearing rather than trivially passing.
- **Takeaway:** A contract test that always passes doesn't prove the
  annotations are wired correctly. Temporarily remove a `@SecurityRequirement`
  (or whatever the test asserts on) and confirm the specific assertion fails
  for the right reason, then restore it. If it doesn't fail, the test isn't
  checking what it claims to.

## Spring Security

### Spring Security's default auto-configuration lockdown
- **Why it matters:** Surprises people the first time — you add the starter
  dependency expecting to configure security later, and instead every
  endpoint (including ones you meant to leave open) is immediately locked
  behind HTTP Basic auth with a random password Spring prints to the console
  on startup.
- **Takeaway:** The moment `spring-boot-starter-security` is on the classpath
  with zero explicit config, Spring Boot's auto-configuration applies a
  default `SecurityFilterChain` that requires authentication for every
  request. This is a deliberate "secure by default" design choice — better to
  accidentally lock yourself out than accidentally ship an open API. Adding
  your own `SecurityFilterChain` bean (as in `SecurityConfig`) fully replaces
  that default, so you become responsible for explicitly permitting the
  routes that should be public (`/api/v1/auth/**` here) — miss one and it's
  silently still open only if you wrote `permitAll()` too broadly, or
  silently still locked if you forgot to list it at all.

### Development-only privileged-account bootstrap
- **Why it matters:** Demonstrates defense in depth and safe idempotent startup
  work without adding a role-escalation API endpoint.
- **Takeaway:** Guard a development admin bootstrap with both `@Profile("dev")`
  and an explicit opt-in property. On repeat startup, accept an existing ADMIN
  unchanged, but fail if the email belongs to a normal user. A unique database
  constraint remains the concurrency authority: on a duplicate-save race,
  re-read and accept only an ADMIN.

### CSRF protection vs. stateless bearer-token APIs
- **Why it matters:** Disabling CSRF protection looks alarming out of context
  ("why are you turning off security?") — being able to justify it precisely
  is the difference between a shortcut and a deliberate architectural choice.
- **Takeaway:** CSRF (Cross-Site Request Forgery) attacks exploit the browser
  automatically attaching **cookies** to requests — a malicious site can
  trigger a request to your API and the browser rides along with the
  victim's existing session cookie, without the attacker ever seeing the
  cookie's value. CSRF protection defends against exactly that ambient-
  credential scenario. This API is a stateless, JSON-only REST API: no
  session cookies, and (once Phase 7 lands) auth is a bearer token the
  client must explicitly attach to an `Authorization` header — something a
  malicious site cannot make a victim's browser do automatically. With no
  cookie-based session to ride, CSRF's attack vector doesn't apply, so
  `csrf(AbstractHttpConfigurer::disable)` is the *correct* call here, not a
  shortcut. (It would very much *not* be correct for a traditional
  server-rendered app using session cookies.)

### JWT signing and claim handling
- **Why it matters:** JWTs are a very common Spring Security interview topic, and interviewers often ask about signing vs encryption and where token data is stored.
- **Takeaway:** A JWT is signed, not encrypted. Put only non-sensitive claims in it (e.g. user id, role), sign it with an HMAC key, and verify the signature before trusting the claims.
- **Takeaway:** In jjwt 0.12/0.13, parse signed tokens with `Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()`. `parserBuilder()` is no longer the current API.

### HMAC key length and JJWT's fail-fast `WeakKeyException`
- **Why it matters:** "How do you make sure a weak JWT secret can't be used?"
  tests whether you know signing algorithms have minimum key-strength
  requirements, and whether failure happens early or silently.
- **Takeaway:** HS256 requires a key of at least 256 bits (32 bytes when the
  secret is UTF-8 encoded). `Keys.hmacShaKeyFor(bytes)` enforces this and
  throws `io.jsonwebtoken.security.WeakKeyException` immediately if the key
  is too short. In this codebase that call happens in `JwtService`'s
  constructor, not at sign time — so a weak secret prevents the bean (and
  therefore the whole application) from starting, rather than quietly
  producing forgeable tokens later. This is the "fail fast" principle:
  detect a misconfiguration at the earliest possible point.

### BCrypt: work factor and why not MD5/SHA for passwords
- **Why it matters:** "Why not just SHA-256 the password?" is a near-universal
  interview question once password storage comes up.
- **Takeaway:** General-purpose hashes like MD5/SHA-256 are built to be
  **fast** — great for checksums, terrible for passwords, because that same
  speed lets an attacker with a stolen password-hash database try billions
  of guesses per second on commodity GPUs. BCrypt (and similarly Argon2,
  scrypt) is a deliberately **slow, adaptive** hashing algorithm built
  specifically for password storage: it takes a tunable "work factor"
  (`strength`, a.k.a. log rounds) where the actual work is `2^strength`
  iterations — so cost increases *exponentially*, not linearly, as the
  factor goes up. `new BCryptPasswordEncoder()` (used in this project's
  `SecurityConfig`) defaults to strength 10 per Spring Security's own
  Javadoc/source. Spring's own docs recommend tuning the strength so a
  single verification takes roughly 1 second on your deployment hardware —
  slow enough to make brute-forcing a stolen hash database impractical,
  fast enough that one legitimate login doesn't feel slow. BCrypt also
  automatically generates and embeds a random salt per password, defeating
  precomputed rainbow-table attacks even before the slowness comes into play.

### TOCTOU race condition + DB unique-constraint defense-in-depth
- **Why it matters:** A concrete, provable concurrency bug (not a theoretical
  one) and a demonstration of understanding that application-level checks
  alone can't guarantee correctness under concurrency.
- **Takeaway:** "Check if the email exists, then insert" is a classic
  **Time-Of-Check-To-Time-Of-Use** race: two concurrent registration
  requests can both pass the `existsByEmail` check (each sees "not taken yet")
  before either has committed, then both attempt to insert. No amount of
  application-level Java code checking a value before acting on it can close
  the timing gap between two separate SQL statements. The actual fix is
  layered (defense in depth): (1) the app-level `existsByEmail` pre-check
  gives a fast, clean 409 for the common case — not racing anyone — and (2)
  the database's own `UNIQUE` constraint on `users.email` is the real,
  atomic source of truth; the DB guarantees only one of two concurrent
  INSERTs can succeed, no matter the timing. `AuthService.register()` catches
  the resulting `DataIntegrityViolationException` from that second INSERT
  and translates it into the same `DuplicateResourceException` (409) the
  pre-check throws, so the race case and the common case look identical to
  the API consumer. Verified with a dedicated unit test
  (`AuthServiceTest`) that stubs `save()` to throw
  `DataIntegrityViolationException` directly, simulating the race without
  needing actual concurrent threads.

## Spring Security & Testing

### Jackson 3 Migration in Spring Boot 4
- **Why it matters:** Spring Boot 4 switched from Jackson 2 (com.fasterxml.jackson)
  to Jackson 3 (tools.jackson). The auto-configured JSON bean is now a
  JsonMapper, not an ObjectMapper—an easy gotcha when wiring JSON into
  security components.
- **Takeaway:** When injecting JSON serialization into security handlers,
  use `tools.jackson.databind.json.JsonMapper` (not the old package).

### @WebMvcTest Changes in Spring Boot 4
- **Why it matters:** Test slices changed. `@WebMvcTest` moved to the
  `spring-boot.webmvc.test.autoconfigure` module and requires
  `spring-boot-starter-webmvc-test` (Spring Boot 4 modularized test infra).
  Interview focus: slice tests vs full integration tests.
- **Takeaway:** Use `@WebMvcTest` for fast web-layer-only tests and
  `@SpringBootTest` for slow, full-context tests.

### @AuthenticationPrincipal and SecurityContextHolder
- **Why it matters:** `@AuthenticationPrincipal` only resolves when the
  SecurityContext is properly set and the argument resolver is registered.
  In slice tests (e.g. `@WebMvcTest`) security wiring may be incomplete,
  leading to a null principal.
- **Takeaway:** If using standalone MockMvc, add an explicit
  `new AuthenticationPrincipalArgumentResolver()` via
  `setCustomArgumentResolvers(...)`.

### User Enumeration Prevention
- **Why it matters:** Security best practice: attackers should not learn which
  emails are registered. Login responses must not differ between
  "unknown email" and "wrong password".
- **Takeaway:** Return the same HTTP status and message for both cases
  (e.g. always respond "Invalid email or password" with 401).

### Stateless JWT Authentication
- **Why it matters:** JWT auth differs from session-based auth: tokens are
  self-contained, no server-side session store is required, and it scales
  horizontally. Trade-off: role/permission changes won’t apply until token
  expiry.
- **Takeaway:** With `SessionCreationPolicy.STATELESS`, a short token TTL
  (commonly ~15 minutes) is the standard pattern.

### Spring Boot 4 Modularization
- **Why it matters:** Boot 4 split test infrastructure into separate starters
  (e.g. `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`).
  Missing starters is a common upgrade failure mode.
- **Takeaway:** On Boot 4 upgrades, verify you added the right test starters
  for the slices you use.

### Slice Testing vs Integration Testing
- **Why it matters:** `@WebMvcTest` loads only the web layer (controllers and
  relevant MVC/security components); `@SpringBootTest` loads the full
  application context.
- **Takeaway:** Prefer slices for fast feedback on isolated components, and
  integration tests for end-to-end behavior across layers.

### Testing the JWT security filter chain
- **Why it matters:** Unit-testing token parsing does not prove that requests
  are authenticated and authorized correctly through Spring Security.
- **Takeaway:** Filter-chain integration tests should cover public access,
  missing or invalid bearer tokens, and authenticated access so the JWT filter,
  `SecurityContext`, and authorization rules are tested together. A real
  `Authorization: Bearer` header minted by `JwtService` exercises the custom
  `JwtAuthenticationFilter`; Spring Security's `jwt()` test helper bypasses it.

### `ApplicationContextRunner` for testing `@ConfigurationProperties` validation
- **Why it matters:** Proving a misconfigured app fails to start (e.g. a
  missing secret) without paying the cost of a full `@SpringBootTest` is a
  practical Spring Boot testing pattern interviewers value.
- **Takeaway:** `ApplicationContextRunner` boots a minimal context containing
  only the beans you register via `.withUserConfiguration(...)`, and
  `.withPropertyValues(...)` sets properties per test without touching real
  config files. Because the context can legitimately fail to start, call
  `.run(context -> ...)` and assert on the `AssertableApplicationContext`
  itself (`assertThat(context).hasFailed()`) rather than expecting an
  exception to propagate out of `.run()`. For `@ConfigurationProperties` +
  JSR-303 validation failures, Spring wraps the real cause as
  `ConfigurationPropertiesBindException` → `BindException` →
  `BindValidationException`, with `BindValidationException` normally being
  the root cause. Asserting `.hasRootCauseInstanceOf(BindValidationException.class)`
  plus checking the field name appears in the stack trace confirms *why* it
  failed, without ever asserting on (or logging) the actual property value.

## Spring / Exception Handling

### `@RestControllerAdvice` and centralised error handling
- **Why it matters:** The standard Spring pattern for turning stack traces into
  clean API errors; also a concrete example of the Chain of
  Responsibility/cross-cutting-concern idea applied to error handling.
- **Takeaway:** `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`
  — handler method return values are serialized straight to the response body
  (JSON), not resolved as a view name, which is what a REST API needs.
  `@ExceptionHandler(SomeException.class)` methods intercept that exception
  type (and subtypes) thrown from *any* controller, so individual controllers
  stay free of try/catch and focus on routing + delegating.

### A custom exception hierarchy: unchecked, with a shared base
- **Why it matters:** "Why extend `RuntimeException` instead of `Exception`?"
  is a common follow-up once you've built a `@RestControllerAdvice`.
- **Takeaway:** Checked exceptions force every method up the call stack to
  declare `throws X` or catch it — ceremony with no payoff when a central
  advice class catches everything regardless. Business/domain exceptions in a
  Spring REST API are almost always unchecked (`extends RuntimeException`). A
  shared abstract base (e.g. `ApiException` carrying an `HttpStatus`) lets one
  `@ExceptionHandler(ApiException.class)` method handle every concrete
  subtype, instead of one handler per exception type.

### Ask the exception for its status vs. using a known constant
- **Why it matters:** Two different, both-correct patterns depending on
  whether an exception's status is genuinely dynamic or fixed by definition —
  worth being deliberate about which one applies.
- **Takeaway:** For your own exceptions where the status varies per instance
  (`ApiException.getHttpStatus()`), read it off the exception. For framework
  exceptions whose status is fixed by what the exception *means* — e.g.
  `MethodArgumentNotValidException.getStatusCode()` is hardcoded in Spring's
  source to always return `HttpStatus.BAD_REQUEST`, there's no scenario where
  it's anything else — just use the constant (`HttpStatus.BAD_REQUEST`)
  directly rather than dynamically resolving it back from an int. Confirmed by
  reading the actual Spring source rather than assuming: don't guess framework
  behaviour, check it.

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

### Maven JDK runtime vs compiler release
- **Why it matters:** Build failures can come from the toolchain rather than application code, and annotation processors often depend on JDK compiler internals.
- **Takeaway:** `<java.version>25</java.version>` sets the class-file/source target; it does not choose the JDK that runs Maven. Maven must itself run on a compatible JDK. Here, Lombok `1.18.46` works with JDK 25 but fails on JDK 27 because JDK 27 removed `com.sun.tools.javac.tree.EndPosTable`. Check `./mvnw -version` and `JAVA_HOME`, not only the POM.

## Java

### Money: use `BigDecimal`, never `double`/`float`
- **Why it matters:** Using floating point for currency is an instant code-review
  red flag and a frequent interview trap.
- **Takeaway:** `double`/`float` are binary floating point and cannot represent
  most decimal fractions exactly (`0.1 + 0.2 != 0.3`), causing rounding errors in
  money. Use `BigDecimal` with explicit `precision`/`scale` (here `DECIMAL(19,4)`),
  and always specify a `RoundingMode` when dividing.

### Money: Comparing monetary `BigDecimal` values with `compareTo`
- **Why it matters:** `equals` is scale-sensitive and operators do not work with objects,
  incorrect comparisons can approve an overdraft.
- **Takeaway:** use `balance.compareTo(amount) < 0` for insufficient-funds checks,
  it compares numeric value regardless of scale.

### Identifiers as `String`, not numeric types
- **Why it matters:** Tests judgement about modelling data correctly.
- **Takeaway:** Account numbers, phone numbers, postal codes are identifiers, not
  quantities — you never do arithmetic on them and leading zeros are significant.
  Store them as `String`. Bonus reasoning: a 12-digit value overflows a Java
  `int` (max ~2.1 billion, 10 digits), so `int` would be wrong on size alone.

### Static factory method naming conventions
- **Why it matters:** Effective Java (Item 1) territory — a common "why not just
  use a constructor" follow-up question, and the naming itself signals whether
  you've read it.
- **Takeaway:** A `static` method that returns an instance of its class, used
  instead of (or alongside) a public constructor. Benefits over a constructor:
  it can have a descriptive **name** (constructors are all just the class name),
  it doesn't have to create a **new** object every call (caching/reuse), and it
  can return a **subtype**. Conventional names carry meaning:
  - `from(OtherType)` — type conversion, one type in, a different type out
    (e.g. `AccountResponse.from(Account account)`).
  - `of(...)` — combines multiple standalone arguments into an instance
    (e.g. `List.of(a, b, c)`).
  - `valueOf(...)` — a more verbose `of`, common on wrapper/enum types
    (`Integer.valueOf(...)`).
  - `getInstance()`/`newInstance()` — singleton or guaranteed-new instance.
  Picking `from` for an entity-to-DTO mapper (rather than a generic `of` or a
  constructor) documents *why* the method exists at the call site.

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

## Account Logic & Service Layer (Phase 8)

### ThreadLocalRandom vs Random vs SecureRandom
- **Why it matters:** Choosing the right RNG affects security and performance — interviewers probe understanding here.
- **Takeaway:** `Random` / `new Random()` defaults to sharing one instance (contention if used concurrently). `ThreadLocalRandom.current()` gives each thread its own instance, faster and less contention. **`SecureRandom`** is cryptographically strong (slow) for security-critical values like tokens/passwords. For non-security randomness (like account number generation), use `ThreadLocalRandom`. For tokens/passwords, use `SecureRandom`.

### Defense-in-depth: app-level uniqueness check + DB constraint
- **Why it matters:** A proven pattern for critical data — guarantees correctness even under race conditions.
- **Takeaway:** Generate account numbers with `ThreadLocalRandom`, check `existsByAccountNumber()` in the app to give a fast, clean error, *and* rely on the database's `UNIQUE` constraint as the atomic source of truth. Two independent mechanisms mean one alone failing (e.g. a race in the app check) is caught by the other. The `AccountNumberGenerator` retries 3 times before giving up; if even that fails, something is genuinely broken (collision is astronomically unlikely on a 12-digit space).

### Service-layer authorization and 404 vs 403
- **Why it matters:** Security best practice that costs nothing — prevents account enumeration attacks.
- **Takeaway:** When a user tries to access a resource they don't own, return `404 Not Found` (same as "doesn't exist"), not `403 Forbidden` (same as "you're not allowed to see it"). An attacker probing for accounts can't distinguish between "account exists but you don't own it" and "account doesn't exist" — both return the same 404. Code path: service layer loads the account, checks ownership, throws `ResourceNotFoundException` in both cases.

### Singleton beans must not hold per-request state
- **Why it matters:** A concurrency bug source and a common gotcha when learning Spring.
- **Takeaway:** Spring services are singleton-scoped (one instance reused for every request). Storing per-request data in instance fields (e.g. `private Account account`) causes concurrent requests to interfere with each other. Inject only dependencies that last the bean's lifetime (repositories, generators, other services). Per-request values (like the account being created in this request) must be local variables, scoped to the method.

### DTO conversion with static factory methods
- **Why it matters:** Keeps data-layer details hidden; a clean separation of concerns.
- **Takeaway:** Use a static `from(Entity)` factory method on the response DTO to convert JPA entities into API responses. This keeps the conversion logic in one place, the response DTO "owns" how it's built from an entity, and callers just call `.from()` without manually extracting fields. Example: `AccountResponse.from(account)` replaces scattered `new AccountResponse(account.getId(), account.getBalance(), ...)` calls.

### Service method signatures: userId vs authenticated principal
- **Why it matters:** Clarifies the boundary between controller and service layers.
- **Takeaway:** The controller extracts `userId` from the authenticated principal and passes it as a plain parameter to the service. The service never knows about Spring Security; it just receives a UUID. This makes the service logic testable without a Spring context and independently callable from anywhere (CLI, scheduled job, etc.). The principal extraction belongs in the controller, the authorization check belongs in the service.

### Controller response status codes and Location headers
- **Why it matters:** REST convention; expected by API clients and standards-checkers.
- **Takeaway:** `POST` (create) returns `201 Created` with a `Location` header pointing to the new resource (e.g. `Location: /api/v1/accounts/{id}`). `GET` returns `200 OK`. Use `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()` to dynamically build the location URI; this is cleaner than hardcoding the path template.

### GlobalExceptionHandler for parameter binding errors
- **Why it matters:** Bad input (malformed UUIDs, invalid JSON) must return a consistent 400 response, not a default framework error page.
- **Takeaway:** Add `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` to catch path variable binding failures (e.g. `GET /accounts/not-a-uuid` when `@PathVariable UUID id` expects a UUID). Return `400 Bad Request` with the standard `ErrorResponse` structure. This handler centralizes error shaping and prevents the controller from needing try/catch blocks.

## Transaction & Service Layer (Phase 9)

### Validate before mutate
- **Why it matters:** Validating first prevents partial balance changes and keeps money movement atomic: a failed transfer must not leave only one balance updated.
- **Takeaway:** Check self-transfer, account existence, ownership, currency, and sufficient funds before mutating balances or saving through repositories. This ordering makes no-save-on-failure behaviour provable with Mockito, without needing a real transaction manager.
