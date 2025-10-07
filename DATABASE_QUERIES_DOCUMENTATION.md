# Database Queries Documentation - Owner Analytics

This document details all the SQL queries implemented in the Owner Analytics feature, perfect for demonstrating DBMS concepts in your course project.

## Overview

The Owner Analytics feature demonstrates various SQL concepts including:
- **Aggregate Functions** (COUNT, SUM, AVG)
- **JOIN Operations** (INNER JOIN, LEFT JOIN)
- **GROUP BY** clauses
- **Subqueries** and filtering
- **ORDER BY** and sorting
- **LIMIT** clauses
- **Date-based queries**

---

## API Endpoints and Queries

### 1. Owner Dashboard Analytics
**Endpoint:** `GET /api/owner-analytics/owner/{ownerId}`

**Purpose:** Get comprehensive statistics for an owner's property portfolio

#### SQL Queries Demonstrated:

**Query 1: Find all apartments by owner**
```sql
SELECT * FROM apartments WHERE owner_id = ?
```
- **Concept**: Basic SELECT with WHERE clause
- **Result**: All apartments owned by a specific owner

**Query 2: Count available apartments**
```sql
SELECT COUNT(*) as available_count
FROM apartments 
WHERE owner_id = ? AND booked = false
```
- **Concept**: Aggregate function (COUNT)
- **Result**: Number of available apartments

**Query 3: Count rented apartments**
```sql
SELECT COUNT(*) as rented_count
FROM apartments 
WHERE owner_id = ? AND booked = true
```
- **Concept**: COUNT with boolean condition
- **Result**: Number of currently rented apartments

**Query 4: Calculate total monthly revenue**
```sql
SELECT SUM(monthly_rent) as total_revenue
FROM apartments 
WHERE owner_id = ? AND booked = true
```
- **Concept**: Aggregate function (SUM)
- **Result**: Total income from all rented properties

**Query 5: Calculate average rent**
```sql
SELECT AVG(monthly_rent) as average_rent
FROM apartments 
WHERE owner_id = ?
```
- **Concept**: Aggregate function (AVG)
- **Result**: Average rent price across all properties

**Query 6: Calculate occupancy rate**
```sql
SELECT 
    COUNT(CASE WHEN booked = true THEN 1 END) * 100.0 / COUNT(*) as occupancy_rate
FROM apartments 
WHERE owner_id = ?
```
- **Concept**: Conditional aggregation, percentage calculation
- **Result**: Percentage of apartments currently rented

---

### 2. Current Tenants Information
**Endpoint:** `GET /api/owner-analytics/owner/{ownerId}/tenants`

**Purpose:** Get detailed information about tenants currently renting apartments

#### SQL Query:
```sql
SELECT 
    a.apartment_id,
    a.title as apartment_title,
    a.monthly_rent,
    a.district,
    u.user_id as tenant_id,
    u.name as tenant_name,
    u.email as tenant_email,
    p.transaction_id,
    p.amount as payment_amount,
    p.created_at as payment_date,
    p.vacate_date
FROM apartments a
INNER JOIN payment p ON a.apartment_id = p.apartment_id
INNER JOIN users u ON p.tenant_id = u.user_id
WHERE a.owner_id = ? 
    AND a.booked = true 
    AND p.status = 'COMPLETED'
```

**DBMS Concepts Demonstrated:**
- **INNER JOIN**: Links three tables (apartments, payment, users)
- **Multiple JOIN conditions**: Joins tables based on foreign key relationships
- **WHERE clause**: Filters for owner's apartments and completed payments
- **Aliasing**: Uses aliases (a, p, u) for cleaner syntax

**Tables Involved:**
1. `apartments` - Property information
2. `payment` - Transaction records
3. `users` - Tenant details

**Result:** Complete tenant profile with apartment and payment details

---

### 3. Revenue Analysis by District
**Endpoint:** `GET /api/owner-analytics/owner/{ownerId}/revenue-by-district`

**Purpose:** Analyze revenue grouped by geographical location

#### SQL Query:
```sql
SELECT 
    district,
    COUNT(*) as apartment_count,
    SUM(monthly_rent) as total_revenue
FROM apartments
WHERE owner_id = ? AND booked = true
GROUP BY district
ORDER BY total_revenue DESC
```

**DBMS Concepts Demonstrated:**
- **GROUP BY**: Groups results by district
- **Aggregate Functions**: COUNT and SUM in same query
- **ORDER BY**: Sorts by revenue (descending)
- **Multiple aggregations**: Shows both count and sum per group

**Business Value:**
- Identifies most profitable locations
- Helps in expansion decisions
- Shows market performance by area

---

### 4. Payment History
**Endpoint:** `GET /api/owner-analytics/owner/{ownerId}/payment-history?limit=20`

**Purpose:** Track all payment transactions chronologically

#### SQL Query:
```sql
SELECT 
    p.payment_id,
    p.transaction_id,
    p.amount,
    p.status,
    p.created_at as payment_date,
    a.title as apartment_title,
    u.name as tenant_name
FROM payment p
INNER JOIN apartments a ON p.apartment_id = a.apartment_id
INNER JOIN users u ON p.tenant_id = u.user_id
WHERE a.owner_id = ?
ORDER BY p.created_at DESC
LIMIT ?
```

**DBMS Concepts Demonstrated:**
- **INNER JOIN**: Multiple table joins
- **ORDER BY with DESC**: Newest payments first
- **LIMIT clause**: Pagination/result limiting
- **Date-based sorting**: Chronological ordering

