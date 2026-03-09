-- Add code to companies for login identification
ALTER TABLE companies ADD COLUMN code VARCHAR(50) UNIQUE;

-- Add username and warehouse_id to users
ALTER TABLE users ADD COLUMN username VARCHAR(100);
ALTER TABLE users ADD COLUMN warehouse_id UUID REFERENCES warehouses(id);

-- Create index for company code lookup
CREATE INDEX idx_companies_code ON companies(code);

-- Create index for username lookup
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email_company ON users(email, company_id);
CREATE INDEX idx_users_username_company ON users(username, company_id);
