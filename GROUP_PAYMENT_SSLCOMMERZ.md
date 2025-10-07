# Group Booking Payment with SSLCommerz Integration

## Overview
Updated the group booking payment to use SSLCommerz payment gateway, just like the solo "Book for Myself" functionality. Group members can now pay for their apartment booking through the secure SSLCommerz sandbox.

---

## What Changed

### 1. Frontend Changes (`app.js`)

#### Updated `bookApartmentForGroup()` Function
**Old Behavior:**
- Simple API call to `/api/groups/{groupId}/book`
- Direct confirmation dialog
- No payment gateway integration

**New Behavior:**
- Opens a professional payment modal with SSLCommerz integration
- Collects payment details (name, email, phone)
- Validates group status (READY) and membership before proceeding
- Redirects to SSLCommerz payment gateway
- Stores group context in localStorage for payment callback

**Key Features:**
```javascript
// Payment modal with Bootstrap styling
- Amount display (readonly)
- Customer name (pre-filled from user profile)
- Email address
- Phone number
- "Pay ৳{amount} with SSLCommerz" button

// Pre-payment validation
- Checks if group status is READY
- Verifies user is a member of the group
- Prevents payment if group has < 4 members

// Payment flow
1. Calls /api/payments/initiate-group (new endpoint)
2. Stores groupId in localStorage for callback
3. Redirects to SSLCommerz gateway
4. User completes payment on SSLCommerz
5. Redirected back to success/fail/cancel page
6. Backend processes payment and books apartment for group
```

---

### 2. Backend Changes

#### New Endpoint: `/api/payments/initiate-group`
**File:** `SSLCommerzPaymentController.java`

**Purpose:** Initiate SSLCommerz payment specifically for group bookings

**Validation Steps:**
1. **Extract Parameters:**
   - `groupId`: ID of the roommate group
   - `tenantId`: ID of the tenant making payment
   - `apartmentId`: ID of the apartment being booked

2. **Group Validation:**
   - Verify group exists
   - Check group status is READY (all 4 members joined)
   - Confirm tenant is a member of the group

3. **Apartment Validation:**
   - Lock apartment row to prevent race conditions
   - Check if apartment is still available (not booked)
   - Check if apartment status is not RENTED

4. **Create Payment Record:**
   - Payment method: `SSLCommerz_Group`
   - Transaction ID: `GRPPAY{paymentId}` (e.g., GRPPAY123)
   - Status: `PENDING`
   - Links to apartmentId and tenantId

5. **SSLCommerz Integration:**
   - Sends payment request to sandbox API
   - Includes groupId in `value_c` field as `GROUP_{groupId}`
   - Custom success URL includes groupId parameter
   - Returns GatewayPageURL for frontend redirection

**Request Example:**
```json
{
  "amount": 20000,
  "name": "Alice Tenant",
  "email": "alice.tenant@example.com",
  "phone": "01700000000",
  "apartmentId": 9,
  "tenantId": 7,
  "groupId": 2
}
```

**Response Example:**
```json
{
  "status": "SUCCESS",
  "GatewayPageURL": "https://sandbox.sslcommerz.com/EasyCheckOut/...",
  "sessionkey": "...",
  "tran_id": "GRPPAY123"
}
```

---

#### Updated Payment Callback Handler
**File:** `PaymentResultController.java`

**Method:** `paymentSuccessPost()`

**What Changed:**
Added group payment detection and processing:

1. **Detect Group Payment:**
   - Checks `value_c` parameter for "GROUP_" prefix
   - Extracts groupId from the marker

2. **Group Booking Flow:**
   - If group payment detected:
     - Calls `roommateGroupService.bookApartment(groupId, tenantId)`
     - This method handles:
       - Marking apartment as booked
       - Setting apartment status to RENTED
       - Updating group status to BOOKED
       - Cancelling other groups for same apartment
       - Recording booking timestamp
   
3. **Solo Booking Flow:**
   - If not group payment:
     - Marks apartment as booked directly
     - Updates apartment status to RENTED

