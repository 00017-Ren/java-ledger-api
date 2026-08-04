package com.hendrik.javaledgerapi.controller;

import com.hendrik.javaledgerapi.dto.request.CreateAccountRequest;
import com.hendrik.javaledgerapi.dto.response.AccountResponse;
import com.hendrik.javaledgerapi.dto.response.BalanceResponse;
import com.hendrik.javaledgerapi.dto.response.TransactionResponse;
import com.hendrik.javaledgerapi.model.Account;
import com.hendrik.javaledgerapi.security.JwtUserPrincipal;
import com.hendrik.javaledgerapi.service.AccountService;
import com.hendrik.javaledgerapi.service.TransactionService;
import jakarta.validation.Valid;
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
public class AccountController {
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

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

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        List<AccountResponse> accounts = accountService.listUserAccounts(principal.id());

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id
    ) {
        AccountResponse response = accountService.getAccountById(id, principal.id());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getAccountBalance(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id
    ) {
        BalanceResponse response = accountService.getAccountBalance(id, principal.id());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<PagedModel<TransactionResponse>> getAccountTransactions(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID id,
            @PageableDefault(size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getAccountHistory(id, principal.id(), pageable);

        return ResponseEntity.ok(new PagedModel<>(response));
    }
}
