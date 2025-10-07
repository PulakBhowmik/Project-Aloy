# 🧪 COMPLETE TESTING & VERIFICATION GUIDE

## ✅ Current System Status

### Application Status: **RUNNING** ✅
- Port: 8080
- API Responding: ✅
- Database Connected: ✅

### Current Database State (October 5, 2025 @ 10:15 PM):

```
Total Apartments: 17
Available: 16
Booked: 1 (apartment #6 - test apartment)

Apartment #6: 
- booked = 0x01 (true) ✅
- status = 'RENTED' ✅
- Used for previous testing
```

### API Verification: ✅

**Test Request:**
```bash
curl http://localhost:8080/api/apartments/6
```

**Response:**
```json
{
    "apartmentId": 6,
    "title": "200B Oak Avenue, Midtown",
    "booked": true,        ← Correctly shows as booked
    "status": "RENTED",    ← Correct status
    "monthlyRate": 1134.0
}
```

## 🎯 Critical Fix Applied

### What Was Fixed:
The booking logic was moved from the POST endpoint (which SSLCommerz sandbox doesn't reliably call) to the GET endpoint (which the user actually hits after payment).

### Code Changes:
- **File:** `PaymentResultController.java`
- **Method:** `paymentSuccessDownload()` (GET endpoint)
- **Added:** Call to `completePaymentAndMarkApartmentBooked(tranId)`
- **Result:** Apartment is marked as booked when user lands on success page

## 📋 Step-by-Step Testing Instructions

### TEST 1: Book a Fresh Apartment (Recommended)

#### Step 1: Reset Test Apartment
```bash
# Choose apartment #14 for testing
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "UPDATE apartments SET booked=0, status='AVAILABLE' WHERE apartment_id=14;"
```

#### Step 2: Verify It's Available
```bash
curl http://localhost:8080/api/apartments/14 | python -m json.tool
```

**Expected Output:**
```json
{
    "apartmentId": 14,
    "title": "Apartment for share",
    "booked": false,           ← Should be false
    "status": "AVAILABLE",     ← Should be AVAILABLE
    "monthlyRate": 2003.0
}
```

#### Step 3: Open Browser and Navigate
```
http://localhost:8080/
```

#### Step 4: Book Apartment #14
1. **Find apartment #14** on the homepage
2. **Verify it shows green "AVAILABLE" badge** ✅
3. **Click "View Details"** or click on the apartment card
4. **Click "Book for Myself"** button
5. **Fill in payment details** (SSLCommerz will auto-fill in sandbox)
6. **Click "Pay Now"** or "Confirm Payment"

#### Step 5: Watch SSLCommerz Sandbox
- SSLCommerz sandbox page will open
- You may see test card numbers or just a success button
- **Click "Success"** or complete the test payment

#### Step 6: Observe the Success Flow
You should see:
1. **"Preparing your receipt..."** message
2. **PDF download starts automatically** (receipt_PAY##.pdf)
3. **"✅ Receipt Downloaded Successfully!"** message
4. **Automatic redirect** to homepage (within 1-2 seconds)

#### Step 7: Verify Frontend Update
After redirect, the homepage should show:
- **Apartment #14 with RED "BOOKED" badge** ✅
- All other apartments still have green "AVAILABLE" badges

#### Step 8: Verify Database Update
```bash
# Check apartment status
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "SELECT apartment_id, title, booked, status FROM apartments WHERE apartment_id=14;"
```

**Expected Output:**
```
+--------------+---------------------+--------+--------+
| apartment_id | title               | booked | status |
+--------------+---------------------+--------+--------+
|           14 | Apartment for share | 0x01   | RENTED |  ← Should be 0x01 (true) and RENTED
+--------------+---------------------+--------+--------+
```

#### Step 9: Verify Payment Record
```bash
# Check most recent payment
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "SELECT payment_id, transaction_id, apartment_id, status, amount FROM payment WHERE apartment_id=14 ORDER BY payment_id DESC LIMIT 1;"
```

**Expected Output:**
```
+------------+----------------+--------------+-----------+----------+
| payment_id | transaction_id | apartment_id | status    | amount   |
+------------+----------------+--------------+-----------+----------+
|         ## | PAY##          |           14 | COMPLETED | 2003.00  |  ← Status should be COMPLETED
+------------+----------------+--------------+-----------+----------+
```

#### Step 10: Verify API Response
```bash
curl http://localhost:8080/api/apartments/14 | python -m json.tool
```

**Expected Output:**
```json
{
    "apartmentId": 14,
    "title": "Apartment for share",
    "booked": true,            ← Changed to true ✅
    "status": "RENTED",        ← Changed to RENTED ✅
    "monthlyRate": 2003.0
}
```

#### Step 11: Try to Book Again (Should Fail)
1. **Hard refresh** the homepage (Ctrl + Shift + R)
2. **Find apartment #14** - should show RED "BOOKED" badge
3. **Click on apartment #14**
4. **Verify:** 
   - Booking buttons are HIDDEN ✅
   - Warning message shown: "⚠️ This apartment is already booked!"
   - Button to browse other apartments

### TEST 2: Verify Console Logs (Debugging Info)

Check the application logs to see the booking process:

```bash
tail -f /tmp/springboot.log
```

**Look for these log messages after booking:**
```
[DEBUG] Payment success GET endpoint called with tranId: PAY##
[DEBUG] Attempting to complete payment for tranId: PAY##
[DEBUG] Payment record found: ID=##, apartmentId=14, status=PENDING
[DEBUG] Payment status updated to COMPLETED
[DEBUG] Marking apartment 14 as booked
[DEBUG] Apartment found - current booked status: false, status: AVAILABLE
[SUCCESS] Apartment 14 successfully marked as BOOKED and RENTED
```

## 🔍 Troubleshooting Guide

### Issue 1: Frontend Still Shows "AVAILABLE" Badge

**Possible Causes:**
1. Browser cache not cleared
2. Frontend didn't detect the `?refresh` parameter
3. API call failed

**Solutions:**
```bash
# 1. Hard refresh browser (Ctrl + Shift + R)
# 2. Clear browser cache completely
# 3. Check browser console for errors (F12)
# 4. Manually refresh: http://localhost:8080/?refresh=1
```

### Issue 2: Database Shows booked=0x00 (false)

**Possible Causes:**
1. Payment callback failed
2. Transaction ID mismatch
3. Database transaction rolled back

**Solutions:**
```bash
# Check application logs for errors
tail -100 /tmp/springboot.log | grep ERROR

# Check if payment record exists
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "SELECT * FROM payment ORDER BY payment_id DESC LIMIT 1;"

# Verify transaction ID matches
# Check console logs for: [DEBUG] Payment success GET endpoint called with tranId: PAY##
```

### Issue 3: Payment Status Still "PENDING"

**Possible Causes:**
1. SSLCommerz redirect didn't include transaction ID
2. `completePaymentAndMarkApartmentBooked()` not called
3. Exception thrown during booking

**Solutions:**
```bash
# Check if GET endpoint was hit
grep "Payment success GET endpoint" /tmp/springboot.log

# Check for exceptions
grep "Failed to mark apartment" /tmp/springboot.log

# Manually update for testing:
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "UPDATE payment SET status='COMPLETED' WHERE transaction_id='PAY##';"
mysql -u rental_user2 -p123456 -D apartment_rental_db -e "UPDATE apartments SET booked=1, status='RENTED' WHERE apartment_id=14;"
```

### Issue 4: PDF Not Downloading

**Possible Causes:**
1. Browser blocked download
2. Payment record not found
3. Transaction ID not passed correctly

**Solutions:**
```bash
# Check browser's download settings
# Look for popup blocker notification

# Try direct receipt URL:
# http://localhost:8080/receipts/PAY## (replace ## with actual transaction ID)

# Check if receipt generation works:
curl http://localhost:8080/receipts/PAY8
```

## 📊 Expected vs Actual Comparison

### Before Fix:
```
Payment → Redirect → PDF Download → ❌ Database: booked=0, status='AVAILABLE'
                                   → ❌ Payment: status='PENDING'
                                   → ❌ Frontend: green "AVAILABLE" badge
```

### After Fix (Expected):
```
Payment → Redirect → ✅ Database Update: booked=1, status='RENTED'
                  → ✅ Payment Update: status='COMPLETED'
                  → PDF Download
                  → Redirect to homepage
                  → ✅ Frontend: red "BOOKED" badge
```

## 🎬 Video Walkthrough (Steps to Record)

If recording a test session:
1. Show homepage with all apartments
2. Click on test apartment (e.g., #14)
3. Click "Book for Myself"
4. Complete SSLCommerz payment
5. Show PDF download
6. Show automatic redirect
7. **Point to the RED "BOOKED" badge** ✅
8. Click on booked apartment to show warning
9. Show database query confirming update
10. Show API response confirming update

## 📈 Success Criteria

All of the following must be TRUE:

- [ ] Frontend shows RED "BOOKED" badge immediately after booking
- [ ] Database apartment.booked = 0x01 (true)
- [ ] Database apartment.status = 'RENTED'
- [ ] Database payment.status = 'COMPLETED'
- [ ] API returns "booked": true
- [ ] API returns "status": "RENTED"
- [ ] Booking buttons hidden on booked apartment
- [ ] Warning message shown for booked apartment
- [ ] PDF receipt downloads successfully
- [ ] Console logs show: "[SUCCESS] Apartment ## successfully marked as BOOKED and RENTED"

## 🚀 Next Steps After Successful Test

1. **Reset test apartment** for future testing:
   ```bash
   mysql -u rental_user2 -p123456 -D apartment_rental_db -e "UPDATE apartments SET booked=0, status='AVAILABLE' WHERE apartment_id=14;"
   ```

2. **Test with multiple apartments** to ensure consistency

3. **Test concurrent bookings** (two users booking same apartment simultaneously)

4. **Test in production** with real SSLCommerz account (not sandbox)

5. **Monitor logs** for any unexpected errors

## 📞 Support Information

If issues persist:
1. Check all console logs thoroughly
2. Verify MySQL connection is stable
3. Ensure port 8080 is not blocked
4. Confirm SSLCommerz sandbox credentials are valid
5. Review the `ROOT_CAUSE_AND_FIX.md` document for detailed explanation

---

**Document Created:** October 5, 2025 @ 10:16 PM  
**System Status:** ✅ Application Running, Fix Applied, Ready for Testing  
**Recommended Test:** Apartment #14 (currently AVAILABLE)
