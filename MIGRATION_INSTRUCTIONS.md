# Database Migration Instructions

## Issue: Business Information Not Persisting

The merchant's business information (Business Name and Business Description) needs to be stored in the database. We've added these fields to the backend code, but the database schema needs to be updated.

## Steps to Fix

### Option 1: Using MySQL Command Line

1. Open Terminal and connect to MySQL:
```bash
mysql -u root -p
```

2. Enter your MySQL password when prompted

3. Run these commands:
```sql
USE user_service;

ALTER TABLE users 
ADD COLUMN business_name VARCHAR(200) DEFAULT NULL AFTER address;

ALTER TABLE users 
ADD COLUMN business_description VARCHAR(1000) DEFAULT NULL AFTER business_name;

-- Verify the changes
DESCRIBE users;

-- You should see the new columns: business_name and business_description
```

4. Exit MySQL:
```sql
EXIT;
```

### Option 2: Using the SQL File

Run the migration script directly:
```bash
mysql -u root -p < user-service/add_business_fields.sql
```

Enter your MySQL password when prompted.

### Option 3: Using MySQL Workbench (GUI)

1. Open MySQL Workbench
2. Connect to your local MySQL server
3. Select the `user_service` database
4. Open a new SQL tab
5. Copy and paste the following:
```sql
ALTER TABLE users 
ADD COLUMN business_name VARCHAR(200) DEFAULT NULL AFTER address;

ALTER TABLE users 
ADD COLUMN business_description VARCHAR(1000) DEFAULT NULL AFTER business_name;
```
6. Click the lightning bolt icon to execute
7. Verify by running: `DESCRIBE users;`

## After Migration

Once the database migration is complete, you need to **restart the user-service**:

1. Stop the current user-service (Ctrl+C in the terminal where it's running)
2. Restart it:
```bash
cd user-service/user-service
./gradlew bootRun
```

## Verify It Works

1. Login as a merchant
2. Go to Settings page
3. Click "Edit Profile"
4. Enter Business Name and Business Description
5. Click "Save Changes"
6. **Refresh the page (F5)**
7. ✅ Business information should still be there!

---

**Note:** If you see any errors about "Unknown column 'business_name'", it means the migration hasn't been run yet. Follow the steps above to add the columns to the database.