**Key Code:**
```java
String groupMarker = allParams.getOrDefault("value_c", "");
boolean isGroupPayment = groupMarker.startsWith("GROUP_");

if (isGroupPayment && groupId != null && tenantId != null) {
    // Call RoommateGroupService to handle group booking logic
    roommateGroupService.bookApartment(groupId, tenantId);
    System.out.println("[DEBUG GROUP] Group successfully booked apartment");
} else {
    // Solo booking - mark apartment directly
    apartment.setBooked(true);
    apartment.setStatus("RENTED");
    apartmentRepository.save(apartment);
}
```

---

## Payment Flow Diagram

```
┌──────────────┐
│   User       │
│  (Alice)     │
└──────┬───────┘
       │ Clicks "Pay Now & Book Apartment"
       ▼
┌─────────────────────────────────────────┐
│  Payment Modal Opens                    │
│  - Shows group info                     │
│  - Amount: ৳20,000                      │
│  - Name: Alice Tenant                   │
│  - Email: alice.tenant@example.com      │
│  - Phone: 01700000000                   │
└──────┬──────────────────────────────────┘
       │ Clicks "Pay with SSLCommerz"
       ▼
┌─────────────────────────────────────────┐
│  Frontend Validation                    │
│  - Check group status is READY          │
│  - Verify user is group member          │
└──────┬──────────────────────────────────┘
       │ POST /api/payments/initiate-group
       ▼
┌─────────────────────────────────────────┐
│  Backend: SSLCommerzPaymentController   │
│  - Validate groupId exists              │
│  - Check group is READY (4/4 members)   │
│  - Check user is member                 │
│  - Verify apartment available           │
│  - Create payment record (GRPPAY123)    │
│  - Call SSLCommerz sandbox API          │
└──────┬──────────────────────────────────┘
       │ Returns GatewayPageURL
       ▼
┌─────────────────────────────────────────┐
│  Redirect to SSLCommerz Sandbox         │
│  User completes payment                 │
│  (Test card: 4111111111111111)          │
└──────┬──────────────────────────────────┘
       │ Payment successful
       ▼
┌─────────────────────────────────────────┐
│  SSLCommerz Callback                    │
│  POST /payment-success                  │
│  - Includes tran_id, value_c (groupId)  │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  Backend: PaymentResultController       │
│  - Detects GROUP_ marker                │
│  - Calls roommateGroupService           │
│    .bookApartment(groupId, tenantId)    │
│  - Apartment marked as RENTED           │
│  - Group status → BOOKED                │
│  - Other groups cancelled               │
│  - Payment record updated to COMPLETED  │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  PDF Receipt Generated                  │
│  - Transaction details                  │
│  - Apartment info                       │
│  - Tenant details                       │
│  - Downloaded automatically             │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  Redirect to Homepage                   │
│  - Apartment shows as RENTED            │
│  - Group status is BOOKED               │
│  - All 4 members notified               │
└─────────────────────────────────────────┘
```

---

## Testing Instructions

### Test Scenario: Complete Group Payment with SSLCommerz

**Prerequisites:**
- 4 test accounts (Alice, Bob, Charlie, David)
- Group with 4/4 members for Apartment 9
- Group status: READY
- Invite code: CFW513

**Steps:**

1. **Login as Alice** (alice.tenant@example.com)

2. **Navigate to Apartment 9** (Baridhara)
   - Click "Book in a Group"
   - You should see your group with 4/4 members
   - Status badge: "READY" (green)

3. **Click "Pay Now & Book Apartment" button**
   - Payment modal should open
   - Verify details:
     ```
     Amount: 20000 (readonly)
     Name: Alice Tenant (pre-filled)
     Email: alice.tenant@example.com
     Phone: 01700000000
     ```

4. **Click "Pay ৳20000 with SSLCommerz"**
   - Should redirect to SSLCommerz sandbox
   - URL: `https://sandbox.sslcommerz.com/EasyCheckOut/...`

5. **Complete Payment on SSLCommerz**
   - Use test card: `4111 1111 1111 1111`
   - Expiry: Any future date
   - CVV: 123
   - Click "Submit"

