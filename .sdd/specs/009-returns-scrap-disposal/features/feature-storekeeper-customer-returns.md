# Feature: Thủ kho & Kế toán Xử lý Hàng hoàn trả từ Đại lý (US-WMS-24)

## 1. Context and Goal
Đại lý thực hiện trả hàng (lỗi hoặc thừa). Thủ kho lập phiếu nhận hàng hoàn (receipt type = 'RETURN'), Nhân viên kho kiểm QC để phân loại hàng Đạt (vào kho thường) / Lỗi (vào Quarantine). Kế toán lập Credit Note cấn trừ công nợ Đại lý.

## 2. Actors
* **Thủ kho**: Nhập phiếu trả hàng.
* **Nhân viên kho (QC)**: Kiểm QC và di chuyển hàng Đạt / Lỗi.
* **Kế toán viên**: Tạo Credit Note cấn trừ tiền.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always route returned goods through QC before admitting them back into available inventory.
* **Event-driven:**
  * WHEN a Thủ kho creates a Return Receipt from a dealer, the system SHALL create a receipt record with type = 'RETURN' requiring: dealer_id, warehouse_id, contact_person, and source_order_code (DO code).
  * WHEN QC inspects returned goods:
    * IF goods pass QC: add to regular inventories, update location_id to a regular bin.
    * IF goods fail QC: move to Quarantine locations.
  * WHEN Kế toán viên creates a Credit Note for returned goods, the system SHALL:
    * Create a `credit_notes` record referencing the receipt.
    * Decrease dealer's `current_balance` by the credit note amount.

## 4. API Endpoints
* `POST /api/v1/returns` - Lập phiếu nhận hàng hoàn (Thủ kho).
* `PUT /api/v1/returns/{id}/qc` - Nhập kết quả QC hàng hoàn (Nhân viên kho).
* `POST /api/v1/returns/{id}/credit-note` - Tạo Credit Note cấn trừ công nợ (Kế toán).

## 5. Acceptance Criteria

**Scenario 1: Inbound returns split QC**
* Given a dealer returns 10 units of product A
* When QC inspects them and marks 8 as passed and 2 as failed
* Then the system SHALL add 8 units to available stock HN and put 2 units in the Quarantine location.

**Scenario 2: Deduct dealer balance by Credit Note**
* Given a dealer has a current balance of `100M`
* When Kế toán viên creates a Credit Note of `5M` for them
* Then the dealer's `current_balance` SHALL be reduced to `95M`.
