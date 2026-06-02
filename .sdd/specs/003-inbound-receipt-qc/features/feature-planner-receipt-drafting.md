# Feature: Planner Tiếp nhận & Lập Lệnh Nhập kho (US-WMS-02)

## 1. Context and Goal
Planner tiếp nhận thông tin hàng về từ Công ty mẹ qua các kênh thủ công (Zalo/Email) và lập Lệnh nhập kho thô trên hệ thống dưới trạng thái chờ tiếp nhận thực tế.

## 2. Actors
* **Planner (Người lập lệnh)**: Nhận thông tin và lập lệnh nhập kho thô trên hệ thống.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always generate a unique receipt code for every receipt document.
* **Event-driven:**
  * WHEN a Planner creates a receipt, the system SHALL require: warehouse_id, expected items (product_id + expected_qty), source reference (PO number hoặc DO hoàn), and source channel (Zalo/Email).

## 4. API Endpoints
* `POST /api/v1/receipts` - Lập phiếu nhập kho thô (trạng thái: `PENDING_RECEIPT`).

## 5. Acceptance Criteria
* **Scenario: Draft a new Inbound Receipt**
  * Given a Planner receives info about 500 units of product A via Zalo
  * When they create the receipt with warehouse_id = HP and channel = Zalo
  * Then the system SHALL create the document with status `PENDING_RECEIPT` and assign a unique receipt number.