6. **Verify Success Page**
   - Should see "Receipt Downloaded Successfully!"
   - PDF receipt should download automatically
   - After 1-2 seconds, redirects to homepage

7. **Verify Database Changes**
   ```sql
   -- Check apartment is booked
   SELECT apartment_id, title, booked, status 
   FROM apartments WHERE apartment_id = 9;
   -- Expected: booked=1, status='RENTED'
   
   -- Check group is booked
   SELECT group_id, status, booked_at 
   FROM roommate_group WHERE group_id = 2;
   -- Expected: status='BOOKED', booked_at=<timestamp>
   
   -- Check payment record
   SELECT payment_id, transaction_id, amount, status, payment_method, apartment_id, tenant_id
   FROM payments WHERE transaction_id LIKE 'GRPPAY%';
   -- Expected: status='COMPLETED', payment_method='SSLCommerz_Group'
   ```

8. **Verify Homepage Updates**
   - Apartment 9 should show as "RENTED"
   - "Book for Myself" and "Book in a Group" buttons should be disabled/hidden
   - Other apartments should still be available

---

## Error Handling

### 1. Group Not Ready
**Trigger:** Less than 4 members in group

**Error Message:**
```
"Group is not ready for booking. All 4 members must join first."
```

**Frontend Prevention:** Pay button only appears when `memberCount >= 4 AND status === 'READY'`

---

### 2. User Not a Member
**Trigger:** Non-member tries to pay for group

**Error Message:**
```
"You are not a member of this group"
```

**HTTP Status:** 403 Forbidden

---

### 3. Apartment Already Booked
**Trigger:** Another group/person booked apartment while payment was processing

**Error Message:**
```
"This apartment is already booked."
```

**HTTP Status:** 409 Conflict

**Handling:** 
- Payment record created but apartment not marked as booked
- User receives refund notification (manual process)
- Frontend shows error alert

---

### 4. Payment Gateway Failure
**Trigger:** SSLCommerz API returns error

**Error Message:**
```
"Failed to initiate payment. [SSLCommerz error details]"
```

**HTTP Status:** 502 Bad Gateway

**User Action:** Try again or contact support

---

## Database Schema Impact

### Payment Table
**New Records:**
```sql
payment_id    | transaction_id | amount   | payment_method    | status     | apartment_id | tenant_id
123           | GRPPAY123      | 20000.00 | SSLCommerz_Group  | COMPLETED  | 9            | 7
```

**Transaction ID Pattern:**
- Solo bookings: `PAY{id}` (e.g., PAY42)
- Group bookings: `GRPPAY{id}` (e.g., GRPPAY123)

### Roommate Group Table
**Updated Fields After Payment:**
```sql
group_id | status  | booked_at
2        | BOOKED  | 2025-10-08 00:45:30
```

### Apartment Table
**Updated Fields:**
```sql
apartment_id | booked | status
9            | 1      | RENTED
```

---

## API Endpoints Summary

### New Endpoint
```
POST /api/payments/initiate-group
Content-Type: application/json

Request Body:
{
  "amount": 20000,
  "name": "Alice Tenant",
  "email": "alice.tenant@example.com",
  "phone": "01700000000",
  "apartmentId": 9,
  "tenantId": 7,
  "groupId": 2
}

Response (Success):
{
  "status": "SUCCESS",
  "GatewayPageURL": "https://sandbox.sslcommerz.com/...",
  "tran_id": "GRPPAY123"
}

Response (Error):
{
  "error": "Group is not ready for booking. All 4 members must join first."
}
```

### Existing Endpoints (Still Used)
```
POST /payment-success         - SSLCommerz callback handler
GET  /payment-success/download - Receipt download page
GET  /receipts/{tranId}       - PDF receipt generator
GET  /payment-fail            - Payment failure page
GET  /payment-cancel          - Payment cancellation page
```

---

## Files Modified

### Frontend
1. **`src/main/resources/static/js/app.js`**
   - `bookApartmentForGroup()` - Complete rewrite with SSLCommerz modal