**Result:** Complete payment audit trail

---

### 5. Top Performing Apartments
**Endpoint:** `GET /api/owner-analytics/owner/{ownerId}/top-apartments?limit=10`

**Purpose:** Rank apartments by total revenue and booking frequency

#### SQL Query:
```sql
SELECT 
    a.apartment_id,
    a.title,
    a.district,
    a.monthly_rent,
    a.status,
    COUNT(p.payment_id) as booking_count,
    SUM(p.amount) as total_revenue
FROM apartments a
LEFT JOIN payment p ON a.apartment_id = p.apartment_id 
    AND p.status = 'COMPLETED'
WHERE a.owner_id = ?
GROUP BY a.apartment_id, a.title, a.district, a.monthly_rent, a.status
ORDER BY total_revenue DESC, booking_count DESC
LIMIT ?
```

**DBMS Concepts Demonstrated:**
- **LEFT JOIN**: Includes apartments with zero bookings
- **Conditional JOIN**: Filters payments in JOIN condition
- **GROUP BY multiple columns**: Groups by apartment attributes
- **Multiple ORDER BY**: Primary and secondary sorting
- **LIMIT**: Top N results
- **Aggregate functions with GROUP BY**: COUNT and SUM per apartment

**Why LEFT JOIN?**
- Includes all apartments, even those never booked
- Shows apartments with $0 revenue (needs attention)
- Complete portfolio view

---

## Database Schema Relationships

### Tables Used:

1. **apartments**
   - `apartment_id` (PK)
   - `owner_id` (FK → users.user_id)
   - `monthly_rent`
   - `district`
   - `booked`
   - `status`

2. **payment**
   - `payment_id` (PK)
   - `apartment_id` (FK → apartments.apartment_id)
   - `tenant_id` (FK → users.user_id)
   - `transaction_id`
   - `amount`
   - `status`
   - `created_at`
   - `vacate_date`

3. **users**
   - `user_id` (PK)
   - `name`
   - `email`
   - `role` (TENANT/OWNER)

### Entity Relationship:
```
users (OWNER) ──< apartments >── payment >── users (TENANT)
    1:N                       1:N         N:1
```

---

## Advanced SQL Concepts Used

### 1. Aggregate Functions
```sql
COUNT(*)     -- Count rows
SUM(column)  -- Total sum
AVG(column)  -- Average value
```

### 2. JOIN Types
```sql
INNER JOIN   -- Only matching rows
LEFT JOIN    -- All left table + matching right
```

### 3. GROUP BY with HAVING (potential extension)
```sql
GROUP BY district
HAVING COUNT(*) > 2  -- Only districts with 3+ apartments
```

### 4. Subqueries (potential extension)
```sql
SELECT * FROM apartments 
WHERE monthly_rent > (SELECT AVG(monthly_rent) FROM apartments)
```

### 5. Window Functions (potential extension)
```sql
SELECT 
    title,
    monthly_rent,
    RANK() OVER (ORDER BY monthly_rent DESC) as rent_rank
FROM apartments
```

---

## Frontend Integration

### Tabs in Owner Dashboard:
1. **My Apartments** - CRUD operations
2. **Current Tenants** - JOIN query results
3. **Revenue by District** - GROUP BY visualization
4. **Payment History** - Chronological data with LIMIT
5. **Top Performers** - Ranked query results

### Visual Features:
- Statistics cards (aggregates)
- Tables with sorting
- Progress bars (GROUP BY visualization)
- Trophy rankings for top apartments

---

## Testing the Queries

### Sample Test Cases:

1. **Create test data:**
   ```sql
   -- Add owner
   INSERT INTO users (name, email, role) VALUES ('Test Owner', 'owner@test.com', 'OWNER');
   
   -- Add apartments
   INSERT INTO apartments (title, owner_id, monthly_rent, district, booked) 
   VALUES ('Apt 1', 1, 1000, 'Gulshan', true);
   
   -- Add payment
   INSERT INTO payment (tenant_id, apartment_id, amount, status) 
   VALUES (2, 1, 1000, 'COMPLETED');
   ```

2. **Verify analytics:**
   - Check total apartments count
   - Verify revenue calculation
   - Test JOIN results
   - Validate GROUP BY aggregation

---

## Performance Considerations

### Indexes Recommended:
```sql
CREATE INDEX idx_apartments_owner ON apartments(owner_id);
CREATE INDEX idx_payment_apartment ON payment(apartment_id);
CREATE INDEX idx_payment_tenant ON payment(tenant_id);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_date ON payment(created_at);
```

### Query Optimization:
- Use EXPLAIN to analyze query plans
- Limit result sets with LIMIT clause
- Filter early in WHERE clause
- Use appropriate JOINs (INNER vs LEFT)

---

## Conclusion

This feature demonstrates comprehensive DBMS concepts:
✅ **SELECT queries** with filtering
✅ **Aggregate functions** (COUNT, SUM, AVG)
✅ **JOIN operations** (INNER, LEFT)
✅ **GROUP BY** with multiple aggregates
✅ **ORDER BY** for sorting
✅ **LIMIT** for pagination
✅ **Subqueries** (implicit in application logic)
✅ **Transaction management** (via @Transactional)
✅ **Foreign key relationships**
✅ **Data normalization** (3NF)

Perfect for demonstrating database concepts in your DBMS course! 🎓
