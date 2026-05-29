# Feature Specification: Kiểm kê & Điều chỉnh Tồn kho (StockTake & Adjustment)

**Spec ID**: 006-stocktake-adjustment
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-13

---

## 1. Context and Goal

Kiểm kê kho là quy trình đối chiếu số liệu tồn kho trên hệ thống với số lượng thực tế
trong kho. Chênh lệch cần được xử lý qua quy trình phê duyệt theo thẩm quyền (bảng định
mức: Trưởng kho 5-100M, CEO >100M) trước khi điều chỉnh inventory.

**Goal:** Xây dựng luồng kiểm kê từ tạo phiếu → khóa sổ → đếm thực tế → nhập kết quả →
tính chênh lệch → phê duyệt → điều chỉnh inventory, đảm bảo audit trail đầy đủ.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Thủ kho | Tạo phiếu kiểm kê, đếm thực tế, nhập kết quả |
| Trưởng kho | Duyệt chênh lệch 5-100M |
| CEO | Duyệt chênh lệch >100M hoặc lỗi nhân viên |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always calculate variance as `actual_qty - system_qty`.
- The system SHALL always require approval before applying inventory adjustments.

**Event-driven:**
- WHEN a Thủ kho creates a StockTake for specific bin locations, the system SHALL
  pre-fill system quantities from current inventory records.
- WHEN a Thủ kho starts a StockTake (status → IN_PROGRESS), the system SHALL
  lock the affected bin locations to prevent concurrent receipt/issue operations.
- WHEN a Thủ kho enters actual counts, the system SHALL auto-calculate variance
  for each StockTakeItem.
- WHEN a StockTake is completed (status → COMPLETED), the system SHALL calculate
  total variance amount.
- WHEN a user requests approval:
  - IF variance < 5M VND: auto-approve (if QC/stocktake confirmed)
  - IF 5M ≤ variance ≤ 100M VND: route to Trưởng kho for approval
  - IF variance > 100M VND OR cause = employee error: route to CEO
- WHEN an approval is granted, the system SHALL:
  - Update inventory.quantity to actual_qty for each item
  - Create audit log with before/after quantities
  - Set StockTake status to ADJUSTED

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | StockTake creation with 100+ items | ≤ 3s |
| NFR-002 | Variance calculation | Real-time (≤ 1s on item entry) |
| NFR-003 | Bin lock operations | ≤ 100ms overhead per transaction |

## 5. Data Model

### StockTake
- `id`, `stocktake_code` (UNIQUE), `warehouse_id` (FK), `created_by` (FK),
  `approved_by` (FK), `status` (DRAFT → IN_PROGRESS → COMPLETED → APPROVED →
  ADJUSTED / REJECTED), `total_variance_amount`, `notes`, `version`

### StockTakeItem
- `id`, `stocktake_id` (FK), `product_id` (FK), `bin_location_id` (FK),
  `system_qty`, `actual_qty`, `variance_qty`, `variance_amount`, `notes`

## 6. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/stocktakes | Bearer | List stocktakes (filterable) |
| POST | /api/v1/stocktakes | STORE_KEEPER | Create stocktake (pre-filled) |
| GET | /api/v1/stocktakes/{id} | Bearer | Get detail with items + variance |
| PUT | /api/v1/stocktakes/{id}/start | STORE_KEEPER | Start stocktake → lock bins |
| PUT | /api/v1/stocktakes/{id}/count | STORE_KEEPER | Enter actual counts |
| PUT | /api/v1/stocktakes/{id}/complete | STORE_KEEPER | Mark completed |
| PUT | /api/v1/stocktakes/{id}/approve | WAREHOUSE_MANAGER / CEO | Approve variance |
| PUT | /api/v1/stocktakes/{id}/adjust | SYSTEM | Apply adjustment to inventory |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| BIN_ALREADY_LOCKED | 409 | Bin in another active stocktake |
| STOCKTAKE_NOT_COMPLETED | 400 | Trying to approve incomplete stocktake |
| APPROVAL_AUTHORITY_EXCEEDED | 403 | User lacks authority for variance amount |
| INVENTORY_VERSION_CONFLICT | 409 | Inventory changed during stocktake |
| NO_VARIANCE | 400 | No difference to adjust |

## 8. Acceptance Criteria

1. Given a product with system_qty = 50,
   when Thủ kho enters actual_qty = 48,
   then variance SHALL = -2.
2. Given a stocktake with variance = 80M VND,
   when a user with WAREHOUSE_MANAGER role approves,
   then system SHALL accept (within 5-100M range).
3. Given a stocktake with variance = 150M VND,
   when a user with WAREHOUSE_MANAGER role approves,
   then system SHALL reject and route to CEO.
4. Given an approved stocktake,
   when adjustment executes,
   then inventory.quantity SHALL equal actual_qty exactly.

## 9. Out of Scope

- Cycle counting (perpetual inventory)
- ABC classification-based counting frequency
- Barcode scanner for counting
- Historical stocktake comparison reports
