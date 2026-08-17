ALTER TABLE transactions
    ADD CONSTRAINT test_transactions_destination_account_description_unique
    UNIQUE (destination_account_id, description)
    DEFERRABLE INITIALLY DEFERRED;