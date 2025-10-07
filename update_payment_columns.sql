-- Migration script to fix payment table structure
-- Run this ONLY if you already have an existing database
USE apartment_rental_db;

-- Step 1: Check if status column exists, if not add it
SELECT 'Checking payment table structure...' as message;

-- Add status column if it doesn't exist
SET @dbname = 'apartment_rental_db';
SET @tablename = 'payment';
SET @columnname = 'status';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column status already exists' AS message;",
  "ALTER TABLE payment ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add created_at column if it doesn't exist
SET @columnname = 'created_at';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column created_at already exists' AS message;",
  "ALTER TABLE payment ADD COLUMN created_at VARCHAR(100);"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Update existing records to have COMPLETED status (for already paid payments)
UPDATE payment 
SET status = 'COMPLETED' 
WHERE status IS NULL OR status = '';

-- Make apartment_id nullable since generic payments don't have apartments
ALTER TABLE payment 
MODIFY COLUMN apartment_id INT NULL;

-- Verify the changes
SELECT '✅ Payment table migration completed successfully!' as message;

-- Show table structure
DESCRIBE payment;

-- Show updated payment records
SELECT 
    payment_id,
    transaction_id,
    amount,
    status,
    payment_method,
    created_at,
    apartment_id,
    tenant_id
FROM payment 
ORDER BY payment_id DESC 
LIMIT 10;
