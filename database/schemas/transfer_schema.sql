-- Transfer Service Database Schema
-- This schema contains money transfer records between accounts

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS transfer_schema;

-- Set search path to transfer schema
SET search_path TO transfer_schema;

-- Transfers table - stores all money transfers between accounts
CREATE TABLE IF NOT EXISTS transfers (
    id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT transfers_different_accounts CHECK (from_account_id != to_account_id),
    CONSTRAINT transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT transfers_status_check CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Transfer history table - stores detailed transfer events
CREATE TABLE IF NOT EXISTS transfer_events (
    id BIGSERIAL PRIMARY KEY,
    transfer_id BIGINT NOT NULL REFERENCES transfers(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT transfer_events_type_check CHECK (event_type IN (
        'TRANSFER_INITIATED', 'BALANCE_CHECKED', 'BALANCE_DEDUCTED', 
        'BALANCE_CREDITED', 'TRANSFER_COMPLETED', 'TRANSFER_FAILED', 
        'TRANSFER_CANCELLED', 'ROLLBACK_INITIATED', 'ROLLBACK_COMPLETED'
    ))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_transfers_from_account ON transfers(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_to_account ON transfers(to_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfers(status);
CREATE INDEX IF NOT EXISTS idx_transfers_created_at ON transfers(created_at);
CREATE INDEX IF NOT EXISTS idx_transfer_events_transfer_id ON transfer_events(transfer_id);
CREATE INDEX IF NOT EXISTS idx_transfer_events_timestamp ON transfer_events(timestamp);

-- Function to validate accounts exist
CREATE OR REPLACE FUNCTION validate_accounts_exist(from_account BIGINT, to_account BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM accounts_schema.accounts 
        WHERE id IN (from_account, to_account)
    );
END;
$$ LANGUAGE plpgsql;

-- Function to check if accounts are different
CREATE OR REPLACE FUNCTION validate_different_accounts(from_account BIGINT, to_account BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN from_account != to_account;
END;
$$ LANGUAGE plpgsql;
