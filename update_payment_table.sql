-- Add a unique index to transaction_id for fast lookup and integrity
ALTER TABLE payment ADD UNIQUE INDEX idx_transaction_id (transaction_id);

-- If your JPA entity uses 'payments' as the table name, rename the table:
-- RENAME TABLE payment TO payments;

-- You can run these statements in your MySQL client or migration tool.
