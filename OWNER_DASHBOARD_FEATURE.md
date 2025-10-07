# Owner Dashboard Feature

## Overview
This feature allows apartment owners to manage all their apartments from a centralized dashboard.

## Features Implemented

### Backend Changes

1. **ApartmentRepository.java**
   - Added `findByOwnerId(Long ownerId)` method to fetch apartments by owner

2. **ApartmentController.java**
   - Added `GET /api/apartments/owner/{ownerId}` endpoint to get owner's apartments
   - Updated `PUT /api/apartments/{id}` with owner verification
   - Owner can only edit their own apartments (returns 403 if trying to edit others)

3. **PageController.java**
   - Added route `/owner-dashboard` to serve the dashboard page

### Frontend Changes

1. **owner-dashboard.html** (NEW)
   - Complete owner dashboard page
   - Statistics card showing:
     - Total apartments
     - Available apartments
     - Rented apartments
     - Monthly revenue
   - Grid display of all owner's apartments
   - Edit and Delete buttons for each apartment

2. **owner-dashboard.js** (NEW)
   - Auto-loads apartments for logged-in owner
   - Edit apartment modal with form validation
   - Delete confirmation modal
   - Real-time statistics updates
   - Owner-only access (redirects non-owners)

3. **index.html**
   - Added "My Apartments" link in navbar for owners
   - Added Bootstrap Icons CDN
   - Link appears next to user info when owner is logged in

## How to Use

### For Owners:
1. Login as an owner
2. Click "My Apartments" link in the navbar
3. View all your apartments with statistics
4. Edit any apartment by clicking the "Edit" button
5. Delete apartments with confirmation
6. Statistics auto-update based on apartment status

### Owner Dashboard URL:
- Direct access: `http://localhost:8080/owner-dashboard`
- Or click "My Apartments" in navbar when logged in as owner

## API Endpoints

### Get Owner's Apartments
```
GET /api/apartments/owner/{ownerId}
Response: Array of apartments owned by the user
```

### Update Apartment (Owner-protected)
```
PUT /api/apartments/{id}
Body: { ownerId, title, description, monthlyRate, ... }
Response: Updated apartment or 403 if not owner
```

### Delete Apartment
```
DELETE /api/apartments/{id}
Response: Success message
```

## Security Features

- Only owners can access the dashboard page (tenant redirected to home)
- Owners can only edit their own apartments (backend validation)
- Delete requires confirmation modal
- All API calls include proper error handling

## Statistics Tracked

1. **Total Apartments** - Count of all apartments owned
2. **Available** - Apartments with status AVAILABLE and not booked
3. **Rented** - Apartments that are booked or have RENTED status
4. **Monthly Revenue** - Sum of monthly rent from booked apartments

## UI Features

- Responsive grid layout (3 columns on large screens)
- Color-coded status badges (green=available, warning=rented)
- Booked/Not Booked badges
- Bootstrap icons throughout
- Modal forms for editing
- Confirmation dialogs for destructive actions
- Real-time updates after changes

## Future Enhancements (Optional)

- Image upload for apartments
- Tenant contact information
- Payment history per apartment
- Booking calendar view
- Export apartment data
- Bulk operations
- Analytics and charts
