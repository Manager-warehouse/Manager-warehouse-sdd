-- =============================================================================
-- V26: Align batches table — add all columns missing from legacy schema
-- =============================================================================
-- Context: DB batches được tạo từ schema cũ thiếu nhiều cột so với V1.
-- V25 đã thêm expiry_date. V26 thêm grade + các cột còn thiếu.
--
-- Chiến lược: ADD COLUMN IF NOT EXISTS để idempotent — an toàn nếu cột đã tồn tại.
-- Sau khi add grade với DEFAULT 'A', constraint CHECK được áp dụng.
-- Dữ liệu hiện có sẽ được gán grade='A' (hàng tốt nhất).
-- =============================================================================

-- 1. Thêm cột grade (BatchGrade enum: A/B/C)
--    Phải dùng DEFAULT trước khi thêm NOT NULL vì có thể đã có rows
ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS grade VARCHAR(1) DEFAULT 'A';

-- Đặt NOT NULL sau khi tất cả rows đã có giá trị
UPDATE batches SET grade = 'A' WHERE grade IS NULL;

ALTER TABLE batches
    ALTER COLUMN grade SET NOT NULL;

-- Thêm CHECK constraint nếu chưa có
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'batches'::regclass
          AND conname = 'batches_grade_check'
    ) THEN
        ALTER TABLE batches
            ADD CONSTRAINT batches_grade_check CHECK (grade IN ('A','B','C'));
    END IF;
END $$;

-- 2. Kiểm tra và đảm bảo các cột bắt buộc khác tồn tại
--    (phòng ngừa trường hợp DB thiếu thêm cột nào nữa)

ALTER TABLE batches
    ADD COLUMN IF NOT EXISTS received_date DATE;

-- Gán received_date = created_at nếu NULL (FIFO fallback)
UPDATE batches
   SET received_date = created_at::DATE
 WHERE received_date IS NULL;

ALTER TABLE batches
    ALTER COLUMN received_date SET NOT NULL;

-- 3. Rebuild indexes
CREATE INDEX IF NOT EXISTS idx_batches_product_expiry
    ON batches(product_id, expiry_date ASC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_batches_product_received
    ON batches(product_id, received_date ASC);

CREATE INDEX IF NOT EXISTS idx_batches_grade
    ON batches(product_id, grade);
