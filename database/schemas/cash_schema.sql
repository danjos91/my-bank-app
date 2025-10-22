-- Cash Service Database Schema
-- This schema contains cash transaction records (deposits and withdrawals)

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS cash_schema;

-- Set search path to cash schema
SET search_path TO cash_schema;

-- Cash transactions table - stores all deposit and withdrawal operations
CREATE TABLE IF NOT EXISTS cash_transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT cash_transactions_type_check CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT cash_transactions_amount_positive CHECK (amount > 0)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_cash_transactions_account_id ON cash_transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_cash_transactions_timestamp ON cash_transactions(timestamp);
CREATE INDEX IF NOT EXISTS idx_cash_transactions_type ON cash_transactions(transaction_type);

-- Function to validate account exists (will be called from service)
CREATE OR REPLACE FUNCTION validate_account_exists(account_id_param BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM accounts_schema.accounts 
        WHERE id = account_id_param
    );
END;
$$ LANGUAGE plpgsql;

-- Sample data for testing
INSERT INTO cash_transactions (account_id, amount, transaction_type, description) 
VALUES (1, 1000.00, 'DEPOSIT', 'Initial deposit for testing')
ON CONFLICT DO NOTHING;
