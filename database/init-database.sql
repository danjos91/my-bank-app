-- Main Database Initialization Script
-- This script creates the database and all schemas for the microservices bank application

-- Create database if it doesn't exist
-- Note: This needs to be run as a superuser or database owner
-- CREATE DATABASE bank_app_db;

-- Connect to the database (uncomment when running manually)
-- \c bank_app_db;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schemas for each microservice
-- This follows the Database per Service pattern

-- Run all schema creation scripts
\i schemas/accounts_schema.sql
\i schemas/cash_schema.sql
\i schemas/transfer_schema.sql
\i schemas/notifications_schema.sql

-- Create a view for cross-schema queries (if needed for reporting)
-- This view shows user information with their account balance
CREATE OR REPLACE VIEW user_account_summary AS
SELECT 
    u.id as user_id,
    u.username,
    u.first_name,
    u.last_name,
    u.email,
    u.birth_date,
    a.id as account_id,
    a.balance,
    u.created_at as user_created_at,
    a.created_at as account_created_at
FROM accounts_schema.users u
LEFT JOIN accounts_schema.accounts a ON u.id = a.user_id;

-- Grant permissions (adjust as needed for your security requirements)
-- GRANT USAGE ON SCHEMA accounts_schema TO bank_app_user;
-- GRANT USAGE ON SCHEMA cash_schema TO bank_app_user;
-- GRANT USAGE ON SCHEMA transfer_schema TO bank_app_user;
-- GRANT USAGE ON SCHEMA notifications_schema TO bank_app_user;

-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA accounts_schema TO bank_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA cash_schema TO bank_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA transfer_schema TO bank_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA notifications_schema TO bank_app_user;

-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA accounts_schema TO bank_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA cash_schema TO bank_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA transfer_schema TO bank_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA notifications_schema TO bank_app_user;

-- Create indexes for cross-schema performance
CREATE INDEX IF NOT EXISTS idx_cash_transactions_account_id ON cash_schema.cash_transactions(account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_from_account ON transfer_schema.transfers(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_to_account ON transfer_schema.transfers(to_account_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications_schema.notifications_log(user_id);

-- Insert some sample data for testing
-- This will be used for development and testing purposes

-- Sample users (passwords are hashed with BCrypt)
-- Password for all test users is 'password123'
INSERT INTO accounts_schema.users (username, password, first_name, last_name, email, birth_date) VALUES
('john.doe', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'John', 'Doe', 'john.doe@example.com', '1985-05-15'),
('jane.smith', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Jane', 'Smith', 'jane.smith@example.com', '1990-08-22'),
('bob.wilson', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Bob', 'Wilson', 'bob.wilson@example.com', '1988-12-03')
ON CONFLICT (username) DO NOTHING;

-- Create accounts for sample users
INSERT INTO accounts_schema.accounts (user_id, balance) 
SELECT id, 5000.00 FROM accounts_schema.users WHERE username = 'john.doe'
ON CONFLICT DO NOTHING;

INSERT INTO accounts_schema.accounts (user_id, balance) 
SELECT id, 7500.00 FROM accounts_schema.users WHERE username = 'jane.smith'
ON CONFLICT DO NOTHING;

INSERT INTO accounts_schema.accounts (user_id, balance) 
SELECT id, 3000.00 FROM accounts_schema.users WHERE username = 'bob.wilson'
ON CONFLICT DO NOTHING;

-- Sample notifications
INSERT INTO notifications_schema.notifications_log (user_id, notification_type, title, message) 
SELECT u.id, 'ACCOUNT_CREATED', 'Welcome to Bank App!', 'Your account has been successfully created.' 
FROM accounts_schema.users u
ON CONFLICT DO NOTHING;

-- Sample cash transactions
INSERT INTO cash_schema.cash_transactions (account_id, amount, transaction_type, description) 
SELECT a.id, 1000.00, 'DEPOSIT', 'Initial deposit' 
FROM accounts_schema.accounts a
ON CONFLICT DO NOTHING;

COMMIT;
