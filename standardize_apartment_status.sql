-- ============================================
-- STANDARDIZE APARTMENT STATUSES
-- ============================================
-- Ensures all apartments have either "AVAILABLE" or "BOOKED" status
-- No other variations allowed
-- ============================================

USE apartment_rental_db;

-- Update all apartments to have consistent status
-- If booked = TRUE or booked = 1, set status = 'BOOKED'
-- If booked = FALSE or booked = 0, set status = 'AVAILABLE'

UPDATE apartments 
SET status = CASE 
    WHEN booked = TRUE OR booked = 1 THEN 'BOOKED'
    ELSE 'AVAILABLE'
END;

-- Verify: Show all apartments with their status
SELECT 
    apartment_id,
    title,
    district,
    monthly_rent,
    booked,
    status,
    CASE 
        WHEN booked = TRUE THEN '✅ Booked matches status'
        WHEN booked = FALSE THEN '✅ Available matches status'
        ELSE '❌ Mismatch'
    END as validation
FROM apartments
ORDER BY apartment_id;

-- Summary
SELECT 
    'Standardization Complete!' as message,
    COUNT(CASE WHEN status = 'AVAILABLE' THEN 1 END) as available_count,
    COUNT(CASE WHEN status = 'BOOKED' THEN 1 END) as booked_count,
    COUNT(CASE WHEN status NOT IN ('AVAILABLE', 'BOOKED') THEN 1 END) as invalid_status_count
FROM apartments;

-- ============================================
-- DONE!
-- ============================================
-- All apartments now have either:
-- - status = 'AVAILABLE' (when booked = FALSE)
-- - status = 'BOOKED' (when booked = TRUE)
-- ============================================
