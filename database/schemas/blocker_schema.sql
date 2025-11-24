-- Blocker Service Database Schema
-- This schema contains blocked suspicious transactions

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS blocker_schema;

-- Set search path to blocker schema
SET search_path TO blocker_schema;

-- Blocked transactions table - stores information about blocked/approved transactions
CREATE TABLE IF NOT EXISTS blocked_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255),
    service_name VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3),
    reason VARCHAR(255),
    decision VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT blocked_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT blocked_transactions_decision_check CHECK (decision IN ('BLOCKED', 'APPROVED'))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_blocked_transactions_service_name ON blocked_transactions(service_name);
CREATE INDEX IF NOT EXISTS idx_blocked_transactions_decision ON blocked_transactions(decision);
CREATE INDEX IF NOT EXISTS idx_blocked_transactions_timestamp ON blocked_transactions(timestamp);

