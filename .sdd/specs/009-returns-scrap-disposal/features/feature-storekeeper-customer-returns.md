# Feature: Thủ kho & Kế toán Xử lý Hàng hoàn trả từ Đại lý (US-WMS-24)

## 1. Context and Goal
Đại lý thực hiện trả hàng (lỗi hoặc thừa). Thủ kho lập phiếu nhận hàng hoàn (receipt type = 'RETURN') và kiểm QC để phân loại hàng Đạt (vào kho thường) / Lỗi (vào Quarantine). Kế toán lập Credit Note cấn trừ công nợ Đại lý.

## 2. Actors
* **Thủ kho kiêm QC**: Nhập phiếu trả hàng, kiểm QC và chỉ định xử lý hàng Đạt / Lỗi.
* **Nhân viên kho**: Hỗ trợ bốc xếp, di chuyển hàng hoàn theo chỉ dẫn của Thủ kho.
* **Kế toán viên**: Tạo Credit Note cấn trừ tiền.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always route returned goods through QC before admitting them back into available inventory.
  * The system SHALL verify the acting Thủ kho is assigned to the return receipt's `warehouse_id`, in addition to the role check, before allowing create/QC actions.
  * The system SHALL allow return receipt create and return QC actions only for warehouse operation roles (`WAREHOUSE_STAFF`, `STOREKEEPER`, `WAREHOUSE_MANAGER`, `ADMIN`, `CEO`); Accountant roles SHALL create Credit Notes only and SHALL NOT create/QC return receipts.
  * Return receipt item responses SHALL expose the canonical `receipt_item_id` used by the QC request payload; clients SHALL submit this identifier as `receiptItemId`.
  * Return receipt list/detail responses SHALL include dealer/source context needed by the UI and accounting handoff: `dealer_id`, `dealer_name`, `delivery_order_id`, `source_order_code`, and `credit_note_generated`.
  * The system SHALL create an audit log entry (actor, actor_role, action, entity_type, entity_id, timestamp, warehouse_id, before/after) for return receipt creation, QC result recording, and Credit Note creation.
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
* `PUT /api/v1/returns/{id}/qc` - Nhập kết quả QC hàng hoàn (Thủ kho).
* `POST /api/v1/returns/{id}/credit-note` - Tạo Credit Note cấn trừ công nợ (Kế toán).

### Return receipt response contract

Return receipt list/detail responses SHALL include:

* `dealer_id` and `dealer_name` - Đại lý trả hàng.
* `delivery_order_id` and `source_order_code` - Đơn xuất gốc được dùng để kiểm soát số lượng hoàn trả.
* `credit_note_generated` - Whether a Credit Note already exists for the return receipt.
* `items[].receipt_item_id` - Receipt item identifier required by the QC payload as `receiptItemId`.

## 5. Acceptance Criteria

**Scenario 1: Inbound returns split QC**
* Given a dealer returns 10 units of product A
* When a warehouse operation user submits QC using each returned item's `receiptItemId` and marks 8 as passed and 2 as failed
* Then the system SHALL add 8 units to available stock HN and put 2 units in the Quarantine location.

**Scenario 2: Deduct dealer balance by Credit Note**
* Given a dealer has a current balance of `100M`
* When Kế toán viên creates a Credit Note of `5M` for them
* Then the dealer's `current_balance` SHALL be reduced to `95M`.

**Scenario 3: Accountant cannot create or QC return receipt**
* Given the authenticated user has role `ACCOUNTANT` or `CHIEF_ACCOUNTANT`
* When the user attempts to create a return receipt or submit return QC
* Then the system SHALL reject the warehouse operation and the UI SHALL NOT present those actions.
