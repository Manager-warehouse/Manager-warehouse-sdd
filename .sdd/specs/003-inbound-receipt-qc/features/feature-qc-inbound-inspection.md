# Feature: Nhân viên kho Kiểm tra Chất lượng Inbound theo Sample (US-WMS-04)

## 1. Context and Goal
Nhân viên kho (WAREHOUSE_STAFF) kiểm tra ngoại quan và chất lượng bằng cách lấy mẫu theo từng lô hàng, ghi nhận số lượng mẫu đạt/lỗi và lý do QC. Storekeeper (STOREKEEPER) rà soát kết quả QC mẫu trước khi chuyển phiếu sang trạng thái `QC_COMPLETED` hoặc `QC_FAILED`. Hàng đạt chuẩn được giữ để Trưởng kho phê duyệt nhập kho, còn hàng lỗi QC mẫu được đánh dấu cần xử lý quarantine/RTV bởi Trưởng kho. Bước QC không tạo batch và không ghi tăng bất kỳ tồn kho nào.

## 2. Actors
* **Nhân viên kho (WAREHOUSE_STAFF) kiêm QC Staff**: Thực hiện kiểm tra ngoại quan/chất lượng theo mẫu trên từng lô và ghi nhận số lượng mẫu đạt/lỗi cùng lý do.
* **Storekeeper (STOREKEEPER)**: Rà soát và kết luận kết quả QC mẫu, xác nhận phân loại đạt/lỗi trước khi chuyển phiếu sang `QC_COMPLETED` hoặc `QC_FAILED`; không duyệt hoặc từ chối phiếu nhập kho chính thức.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create `RECEIPT_QC_SUBMIT` and `RECEIPT_QC_CONFIRM` audit log entries for QC result submission and Storekeeper confirmation.
* **Event-driven:**
  * WHEN a receipt item belongs to a supplier with fewer than 5 previous `APPROVED` receipts, the system SHALL default `qc_sampling_method` to `FULL_INSPECTION`.
  * WHEN a receipt item belongs to a supplier with at least 5 previous `APPROVED` receipts, the system SHALL default `qc_sampling_method` to `RANDOM_SAMPLE`.
  * WHEN a Warehouse Staff submits QC sample results on a `DRAFT` receipt, the system SHALL:
    * Record `sample_qty`, `sample_passed_qty`, `sample_failed_qty`, and `qc_sampling_method` for each receipt item/lot.
    * Ensure `sample_passed_qty + sample_failed_qty = sample_qty`.
    * Set `qc_result` status: `PENDING`/`PASSED`/`FAILED`/`PARTIAL`.
    * The system SHALL NOT create or update batch records at this step.
    * The system SHALL NOT create or increase inventory at this step.
  * WHEN a Storekeeper confirms sample results that pass quality thresholds, the system SHALL update the receipt status to `QC_COMPLETED`.
  * WHEN a Storekeeper confirms sample results that fail quality thresholds, the system SHALL:
    * Update the receipt status to `QC_FAILED`.
    * Mark the whole failed lot as requiring Trưởng kho quarantine/RTV handling.
    * The system SHALL NOT create or update batch records at this step.
    * The system SHALL NOT increase regular inventory or quarantine inventory at this step.
* **State-driven:**
  * WHILE a receipt is `DRAFT`, the system SHALL treat all QC sample data as inspection data only and SHALL NOT update batch, regular inventory, or quarantine inventory.
  * WHILE a receipt is `QC_COMPLETED`, the system SHALL hold the lot for Trưởng kho approval and SHALL NOT increase available inventory until `APPROVED`.
  * WHILE a receipt is `QC_FAILED`, the system SHALL prevent official receipt approval and SHALL wait for Trưởng kho quarantine/RTV handling; inventory remains unchanged until that handling decision is confirmed.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/qc` - Nhập kết quả QC đạt/lỗi và lý do cụ thể cho hàng lỗi.

## 5. Acceptance Criteria
* **Scenario: Sample-based QC on a receipt lot**
  * Given a receipt in `DRAFT` state with actual count 100 units
  * When Warehouse Staff inspects a random sample of 10 units and records 9 passed and 1 failed due to ngoại quan
  * And Storekeeper confirms the QC result
  * Then the system SHALL mark the receipt `QC_FAILED` and flag the full 100 units for Trưởng kho quarantine/RTV handling.
  * And the system SHALL NOT create batch records, increase regular inventory, or increase quarantine inventory.
