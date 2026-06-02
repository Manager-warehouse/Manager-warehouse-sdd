# Feature: Hệ thống Tự động Tính Giá vốn COGS & Áp giá Giao dịch (US-WMS-14)

## 1. Context and Goal
Đảm bảo hệ thống tự động tra cứu chính xác giá vốn (COGS) và giá bán tương ứng từ lịch sử biến động giá (`price_history`) tại ngày phát sinh giao dịch xuất hàng để phục vụ hạch toán tài chính.

## 2. Actors
* **System**: Hệ thống tự động tính toán và áp giá.
* **Kế toán trưởng**: Người giám sát.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always use the approved price valid at the shipment date to calculate COGS and invoice amounts.
* **Event-driven:**
  * WHEN a delivery order is created, the system SHALL look up the `selling_price` valid at the current date from `price_history` where `status = 'APPROVED'`.
  * WHEN calculating COGS for a delivery, the system SHALL use the `cost_price` valid at the shipment date from `price_history` where `status = 'APPROVED'`.
* **State-driven:**
  * WHILE the current date is outside a price history record's effective range (before `effective_date` or after `end_date`), the system SHALL NOT use it.

## 4. API Endpoints
* Không có API riêng biệt; việc tính toán được tích hợp trực tiếp vào luồng xử lý xuất hàng (`POST /api/v1/delivery-orders`) và hóa đơn (`POST /api/v1/invoices`).

## 5. Acceptance Criteria

**Scenario 1: Selling price lookup**
* Given product X has an approved price list with effective date 2026-06-01 to 2026-06-30
* When a delivery order is created on 2026-06-15
* Then the system SHALL use the selling_price from that price list.

**Scenario 2: Block due to missing price**
* Given product X has no price list active on 2026-05-30
* When a delivery order is created on 2026-05-30
* Then the system SHALL block the DO creation with `MISSING_PRICE` error.
