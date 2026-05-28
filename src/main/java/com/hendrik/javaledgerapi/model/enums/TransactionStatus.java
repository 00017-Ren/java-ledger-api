package com.hendrik.javaledgerapi.model.enums;

/**
 * Lifecycle state of a transaction.
 * PENDING   = transaction initiated but not yet finalized.
 * COMPLETED = transaction successfully processed.
 * FAILED    = transaction could not be completed (e.g., insufficient funds).
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
