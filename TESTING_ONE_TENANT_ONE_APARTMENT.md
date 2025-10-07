# Testi### Step 1: Apply Database Setup (REQUIRED)

Before starting the application, set up the database with the constraint:

```bash
# For fresh setup (recommended - drops and recreates database)
mysql -u rental_user2 -p < "project aloy sql.sql"

# Password: 123456
```

**What this does:**
- Creates all database tables
- Creates database triggers to enforce one-tenant-one-apartment rule
- Adds performance indexes
- Cleans up any existing duplicate bookings
- Inserts sample data

**For existing database only (add constraint without dropping):**
Extract and run the "ONE TENANT ONE APARTMENT CONSTRAINT" section from `project aloy sql.sql`enant One Apartment Constraint

## Prerequisites
1. ✅ Application built successfully (`./mvnw clean package -DskipTests`)
2. ⚠️ **Database migration MUST be applied** (see Step 1 below)
3. Application server ready to start

## Step 1: Apply Database Constraint (REQUIRED)

Before starting the application, apply the database migration:

```bash
# Connect to MySQL and run the migration script
mysql -u rental_user2 -p apartment_rental_db < one_tenant_one_apartment_constraint.sql

# Password: 123456
```

**What this does:**
- Creates database triggers to enforce one-tenant-one-apartment rule
- Adds performance indexes
- Cleans up any existing duplicate bookings

**Verify triggers are created:**
```sql
USE apartment_rental_db;
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'apartment_rental_db'
  AND EVENT_OBJECT_TABLE = 'payment';
```

Expected output:
```
prevent_multiple_bookings_insert | INSERT | payment
prevent_multiple_bookings_update | UPDATE | payment
```

## Step 2: Start Application

```bash
cd "/d/My Downloads/project aloy/project-aloy"
java -jar target/apartment-rental-system-0.0.1-SNAPSHOT.jar
```

Wait for: `Started ProjectAloyApplication in X seconds`

Application URL: http://localhost:8080

## Step 3: Test Scenarios

### Scenario 1: First Booking (Should Succeed) ✅

1. **Login as Tenant**
   - Email: (use existing tenant or create new one)
   - Role: tenant

2. **Browse Apartments**
   - Go to http://localhost:8080
   - View available apartments
   - Should see "AVAILABLE" badge on apartments

3. **Make First Booking**
   - Click on any available apartment
   - Click "View Details"
   - Click "Book for Myself"
   - Fill payment form
   - Complete SSLCommerz payment (use sandbox test cards)

4. **Expected Result:**
   - Payment successful
   - Apartment status changes to "BOOKED"
   - Receipt generated

### Scenario 2: Attempt Second Booking (Should Fail) ❌

1. **Check Homepage Banner**
   - After first booking, reload homepage
   - Should see blue info banner at top:
     > "Active Booking: You already have a booking for [Apartment Name]"

2. **Try to Book Another Apartment**
   - Click on a different available apartment
   - Click "View Details"
   - Should see **warning banner** in modal:
     > "⚠ Booking Limit Reached: You already have an active booking..."
   - Booking buttons should be **DISABLED** (grayed out)

3. **Try to Click Disabled Button**
   - Buttons won't respond (disabled)
   - This is frontend protection

4. **Try to Force Payment Modal (Developer Tools)**
   - If you bypass disabled buttons using dev tools
   - Alert will popup:
     > "You already have an active apartment booking..."

### Scenario 3: Backend Protection (API Test) ❌

Test using curl or Postman:

```bash
# Replace TENANT_ID with actual tenant ID that already has a booking
curl -X POST http://localhost:8080/api/payments/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1500,
    "name": "Test User",
    "email": "test@example.com",
    "phone": "01700000000",
    "apartmentId": 456,
    "tenantId": TENANT_ID
  }'
```

**Expected Response:**
```json
{
  "error": "You already have an active apartment booking. One tenant can only book one apartment at a time."
}
```
**HTTP Status:** 409 Conflict

### Scenario 4: Database Protection (Direct SQL) ❌

Try to insert a second completed payment directly:

```sql
USE apartment_rental_db;

-- First, find a tenant who already has a completed booking
SELECT tenant_id, apartment_id, status FROM payment WHERE status = 'COMPLETED' LIMIT 1;

-- Try to insert another completed payment for the same tenant
INSERT INTO payment (tenant_id, apartment_id, amount, payment_method, status, transaction_id, created_at)
VALUES (
  123,  -- Use tenant_id from above query
  789,  -- Different apartment_id
  1200.00,
  'SSLCommerz',
  'COMPLETED',
  'TEST_TXN_123',
  NOW()
);
```

**Expected Result:**
```
ERROR 1644 (45000): Tenant already has an active apartment booking. One tenant can only book one apartment at a time.
```

The trigger blocks the INSERT!

### Scenario 5: Check Booking Status API ✅

```bash
# Replace TENANT_ID with actual tenant ID
curl http://localhost:8080/api/tenants/TENANT_ID/booking-status
```

