# Yellow Vacate Box - Testing Guide

## Current Status

✅ **Code is correct** - The vacate box has proper yellow styling (`border-warning` and `bg-warning`)
✅ **API is working** - `/api/tenants/{userId}/booking-status` endpoint exists
✅ **Logic is correct** - Only shows for tenants with COMPLETED payments (no vacate date)

## Why the Yellow Box Might Not Be Showing

The vacate box **only appears** when ALL these conditions are met:

1. ✅ User is logged in as a **TENANT** (not owner, not guest)
2. ✅ Tenant has a **COMPLETED payment** in the database
3. ✅ Payment is linked to an **apartment**
4. ✅ Payment has **NO vacate date** (vacateDate = NULL)

## How to Make the Yellow Box Appear

### Step 1: Login as Tenant
1. Go to http://localhost:8080
2. Click **Login**
3. Use credentials: **t900** / **pass** (or any tenant account)

### Step 2: Book an Apartment
1. Find an **AVAILABLE** apartment (green tag)
2. Click on it
3. Click **"Book for Myself"** or **"Book in a Group"**
4. Click **"Pay with SSLCommerz"**
5. In the payment page:
   - Card: **4111111111111111**
   - Expiry: **12/25**
   - CVV: **123**
6. Click **Submit**
7. After success, click **"Download Receipt"**

### Step 3: Go Back to Home Page
1. Click **"Back to Search"** or navigate to http://localhost:8080
2. **The yellow vacate box should now appear on the right sidebar!**

## What the Yellow Box Looks Like

```
┌─────────────────────────────────┐
│ 🏠 Your Current Booking         │ ← Yellow header (bg-warning)
├─────────────────────────────────┤
│ Apartment Name                   │
│ Monthly Rent: ৳5000             │
│ Transaction ID: PAY123          │
│                                  │
│ [Vacate Apartment] ← Red button │
└─────────────────────────────────┘
```

## Troubleshooting

### If the box still doesn't appear:

1. **Check browser console** (F12):
   - Look for errors in red
   - Check if API call is made: `/api/tenants/{userId}/booking-status`
   - Check the response - should show `"hasBooking": true`

2. **Check localStorage**:
   - Open browser DevTools (F12)
   - Go to **Application** tab → **Local Storage**
   - Check if `user` object exists with valid `userId`

3. **Check database**:
   ```sql
   -- Find payments for tenant t900 (user ID 8)
   SELECT * FROM payment WHERE tenant_id = 8 AND status = 'COMPLETED';
   
   -- Should show a record with vacate_date = NULL
   ```

4. **Clear browser cache**:
   - Hard refresh: **Ctrl + Shift + R**
   - Or clear cache and reload

## Database Check

If you want to manually verify the payment exists:

```sql
-- Check if tenant 8 (t900) has active booking
SELECT p.*, a.title as apartment_title 
FROM payment p
JOIN apartments a ON p.apartment_id = a.apartment_id
WHERE p.tenant_id = 8 
  AND p.status = 'COMPLETED' 
  AND p.vacate_date IS NULL;
```

If this returns a row, the yellow box **should** appear!

## Code Location

- **Frontend**: `src/main/resources/static/js/app.js` (line 58-76)
- **Backend**: `src/main/java/com/example/project/aloy/controller/TenantController.java` (line 30-75)
- **HTML**: `src/main/resources/templates/index.html` (line 75)

## Server Status

- Running: ✅ PID 8092
- Port: 8080
- Latest build: Oct 9, 2025 13:36
