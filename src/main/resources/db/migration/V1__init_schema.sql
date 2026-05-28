-- Enable UUID generation in PostgreSQL.
-- This extension provides the uuid_generate_v4() function we use as default IDs.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table: stores registered users and their credentials.
-- UUID is used for IDs to prevent enumeration attacks.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Accounts table: each user can have multiple accounts.
-- balance uses DECIMAL(19,4) to avoid floating-point errors with money.
-- version is for optimistic locking during transfers.
-- CHECK constraint ensures balance can never go negative.
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    account_number VARCHAR(12) NOT NULL UNIQUE,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_balance_nonnegative CHECK (balance >= 0)
);

-- Transactions table: records every deposit, withdrawal, or transfer.
-- source_account_id and destination_account_id are nullable because:
--   - a DEPOSIT has no source account
--   - a WITHDRAWAL has no destination account
--   - a TRANSFER has both
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_account_id UUID REFERENCES accounts(id),
    destination_account_id UUID REFERENCES accounts(id),
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes to speed up transaction history lookups.
CREATE INDEX idx_transactions_source ON transactions(source_account_id);
CREATE INDEX idx_transactions_destination ON transactions(destination_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
