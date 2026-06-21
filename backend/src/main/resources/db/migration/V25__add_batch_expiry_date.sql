-- =============================================================================
-- V25: Align batches table — add expiry_date column if missing
-- =============================================================================
-- Context: Hibernate validate báo thiếu cột expiry_date trong table batches.
-- Cột này có trong V1__init_schema.sql nhưng DB thực tế không có
-- (có thể DB được khởi tạo từ snapshot cũ hoặc V1 đã bị replay khác).
--
-- Fix: ADD COLUMN IF NOT EXISTS để an toàn kể cả khi cột đã tồn tại.
-- expiry_date = NULL → hàng gia dụng thông thường (FIFO)
-- expiry_date ≠ NULL → sản phẩm has_expiry=true, dùng FEFO
-- =============================================================================

ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS expiry_date DATE;  -- NULL = không có hạn; dùng cho FEFO

-- Rebuild index nếu chưa tồn tại
CREATE INDEX IF NOT EXISTS idx_batches_product_expiry
    ON batches(product_id, expiry_date ASC NULLS LAST);
