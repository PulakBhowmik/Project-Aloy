# Tenant Booking Constraint Feature

## Overview
This feature ensures that **one tenant can only book one apartment at a time**. The constraint is enforced at both the backend and frontend levels to provide a seamless user experience.

---

## Backend Implementation

### 1. **New Controller: `TenantBookingController`**
Location: `src/main/java/com/example/project/aloy/controller/TenantBookingController.java`

**Purpose:** Provides an API endpoint to check if a tenant already has an active booking.

**Endpoint:**
```
GET /api/tenants/{tenantId}/booking-status
```

**Response:**
```json
{
  "hasBooking": true,
  "apartmentId": 14,
  "paymentId": 19,
  "transactionId": "PAY19",
  "amount": 1200.00,
  "apartmentTitle": "Luxury Apartment",
  "apartmentAddress": "123 Main St, Dhaka",
  "monthlyRent": 1200.00
}
```

Or if no booking exists:
```json
{
  "hasBooking": false
}
```

---

### 2. **Enhanced `SSLCommerzPaymentController`**
Location: `src/main/java/com/example/project/aloy/controller/SSLCommerzPaymentController.java`

**Enhancement:** Added validation at the beginning of the payment initiation process to check if the tenant already has a COMPLETED payment.

**Logic:**
1. Extract `tenantId` from the payment request
2. Query all payments for that tenant using `paymentRepository.findByTenantId(tenantId)`
3. Loop through payments and check if any have:
   - `status = "COMPLETED"`
   - `apartmentId` is not null
4. If such a payment exists, return HTTP 409 (Conflict) with error message:
   ```json
   {
     "error": "You already have an active apartment booking. One tenant can only book one apartment at a time."
   }
   ```

**Code Snippet:**
```java
// CRITICAL: Check if tenant already has a booking
Long tenantId = null;
if (paymentRequest.get("tenantId") != null) {
    try {
        tenantId = Long.parseLong(String.valueOf(paymentRequest.get("tenantId")));
        System.out.println("[DEBUG] Checking if tenant " + tenantId + " already has a booking...");
        
        // Find any COMPLETED payment for this tenant
        java.util.List<Payment> tenantPayments = paymentRepository.findByTenantId(tenantId);
        for (Payment p : tenantPayments) {
            if ("COMPLETED".equalsIgnoreCase(p.getStatus()) && p.getApartmentId() != null) {
                System.out.println("[WARNING] Tenant " + tenantId + " already has a completed booking for apartment " + p.getApartmentId());
                return ResponseEntity.status(409).body(Collections.singletonMap("error", 
                    "You already have an active apartment booking. One tenant can only book one apartment at a time."));
            }
        }
        System.out.println("[DEBUG] Tenant " + tenantId + " has no existing bookings. Proceeding with payment initiation.");
    } catch (NumberFormatException ignored) {}
}
```

---

### 3. **Enhanced `PaymentRepository`**
Location: `src/main/java/com/example/project/aloy/repository/PaymentRepository.java`

**Enhancement:** Added a new query method to find all payments by tenant ID.

```java
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByApartmentId(Long apartmentId);
    List<Payment> findByTenantId(Long tenantId); // NEW
}
```

---

## Frontend Implementation

### 1. **Booking Status Check**
Location: `src/main/resources/static/js/app.js`

**Global Variables:**
```javascript
let userHasBooking = false;
let userBookingDetails = null;
```

**Function: `checkTenantBookingStatus()`**
- Called when the page loads
- Extracts current user from localStorage
- Calls `/api/tenants/{userId}/booking-status` endpoint
- Updates global variables based on response

**Code:**
```javascript
function checkTenantBookingStatus() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || !user.userId) {
        console.log('[INFO] No user logged in, skipping booking status check');
        return;
    }

    fetch(`/api/tenants/${user.userId}/booking-status`)
        .then(response => response.json())
        .then(data => {
            if (data.hasBooking) {
                userHasBooking = true;
                userBookingDetails = data;
                console.log('[INFO] User already has a booking:', data);
                showBookingWarningBanner(data);
            } else {
                userHasBooking = false;
                userBookingDetails = null;
                console.log('[INFO] User has no existing booking');
            }
        })
        .catch(error => {
            console.error('[ERROR] Failed to check tenant booking status:', error);
        });
}
```

---

### 2. **Warning Banner**

**Function: `showBookingWarningBanner(bookingData)`**
- Displays a dismissible warning banner at the top of the apartments list
- Shows details of the existing booking (apartment title, address, rent)
- Informs user they cannot book another apartment

**Visual:**
```
⚠️ You Already Have an Active Booking
Apartment: Luxury Apartment
Location: 123 Main St, Dhaka
Monthly Rent: $1200
Note: You can only book one apartment at a time. To book another apartment, please contact support.
```

---

### 3. **Disabled Booking Buttons**

**Function: `showApartmentDetailsModal(apartmentId)` (Enhanced)**
- Checks the global `userHasBooking` variable
- If true:
  - Adds `disabled` attribute to booking buttons
  - Displays a warning message in the modal
  - Prevents button clicks

