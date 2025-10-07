-- Apartment rental DB setup and queries (clean)
DROP DATABASE IF EXISTS apartment_rental_db;

CREATE DATABASE apartment_rental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE apartment_rental_db;
CREATE USER IF NOT EXISTS 'rental_user2'@'%' IDENTIFIED BY '123456';

-- Create users table first (referenced by other tables)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    permanent_address VARCHAR(500),
    house_no VARCHAR(50),
    street VARCHAR(150),
    district VARCHAR(100),
    nid VARCHAR(50),
    dob DATE,
    gender VARCHAR(10),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
);

-- Create apartments table (matches Apartment JPA entity)
CREATE TABLE IF NOT EXISTS apartments (
    apartment_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    monthly_rent DECIMAL(10,2) DEFAULT 0.00,
    availability DATE,
    district VARCHAR(100),
    street VARCHAR(150),
    house_no VARCHAR(50),
    address VARCHAR(255),
    allowed_for VARCHAR(255),
    owner_id INT,
    booked TINYINT(1) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    bedrooms INT DEFAULT NULL,
    bathrooms INT DEFAULT NULL,
    area_sqft DECIMAL(8,2) DEFAULT NULL,
    floor INT DEFAULT NULL,
    furnished TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS apartment_images (
    image_id INT AUTO_INCREMENT PRIMARY KEY,
    apartment_id INT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roommate_group (
    group_id INT AUTO_INCREMENT PRIMARY KEY,
    apartment_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roommate_group_members (
    group_member_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    tenant_id INT NOT NULL,
    FOREIGN KEY (group_id) REFERENCES roommate_group(group_id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Payment Table
CREATE TABLE IF NOT EXISTS payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    apartment_id INT,
    tenant_id INT,
    group_id INT,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at VARCHAR(100),
    paid_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (apartment_id) REFERENCES apartments(apartment_id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES roommate_group(group_id) ON DELETE CASCADE,
    UNIQUE INDEX idx_transaction_id (transaction_id)
);

GRANT ALL PRIVILEGES ON apartment_rental_db.* TO 'rental_user2'@'%';
FLUSH PRIVILEGES;

SELECT '✅ Setup completed successfully!' as message;
SELECT DATABASE();
SHOW TABLES;

-- Sample data inserts (use actual owner IDs from your users table)
INSERT INTO users (name, email, phone_number, password, role, district, street, house_no) 
VALUES ('John Owner', 'john.owner@example.com', '111-222-3333', 'owner123', 'owner', 'Downtown', 'Main Street', '100');

INSERT INTO users (name, email, phone_number, password, role, district, street, house_no) 
VALUES ('Sarah Owner', 'sarah.owner@example.com', '444-555-6666', 'owner123', 'owner', 'Midtown', 'Oak Avenue', '200');

-- Replace owner_id values below with the actual IDs
INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Modern Downtown Studio', 'Beautiful studio apartment with city views', 1200.00, '2024-01-15', 'Downtown', 'Main Street', '100A', '100A Main Street, Downtown', 'Single professionals', 5);

INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Cozy 2-Bedroom Family Home', 'Spacious apartment perfect for families', 1800.00, '2024-02-01', 'Midtown', 'Oak Avenue', '200B', '200B Oak Avenue, Midtown', 'Families', 6);

INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Luxury Penthouse', 'Stunning penthouse with panoramic views', 3500.00, '2024-01-01', 'Waterfront', 'Marina Drive', '300C', '300C Marina Drive, Waterfront', 'Professionals', 5);

-- Verification queries
SELECT 
    a.apartment_id,
    a.title,
    a.monthly_rent,
    a.district,
    u.name as owner_name,
    u.email as owner_email
FROM apartments a
JOIN users u ON a.owner_id = u.user_id
ORDER BY a.apartment_id;

SELECT COUNT(*) as total_apartments FROM apartments;

-- Search examples
SELECT * FROM apartments WHERE district LIKE '%Downtown%' ORDER BY monthly_rent;
SELECT * FROM apartments WHERE monthly_rent BETWEEN 1000 AND 2500 ORDER BY monthly_rent;
SELECT * FROM apartments WHERE availability <= '2024-01-15' ORDER BY availability;
SELECT user_id, name, email FROM users WHERE role = 'OWNER' ORDER BY user_id DESC;

-- 3. Create Apartment 4 (Budget Studio)
-- Replace X with actual owner_id from step 2
INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Budget Studio', 'Affordable studio perfect for students', 800.00, '2024-01-01', 'University District', 'Campus Road', '400D', '400D Campus Road, University District', 'Students', 6); -- Replace X

-- 4. Create Apartment 5 (Family-Friendly Townhouse)
-- Replace Y with actual owner_id from step 2
INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Family-Friendly Townhouse', 'Spacious townhouse with backyard', 2200.00, '2024-03-01', 'Suburban Heights', 'Maple Street', '500E', '500E Maple Street, Suburban Heights', 'Families', 7); -- Replace Y

-- 5. Create Apartment 6 (Luxury Waterfront Condo)
-- Replace X with actual owner_id from step 2
INSERT INTO apartments (title, description, monthly_rent, availability, district, street, house_no, address, allowed_for, owner_id) 
VALUES ('Luxury Waterfront Condo', 'Premium condo with marina views', 4500.00, '2024-01-15', 'Waterfront', 'Marina Boulevard', '600F', '600F Marina Boulevard, Waterfront', 'Professionals', 6); -- Replace X

-- 6. Verify all apartments
SELECT 
    apartment_id,
    title,
    monthly_rent,
    district,
    availability,
    owner_id
FROM apartments 
ORDER BY apartment_id;

-- View payment records with details
SELECT 
    p.payment_id,
    p.transaction_id,
    p.amount,
    p.status,
    p.payment_method,
    p.created_at,
    a.title as apartment_name,
    u.name as tenant_name,
    u.email as tenant_email
FROM payment p
LEFT JOIN apartments a ON p.apartment_id = a.apartment_id
LEFT JOIN users u ON p.tenant_id = u.user_id
ORDER BY p.payment_id DESC
LIMIT 20;

