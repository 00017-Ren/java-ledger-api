package com.hendrik.javaledgerapi.controller;

import com.hendrik.javaledgerapi.config.OpenApiConfig;
import com.hendrik.javaledgerapi.dto.request.DepositRequest;
import com.hendrik.javaledgerapi.dto.request.TransferRequest;
import com.hendrik.javaledgerapi.dto.response.ErrorResponse;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import com.hendrik.javaledgerapi.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Deposits, transfers, and transaction history.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(operationId = "deposit", summary = "Deposit funds (admin only)",
            description = "Credits an account with newly created funds. Restricted to callers "
                    + "with the ADMIN role; the check happens in the service layer, not "
                    + "declaratively. The resulting transaction has no source account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Deposit recorded",
                    headers = @Header(name = "Location",
                            description = "URL of the new transaction, e.g. /api/v1/transactions/{id}"),
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid account number or amount",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Destination account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent update to the "
                    + "destination account; retry the request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody DepositRequest request) {

        TransactionResponse response = transactionService.deposit(principal.role(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/transactions/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "transfer", summary = "Transfer funds between accounts",
            description = "Moves funds from an account owned by the caller to another account. "
                    + "Both accounts must share the same currency. A source account that "
                    + "doesn't exist or isn't owned by the caller is reported identically to "
                    + "avoid confirming another user's account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer recorded",
                    headers = @Header(name = "Location",
                            description = "URL of the new transaction, e.g. /api/v1/transactions/{id}"),
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Same source/destination account, "
                    + "differing currencies, or an invalid account number/amount",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination account "
                    + "not found, or the source account is not owned by the caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Source account has insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Concurrent update to the source or "
                    + "destination account; retry the request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(principal.id(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/transactions/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "getTransactionById", summary = "Get transaction by id",
            description = "Returns a transaction the caller is a party to, as the owner of "
                    + "either the source or destination account. A transaction the caller "
                    + "isn't party to is reported identically to one that doesn't exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed transaction id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found, or the "
                    + "caller is not a party to it",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "Transaction identifier.",
                    example = "b6c1e6b0-6e2e-4f0a-9f0e-2c8e8f6c2a10")
            @PathVariable UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id, principal.id());

        return ResponseEntity.ok(response);
    }
}
