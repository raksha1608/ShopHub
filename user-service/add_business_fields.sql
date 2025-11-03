-- Add business_name and business_description columns to users table
-- Run this script on the user_service database

USE user_service;

-- Add business_name column
ALTER TABLE users 
ADD COLUMN business_name VARCHAR(200) DEFAULT NULL AFTER address;

-- Add business_description column
ALTER TABLE users 
ADD COLUMN business_description VARCHAR(1000) DEFAULT NULL AFTER business_name;

-- Verify the changes
DESCRIBE users;

SELECT 'Migration completed successfully!' AS status;

