package com.hendrik.javaledgerapi.model.enums;

/**
 * User roles for Role-Based Access Control (RBAC).
 * USER  = regular customer, can manage own accounts and transfer money.
 * ADMIN = can view all accounts and perform admin-only deposits.
 */
public enum Role {
    USER,
    ADMIN
}
