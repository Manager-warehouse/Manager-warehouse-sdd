-- Remove product grading from inbound receipt items.

ALTER TABLE receipt_items
    DROP COLUMN IF EXISTS grade;
