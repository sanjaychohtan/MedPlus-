-- V3: Add expiry date and usage limit to coupons table
ALTER TABLE coupons ADD COLUMN expiry_date DATE;
ALTER TABLE coupons ADD COLUMN usage_limit INT;
