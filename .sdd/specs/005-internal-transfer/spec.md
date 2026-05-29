# Feature Specification: Điều chuyển Kho Nội bộ (Internal Transfer)

**Spec ID**: 005-internal-transfer
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-11, US-WMS-12

---

## 1. Context and Goal

Phúc Anh vận hành 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). Hàng hóa cần được
điều chuyển giữa các kho để cân bằng tồn kho, tránh đứt gãy nguồn cung. Hệ thống sử
dụng kho ảo In-Transit để track hàng đang trên đường vận chuyển bằng xe nội bộ.

**Goal:** Xây dựng luồng điều chuyển nội bộ từ gợi ý (Planning Dashboard) → tạo phiếu →
phê duyệt → xuất hàng → In-Transit → xác nhận nhận → xử lý chênh lệch.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Planner | Xem Planning Dashboard, tạo phiếu điều chuyển |
| Trưởng kho nguồn | Duyệt phiếu điều chuyển |
| Thủ kho nguồn | Xuất hàng lên xe |
| Trưởng kho đích | Xác nhận nhận hàng |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always route all inter-warehouse transfers through a virtual
  In-Transit warehouse for tracking.
- The system SHALL always enforce `source_warehouse_id ≠ dest_warehouse_id`.

**Event-driven:**
- WHEN a Planner creates a transfer order, the system SHALL require:
  source_warehouse, dest_warehouse, items (product + batch + quantity_sent).
- WHEN a Trưởng kho nguồn approves a transfer, the system SHALL verify
  available inventory at the source before approval.
- WHEN a Thủ kho nguồn confirms shipment, the system SHALL:
  - Decrease source warehouse inventory: `quantity -= quantity_sent`
  - Increase In-Transit warehouse: `quantity += quantity_sent`
  - Update status to IN_TRANSIT
- WHEN a Trưởng kho đích confirms receipt:
  - IF `quantity_received == quantity_sent`:
    - Decrease In-Transit, increase destination inventory
    - Set status to COMPLETED
  - IF `quantity_received ≠ quantity_sent`:
    - Create an adjustment record for the discrepancy
    - Log the reason in the audit trail
    - Set status to COMPLETED_WITH_DISCREPANCY

**Optional:**
- WHERE the Planning Dashboard is enabled, the system SHALL compare available
  inventory against minimum stock thresholds and suggest inter-warehouse transfers.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Transfer creation + inventory update | ≤ 2s |
| NFR-002 | In-Transit inventory query | Real-time (≤ 500ms) |
| NFR-003 | Discrepancy adjustment record | Must be immutable after creation |

## 5. Data Model

### TransferOrder
- `id`, `transfer_code` (UNIQUE), `source_warehouse_id` (FK), `dest_warehouse_id` (FK),
  `created_by` (FK), `approved_by` (FK), `status` (DRAFT → PENDING_APPROVAL → APPROVED →
  IN_TRANSIT → COMPLETED / COMPLETED_WITH_DISCREPANCY / CANCELLED),
  `notes`, `version`

### TransferOrderItem
- `id`, `transfer_order_id` (FK), `product_id` (FK), `batch_id` (FK),
  `quantity_sent`, `quantity_received`

### Inventory (shared entity)
- In-Transit warehouse: a special warehouse record with `is_virtual = true`

## 6. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/transfers | Bearer | List transfers (filterable) |
| POST | /api/v1/transfers | PLANNER | Create transfer order |
| GET | /api/v1/transfers/{id} | Bearer | Get transfer detail |
| PUT | /api/v1/transfers/{id}/approve | WAREHOUSE_MANAGER | Approve transfer |
| PUT | /api/v1/transfers/{id}/ship | STORE_KEEPER | Confirm shipment → In-Transit |
| PUT | /api/v1/transfers/{id}/receive | WAREHOUSE_MANAGER | Confirm receipt → complete |
| PUT | /api/v1/transfers/{id}/cancel | PLANNER | Cancel transfer |

### Planning Dashboard
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/planning/suggestions | PLANNER | Get auto-suggested transfers |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| SAME_WAREHOUSE | 422 | source = dest |
| INSUFFICIENT_TRANSFER_STOCK | 422 | Source warehouse lacks available qty |
| TRANSFER_ALREADY_APPROVED | 409 | Duplicate approval |
| DISCREPANCY_REQUIRES_REASON | 400 | Quantity mismatch without reason |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

## 8. Acceptance Criteria

1. Given source warehouse has qty = 50 of product X,
   when Planner creates transfer of 30 units to dest warehouse and Trưởng kho approves,
   then source inventory SHALL show qty = 20 after shipment confirmation.
2. Given 30 units shipped (status IN_TRANSIT),
   when Trưởng kho đích confirms receipt of 28 units (2 units short),
   then system SHALL create discrepancy adjustment and set status COMPLETED_WITH_DISCREPANCY.
3. Given source warehouse has qty = 10,
   when Planner attempts to transfer 20 units,
   then system SHALL reject with INSUFFICIENT_TRANSFER_STOCK.

## 9. Out of Scope

- Automated replenishment algorithms (batch job suggestion only)
- Multi-warehouse transfer optimization
- Transfer cost tracking
- Third-party logistics (3PL) — internal fleet only
