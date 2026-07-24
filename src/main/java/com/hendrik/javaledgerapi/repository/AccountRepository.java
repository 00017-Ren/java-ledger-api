package com.hendrik.javaledgerapi.repository;

import com.hendrik.javaledgerapi.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository <Account, UUID> {

    List<Account> findByUserId(UUID userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
