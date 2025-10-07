# Fix Database Schema Issues

## Problem
Foreign key constraint errors due to data type mismatches between database (INT) and JPA entities (BIGINT).

## Solution Options

### Option 1: Drop and Recreate Database (EASIEST - Loses Data)

```bash
mysql -u rental_user2 -p123456 -e "DROP DATABASE apartment_rental_db; CREATE DATABASE apartment_rental_db;"
```

Then restart your application - Hibernate will create the correct schema.

### Option 2: Drop Foreign Keys, Change Types, Re-add Constraints (Preserves Data)

```sql
-- Run this in MySQL:
USE apartment_rental_db;

-- Drop all foreign keys
ALTER TABLE apartment_images DROP FOREIGN KEY apartment_images_ibfk_1;
ALTER TABLE payment DROP FOREIGN KEY payment_ibfk_1;
ALTER TABLE payment DROP FOREIGN KEY payment_ibfk_2;
ALTER TABLE apartments DROP FOREIGN KEY apartments_ibfk_1;
ALTER TABLE roommate_group DROP FOREIGN KEY roommate_group_ibfk_1;
ALTER TABLE roommate_group_members DROP FOREIGN KEY roommate_group_members_ibfk_1;
ALTER TABLE roommate_group_members DROP FOREIGN KEY roommate_group_members_ibfk_2;

-- Change all column types to BIGINT
ALTER TABLE users MODIFY COLUMN user_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE apartments MODIFY COLUMN apartment_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE apartments MODIFY COLUMN owner_id BIGINT;
ALTER TABLE apartment_images MODIFY COLUMN apartment_id BIGINT NOT NULL;
ALTER TABLE payment MODIFY COLUMN apartment_id BIGINT;
ALTER TABLE payment MODIFY COLUMN tenant_id BIGINT;
ALTER TABLE roommate_group MODIFY COLUMN group_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE roommate_group MODIFY COLUMN apartment_id BIGINT NOT NULL;
ALTER TABLE roommate_group_members MODIFY COLUMN group_id BIGINT NOT NULL;
ALTER TABLE roommate_group_members MODIFY COLUMN tenant_id BIGINT NOT NULL;

-- Re-add foreign keys
ALTER TABLE apartment_images ADD CONSTRAINT apartment_images_ibfk_1 
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE CASCADE;

ALTER TABLE payment ADD CONSTRAINT payment_ibfk_1 
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE SET NULL;

ALTER TABLE payment ADD CONSTRAINT payment_ibfk_2 
    FOREIGN KEY (tenant_id) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE apartments ADD CONSTRAINT apartments_ibfk_1 
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE roommate_group ADD CONSTRAINT roommate_group_ibfk_1 
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE CASCADE;

ALTER TABLE roommate_group_members ADD CONSTRAINT roommate_group_members_ibfk_1 
    FOREIGN KEY (group_id) REFERENCES roommate_group(group_id) ON DELETE CASCADE;

ALTER TABLE roommate_group_members ADD CONSTRAINT roommate_group_members_ibfk_2 
    FOREIGN KEY (tenant_id) REFERENCES users(user_id) ON DELETE CASCADE;
```

### Option 3: Change Hibernate DDL Mode (Temporary - Not Recommended)

Edit `src/main/resources/application.properties`:

```properties
# Change from 'update' to 'create' (WARNING: This will drop all tables!)
spring.jpa.hibernate.ddl-auto=create
```

After running once, change it back to:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Recommended Solution

**Use Option 1** if you're still in development and don't need to preserve data:

```bash
mysql -u rental_user2 -p123456 apartment_rental_db -e "DROP DATABASE apartment_rental_db; CREATE DATABASE apartment_rental_db;"
```

Then start your application - it will automatically create the correct schema.

## After Fixing

1. Start the application
2. Test tenant booking constraint feature
3. Verify all functionality works
