# API Documentation Contract

This is the Phase 11 (Step 1) documentation contract: an inventory of every
REST operation's purpose, authentication requirement, role/ownership rule,
request shape, success status, and error statuses, taken from the actual
implementation. This file drives the OpenAPI annotations, the contract
integration test, and the README updates that follow it in later steps of
Phase 11 — it is not itself the API documentation.

## Auth (`/api/v1/auth`) — public

| Operation | Method / Path | Auth | Request | Success | Errors |
|---|---|---|---|---|---|
| Register | `POST /register` | none | `RegisterRequest{email, password}` — email format required, password 8-72 chars | `201` `UserResponse` (no `Location` header) | `400` invalid body · `409` duplicate email |
| Login | `POST /login` | none | `LoginRequest{email, password}` — both `@NotBlank`, no email-format check here | `200` `AuthResponse{token, tokenType:"Bearer"}` | `400` blank fields · `401` bad credentials |

## Users (`/api/v1/users`) — authenticated

| Operation | Method / Path | Auth | Ownership | Success | Errors |
|---|---|---|---|---|---|
| Current user | `GET /me` | bearer | self only | `200` `UserResponse` | `401` no/bad token · `404` user row missing (edge case) |

## Accounts (`/api/v1/accounts`) — authenticated

| Operation | Method / Path | Ownership | Success | Errors |
|---|---|---|---|---|
| Create | `POST /` | created for caller only | `201` + `Location` header, `AccountResponse` | `400` invalid currency · see "Known gap" below |
| List mine | `GET /` | caller's own only | `200` `List<AccountResponse>` (not paginated) | — |
| Get by id | `GET /{id}` | not-owner returns **404**, not 403 | `200` `AccountResponse` | `400` malformed UUID · `404` missing/not-owned |
| Balance | `GET /{id}/balance` | same hidden-404 rule | `200` `BalanceResponse{balance, currency}` | `400`/`404` as above |
| History | `GET /{id}/transactions` | same hidden-404 rule | `200` `PagedModel<TransactionResponse>` | `400`/`404` as above |

**Transaction history paging**: default `size=20`, sort `createdAt,id` **DESC**,
global `max-page-size=100` (`application.yml`). Oversized `size` requests are
silently clamped by Spring Data, not rejected.

## Transactions (`/api/v1/transactions`) — authenticated

| Operation | Method / Path | Role / Ownership | Success | Errors |
|---|---|---|---|---|
| Deposit | `POST /deposit` | **ADMIN only**, enforced in the service layer (not `@PreAuthorize`) | `201` + `Location`, `TransactionResponse` (`sourceAccountNumber: null`) | `400` invalid body · `403` caller not ADMIN · `404` destination account missing · `409` optimistic-lock conflict |
| Transfer | `POST /transfer` | source account must belong to caller; not-owner is **404** | `201` + `Location`, `TransactionResponse` | `400` same account / cross-currency · `404` source or destination missing/not-owned · `422` insufficient funds · `409` optimistic-lock conflict |
| Get by id | `GET /{id}` | caller must be party to it (source or destination owner), else **404** | `200` `TransactionResponse` | `400` malformed UUID · `404` missing/not-a-party |

## Global error shape

All `ApiException` subclasses, `MethodArgumentNotValidException`,
`ObjectOptimisticLockingFailureException`, and
`MethodArgumentTypeMismatchException` map to one `ErrorResponse{timestamp,
status, error, message, path}` via `GlobalExceptionHandler`.
`JwtAuthenticationEntryPoint` (401) and `JwtAccessDeniedHandler` (403) build
the same shape by hand at the security-filter layer — two code paths
producing one contract, not one path.

## Cross-cutting rules to carry into annotations

- Ownership violations return **404, never 403**, across account and
  transaction lookups — a deliberate "don't confirm existence" choice, not an
  oversight. Document it explicitly wherever it applies rather than assuming
  a reader infers it.
- The admin-only deposit rule lives in `TransactionService`, not
  `SecurityConfig` — document it as "authenticated + ADMIN role, checked in
  the service layer."
- Insufficient funds returns **422**, not 400 or 409 — call this out since
  it's a less common status choice.

## Known gaps (explicitly deferred, not fixed in this phase)

- **`Role.java`'s doc comment** claims admins can view all accounts; no such
  capability exists. The comment is stale and will be corrected separately,
  outside this documentation phase. Do not document the false capability.
- **`AccountService.createAccount`'s unhandled `DataIntegrityViolationException`**:
  a DB constraint violation on account creation (e.g. a duplicate account
  number collision) currently surfaces as an unhandled `500` with Spring
  Boot's default error body, not the API's `ErrorResponse` shape. This is a
  known limitation, deferred rather than fixed as part of documentation work.
  It belongs in the README's "Known limitations" list (Phase 11, Step 6).
- **`application-dev.yml`'s refresh-token expiry setting** has no
  corresponding implementation in `JwtProperties` or `AuthResponse`. Do not
  imply refresh-token support anywhere in the generated docs.

## Enums

- `Role`: `USER`, `ADMIN`.
- `TransactionStatus`: `PENDING`, `COMPLETED`, `FAILED` — every transaction
  created today (`deposit`, `transfer`) is set directly to `COMPLETED`;
  `PENDING`/`FAILED` are declared but never assigned.
- `TransactionType`: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER` — `WITHDRAWAL` has no
  corresponding endpoint or service method yet.
