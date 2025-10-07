# One Tenant One Apartment Constraint Implementation

## Overview
This document describes the implementation of a critical business rule: **One tenant can only book one apartment at a time**. This constraint prevents tenants from making multiple simultaneous bookings.

## Problem Statement
Previously, the system allowed a single tenant to:
- Book multiple apartments simultaneously
- Make payments for different apartments
- Have multiple active bookings

This caused issues with:
- Resource allocation
- Payment tracking
- Business logic integrity

## Solution Architecture

### 1. Database Layer (MySQL)

#### Database Triggers
Two triggers enforce the constraint at the database level:

**`prevent_multiple_bookings_insert`**
- Fires BEFORE INSERT on the `payment` table
- Checks if tenant already has a COMPLETED payment for an apartment
- Raises SQL error (SQLSTATE 45000) if constraint violated

**`prevent_multiple_bookings_update`**
- Fires BEFORE UPDATE on the `payment` table
- Checks when payment status changes from non-COMPLETED to COMPLETED
- Prevents updating if tenant already has another COMPLETED booking

#### Database Index
```sql
CREATE INDEX idx_tenant_status_apartment 
ON payment(tenant_id, status, apartment_id);
```
This index optimizes constraint checking queries.

#### Migration Script
Location: `project aloy sql.sql` (integrated into main database setup)

This script includes:
1. All original database table creation
2. Cleanup of any existing duplicate bookings
3. Database triggers for constraint enforcement
4. Performance indexes
5. Sample data
6. Verification queries

**To apply the full database setup with constraint:**
```bash
mysql -u rental_user2 -p < "project aloy sql.sql"
```

**To apply to existing database (constraint only):**
Extract and run only the "ONE TENANT ONE APARTMENT CONSTRAINT" section from the file.

### 2. Backend Layer (Spring Boot)

#### PaymentService Enhancement
**New Method: `tenantHasActiveBooking(Long tenantId)`**
- Centralized method to check if tenant has an active booking
- Returns `true` if tenant has any COMPLETED payment with an apartment
- Used throughout the application for consistent checking

**Updated Method: `completePaymentAndMarkApartmentBooked(String tranId)`**
- Added pre-completion check before marking payment as COMPLETED
- Throws RuntimeException if tenant already has a completed booking
- Prevents race conditions during payment completion

Location: `src/main/java/com/example/project/aloy/service/PaymentService.java`

#### SSLCommerzPaymentController Enhancement
**Updated `/api/payments/initiate` endpoint:**
- Added `@Autowired PaymentService` dependency
- Uses `paymentService.tenantHasActiveBooking()` for early validation
- Returns HTTP 409 (Conflict) with error message if tenant has booking
- Additional check for recent PENDING payments (within 10 minutes)
- Prevents double-clicking and multiple simultaneous payment attempts

Location: `src/main/java/com/example/project/aloy/controller/SSLCommerzPaymentController.java`

### 3. Frontend Layer (JavaScript)

#### Global State Management
```javascript
let userHasBooking = false;
let userBookingDetails = null;
```
These variables track the current tenant's booking status.

#### Booking Status Check
**Function: `checkTenantBookingStatus()`**
- Called on page load
- Fetches tenant's booking status from `/api/tenants/{tenantId}/booking-status`
- Updates global state
- Shows/removes booking constraint banner

#### UI Components

**Booking Constraint Banner**
- Appears at top of page when tenant has active booking
- Shows apartment name and constraint message
- Can be dismissed but status remains enforced
- Functions: `showBookingConstraintBanner()`, `removeBookingConstraintBanner()`

**Enhanced Apartment Details Modal**
- Shows warning banner if tenant already has booking
- Disables booking buttons if:
  - Tenant already has a booking, OR
  - Apartment is already booked
- Displays apartment status badge (AVAILABLE/BOOKED)
- Function: `showApartmentDetailsModal()`

**Payment Modal Protection**
- Pre-checks `userHasBooking` before showing payment form
- Shows alert with current booking details if constraint violated
- Function: `showPaymentModal()`

**Error Handling in Payment Flow**
- Catches HTTP 409 responses from backend
- Displays detailed error messages
- Updates `userHasBooking` flag on error
- Prevents retry attempts with clear messaging

Location: `src/main/resources/static/js/app.js`

## Constraint Enforcement Points

### Point 1: Frontend Pre-Check
- **When:** Before showing payment modal
- **Method:** `showPaymentModal()` checks `userHasBooking`
- **Action:** Show alert and block modal

### Point 2: Payment Initiation
- **When:** POST to `/api/payments/initiate`
- **Method:** `SSLCommerzPaymentController.initiatePayment()`
- **Action:** Return 409 Conflict if tenant has booking

### Point 3: Payment Completion
- **When:** Updating payment status to COMPLETED
- **Method:** `PaymentService.completePaymentAndMarkApartmentBooked()`
- **Action:** Throw exception if tenant has booking

### Point 4: Database Trigger
- **When:** INSERT/UPDATE on payment table with status=COMPLETED
- **Method:** Database triggers
- **Action:** Raise SQL error (SQLSTATE 45000)

## Error Messages

### Frontend
- **Pre-modal check:** "You already have an active apartment booking.\n\nOne tenant can only book one apartment at a time.\n\nYour current booking: [Apartment Name]\n\nPlease contact support if you need to change your booking."

