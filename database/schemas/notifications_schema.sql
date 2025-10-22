-- Notifications Service Database Schema
-- This schema contains user notification logs and activity tracking

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS notifications_schema;

-- Set search path to notifications schema
SET search_path TO notifications_schema;

-- Notifications log table - stores all user notifications
CREATE TABLE IF NOT EXISTS notifications_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT notifications_type_check CHECK (notification_type IN (
        'ACCOUNT_CREATED', 'ACCOUNT_UPDATED', 'ACCOUNT_DELETED',
        'DEPOSIT_SUCCESS', 'DEPOSIT_FAILED', 'WITHDRAWAL_SUCCESS', 'WITHDRAWAL_FAILED',
        'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'TRANSFER_FAILED',
        'PASSWORD_CHANGED', 'PROFILE_UPDATED', 'BALANCE_LOW', 'SUSPICIOUS_ACTIVITY'
    ))
);

-- Notification preferences table - stores user notification settings
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE(user_id, notification_type),
    CONSTRAINT notification_preferences_type_check CHECK (notification_type IN (
        'ACCOUNT_CREATED', 'ACCOUNT_UPDATED', 'ACCOUNT_DELETED',
        'DEPOSIT_SUCCESS', 'DEPOSIT_FAILED', 'WITHDRAWAL_SUCCESS', 'WITHDRAWAL_FAILED',
        'TRANSFER_SENT', 'TRANSFER_RECEIVED', 'TRANSFER_FAILED',
        'PASSWORD_CHANGED', 'PROFILE_UPDATED', 'BALANCE_LOW', 'SUSPICIOUS_ACTIVITY'
    ))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications_log(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications_log(notification_type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications_log(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications_log(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_notification_preferences_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_notification_preferences_updated_at 
    BEFORE UPDATE ON notification_preferences 
    FOR EACH ROW EXECUTE FUNCTION update_notification_preferences_updated_at();

-- Function to create notification
CREATE OR REPLACE FUNCTION create_notification(
    p_user_id BIGINT,
    p_notification_type VARCHAR(50),
    p_title VARCHAR(200),
    p_message TEXT
)
RETURNS BIGINT AS $$
DECLARE
    notification_id BIGINT;
BEGIN
    INSERT INTO notifications_log (user_id, notification_type, title, message)
    VALUES (p_user_id, p_notification_type, p_title, p_message)
    RETURNING id INTO notification_id;
    
    RETURN notification_id;
END;
$$ LANGUAGE plpgsql;

-- Function to mark notification as read
CREATE OR REPLACE FUNCTION mark_notification_read(notification_id BIGINT)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE notifications_log 
    SET is_read = TRUE, read_at = CURRENT_TIMESTAMP
    WHERE id = notification_id;
    
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;