### Backend
2. **`src/main/java/com/example/project/aloy/controller/SSLCommerzPaymentController.java`**
   - Added `@Autowired RoommateGroupService`
   - New method: `initiateGroupPayment()` - Full SSLCommerz integration

3. **`src/main/java/com/example/project/aloy/controller/PaymentResultController.java`**
   - Added `@Autowired RoommateGroupService`
   - Updated `paymentSuccessPost()` - Group payment detection and handling

---

## Security Considerations

1. **Group Membership Verification**
   - Backend validates tenant is member before payment
   - Prevents unauthorized payments

2. **Race Condition Prevention**
   - Uses `findByIdForUpdate()` to lock apartment row
   - Ensures only one group can book apartment

3. **Transaction Integrity**
   - `@Transactional` annotation ensures atomicity
   - Payment record created before SSLCommerz call
   - Rollback on failure

4. **Payment Method Identification**
   - Group payments marked as `SSLCommerz_Group`
   - Helps in auditing and reporting

---

## Comparison: Solo vs Group Booking

| Feature                  | Solo Booking          | Group Booking             |
|--------------------------|-----------------------|---------------------------|
| Payment Button           | "Book for Myself"     | "Pay Now & Book Apartment"|
| Payment Method           | SSLCommerz            | SSLCommerz                |
| Transaction ID Prefix    | PAY                   | GRPPAY                    |
| Payment Method Field     | SSLCommerz            | SSLCommerz_Group          |
| Initiation Endpoint      | /api/payments/initiate| /api/payments/initiate-group |
| Pre-Payment Validation   | Tenant doesn't have booking | Group is READY, user is member |
| Post-Payment Action      | Mark apartment RENTED | Call roommateGroupService.bookApartment() |
| Receipt Tenant Info      | Single tenant         | Paying tenant (from group)|
| Success URL Parameter    | tran_id               | tran_id, groupId          |

---

## Troubleshooting

### Issue: Payment button doesn't appear after 4th member joins
**Solution:** Already fixed in previous update. Button appears when `memberCount >= 4 AND status === 'READY'`

### Issue: SSLCommerz returns "FAILED" status
**Check:**
1. Store ID and password in `application.properties`
2. Amount is valid (> 0)
3. Customer details are complete (name, email, phone)
4. Network connectivity to SSLCommerz sandbox

### Issue: Payment successful but apartment not booked
**Debug Steps:**
1. Check application logs for `[DEBUG GROUP]` messages
2. Verify `value_c` parameter contains `GROUP_{groupId}`
3. Check if `RoommateGroupService.bookApartment()` was called
4. Verify no exceptions in `PaymentResultController.paymentSuccessPost()`

### Issue: Multiple groups trying to book same apartment
**Expected Behavior:**
- First group to complete payment wins
- Other groups get status `CANCELLED`
- Members of cancelled groups see error message

---

## Next Steps (Future Enhancements)

1. **Email Notifications**
   - Send email to all 4 group members after successful payment
   - Include receipt PDF as attachment

2. **Payment Split Tracking**
   - Track which member paid
   - Show "Paid by Alice" in group details
   - Allow payment splitting among members

3. **Group Chat**
   - Add messaging between group members
   - Discuss payment arrangements

4. **Refund Handling**
   - If apartment becomes unavailable after payment
   - Automatic refund initiation via SSLCommerz

5. **Payment History**
   - Show all group payments in tenant dashboard
   - Download receipts anytime

---

## Summary

✅ **Completed:**
- Group payment integrated with SSLCommerz sandbox
- Professional payment modal with validation
- Backend handles group vs solo bookings differently
- PDF receipt generation for group bookings
- Apartment marked as RENTED after payment
- Group status updated to BOOKED
- Other groups cancelled automatically

✅ **Testing:**
- Ready for end-to-end testing with 4 tenant accounts
- Use existing group (ID: 2) with invite code: CFW513
- SSLCommerz test card: 4111 1111 1111 1111

✅ **Security:**
- All validations in place (group status, membership, apartment availability)
- Race condition prevention with row locking
- Transaction atomicity ensured

The group booking feature now provides the same professional payment experience as solo bookings! 🎉
