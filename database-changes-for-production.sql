-- =========================================================================
-- Smart Dispatch - Production Database Migration Scripts
-- Apply these SQL queries to your Production PostgreSQL database
-- =========================================================================

-- 1. Remove the old Enum constraint from the Orders table
-- Reason: We transitioned from `CREATED` status to `REQUESTED`, `PAYMENT_PENDING`, etc. 
-- The old check constraint was rejecting the new enum values.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- 2. Remove the old Enum constraint from the Order_Timeline table
-- Reason: Same as above. The timeline uses the same statuses.
ALTER TABLE order_timeline DROP CONSTRAINT IF EXISTS order_timeline_status_check;

-- 3. Update all legacy orders that had 'CREATED' to 'REQUESTED'
-- Reason: We removed the CREATED enum. Old records will cause runtime exceptions (No enum constant).
UPDATE orders SET status = 'REQUESTED' WHERE status = 'CREATED';
UPDATE order_timeline SET status = 'REQUESTED' WHERE status = 'CREATED';

-- 4. Increase the length of `payment_url` in the payments table
-- Reason: Stripe Checkout Session URLs are significantly longer than 255 characters (VARCHAR default).
-- We change it to TEXT so it can store urls of any length.
ALTER TABLE payments ALTER COLUMN payment_url TYPE text;

-- =========================================================================
-- End of Migration Script
-- =========================================================================
