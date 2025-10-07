# Booking Status Real-Time Update Fix

## Problem Description
After booking an apartment and downloading the PDF receipt, the apartment status remained as "AVAILABLE" on the homepage even after hard reload. The booked apartment should immediately show as "BOOKED" when returning to the homepage.

## Root Cause
The homepage (`index.html`) was loading the apartment list only once on initial page load. When users returned from the payment success page, the browser was displaying cached apartment data, and even with hard refresh, the JavaScript didn't automatically refetch the updated apartment list from the server.

## Solution Implemented

### 1. Payment Success Flow Enhancement (`PaymentResultController.java`)

**Added cache-busting URL parameter when redirecting to homepage:**

```java
// Before:
setTimeout(function(){ window.location = '/'; }, 1200);

// After:
setTimeout(function(){ window.location = '/?refresh=' + Date.now(); }, 1200);
```

This adds a unique timestamp parameter (`?refresh=1728146712345`) to the homepage URL, which:
- Signals that the page is being loaded after a successful payment
- Prevents browser from serving a cached version
- Triggers special refresh logic in the frontend

**Enhanced success message:**
```java
document.body.innerHTML = '<h2>✅ Receipt Downloaded Successfully!</h2><p>You will be redirected to the home page in a moment...</p>';
```

### 2. Frontend Auto-Refresh Logic (`index.html`)

**Added `checkForPaymentRefresh()` function:**

```javascript
function checkForPaymentRefresh() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('refresh')) {
        console.log('Detected payment success - forcing apartment list refresh with cache bypass');
        // Force refresh apartments from API with cache-busting
        fetch('/api/apartments?nocache=' + Date.now(), {
            cache: 'no-store',
            headers: {
                'Cache-Control': 'no-cache, no-store, must-revalidate',
                'Pragma': 'no-cache'
            }
        })
        .then(response => response.json())
        .then(apartments => {
            displayApartments(apartments);
            calculateStats(apartments);
            // Clean URL without page reload
            window.history.replaceState({}, document.title, window.location.pathname);
        })
        .catch(error => console.error('Error refreshing apartments:', error));
    }
}

// Called on page load
checkForPaymentRefresh();
```

**How it works:**
1. Detects the `?refresh=` parameter in the URL
2. Makes a fresh API call to `/api/apartments` with:
   - Cache-busting query parameter (`?nocache=timestamp`)
   - HTTP headers forcing no caching (`Cache-Control`, `Pragma`)
3. Updates the apartment display with fresh data from server
4. Removes the `?refresh` parameter from URL (clean URL bar)

### 3. Backend Data Flow (Already Working)

The backend booking flow was already working correctly:

```java
// PaymentResultController.java - Payment success handler
if (!apt.isBooked()) {
    apt.setBooked(true);
    apt.setStatus("RENTED");
    apartmentRepository.save(apt);  // Saves to database immediately
}
```

**Database is updated correctly:**
- `booked` field set to `1` (true)
- `status` field set to `'RENTED'`
- Changes persist in MySQL database

### 4. API Endpoint Serving All Apartments

The `ApartmentController` returns ALL apartments (including booked ones):

```java
@GetMapping
public List<Apartment> getAllApartments() {
    return apartmentRepository.findAll();  // Returns all apartments with booking status
}
```

Frontend displays visual indicators:
- **Green badge**: "AVAILABLE" - apartment can be booked
- **Red badge**: "BOOKED" - apartment is rented, booking buttons hidden

## Complete Flow After Fix

### User Journey:
1. **User books apartment** → Payment processed successfully
2. **PDF receipt generated** → Browser downloads PDF
3. **Redirect with cache-bust** → `window.location = '/?refresh=1728146712345'`
4. **Homepage detects refresh param** → Triggers `checkForPaymentRefresh()`
5. **Fresh API call** → `GET /api/apartments?nocache=1728146712345` (no cache)
6. **Server returns updated data** → Includes the newly booked apartment with `booked=true, status='RENTED'`
7. **Frontend displays badges** → Red "BOOKED" badge shown for the booked apartment
8. **URL cleaned** → `/?refresh=...` becomes `/` (clean URL bar)

### Technical Flow:
```
[Payment Success] 
    ↓
[Mark apartment as booked in DB]
    ↓
[Generate PDF receipt]
    ↓
[Redirect to /?refresh=timestamp]
    ↓
[Homepage loads]
    ↓
[checkForPaymentRefresh() detects refresh param]
    ↓
[Force fetch /api/apartments with no-cache headers]
    ↓
[Display updated apartment list with badges]
    ↓
[Clean URL to /]
```

## Files Modified

### 1. `PaymentResultController.java`
- **Line ~71-86**: Updated redirect URLs to include `?refresh=' + Date.now()`
- **Line ~82**: Added success message after PDF download
- **Line ~63**: Error case also uses cache-bust redirect

### 2. `index.html`
- **Lines ~286-310**: Added `checkForPaymentRefresh()` function
- **Line ~313**: Added function call on page load: `checkForPaymentRefresh();`

## Testing

### Before Fix:
1. Book apartment → PDF downloads → Return to home
2. Apartment shows green "AVAILABLE" badge
3. Hard refresh (Ctrl+Shift+R) → Still shows green badge
4. **Problem**: Cached data displayed

### After Fix:
1. Book apartment → PDF downloads → "✅ Receipt Downloaded Successfully!" message
2. Auto-redirect to `/?refresh=1728146712345`
3. Apartment list auto-refreshes from API
4. Apartment shows red "BOOKED" badge immediately
5. **Success**: Real-time status update!

## Benefits

✅ **Immediate feedback** - Users see booking status change instantly  
✅ **No manual refresh needed** - Automatic cache-busting  
✅ **Clean user experience** - Smooth transition from payment to updated list  
✅ **Prevents confusion** - No stale data displayed  
✅ **Cache-proof** - Works even with aggressive browser caching  
✅ **Database integrity** - Backend correctly saves booking status  

## Prevention of Double Booking

The system still prevents double booking through:
1. **Pessimistic locking** - Row-level locks during booking
2. **HTTP 409 responses** - Backend rejects already-booked apartments
3. **Visual indicators** - Frontend hides booking buttons for booked apartments
4. **Database constraints** - `booked` flag and `status` field validation

## Browser Cache Strategy

### API Request Headers (Frontend):
```javascript
cache: 'no-store',
headers: {
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache'
}
```

### URL Cache-Busting:
```javascript
'/api/apartments?nocache=' + Date.now()  // Unique URL each time
'/?refresh=' + Date.now()                // Homepage redirect with timestamp
```

This ensures:
- No cached apartment data served after booking
- Fresh database query on every refresh
- Real-time status updates visible to users

## Deployment

After code changes:
1. `./mvnw clean compile` - Rebuild project
2. `./mvnw spring-boot:run` - Restart application
3. Clear browser cache or hard refresh (Ctrl+Shift+R)
4. Test booking flow end-to-end

## Related Files

- `PaymentResultController.java` - Payment success handler
- `index.html` - Homepage with apartment listings
- `ApartmentController.java` - API endpoint returning all apartments
- `apartment-details.html` - Individual apartment page (already shows correct status)

---

**Fix implemented:** October 5, 2025  
**Status:** ✅ Completed and tested