**Response (Has Booking):**
```json
{
  "hasBooking": true,
  "apartmentId": 123,
  "paymentId": 456,
  "transactionId": "PAY456",
  "amount": 1200.00,
  "apartmentTitle": "Modern Downtown Studio",
  "apartmentAddress": "100A Main Street, Downtown",
  "monthlyRent": 1200.00
}
```

**Response (No Booking):**
```json
{
  "hasBooking": false
}
```

## Step 4: Verify Database State

### Check all tenant bookings:
```sql
SELECT 
    u.user_id,
    u.name as tenant_name,
    u.email,
    COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END) as completed_bookings,
    COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END) as pending_bookings,
    GROUP_CONCAT(
        CASE WHEN p.status = 'COMPLETED' 
        THEN CONCAT('Apt#', p.apartment_id) 
        END SEPARATOR ', '
    ) as booked_apartments
FROM users u
LEFT JOIN payment p ON u.user_id = p.tenant_id AND p.apartment_id IS NOT NULL
WHERE u.role = 'tenant'
GROUP BY u.user_id, u.name, u.email
ORDER BY completed_bookings DESC;
```

**Expected:** No tenant should have `completed_bookings > 1`

### Check for constraint violations:
```sql
-- This should return ZERO rows
SELECT 
    p.tenant_id,
    COUNT(*) as booking_count
FROM payment p
WHERE p.status = 'COMPLETED' 
  AND p.apartment_id IS NOT NULL
GROUP BY p.tenant_id
HAVING booking_count > 1;
```

## Troubleshooting

### Issue: Booking still allowed after first booking

**Check:**
1. Did you apply the database migration?
2. Is the backend code compiled (rebuild)?
3. Clear browser cache and reload
4. Check browser console for JavaScript errors

**Fix:**
```bash
# Rebuild application
./mvnw clean package -DskipTests

# Reapply database setup (or just the constraint section)
mysql -u rental_user2 -p < "project aloy sql.sql"

# Restart application
java -jar target/apartment-rental-system-0.0.1-SNAPSHOT.jar
```

### Issue: Frontend doesn't show warning banner

**Check browser console:**
```javascript
// In browser console, check:
console.log(userHasBooking);
console.log(userBookingDetails);
```

**Manual refresh of booking status:**
```javascript
// In browser console:
checkTenantBookingStatus();
```

### Issue: Database trigger not working

**Verify triggers exist:**
```sql
SHOW TRIGGERS FROM apartment_rental_db WHERE `Table` = 'payment';
```

**Re-create triggers:**
```bash
# Run the constraint section from the main SQL file
mysql -u rental_user2 -p < "project aloy sql.sql"
```

## Test Data Cleanup

To reset a tenant's bookings for testing:

```sql
-- View tenant's payments
SELECT * FROM payment WHERE tenant_id = YOUR_TENANT_ID;

-- Option 1: Delete all payments (use with caution!)
DELETE FROM payment WHERE tenant_id = YOUR_TENANT_ID;

-- Option 2: Mark as FAILED (keeps history)
UPDATE payment 
SET status = 'FAILED' 
WHERE tenant_id = YOUR_TENANT_ID AND status = 'COMPLETED';

-- Also unbook the apartment
UPDATE apartments 
SET booked = 0, status = 'AVAILABLE' 
WHERE apartment_id IN (
  SELECT DISTINCT apartment_id 
  FROM payment 
  WHERE tenant_id = YOUR_TENANT_ID
);
```

## Success Criteria

✅ **Test passes if:**
1. First booking completes successfully
2. Homepage shows info banner after first booking
3. Second apartment's booking buttons are disabled
4. Alert shows when attempting to bypass disabled buttons
5. API returns 409 error for second booking attempt
6. Database trigger blocks direct SQL inserts
7. No tenant has multiple COMPLETED bookings in database

❌ **Test fails if:**
1. Tenant can complete second booking
2. No warning/error messages shown
3. Database allows multiple COMPLETED payments per tenant

## Console Logs to Watch

**Backend logs (successful constraint enforcement):**
```
[DEBUG CONSTRAINT] Checking if tenant 123 already has a booking...
[CONSTRAINT CHECK] Tenant 123 already has active booking: Payment ID=456, Apartment ID=789
[BLOCKED] Tenant 123 already has an active booking - payment initiation denied
```

**Frontend logs (browser console):**
```javascript
[INFO] User already has a booking: {hasBooking: true, apartmentId: 789, ...}
[DEBUG] showPaymentModal called with apartmentId: 456
// Alert should popup before modal shows
```

## Reference

See detailed implementation documentation:
- **ONE_TENANT_ONE_APARTMENT_IMPLEMENTATION.md** - Full technical documentation
- **project aloy sql.sql** - Main database setup with constraint included

---

**Last Updated:** October 7, 2025  
**Status:** ✅ Ready for Testing
