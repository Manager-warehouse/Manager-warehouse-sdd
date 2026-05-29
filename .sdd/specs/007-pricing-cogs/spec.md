# Feature Specification: Bảng giá & Giá vốn (Pricing & COGS)

**Spec ID**: 007-pricing-cogs
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-14

---

## 1. Context and Goal

Giá bán và giá vốn hàng hóa thay đổi theo thời gian. Mỗi kỳ kinh doanh có bảng giá riêng
với ngày hiệu lực và hết hạn. Hệ thống cần lưu lịch sử giá (price_history) để tính COGS
chính xác tại ngày xuất hàng và phục vụ báo cáo P&L.

**Goal:** Xây dựng module quản lý bảng giá cho phép Kế toán viên tạo, Kế toán trưởng duyệt,
lưu lịch sử biến động giá, và tra cứu giá hiệu lực tại thời điểm xuất hàng.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Kế toán viên | Tạo bảng giá mới (Maker) |
| Kế toán trưởng | Duyệt bảng giá (Checker) |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always record price changes in the `price_history` table
  with effective_date, end_date, cost_price, selling_price, and actor info.
- The system SHALL always use the price valid at the shipment date to calculate
  COGS and invoice amounts.

**Event-driven:**
- WHEN a Kế toán viên creates a price list, the system SHALL require:
  product_id, cost_price, selling_price, effective_date, end_date.
- WHEN a Kế toán viên submits a price list for approval, the system SHALL
  set status to PENDING and notify Kế toán trưởng.
- WHEN a Kế toán trưởng approves a price list, the system SHALL:
  - Set status to APPROVED
  - Create entries in price_history with effective range
- WHEN a delivery order is created, the system SHALL look up the selling_price
  valid at the current date from price_history.
- WHEN calculating COGS for a delivery, the system SHALL use the cost_price
  valid at the shipment date.

**State-driven:**
- WHILE a price list is PENDING, the system SHALL NOT use it for any transaction.
- WHILE the current date is outside a price list's effective range
  (before effective_date or after end_date), the system SHALL NOT use it.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Price lookup at transaction time | ≤ 100ms |
| NFR-002 | Price history query for a product | ≤ 500ms |
| NFR-003 | Excel import for 1000 SKUs | ≤ 5s |

## 5. Data Model

### PriceList
- `id`, `name`, `status` (DRAFT / PENDING / APPROVED / REJECTED),
  `created_by` (FK), `approved_by` (FK), `approved_at`

### PriceHistory
- `id`, `product_id` (FK), `price_list_id` (FK),
  `effective_date` (DATE), `end_date` (DATE),
  `cost_price` (DECIMAL), `selling_price` (DECIMAL),
  `created_by` (FK), `approved_by` (FK), `approved_at`

## 6. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/price-lists | Bearer | List price lists |
| POST | /api/v1/price-lists | ACCOUNTANT | Create price list |
| GET | /api/v1/price-lists/{id} | Bearer | Get detail with items |
| PUT | /api/v1/price-lists/{id}/submit | ACCOUNTANT | Submit for approval |
| PUT | /api/v1/price-lists/{id}/approve | ACCOUNTANT_MANAGER | Approve price list |
| PUT | /api/v1/price-lists/{id}/reject | ACCOUNTANT_MANAGER | Reject price list |
| POST | /api/v1/price-lists/import | ACCOUNTANT | Import from Excel |
| GET | /api/v1/products/{id}/price-history | Bearer | Get price history |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| OVERLAPPING_EFFECTIVE_DATE | 409 | New price overlaps existing active period |
| PRICE_LIST_ALREADY_APPROVED | 409 | Duplicate approval |
| MISSING_PRICE | 400 | Product has no active price at transaction date |
| PRICE_LIST_PENDING | 400 | Cannot use price list not yet approved |

## 8. Acceptance Criteria

1. Given product X has a price list with effective date 2026-06-01 to 2026-06-30,
   when a delivery order is created on 2026-06-15,
   then the system SHALL use the selling_price from that price list.
2. Given product X has no price list active on 2026-05-30,
   when a delivery order is created on 2026-05-30,
   then the system SHALL block with MISSING_PRICE error.
3. Given a price list in PENDING status,
   when any system component tries to reference it,
   then the system SHALL reject.

## 9. Out of Scope

- Automated price optimization / dynamic pricing
- Multi-currency pricing
- Promotional/discount pricing rules
- Bulk pricing tiers by dealer segment
