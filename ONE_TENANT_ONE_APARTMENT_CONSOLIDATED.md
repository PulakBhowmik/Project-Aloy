# ✅ ONE TENANT ONE APARTMENT - CONSOLIDATED

## 📝 Summary of Changes

✅ **All SQL work is now in ONE file:** `project aloy sql.sql`

## 🗂️ File Changes

### Files Modified
1. ✅ `project aloy sql.sql` - **UPDATED** with constraint logic
2. ✅ `src/main/java/com/example/project/aloy/service/PaymentService.java` - Added constraint checking
3. ✅ `src/main/java/com/example/project/aloy/controller/SSLCommerzPaymentController.java` - Enhanced validation
4. ✅ `src/main/resources/static/js/app.js` - Frontend constraint enforcement

### Files Created
1. ✅ `ONE_TENANT_ONE_APARTMENT_IMPLEMENTATION.md` - Technical documentation
2. ✅ `ONE_TENANT_ONE_APARTMENT_SUMMARY.md` - Quick summary
3. ✅ `TESTING_ONE_TENANT_ONE_APARTMENT.md` - Testing guide
4. ✅ `ONE_TENANT_ONE_APARTMENT_CONSOLIDATED.md` - This file

### Files Removed
1. ✅ `one_tenant_one_apartment_constraint.sql` - **DELETED** (merged into main SQL file)

## 🚀 Quick Start

### Option 1: Fresh Database Setup (Recommended)
```bash
# This will drop and recreate everything with the constraint
mysql -u rental_user2 -p < "project aloy sql.sql"
# Password: 123456
```

### Option 2: Add Constraint to Existing Database
If you already have data and don't want to lose it:

1. Open `project aloy sql.sql`
2. Find the section: `-- ONE TENANT ONE APARTMENT CONSTRAINT`
3. Copy from that section to the end of triggers (before sample data)
4. Run only that section in MySQL

```sql
USE apartment_rental_db;

-- Paste the constraint section here:
-- Clean up duplicates
DELETE p1 FROM payment p1...
-- Create index
CREATE INDEX IF NOT EXISTS...
-- Create triggers
DELIMITER //
DROP TRIGGER IF EXISTS...
-- etc.
```

### Start Application
```bash
cd "/d/My Downloads/project aloy/project-aloy"
./mvnw clean package -DskipTests
java -jar target/apartment-rental-system-0.0.1-SNAPSHOT.jar
```

## 📋 What's in the Main SQL File Now

The `project aloy sql.sql` file now contains:

1. ✅ Database creation
2. ✅ User creation and grants
3. ✅ All table definitions (users, apartments, payments, etc.)
4. ✅ **NEW: One tenant one apartment constraint section**
   - Duplicate cleanup
   - Performance index
   - Database triggers (INSERT and UPDATE)
5. ✅ Sample data inserts
6. ✅ Verification queries
7. ✅ **NEW: Constraint verification queries**

## 🎯 Key Features of the Constraint

### Database Level (in main SQL file)
- **Trigger on INSERT**: Blocks creating new COMPLETED payments if tenant already has one
- **Trigger on UPDATE**: Blocks updating payment to COMPLETED if tenant already has one
- **Index**: Speeds up constraint checking queries
- **Cleanup**: Removes any existing duplicate bookings before applying constraint

### Backend Level
- **PaymentService**: `tenantHasActiveBooking()` method for centralized checking
- **Controller**: Early validation before payment initiation (HTTP 409 if violated)

### Frontend Level
- **Info Banner**: Shows on homepage if tenant has active booking
- **Disabled Buttons**: Booking buttons disabled for tenants with bookings
- **Warning Modal**: Shows alert in apartment details
- **Error Handling**: Clear messages when constraint is violated

## 🔍 Quick Verification

After running the SQL file, verify the triggers are created:

```sql
USE apartment_rental_db;

SELECT TRIGGER_NAME, EVENT_MANIPULATION 
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'apartment_rental_db'
  AND EVENT_OBJECT_TABLE = 'payment';
```

**Expected Output:**
```
prevent_multiple_bookings_insert | INSERT
prevent_multiple_bookings_update | UPDATE
```

## 📊 Check for Violations

Run this query to ensure no tenant has multiple bookings:

```sql
SELECT 
    p.tenant_id,
    u.name,
    COUNT(*) as booking_count
FROM payment p
JOIN users u ON p.tenant_id = u.user_id
WHERE p.status = 'COMPLETED' 
  AND p.apartment_id IS NOT NULL
GROUP BY p.tenant_id, u.name
HAVING booking_count > 1;
```

**Expected:** 0 rows (no violations)

## 🎉 Benefits of Consolidation

✅ **Single Source of Truth**: All database setup in one file  
✅ **Easier Deployment**: Run one file to set up everything  
✅ **Better Maintenance**: No need to track multiple SQL files  
✅ **Cleaner Project**: Less file clutter  
✅ **Consistent Setup**: Fresh installations automatically get the constraint  

## 📚 Documentation

- **Full Technical Details**: `ONE_TENANT_ONE_APARTMENT_IMPLEMENTATION.md`
- **Testing Guide**: `TESTING_ONE_TENANT_ONE_APARTMENT.md`
- **Quick Summary**: `ONE_TENANT_ONE_APARTMENT_SUMMARY.md`
- **This Quick Reference**: `ONE_TENANT_ONE_APARTMENT_CONSOLIDATED.md`

## 💡 Pro Tip

If you ever need to disable the constraint temporarily:

```sql
DROP TRIGGER IF EXISTS prevent_multiple_bookings_insert;
DROP TRIGGER IF EXISTS prevent_multiple_bookings_update;
```

To re-enable, just run the constraint section from `project aloy sql.sql` again.

---

**Status:** ✅ Consolidated and Ready  
**Date:** October 7, 2025  
**All SQL work is in:** `project aloy sql.sql`