**Code:**
```javascript
const bookingDisabled = userHasBooking;
const disabledAttr = bookingDisabled ? 'disabled' : '';

let warningMsg = '';
if (bookingDisabled) {
    warningMsg = '<div class="alert alert-danger mb-3">' +
        '<strong>Booking Disabled:</strong> You already have an active apartment booking. ' +
        'One tenant can only book one apartment at a time.' +
        '</div>';
}

// Buttons with disabled attribute
'<button class="btn btn-success ' + disabledClass + '" id="bookSoloBtn" ' + disabledAttr + '>Book for Myself</button>'
'<button class="btn btn-info ' + disabledClass + '" id="bookGroupBtn" ' + disabledAttr + '>Book in a Group</button>'
```

---

### 4. **Enhanced Error Handling**

**Enhancement in Payment Modal:**
- Catches HTTP 409 errors from the backend
- Updates `userHasBooking` global variable to `true`
- Displays a user-friendly error message in the payment modal
- Auto-refreshes the page after 2 seconds to show the warning banner

**Code:**
```javascript
.then(function(res) { 
    if (!res.ok) {
        if (res.status === 409) {
            // Tenant already has a booking
            return res.json().then(function(errorData) {
                userHasBooking = true;
                throw new Error(errorData.error || 'You already have an active apartment booking.');
            });
        }
    }
    return res.json(); 
})
.catch(function(error) {
    document.getElementById('paymentMsg').innerHTML = 
        '<div class="alert alert-danger mt-3">' +
        '<strong>Error:</strong> ' + error.message + 
        '</div>';
    
    // Auto-refresh if booking constraint violated
    if (userHasBooking) {
        setTimeout(function() {
            window.location.reload();
        }, 2000);
    }
});
```

---

## Testing the Feature

### Test Case 1: User with No Booking
**Expected Behavior:**
1. User logs in
2. No warning banner appears
3. User can view apartment details
4. "Book for Myself" and "Book in a Group" buttons are enabled
5. User can proceed to payment

### Test Case 2: User with Existing Booking (Page Load)
**Expected Behavior:**
1. User logs in (already has a completed booking)
2. Page loads and checks booking status
3. Yellow warning banner appears at top of apartments list showing booking details
4. User can still browse apartments
5. When viewing apartment details, booking buttons are disabled
6. A red warning message appears in the modal

### Test Case 3: User Tries to Book Second Apartment (Backend Validation)
**Expected Behavior:**
1. User somehow bypasses frontend validation
2. User clicks "Book for Myself" and fills payment form
3. Frontend sends payment initiation request
4. **Backend returns HTTP 409** with error message
5. Frontend displays error in payment modal
6. Page auto-refreshes after 2 seconds
7. Warning banner appears after refresh

### Test Case 4: User with Existing Booking Tries to Book (Frontend Validation)
**Expected Behavior:**
1. User already has a booking (userHasBooking = true)
2. User views apartment details
3. Booking buttons are grayed out and disabled
4. Red warning message explains why buttons are disabled
5. User cannot click the buttons

---

## Database Schema

The feature relies on the existing `payment` table:

```sql
SELECT 
    payment_id,
    transaction_id,
    apartment_id,
    tenant_id,
    status,
    amount
FROM payment
WHERE tenant_id = ? AND status = 'COMPLETED' AND apartment_id IS NOT NULL;
```

**Index Recommendation:** Consider adding an index on `tenant_id` for better query performance:
```sql
CREATE INDEX idx_payment_tenant_id ON payment(tenant_id);
```

---

## Flow Diagram

```
User Logs In
    |
    v
Frontend: checkTenantBookingStatus()
    |
    v
GET /api/tenants/{tenantId}/booking-status
    |
    v
Backend: Query payment table for COMPLETED payments
    |
    +------- Has Booking? -------+
    |                            |
   YES                          NO
    |                            |
    v                            v
Show Warning Banner      Allow Normal Browsing
Disable Booking Buttons  Enable Booking Buttons
    |                            |
    |                            v
    |                    User Clicks "Book for Myself"
    |                            |
    |                            v
    |                    POST /api/payments/initiate
    |                            |
    |                            v
    |                Backend: Check tenant booking status
    |                            |
    |                    +------- Has COMPLETED Payment? -------+
    |                    |                                      |
    |                   YES                                    NO
    |                    |                                      |
    +--------------------+                              Proceed with Payment
                         |                                      |
                         v                                      v
            Return HTTP 409 Error                  Create PENDING Payment
                         |                                      |
                         v                                      v
            Frontend: Show Error Message           Redirect to SSLCommerz
                         |
                         v
            Auto-refresh Page After 2s
                         |
                         v
            Show Warning Banner
```

---

## Key Benefits

1. **Prevents Double Booking:** One tenant cannot have multiple active apartment bookings
2. **Backend Security:** Validation at API level prevents bypassing frontend checks
3. **User-Friendly UI:** Clear warning messages and disabled buttons guide user behavior
4. **Graceful Error Handling:** HTTP 409 errors are caught and displayed nicely
5. **Automatic State Sync:** Page auto-refreshes to show warning banner after constraint violation
6. **Performance:** Minimal overhead - only one additional API call on page load

---

## Future Enhancements

1. **Booking Cancellation:** Add feature to allow tenants to cancel existing bookings
2. **Admin Override:** Admin panel to manage tenant bookings
3. **Booking History:** Show tenants their booking history
4. **Email Notifications:** Notify tenants when attempting to book a second apartment
5. **Booking Expiry:** Automatically expire bookings after a certain period

---

## Support

If you encounter any issues with the tenant booking constraint feature, please contact the development team or create an issue in the project repository.
