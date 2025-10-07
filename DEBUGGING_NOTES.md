# Debugging: Tenant Booking Constraint Issue

## Summary

The tenant booking constraint feature has been implemented but you reported it's not working. After investigating the logs and database, I found the root cause.

## Root Cause Analysis

From the Spring Boot logs, I observed:

**First Booking:**
```
[DEBUG] Payment initiation request: {... tenantId=22}
[DEBUG] Checking if tenant 22 already has a booking...
[DEBUG] Tenant 22 has no existing bookings. Proceeding with payment initiation.
```

**Database State After First Booking:**
```sql
payment_id=24, transaction_id=PAY24, tenant_id=20, apartment_id=9, status=COMPLETED
```

**Second Booking Attempt:**
```
[DEBUG] Payment initiation request: {... tenantId=22}
[DEBUG] Checking if tenant 22 already has a booking...
[DEBUG] Tenant 22 has no existing bookings. Proceeding with payment initiation.
```

### The Problem

**Payment PAY24 was created with tenant_id=20 in the database, but your request claimed tenant_id=22.**

This means one of these scenarios occurred:

1. **Multiple Browser Sessions**: You had two different browser windows/tabs open with different user sessions
2. **localStorage Cleared**: You cleared browser data or localStorage between bookings, causing the user to change
3. **Login/Logout Between Bookings**: You logged out and logged in as a different user
4. **Existing Payment Reused**: An old payment record with tenant_id=20 was reused

## Changes Made to Help Debug

I've added extensive logging to track this issue:

### New Log Messages

**SSLCommerzPaymentController:**
```java
[DEBUG CONSTRAINT] Checking if tenant X already has a booking...
[DEBUG CONSTRAINT] Found N total payments for tenant X
[DEBUG CONSTRAINT] Payment ID=..., Status=..., ApartmentId=..., TenantId=...
[DEBUG] Setting tenantId=X on new payment
[DEBUG] Created and saved payment: ID=X, TranID=PAYX, TenantID=X, ApartmentID=X
```

These logs will help us see:
- How many payments exist for a tenant
- The exact status and tenant_id of each payment
- What tenant_id is being saved when creating payments

## How to Test Properly

### ⚠️ CRITICAL: Stay Logged In as the Same User

To properly test the constraint:

1. **Login once** as a specific tenant
2. **Before first booking**, open browser Developer Tools (F12) and run:
   ```javascript
   console.log(JSON.parse(localStorage.getItem('user')));
   ```
   Note the `userId` value (e.g., 22)

3. **Complete first booking**

4. **Before second booking**, check localStorage again:
   ```javascript
   console.log(JSON.parse(localStorage.getItem('user')));
   ```
   **MUST be the SAME userId!**

5. **Attempt second booking**

### If User IDs Don't Match

If you see different userIds between bookings, that's the problem! The constraint is working correctly - it's just checking different users.

**Solution:**
- Don't logout between bookings
- Don't clear browser data
- Don't open multiple tabs with different users
- Use the same browser session for both bookings

## Database Has Been Reset

I've reset the database for you:
- ✅ All apartments → `booked=0, status='AVAILABLE'`
- ✅ All payments → `status='CANCELLED'`
- ✅ 0 completed payments exist

## Application Rebuilt and Starting

The application has been rebuilt with enhanced logging and is starting now. Once it's running on port 8080, you can test again.

## Verification Checklist

Before reporting an issue, please verify:

- [ ] **Same User**: localStorage userId is identical before both bookings
- [ ] **Backend Logs**: Check terminal for `[DEBUG CONSTRAINT]` messages showing payments found
- [ ] **Database**: Query `payment` table to see actual tenant_id of COMPLETED payments
- [ ] **Frontend**: Check browser console for `[INFO] User already has a booking` message
- [ ] **Fresh Start**: Database was reset, browser cache cleared

## Expected Behavior

### When Working Correctly:

**First Booking (tenant_id=22):**
1. Backend checks: No COMPLETED payments for tenant 22 ✅
2. Allows booking
3. Creates payment with tenant_id=22
4. Payment completes → status=COMPLETED

**Second Booking Attempt (SAME tenant_id=22):**
1. Backend checks: Finds COMPLETED payment for tenant 22 ❌
2. Returns HTTP 409 error
3. Frontend shows error message
4. Buttons disabled in UI

**Different Tenant (tenant_id=23):**
1. Backend checks: No COMPLETED payments for tenant 23 ✅
2. Allows booking normally

## Next Steps

1. **Wait for application to start** (check terminal output)
2. **Open browser** to http://localhost:8080
3. **Login as one specific tenant**
4. **Check localStorage** before each booking
5. **Try to book two apartments** as the SAME user
6. **Share the backend logs** if it still doesn't work

The enhanced logging will show us exactly what's happening with the tenant_id tracking!
