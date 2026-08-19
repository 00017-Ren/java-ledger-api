package com.hendrik.javaledgerapi.controller;

import com.hendrik.javaledgerapi.config.OpenApiConfig;
import com.hendrik.javaledgerapi.dto.request.CreateAccountRequest;
import com.hendrik.javaledgerapi.dto.response.AccountResponse;
import com.hendrik.javaledgerapi.dto.response.BalanceResponse;
import com.hendrik.javaledgerapi.dto.response.ErrorResponse;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import com.hendrik.javaledgerapi.service.AccountService;
import com.hendrik.javaledgerapi.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account creation, listing, and balance lookup.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class AccountController {
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @Operation(operationId = "createAccount", summary = "Create an account",
            description = "Creates a new account owned by the authenticated caller. A "
                    + "duplicate account-number collision at the database layer is a known, "
                    + "currently unhandled gap that surfaces as a generic 500 rather than this "
                    + "API's error format.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    headers = @Header(name = "Location",
                            description = "URL of the new account, e.g. /api/v1/accounts/{id}"),
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing currency code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal JwtUserPrincipal jwtUserPrincipal,
            @Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(jwtUserPrincipal.id(), request.currency());
        AccountResponse response = AccountResponse.from(account);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "listAccounts", summary = "List my accounts",
            description = "Returns every account owned by the authenticated caller. Not paginated.")
    @ApiResponse(responseCode = "200", description = "Accounts owned by the caller",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = AccountResponse.class))))
    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<AccountResponse> accounts = accountService.listUserAccounts(principal.id());

        return ResponseEntity.ok(accounts);
    }

    @Operation(operationId = "getAccountById", summary = "Get account by id",
            description = "Returns an account by id. An id that belongs to another user is "
                    + "reported identically to one that doesn't exist, to avoid confirming "
                    + "another user's account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed account id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found, or not owned "
                    + "by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "Account identifier.",
                    example = "8f14e45f-ceea-467e-adc9-15e5a4c1c6e6")
            @PathVariable UUID id
    ) {
        AccountResponse response = accountService.getAccountById(id, principal.id());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getAccountBalance", summary = "Get account balance",
            description = "Returns the current balance and currency for an account. Same "
                    + "ownership-hidden 404 rule as getting the account by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed account id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found, or not owned "
                    + "by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "Account identifier.",
                    example = "8f14e45f-ceea-467e-adc9-15e5a4c1c6e6")
            @PathVariable UUID id
    ) {
        BalanceResponse response = accountService.getAccountBalance(id, principal.id());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getAccountTransactionHistory",
            summary = "Get account transaction history",
            description = "Returns a page of transactions where this account was the source or "
                    + "destination, newest first. Defaults to 20 items per page, sorted by "
                    + "createdAt then id descending; the maximum page size is 100 and larger "
                    + "requests are silently clamped. Same ownership-hidden 404 rule as getting "
                    + "the account by id. The response is a Spring HATEOAS PagedModel with "
                    + "`content` (the transactions) and `page` (size, number, totalElements, "
                    + "totalPages) fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of transactions"),
            @ApiResponse(responseCode = "400", description = "Malformed account id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found, or not owned "
                    + "by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/transactions")
    public ResponseEntity<PagedModel<TransactionResponse>> getAccountTransactions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "Account identifier.",
                    example = "8f14e45f-ceea-467e-adc9-15e5a4c1c6e6")
            @PathVariable UUID id,
            @ParameterObject
            @PageableDefault(size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getAccountHistory(id, principal.id(), pageable);

        return ResponseEntity.ok(new PagedModel<>(response));
    }
}
