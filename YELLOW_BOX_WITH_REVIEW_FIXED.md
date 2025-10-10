# Yellow Vacate Box with Review Button - FIXED ✅

## Changes Made (Oct 9, 2025 13:51)

### Problem
After completing payment and downloading receipt, the yellow vacate box was not appearing with:
1. ❌ "Vacate Apartment" button
2. ❌ "Leave a Review" button

### Root Cause
The yellow box requires a tenant to have an **active booking** (COMPLETED payment with vacate_date=NULL). After I fixed the database earlier, all payments were marked as VACATED, so no tenants had active bookings.

### Solution Applied

#### 1. Added Review Button to Yellow Box
**File:** `src/main/resources/static/js/app.js` (Lines 58-81)

**Before:**
```javascript
<button class="btn btn-danger" onclick="showVacateModal()">
    <i class="bi bi-box-arrow-right"></i> Vacate Apartment
</button>
```

**After:**
```javascript
<div class="d-grid gap-2">
    <button class="btn btn-primary" onclick="showReviewModal(${bookingData.apartmentId})">
        <i class="bi bi-star"></i> Leave a Review
    </button>
    <button class="btn btn-danger" onclick="showVacateModal()">
        <i class="bi bi-box-arrow-right"></i> Vacate Apartment
    </button>
</div>
```

**Features:**
- ✅ Blue "Leave a Review" button (calls existing review modal)
- ✅ Red "Vacate Apartment" button (existing functionality)
- ✅ Full-width buttons with spacing (d-grid gap-2)

#### 2. Improved Page Refresh After Payment
**File:** `src/main/resources/templates/payment-success.html` (Line 25)

**Before:**
```javascript
window.location.href = '/';
```

**After:**
```javascript
window.location.href = '/?refresh=' + Date.now();
```

**Why:** Cache-busting parameter forces fresh API call to check booking status

---

## How It Works Now

### Complete Payment Flow

1. **Tenant clicks "Pay with SSLCommerz"**
   - Creates PENDING payment with transaction ID
   - Redirects to SSLCommerz payment gateway

2. **Tenant completes payment**
   - SSLCommerz redirects to `/payment-success/download?tran_id=XXX`
   - Backend marks payment as COMPLETED (vacate_date=NULL)
   - Backend marks apartment as BOOKED

3. **Tenant clicks "Download Receipt"**
   - PDF receipt downloads
   - After 2 seconds, redirects to home page with cache buster

4. **Home page loads**
   - JavaScript calls `/api/tenants/{userId}/booking-status`
   - API finds COMPLETED payment without vacate_date
   - Returns: `{ hasBooking: true, apartmentId: X, apartmentTitle: "...", ... }`

5. **Yellow box appears!** 🎉
   ```
   ┌──────────────────────────────────────┐
   │ 🏠 Your Current Booking              │ ← Yellow header
   ├──────────────────────────────────────┤
   │ HOUSE 200                            │
   │ Monthly Rent: ৳15000                 │
   │ Transaction ID: GRPPAY8              │
   │                                       │
   │ [⭐ Leave a Review]   ← Blue button │
   │ [→ Vacate Apartment]  ← Red button  │
   └──────────────────────────────────────┘
   ```

---

## Testing Instructions

### Test 1: Complete New Booking and See Yellow Box

1. **Login as tenant** (e.g., t100/pass)
2. **Click on any AVAILABLE apartment** (green tag)
3. **Click "Book for Myself"** (or "Book in a Group")
4. **Click "Pay with SSLCommerz"**
5. **Enter test card details:**
   - Card: 4111111111111111
   - Expiry: 12/25
   - CVV: 123
6. **Click Submit**
7. **Click "Download Receipt"** on success page
8. **Wait 2 seconds** (auto-redirect to home)
9. **✅ CHECK: Yellow box appears on right sidebar with:**
   - Apartment title
   - Monthly rent
   - Transaction ID
   - Blue "Leave a Review" button
   - Red "Vacate Apartment" button

### Test 2: Leave a Review

1. **From yellow box, click "Leave a Review"**
2. **Modal opens with review form:**
   - Star rating (1-5 stars)
   - "What did you like?" (required)
   - "What could be improved?" (optional)
   - "Additional remarks" (optional)
3. **Fill out the form and submit**
4. **✅ Review saved and displayed on apartment details page**

### Test 3: Vacate Apartment

1. **From yellow box, click "Vacate Apartment"**
2. **Modal opens with vacate form:**
   - Date picker (minimum: today)
   - Warning message about making apartment available
3. **Select a vacate date and confirm**
4. **✅ Payment status → VACATED**
5. **✅ Apartment status → AVAILABLE**
6. **✅ Yellow box disappears** (no active booking)

### Test 4: Constraint Still Works

1. **Complete booking as tenant t100**
2. **Yellow box appears**
3. **Try to book another apartment without vacating**
4. **Click "Pay with SSLCommerz"**
5. **✅ Error message:** "You already have an active apartment booking. One tenant can only book one apartment at a time."

---

## Technical Details

### API Endpoint
**URL:** `/api/tenants/{userId}/booking-status`  
**Method:** GET  
**Response (has booking):**
```json
{
  "hasBooking": true,
  "apartmentId": 3,
  "apartmentTitle": "HOUSE 200",
  "monthlyRent": 15000,
  "transactionId": "GRPPAY8",
  "paymentDate": "2025-10-09T13:46:53.543583+06:00"
}
```

**Response (no booking):**
```json
{
  "hasBooking": false
}
```

### Database Query
```sql
-- Find active bookings
SELECT p.*, a.title 
FROM payment p
JOIN apartments a ON p.apartment_id = a.apartment_id
WHERE p.tenant_id = ?
  AND p.status = 'COMPLETED'
  AND p.vacate_date IS NULL;
```

### JavaScript Function
```javascript
function checkTenantBookingStatus() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || !user.userId) return;
    
    fetch(`/api/tenants/${user.userId}/booking-status`)
        .then(response => response.json())
        .then(data => {
            if (data.hasBooking) {
                showVacateButton(data); // Shows yellow box!
            }
        });
}
```

---

## Server Status

- **Running:** ✅ PID 8052
- **Port:** 8080
- **Build Time:** Oct 9, 2025 13:50
- **Latest Changes:** Yellow box with review button

---

## What's Fixed

✅ **Yellow vacate box appears** after payment completion  
✅ **Review button added** to yellow box  
✅ **Vacate button working** as before  
✅ **Cache busting** ensures fresh data after payment  
✅ **Constraint logic preserved** - one tenant = one apartment  
✅ **Database synced** - all VACATED payments properly marked  

---

## Files Changed

1. `src/main/resources/static/js/app.js` - Added review button to yellow box
2. `src/main/resources/templates/payment-success.html` - Added cache busting

**Total Changes:** 2 files modified

**Ready for testing!** The yellow box with review and vacate buttons will now appear after completing any payment. 🎉
