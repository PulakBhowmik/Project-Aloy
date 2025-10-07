# Review System Feature Documentation

## Overview
A comprehensive review system that allows tenants to leave feedback after vacating apartments. Future tenants can view these reviews to make informed decisions.

## Features Implemented

### 1. Review Model (Review.java)
- **Star Rating**: 1-5 star rating system
- **Good Sides**: Up to 500 characters for positive feedback
- **Bad Sides**: Up to 500 characters for areas of improvement
- **Tenant Name**: Display name of the reviewer
- **Date**: Timestamp of when review was submitted
- **User & Apartment References**: Links to reviewer and reviewed apartment

### 2. Backend API Endpoints

#### ReviewController.java
- **GET /api/reviews/apartment/{apartmentId}**
  - Returns all reviews for an apartment
  - Includes average rating and total review count
  - Reviews sorted by date (newest first)

- **POST /api/reviews/submit**
  - Submit a new review
  - Prevents duplicate reviews from same user
  - Required fields: userId, apartmentId, rating, goodSides
  - Optional field: badSides
  
- **GET /api/reviews/can-review/{userId}/{apartmentId}**
  - Check if user can leave a review
  - Returns whether user has already reviewed

### 3. Repository Methods (ReviewRepository.java)
- `findByApartmentIdOrderByDateDesc()` - Get all reviews for apartment
- `existsByUserIdAndApartmentId()` - Check for existing review
- `findAverageRatingByApartmentId()` - Calculate average rating
- `countByApartmentId()` - Count total reviews

### 4. Frontend Features

#### Review Submission
- **When**: Prompted after tenant vacates apartment
- **Interactive Star Rating**: Click to select 1-5 stars with hover effects
- **Two Text Areas**:
  - "What did you like?" (Required, max 500 chars)
  - "What could be improved?" (Optional, max 500 chars)
- **Validation**: Ensures rating is selected and good sides are filled
- **Skip Option**: User can skip review if desired

#### Review Display
- **Apartment Details Page**: Full review section with all reviews
- **Home Page Modal**: Compact view showing top 3 recent reviews
- **Review Cards**: Professional layout with:
  - Reviewer name
  - Star rating visualization
  - Good sides (with thumbs up icon)
  - Bad sides (with thumbs down icon)
  - Review date

#### Summary Statistics
- **Average Rating**: Displayed prominently with stars
- **Total Review Count**: Shows number of reviews
- **Visual Bar Chart**: For owner dashboard revenue analytics

### 5. User Flow

1. **Tenant Books Apartment** → Lives there → Decides to vacate
2. **Tenant Clicks "Vacate" Button** → Selects vacate date → Confirms
3. **System Shows Success Message** with "Leave a Review" button
4. **Tenant Clicks Review Button** → Modal opens with star rating and text fields
5. **Tenant Fills Form**:
   - Selects star rating (1-5)
   - Writes what they liked (required)
   - Writes what could improve (optional)
6. **Submits Review** → Success message → Page refreshes
7. **Future Tenants View Reviews** on apartment details page

### 6. Design Highlights

#### Star Rating Component
- **Interactive**: Hover effects show preview
- **Visual Feedback**: Stars fill with gold color on selection
- **Large Icons**: 2rem font size for easy clicking
- **Responsive**: Works on mobile devices

#### Review Cards
- **Gradient Background**: Purple gradient for summary header
- **Color Coding**:
  - Green for good sides (👍)
  - Red for bad sides (👎)
  - Gold for stars (⭐)
- **Border Accent**: Blue left border on review cards
- **Professional Layout**: Card-based design with proper spacing

### 7. Database Schema

```sql
CREATE TABLE reviews (
    review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rating INT NOT NULL,
    remarks VARCHAR(255),
    good_sides VARCHAR(500),
    bad_sides VARCHAR(500),
    tenant_name VARCHAR(255),
    user_id BIGINT,
    apartment_id BIGINT,
    date DATE
);
```

### 8. Validation & Security

- **Duplicate Prevention**: Users can only review once per apartment
- **Input Validation**: 
  - Rating must be 1-5
  - Good sides required, max 500 characters
  - Bad sides optional, max 500 characters
- **User Authentication**: Must be logged in to submit review
- **XSS Protection**: Text properly escaped in display

### 9. CSS Styling (style.css)

Added styles for:
- `.rating-star` - Interactive star buttons
- `.star-rating` - Star container with flexbox
- `.star-display` - Star visualization in reviews
- `.review-card` - Review container with border and padding
- `.reviews-summary` - Gradient background summary section
- Responsive adjustments for mobile devices

### 10. Integration Points

#### With Vacate Feature
- Review prompt appears after successful vacate
- User can choose to review or skip
- Review modal auto-populated with apartment info

#### With Apartment Listings
- Reviews display on apartment detail pages
- Average rating shown in apartment modals
- Review count displayed prominently

#### With Owner Dashboard
- Future enhancement: Owners can view reviews of their apartments
- Analytics can include review ratings

## Testing Checklist

- [x] Build successful
- [ ] Submit review after vacating
- [ ] Star rating interaction works
- [ ] Good/bad sides text areas functional
- [ ] Review displays on apartment details
- [ ] Average rating calculates correctly
- [ ] Duplicate review prevention works
- [ ] Review sorting (newest first)
- [ ] Mobile responsive design
- [ ] Character limits enforced

## Future Enhancements

1. **Owner Response**: Allow owners to respond to reviews
2. **Review Moderation**: Admin panel to moderate inappropriate reviews
3. **Verified Badge**: Show "Verified Tenant" badge for confirmed rentals
4. **Review Photos**: Allow tenants to upload photos with reviews
5. **Helpful Votes**: Let users vote reviews as helpful/not helpful
6. **Filter Reviews**: Filter by rating (5 stars, 4+ stars, etc.)
7. **Review Statistics**: More detailed analytics for owners
8. **Email Notifications**: Notify owners of new reviews

## Files Modified/Created

### Created:
- `ReviewController.java` - REST API endpoints

### Modified:
- `Review.java` - Added goodSides, badSides, tenantName fields
- `ReviewRepository.java` - Added query methods
- `app.js` - Added review modal and display functions
- `index.html` - Added review display in apartment modal
- `apartment-details.html` - Added full review section
- `style.css` - Added review styling

### No Changes Needed:
- VacateController.java (already returns success message)
- Database config (auto-updates with Hibernate)

## Usage Instructions

### For Tenants:
1. After vacating apartment, click "Leave a Review" button
2. Select star rating (1-5 stars)
3. Write what you liked about the apartment (required)
4. Optionally write what could be improved
5. Click "Submit Review" or "Skip"

### For Future Tenants:
1. Browse apartments on home page
2. Click "View Details" on any apartment
3. Scroll down to "Tenant Reviews" section
4. Read reviews with star ratings and feedback
5. See average rating and total review count

### For Owners:
- Reviews automatically display on apartment listings
- Future tenants can see transparent feedback
- Helps build trust and credibility

## API Examples

### Submit Review
```json
POST /api/reviews/submit
{
  "userId": 123,
  "apartmentId": 456,
  "rating": 5,
  "goodSides": "Great location, clean, friendly landlord",
  "badSides": "Water pressure could be better"
}
```

### Get Apartment Reviews
```json
GET /api/reviews/apartment/456

Response:
{
  "reviews": [...],
  "averageRating": 4.5,
  "totalReviews": 12
}
```

## Currency Note
All monetary displays use Bangladeshi Taka (৳) as per previous update.

---
**Date Implemented**: October 8, 2025  
**Version**: 1.0  
**Status**: ✅ Build Successful
