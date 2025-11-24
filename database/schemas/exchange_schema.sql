-- Exchange Service Database Schema
-- This schema contains currency exchange rates

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS exchange_schema;

-- Set search path to exchange schema
SET search_path TO exchange_schema;

-- Exchange rates table - stores currency exchange rates
CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    currency VARCHAR(3) UNIQUE NOT NULL,
    buy_rate DECIMAL(19,6) NOT NULL,
    sell_rate DECIMAL(19,6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT exchange_rates_currency_check CHECK (currency IN ('RUB', 'USD', 'CNY')),
    CONSTRAINT exchange_rates_buy_rate_positive CHECK (buy_rate > 0),
    CONSTRAINT exchange_rates_sell_rate_positive CHECK (sell_rate > 0)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_exchange_rates_currency ON exchange_rates(currency);

-- Triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_exchange_rates_updated_at 
    BEFORE UPDATE ON exchange_rates 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default rates (RUB is base currency, so it's not stored)
-- USD: 1 USD = 100 RUB (buy), 1 USD = 95 RUB (sell)
-- CNY: 1 CNY = 14 RUB (buy), 1 CNY = 13 RUB (sell)
INSERT INTO exchange_rates (currency, buy_rate, sell_rate) 
VALUES 
    ('USD', 100.0, 95.0),
    ('CNY', 14.0, 13.0)
ON CONFLICT (currency) DO NOTHING;

