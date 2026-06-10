# Feature: Planner Tiếp nhận & Lập Lệnh Nhập kho (US-WMS-02)

## 1. Context and Goal
Planner tiếp nhận thông tin hàng mua từ supplier qua các kênh thủ công (Zalo/Email) và lập Lệnh nhập kho thô trên hệ thống dưới trạng thái chờ tiếp nhận thực tế. Feature này chỉ áp dụng cho đơn mua inbound, không áp dụng cho luồng hoàn hàng.

## 2. Actors

- **Planner (Người lập lệnh)**: Nhận thông tin và lập lệnh nhập kho thô trên hệ thống.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always generate a unique receipt code for every receipt document.
  * The system SHALL only allow this feature to create purchase receipts for supplier inbound orders, not return receipts.
  * The system SHALL treat `source_channel` as an enum field with only two allowed values: `ZALO` and `EMAIL`.
  * The system SHALL create a `RECEIPT_CREATE` audit log entry for every successful receipt creation, including actor, source reference, warehouse, source channel, and before/after status.
* **Event-driven:**
  * WHEN a Planner creates a receipt, the system SHALL require: supplier_id, contact_person, warehouse_id, expected items, source reference (PO number), and source channel.
  * WHEN a Planner creates a receipt, the system SHALL require each expected item to contain exactly one product_id and one expected_qty.
  * WHEN a Planner creates a receipt, the system SHALL validate that each expected_qty is an integer greater than zero and that each product referenced is active.
  * WHEN a Planner creates a receipt, the system SHALL reject duplicate source references for the same supplier and warehouse.
  * WHEN a Planner creates a receipt, the system SHALL reject any attempt to create a return receipt in this flow.

## 4. API Endpoints

- `POST /api/v1/receipts` - Lập phiếu nhập kho thô (trạng thái: `PENDING_RECEIPT`).

## 5. Acceptance Criteria
* **Scenario: Draft a new Inbound Receipt**
  * Given a Planner receives info about 500 units of product A via Zalo
  * When they create the receipt with supplier_id, contact_person, warehouse_id = HP, channel = Zalo, and expected item data
  * Then the system SHALL create the document with status `PENDING_RECEIPT` and assign a unique receipt number.

* **Scenario: Single product per expected item**
  * Given a Planner adds an expected item to the receipt
  * When the item is saved
  * Then the system SHALL store exactly one `product_id` and one integer `expected_qty` greater than 0 for that line.

* **Scenario: Reject missing mandatory fields**
  * Given a Planner omits supplier_id or contact_person
  * When they submit the receipt
  * Then the system SHALL reject the request and show a validation error.

* **Scenario: Reject non-purchase receipt**
  * Given a Planner tries to create a receipt for a return order
  * When they submit the request through this flow
  * Then the system SHALL reject it because this feature is for supplier purchase receipts only.

* **Scenario: Reject invalid receipt items**
  * Given a receipt item has quantity less than or equal to zero or references an inactive product
  * When the Planner submits the receipt
  * Then the system SHALL reject the request and list the invalid item fields.

* **Scenario: Reject duplicate source reference**
  * Given a supplier and warehouse already have a receipt for the same PO number
  * When the Planner attempts to create another receipt with the same source reference
  * Then the system SHALL reject the duplicate receipt creation.
