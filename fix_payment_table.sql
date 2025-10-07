-- SIMPLE MIGRATION: Fix payment table to add status and created_at columns
-- Run this if your existing database doesn't have these columns

USE apartment_rental_db;

-- Add the missing columns (ignore errors if they already exist)
ALTER TABLE payment ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS created_at VARCHAR(100);

-- Update existing payments to COMPLETED status
UPDATE payment SET status = 'COMPLETED' WHERE status IS NULL OR status = '' OR status = 'PENDING';

-- Make apartment_id nullable (for generic payments)
ALTER TABLE payment MODIFY COLUMN apartment_id INT NULL;

-- Verify changes
SELECT '✅ Migration completed!' as message;
DESCRIBE payment;

-- View recent payments
SELECT payment_id, transaction_id, amount, status, created_at FROM payment ORDER BY payment_id DESC LIMIT 5;