- **Payment error:** "Booking Constraint: You already have an active apartment booking. You can only book one apartment at a time. Please contact support to change your booking."

### Backend
- **HTTP 409 Response:** `{"error": "You already have an active apartment booking. One tenant can only book one apartment at a time."}`

- **Service Exception:** `"Tenant already has an active apartment booking. One tenant can only book one apartment at a time."`

### Database
- **SQL Error:** `SQLSTATE 45000: "Tenant already has an active apartment booking. One tenant can only book one apartment at a time."`

## Testing

### Test Case 1: Prevent Multiple Bookings
1. Login as tenant
2. Book apartment A (complete payment)
3. Try to book apartment B
4. **Expected:** Error message, booking blocked

### Test Case 2: View Booking Status
1. Login as tenant with existing booking
2. Check homepage
3. **Expected:** Info banner showing current booking

### Test Case 3: Disabled Booking Buttons
1. Login as tenant with existing booking
2. Open any apartment details
3. **Expected:** Warning message, disabled booking buttons

### Test Case 4: Backend Protection
1. Use API client (Postman/curl)
2. POST to `/api/payments/initiate` with tenantId that has booking
3. **Expected:** HTTP 409 response with error

### Test Case 5: Database Protection
1. Manually try to INSERT second COMPLETED payment for same tenant
2. **Expected:** SQL error from trigger

## Database Verification Queries

### Check for Multiple Bookings
```sql
SELECT 
    u.user_id,
    u.name as tenant_name,
    COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END) as completed_bookings,
    GROUP_CONCAT(
        CASE WHEN p.status = 'COMPLETED' 
        THEN CONCAT('Apt:', p.apartment_id, ' (', p.transaction_id, ')') 
        END SEPARATOR ', '
    ) as booked_apartments
FROM users u
LEFT JOIN payment p ON u.user_id = p.tenant_id AND p.apartment_id IS NOT NULL
WHERE u.role = 'tenant'
GROUP BY u.user_id, u.name
HAVING completed_bookings > 1;
```

### View All Tenant Bookings
```sql
SELECT 
    p.payment_id,
    p.transaction_id,
    p.status,
    p.amount,
    u.name as tenant_name,
    a.title as apartment_name
FROM payment p
LEFT JOIN users u ON p.tenant_id = u.user_id
LEFT JOIN apartments a ON p.apartment_id = a.apartment_id
WHERE p.apartment_id IS NOT NULL
ORDER BY p.tenant_id, p.status, p.payment_id;
```

## Rollback Plan

If you need to remove the constraint:

```sql
-- Remove triggers
DROP TRIGGER IF EXISTS prevent_multiple_bookings_insert;
DROP TRIGGER IF EXISTS prevent_multiple_bookings_update;

-- Remove index
DROP INDEX IF EXISTS idx_tenant_status_apartment ON payment;
```

Then revert the backend and frontend code changes.

## API Endpoints

### GET `/api/tenants/{tenantId}/booking-status`
**Purpose:** Check if tenant has active booking

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

### POST `/api/payments/initiate`
**Purpose:** Initiate payment (includes constraint check)

**Request:**
```json
{
  "amount": 1200.00,
  "name": "John Tenant",
  "email": "john@example.com",
  "phone": "01700000000",
  "apartmentId": 123,
  "tenantId": 789
}
```

**Response (Success):**
```json
{
  "status": "SUCCESS",
  "GatewayPageURL": "https://sandbox.sslcommerz.com/gwprocess/...",
  "tran_id": "PAY456",
  ...
}
```

**Response (Constraint Violation):**
```json
{
  "error": "You already have an active apartment booking. One tenant can only book one apartment at a time."
}
```
HTTP Status: 409 Conflict

## Performance Considerations

- Database index `idx_tenant_status_apartment` optimizes constraint checking
- Frontend caches booking status in memory (`userHasBooking`)
- Backend method `tenantHasActiveBooking()` is efficient (indexed query)
- Triggers execute in microseconds (minimal overhead)

## Future Enhancements

1. **Booking Management Page**
   - Allow tenants to view their current booking
   - Option to cancel/change booking with owner approval

2. **Admin Override**
   - Allow admins to bypass constraint for special cases
   - Audit logging for constraint overrides

3. **Time-based Booking**
   - Support booking end dates
   - Auto-release completed bookings after lease ends

4. **Waiting List**
   - Allow tenants to join waiting list for apartments
   - Notify when booking slot becomes available

## Summary

The one tenant one apartment constraint is now enforced at **four levels**:
1. ✅ **Frontend:** Pre-checks and UI disabling
2. ✅ **Controller:** Payment initiation validation
3. ✅ **Service:** Payment completion validation
4. ✅ **Database:** Triggers and constraints

This multi-layered approach ensures data integrity and prevents constraint violations even under edge cases like concurrent requests, direct database access, or API manipulation.

---

**Implementation Date:** October 7, 2025  
**Status:** ✅ Completed  
**Files Modified:**
- `one_tenant_one_apartment_constraint.sql` (NEW)
- `src/main/java/com/example/project/aloy/service/PaymentService.java`
- `src/main/java/com/example/project/aloy/controller/SSLCommerzPaymentController.java`
- `src/main/resources/static/js/app.js`
- `ONE_TENANT_ONE_APARTMENT_IMPLEMENTATION.md` (THIS FILE)
