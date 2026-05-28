package com.hendrik.javaledgerapi.model.enums;

/**
 * Types of financial transactions supported by the ledger.
 * DEPOSIT     = money added to an account (admin-only in this system).
 * WITHDRAWAL  = money removed from an account (not yet implemented as a standalone endpoint).
 * TRANSFER    = money moved between two accounts.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
