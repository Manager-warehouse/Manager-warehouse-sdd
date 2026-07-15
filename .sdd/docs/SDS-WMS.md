**Warehouse Management System (WMS) — Công ty Phúc Anh**

**Software Design Specification**

– Hà Nội, 2026 –

# Record of Changes

| Date       | A\*M, D | In charge    | Change Description                                                                           |
| ---------- | ------- | ------------ | -------------------------------------------------------------------------------------------- |
| 2026-07-14 | A       | WMS Dev Team | Khởi tạo SDS dựa trên backend thực tế `backend/src/main/java/com/wms/`, tương ứng RDS-WMS.md |
| 2026-07-15 | M       | WMS Dev Team | Đồng bộ catalog spec 001–012; Spec 011–012 là quality/testing cross-cutting. SDS này vẫn mô tả chi tiết luồng inbound đại diện của Spec 003. |

_A - Added M - Modified D - Deleted_

---

# I. Overview

## 1. Code Packages

Backend theo layered architecture bắt buộc **Controller → Service → Repository → Entity** (xem `backend/CLAUDE.md`), main package `com.wms`. Package diagram tổng quan:

```
com.wms
 ├── controller ───► service / service.impl ───► repository ───► entity
 │                         │
 │                         ├──► dto.request / dto.response / dto.auth
 │                         ├──► enums (state machines, role, action)
 │                         ├──► exception (typed exceptions)
 │                         ├──► mapper
 │                         └──► util (FIFOSelector, AuditLogUtil, ...)
 ├── config (Security, JWT filter, Flyway, mail, UploadResourceConfig)
 └── aop (cross-cutting behavior khi có)
```

**_Package descriptions_**

| No  | Package                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                           |
| --- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `com.wms.controller`                                                | REST controllers dưới `/api/v1/...`. Chỉ nhận HTTP request, validate DTO (`@Valid`), trả HTTP status, không chứa business logic. Lấy actor hiện tại qua `CurrentUserService`/Spring Security context.                                                                                                                                                                                                                 |
| 02  | `com.wms.service` / `com.wms.service.impl`                          | Business logic, quản lý transaction (`@Transactional`), authorization (role + warehouse scope), state transition, audit logging, invariant tồn kho/QC/transfer/accounting. Một số service (VD `ReceiptService`, `ReceiptApprovalService`, `QuarantineRtvService`) được implement trực tiếp không tách interface/impl; một số khác (VD `PaymentReceiptService`, `AutoInvoiceService`) tách interface + `*ServiceImpl`. |
| 03  | `com.wms.repository`                                                | Spring Data JPA interface, không dùng raw SQL trong application code.                                                                                                                                                                                                                                                                                                                                                 |
| 04  | `com.wms.entity`                                                    | JPA entity mapping bảng DB và quan hệ, ưu tiên Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`, không dùng `@Data` cho entity có lazy relationship.                                                                                                                                                                                                                                          |
| 05  | `com.wms.dto.request` / `com.wms.dto.response` / `com.wms.dto.auth` | DTO cho request (Jakarta Validation) và response, không trả entity trực tiếp từ controller.                                                                                                                                                                                                                                                                                                                           |
| 06  | `com.wms.enums`                                                     | State machine và constant miền (VD `ReceiptStatus`, `ReceiptType`, `UserRole`, `InterWarehouseTransferStatus`, `AuditAction`, `AuditEntityType`).                                                                                                                                                                                                                                                                     |
| 07  | `com.wms.exception`                                                 | Typed exception + `GlobalExceptionHandler` — mapping lỗi tập trung.                                                                                                                                                                                                                                                                                                                                                   |
| 08  | `com.wms.config`                                                    | Security config, JWT filter, Flyway, mail config, `UploadResourceConfig` cho `/uploads/**`.                                                                                                                                                                                                                                                                                                                           |
| 09  | `com.wms.mapper`                                                    | DTO ⇄ entity mapping helper.                                                                                                                                                                                                                                                                                                                                                                                          |
| 10  | `com.wms.util`                                                      | Utility tập trung, không tạo side-effect nghiệp vụ ẩn (VD `FIFOSelector`, `AuditLogUtil` — build diff before/after, filter sensitive field).                                                                                                                                                                                                                                                                          |
| 11  | `com.wms.aop`                                                       | Cross-cutting concern khi cần (logging, timing...).                                                                                                                                                                                                                                                                                                                                                                   |

Frontend (React 18 + Tailwind, ngoài phạm vi chi tiết của SDS backend này): `components/` (PascalCase), `pages/`, `hooks/` (camelCase), `services/` (API client Axios), `stores/` (Zustand), `utils/`.

## 2. Database Design

### a. Database Schema

PostgreSQL 18, quản lý version qua Flyway (`backend/src/main/resources/db/migration/`), naming convention `snake_case`. Nhóm quan hệ chính (chi tiết ERD từng domain nên vẽ riêng khi cần trình bày trực quan):

```
users ──< user_warehouse_assignments >── warehouses ──< warehouse_locations
warehouses ──< receipts ──< receipt_items >── products ──< batches
receipts ──< adjustments (RETURN_TO_VENDOR) ──< debit_notes
warehouses ──< delivery_orders ──< delivery_order_items ──< delivery_order_item_allocations
delivery_orders ──< deliveries ──< delivery_otp_attempts
warehouses ──< transfers ──< transfer_items
warehouses ──< inventories >── products >── batches >── warehouse_locations
warehouses ──< stocktakes ──< stocktake_items
products ──< price_history
dealers ──< invoices ──< invoice_lines
dealers ──< payment_receipts
dealers ──< credit_notes
all mutating tables ──> audit_logs (append-only, actor_id nullable cho SYSTEM)
```

### b. Table Description

| No  | Table                             | Description                                                                                                                                                                                                                 |
| --- | --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `users`                           | Tài khoản người dùng, `role` (VARCHAR CHECK theo `UserRole` enum: `ADMIN, CEO, WAREHOUSE_MANAGER, STOREKEEPER, WAREHOUSE_STAFF, ACCOUNTANT, ACCOUNTANT_MANAGER, PLANNER, DISPATCHER, DRIVER`).                              |
| 02  | `user_warehouse_assignments`      | Gán user vào 1+ kho cho RBAC warehouse-scope.                                                                                                                                                                               |
| 03  | `warehouses`                      | 3 kho vật lý (Hải Phòng/Hà Nội/HCM) + kho ảo `IN_TRANSIT`.                                                                                                                                                                  |
| 04  | `warehouse_locations`             | Zone + Bin Location; `is_quarantine`, `capacity`.                                                                                                                                                                           |
| 05  | `products`                        | Danh mục SKU đồ gia dụng — không serial, không hạn dùng, không grade.                                                                                                                                                       |
| 06  | `price_history`                   | Giá vốn/giá bán theo kỳ hiệu lực (`effective_date`/`end_date`).                                                                                                                                                             |
| 07  | `batches`                         | Lô hàng theo product + `receipt_id` + `received_date` — khóa FIFO.                                                                                                                                                          |
| 08  | `inventories`                     | `total_qty`, `reserved_qty`, `version` (optimistic lock); CHECK `total_qty >= 0`, `reserved_qty >= 0`.                                                                                                                      |
| 09  | `receipts`                        | `status` CHECK (`ReceiptStatus`): `PENDING_RECEIPT, DRAFT, QC_COMPLETED, QC_FAILED, APPROVED, RETURN_TO_SUPPLIER_PENDING, RETURNED_TO_SUPPLIER`. Dùng chung cho `type = PURCHASE` (Spec 003) và `type = RETURN` (Spec 009). |
| 10  | `receipt_items`                   | `expected_qty`, `actual_qty`, `over_received_qty`, `qc_result`, `qc_failure_reason`.                                                                                                                                        |
| 11  | `adjustments`                     | `type` CHECK (`DISPOSAL, RETURN_TO_VENDOR, TRANSFER_DISCREPANCY, STOCKTAKE, ...`).                                                                                                                                          |
| 12  | `debit_notes`                     | Chứng từ đòi bồi hoàn NCC khi RTV, liên kết `receipt_id`/`supplier_id`.                                                                                                                                                     |
| 13  | `quarantine_records`              | Bản ghi hàng lỗi trong Quarantine, giữ `origin` (`RECEIPT_QC_FAIL`/`INTERNAL_TRANSFER`) để truy vết.                                                                                                                        |
| 14  | `delivery_orders`                 | `status`: `NEW, WAITING_PICKING, QC_PENDING_APPROVAL, QC_COMPLETED, WAREHOUSE_APPROVED, IN_TRANSIT, COMPLETED, RETURNED, CLOSED, REJECTED, CANCELLED`.                                                                      |
| 15  | `delivery_order_items`            | Dòng hàng DO, `unit_price` snapshot tại thời điểm tạo DO.                                                                                                                                                                   |
| 16  | `delivery_order_item_allocations` | Kế hoạch lấy hàng theo `batch_id`/`location_id`/`zone_id`, có `status`/`version`.                                                                                                                                           |
| 17  | `trips`                           | `trip_type` (`DELIVERY`/`TRANSFER`), `vehicle_id`, `driver_id`, `warehouse_id`.                                                                                                                                             |
| 18  | `deliveries`                      | Attempt giao vật lý của 1 DO, `attempt_number`, `status` (`IN_TRANSIT/DELIVERED/FAILED`).                                                                                                                                   |
| 19  | `delivery_otp_attempts`           | Chỉ lưu hash/verifier OTP, `expires_at`, `attempt_count`, `status`.                                                                                                                                                         |
| 20  | `transfers`                       | `status` CHECK: `NEW, APPROVED, REJECTED, IN_TRANSIT, COMPLETED, COMPLETED_WITH_DISCREPANCY, CANCELLED, QUARANTINED`.                                                                                                       |
| 21  | `transfer_items`                  | `sent_qty`, `received_qty`, `qc_passed_qty`, `qc_failed_qty`.                                                                                                                                                               |
| 22  | `stocktakes` / `stocktake_items`  | Phiếu kiểm kê và dòng đếm thực tế, tính Variance.                                                                                                                                                                           |
| 23  | `dealers`                         | `current_balance`, `credit_limit`, `credit_status` (`ACTIVE`/`CREDIT_HOLD`), `payment_term_days`.                                                                                                                           |
| 24  | `suppliers`                       | Hồ sơ NCC phục vụ Receipt/RTV/Debit Note.                                                                                                                                                                                   |
| 25  | `vehicles` / `drivers`            | Xe tải và tài xế nội bộ, phạm vi kho hoạt động.                                                                                                                                                                             |
| 26  | `invoices` / `invoice_lines`      | Hóa đơn tự động tạo khi DO `COMPLETED`, `issue_date`, `due_date = issue_date+30`.                                                                                                                                           |
| 27  | `payment_receipts`                | Phiếu thu, cấn trừ hóa đơn.                                                                                                                                                                                                 |
| 28  | `credit_notes`                    | Ghi giảm công nợ khi hàng hoàn trả.                                                                                                                                                                                         |
| 29  | `billing_notifications`           | Worklist đối chiếu DO đã giao (không phải bước bắt buộc tạo invoice).                                                                                                                                                       |
| 30  | `accounting_periods`              | Kỳ kế toán, `status` (`OPEN`/`CLOSED`).                                                                                                                                                                                     |
| 31  | `damage_reports`                  | Biên bản hư hỏng phục vụ tiêu hủy.                                                                                                                                                                                          |
| 32  | `stock_alerts`                    | Cảnh báo tồn kho thấp tự động, unique `(warehouse_id, product_id, alert_type, is_resolved=false)`.                                                                                                                          |
| 33  | `system_configs`                  | Tham số hệ thống dạng key-value có kiểm soát.                                                                                                                                                                               |
| 34  | `audit_logs`                      | `actor_id` (nullable), `actor_role`, `action`, `entity_type`, `entity_id`, `timestamp`, `old_value`, `new_value` (JSON); append-only.                                                                                       |

---

# II. Code Designs

> **Phạm vi phần này:** thiết kế chi tiết Class Diagram / Class Specifications / Sequence Diagram / Database Queries cho nhóm nghiệp vụ đại diện **Spec 003 – Inbound Receipt & QC** (đồng bộ với `docs/RDS-WMS.md`). Các class/method liệt kê dưới đây lấy trực tiếp từ code thực tế tại `backend/src/main/java/com/wms/`. Khi cần mở rộng cho các nhóm nghiệp vụ khác (Outbound, Transfer, Stocktake, Pricing, Finance...), áp dụng cùng mẫu, tham chiếu class tương ứng trong `backend/CLAUDE.md` mục "Current Backend Flows".

## 1. Receipt Creation & Physical Counting

### a. Class Diagram

```
ReceiptController ──uses──► ReceiptService ──uses──► ReceiptRepository ──maps──► Receipt (entity)
        │                         │                          └────────────────► ReceiptItem (entity)
        │                         ├──uses──► ReceiptItemRepository
        │                         └──uses──► ReceiptValidationService (shared validation: warehouse access, request shape)
        └──DTO: CreateReceiptRequest / ReceiveReceiptRequest ──► ReceiptResponse
```

Quan hệ: `ReceiptController` (`@RestController`, `@RequestMapping("/api/v1/receipts")`) chỉ điều phối HTTP, gọi `ReceiptService` (business logic + `@Transactional`). `ReceiptService` phụ thuộc `ReceiptRepository`, `ReceiptItemRepository` (Spring Data JPA) để truy xuất `Receipt`/`ReceiptItem` entity, và ghi audit qua `AuditLogUtil`/`AuditLogService`.

### b. Class Specifications

#### ReceiptController Class

| No  | Method                                                   | Description                                                                                                                                                                                                 |
| --- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getReceipts(...)`                                       | `GET /api/v1/receipts` — trả danh sách phiếu nhập theo filter (kho, type, status); yêu cầu warehouse-scope của actor hiện tại.                                                                              |
| 02  | `getReceiptById(Long id)`                                | `GET /api/v1/receipts/{id}` — trả chi tiết 1 phiếu nhập kèm danh sách `receipt_items`.                                                                                                                      |
| 03  | `createReceipt(CreateReceiptRequest request)`            | `POST /api/v1/receipts` — role `PLANNER`. Input: kho, `source_order_code`, danh sách SKU + `expected_qty`. Output: `ReceiptResponse` với `status = PENDING_RECEIPT`.                                        |
| 04  | `receiveReceipt(Long id, ReceiveReceiptRequest request)` | `PUT /api/v1/receipts/{id}/receive` — role `WAREHOUSE_STAFF`/`ADMIN`. Input: `counted_qty` cho toàn bộ dòng hàng. Output: `ReceiptResponse` với `actual_qty`/`over_received_qty` đã tính, `status = DRAFT`. |
| 05  | `processQc(...)`                                         | `PUT /api/v1/receipts/{id}/qc` — role `WAREHOUSE_STAFF`/`STOREKEEPER`/`WAREHOUSE_MANAGER`/`ADMIN`. Ủy quyền cho `ReceiptQcService.processQc` (xem mục 2).                                                   |

#### ReceiptService Class

| No           | Method                                                                            | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ------------ | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01           | `getReceiptsByWarehouse(Long warehouseId, User actor)`                            | Trả danh sách `ReceiptResponse` theo kho, kiểm tra `actor` có quyền truy cập kho đó.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 02           | `getReceiptsByWarehouseAndType(Long warehouseId, ReceiptType type, User actor)`   | Lọc thêm theo `type` (`PURCHASE`/`RETURN`) nếu truyền vào; nếu không truyền, lấy tất cả loại.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| 03           | `getReceiptById(Long id, User actor)`                                             | Đọc `Receipt` + join `ReceiptItem` (sắp theo `id ASC`), throw `NotFoundException` nếu không tồn tại hoặc actor không có quyền.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 04           | `createPurchaseReceipt(CreateReceiptRequest request, User actor)`                 | **Input**: request (kho, `source_order_code`, items), actor (phải role `PLANNER`, qua `requirePlanner`). **Xử lý nội bộ**: `validateRequest` → `validateItems` → `validateDuplicateSource` (chặn trùng `source_order_code` cùng kho) → `buildItems` map DTO sang entity → lưu `Receipt` (`PENDING_RECEIPT`) và `ReceiptItem[]` → ghi audit `RECEIPT_CREATE` với `snapshot` before/after. **Output**: `ReceiptResponse`.                                                                                                                                                 |
| 05           | `receiveReceiptCounts(Long receiptId, ReceiveReceiptRequest request, User actor)` | **Input**: `receiptId`, request (`counted_qty` mọi dòng), actor (phải `WAREHOUSE_STAFF`, qua `requireWarehouseStaff` + `requireWarehouseAccess`). **Xử lý nội bộ**: `validateReceiveRequest` (đủ dòng, `counted_qty` nguyên dương, không trùng `receipt_item_id`) → với mỗi item tính `actual_qty = min(counted_qty, expected_qty)`, `over_received_qty = max(counted_qty - expected_qty, 0)` → lưu `ReceiptItem[]` → cập nhật `Receipt.status = DRAFT`, nếu đã có QC data thì clear QC field → ghi audit `RECEIPT_RECEIVE`. **Output**: `ReceiptResponse` đã cập nhật. |
| 06 (private) | `requirePlanner` / `requireWarehouseStaff` / `requireWarehouseAccess`             | Kiểm tra role và warehouse-scope của actor; throw `ForbiddenException` nếu vi phạm (BR-SEC-01).                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 07 (private) | `validateDuplicateSource` / `validateRequest` / `validateItems`                   | Validate input theo business rule (không cho request rỗng, `counted_qty <= 0`, item không thuộc receipt...).                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 08 (private) | `buildItems` / `snapshot`                                                         | `buildItems` map `CreateReceiptItemRequest → ReceiptItem` entity; `snapshot` build `Map<String,Object>` before/after cho audit log.                                                                                                                                                                                                                                                                                                                                                                                                                                     |

### c. Sequence Diagram(s)

**Sequence: Create Purchase Receipt (`POST /api/v1/receipts`)**

```
Planner → ReceiptController.createReceipt(request)
  ReceiptController → ReceiptService.createPurchaseReceipt(request, actor)
    ReceiptService → requirePlanner(actor)                         [role check]
    ReceiptService → validateRequest(request)                      [shape check]
    ReceiptService → validateItems(request.items)                  [SKU/qty check]
    ReceiptService → receiptRepository.existsBySourceOrderCode(...) [duplicate check]
    ReceiptService → buildItems(request.items, savedReceipt)
    ReceiptService → receiptRepository.save(receipt)                → Receipt{status=PENDING_RECEIPT}
    ReceiptService → receiptItemRepository.saveAll(items)
    ReceiptService → auditLogService.log(RECEIPT_CREATE, before=null, after=snapshot)
  ReceiptService --> ReceiptController : ReceiptResponse
ReceiptController --> Planner : 201 Created (ReceiptResponse)
```

**Sequence: Record Physical Receive Count (`PUT /api/v1/receipts/{id}/receive`)**

```
Nhân viên kho → ReceiptController.receiveReceipt(id, request)
  ReceiptController → ReceiptService.receiveReceiptCounts(id, request, actor)
    ReceiptService → requireWarehouseStaff(actor)
    ReceiptService → receiptRepository.findById(id)                → Receipt
    ReceiptService → requireWarehouseAccess(actor, receipt.warehouseId)
    ReceiptService → validateReceiveRequest(request, items)
    loop each ReceiptItem
      ReceiptService → compute actual_qty / over_received_qty
    end
    ReceiptService → receiptItemRepository.saveAll(items)
    ReceiptService → receipt.setStatus(DRAFT); if hadQcData: clear QC fields
    ReceiptService → receiptRepository.save(receipt)
    ReceiptService → auditLogService.log(RECEIPT_RECEIVE, before, after)
  ReceiptService --> ReceiptController : ReceiptResponse
ReceiptController --> Nhân viên kho : 200 OK (ReceiptResponse)
```

### d. Database Queries

```sql
-- 1/ Kiểm tra trùng nguồn nhập trong cùng kho khi tạo Receipt
SELECT EXISTS (
    SELECT 1 FROM receipts
    WHERE warehouse_id = ? AND source_order_code = ? AND type = 'PURCHASE'
);

-- 2/ Tạo Receipt (PENDING_RECEIPT) và các dòng hàng
INSERT INTO receipts (receipt_number, source_order_code, type, warehouse_id, status, created_by, created_at)
VALUES (?, ?, 'PURCHASE', ?, 'PENDING_RECEIPT', ?, NOW())
RETURNING id;

INSERT INTO receipt_items (receipt_id, product_id, expected_qty)
VALUES (?, ?, ?);

-- 3/ Đọc phiếu + dòng hàng theo id, sắp theo thứ tự tạo
SELECT * FROM receipts WHERE id = ?;
SELECT * FROM receipt_items WHERE receipt_id = ? ORDER BY id ASC;

-- 4/ Cập nhật số lượng đếm thực tế và chuyển trạng thái DRAFT
UPDATE receipt_items
SET actual_qty = ?, over_received_qty = ?
WHERE id = ? AND receipt_id = ?;

UPDATE receipts
SET status = 'DRAFT', updated_at = NOW()
WHERE id = ? AND status IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','QC_FAILED');
```

---

## 2. Inbound QC Inspection

### a. Class Diagram

```
ReceiptController.processQc(...) ──delegates──► ReceiptQcService.processQc(...)
                                                      │
                                                      ├──uses──► ReceiptRepository
                                                      ├──uses──► ReceiptItemRepository
                                                      └──uses──► AuditLogService
```

### b. Class Specifications

#### ReceiptQcService Class

| No  | Method                                                                   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| --- | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `processQc(Long receiptId, ReceiptQcRequest request, String actorEmail)` | **Input**: `receiptId`, request (danh sách `receipt_item_id` + `qc_result` Đạt/Lỗi + `qc_failure_reason` khi Lỗi), `actorEmail` để resolve actor + role. **Xử lý nội bộ**: kiểm tra `Receipt.status == DRAFT`; validate mỗi dòng có `qc_failure_reason` khi `qc_result = FAILED`; cập nhật `ReceiptItem.qc_result`/`qc_failure_reason`; tính trạng thái tổng của Receipt — `QC_COMPLETED` nếu có ít nhất 1 dòng đạt, `QC_FAILED` nếu toàn bộ lỗi; ghi audit `RECEIPT_QC_RECORD`. **Output**: `ReceiptQcResponse` (trạng thái mới + tổng hợp Đạt/Lỗi theo dòng). |

### c. Sequence Diagram(s)

```
Nhân viên kho/Thủ kho → ReceiptController.processQc(id, request)
  ReceiptController → ReceiptQcService.processQc(id, request, actorEmail)
    ReceiptQcService → receiptRepository.findById(id)         → Receipt (must be DRAFT)
    ReceiptQcService → validate qc_failure_reason khi FAILED
    ReceiptQcService → receiptItemRepository.saveAll(updatedItems)
    ReceiptQcService → compute overall status (QC_COMPLETED / QC_FAILED)
    ReceiptQcService → receiptRepository.save(receipt)
    ReceiptQcService → auditLogService.log(RECEIPT_QC_RECORD, before, after)
  ReceiptQcService --> ReceiptController : ReceiptQcResponse
ReceiptController --> Actor : 200 OK
```

### d. Database Queries

```sql
UPDATE receipt_items
SET qc_result = ?, qc_failure_reason = ?
WHERE id = ? AND receipt_id = ?;

UPDATE receipts
SET status = ?, updated_at = NOW()
WHERE id = ? AND status = 'DRAFT';
```

---

## 3. Receipt Approval & Putaway

### a. Class Diagram

```
ReceiptApprovalController ──uses──► ReceiptApprovalService
                                          │
                                          ├──uses──► ReceiptRepository
                                          ├──uses──► ReceiptItemRepository
                                          ├──uses──► BatchRepository
                                          ├──uses──► InventoryRepository
                                          ├──uses──► WarehouseLocationRepository
                                          ├──uses──► ReceiptValidationService
                                          └──uses──► AuditLogService
```

### b. Class Specifications

#### ReceiptApprovalController Class

| No  | Method                                                          | Description                                                                                                |
| --- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| 01  | `approve(Long id, ...)`                                         | `PUT /api/v1/receipts/{id}/approve` — role Trưởng kho. Ủy quyền `ReceiptApprovalService.approveReceipt`.   |
| 02  | `reject(Long id, ReceiptDecisionRequest)`                       | `PUT /api/v1/receipts/{id}/reject` — role Trưởng kho, bắt buộc lý do. Ủy quyền `rejectReceipt`.            |
| 03  | `confirmReturnToSupplier(Long id, ReceiptReturnConfirmRequest)` | `PUT /api/v1/receipts/{id}/return-to-supplier/confirm` — role Thủ kho. Ủy quyền `confirmReturnToSupplier`. |
| 04  | `complete(Long id, ReceiptPutawayRequest)`                      | `PUT /api/v1/receipts/{id}/complete` — role Thủ kho, cất hàng vào Bin. Ủy quyền `completePutaway`.         |

#### ReceiptApprovalService Class

| No  | Method                                                                              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| --- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `approveReceipt(Long receiptId, ...)`                                               | **Input**: `receiptId`, actor (Trưởng kho, đúng kho). **Xử lý**: kiểm tra `Receipt.status == QC_COMPLETED`; cập nhật `status = APPROVED`; KHÔNG cộng tồn kho ở bước này; ghi audit `RECEIPT_APPROVE`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                                                                |
| 02  | `rejectReceipt(Long receiptId, ReceiptDecisionRequest request, ...)`                | **Input**: `receiptId`, `request.reason` (bắt buộc), actor. **Xử lý**: kiểm tra `status == QC_COMPLETED`; cập nhật `status = RETURN_TO_SUPPLIER_PENDING`, lưu `reject_reason`; KHÔNG tạo inventory/batch/RTV/Debit Note; ghi audit `RECEIPT_REJECT`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                 |
| 03  | `confirmReturnToSupplier(Long receiptId, ReceiptReturnConfirmRequest request, ...)` | **Input**: `receiptId`, actor (Thủ kho). **Xử lý**: kiểm tra `status == RETURN_TO_SUPPLIER_PENDING`; cập nhật `status = RETURNED_TO_SUPPLIER`; ghi audit `RECEIPT_RETURN_CONFIRM`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                                                                                   |
| 04  | `completePutaway(Long receiptId, ReceiptPutawayRequest request, ...)`               | **Input**: `receiptId`, `request.items[]` (mỗi item: `receipt_item_id`, `location_id`), actor (Thủ kho). **Xử lý**: kiểm tra `status == APPROVED`; với mỗi dòng: kiểm tra `warehouse_locations.is_quarantine = false` và còn đủ `capacity` (qua `warehouseLocationRepository` + `inventoryRepository` — BR-BAT-02); tạo/tìm `batches` theo product + receipt + `received_date`; cộng `inventories.total_qty` với optimistic lock (`version`); ghi audit `RECEIPT_PUTAWAY_COMPLETE`. Đây là bước DUY NHẤT tăng available inventory của luồng inbound. **Output**: `ReceiptActionResponse`. |

### c. Sequence Diagram(s)

**Sequence: Approve Receipt & Putaway**

```
Trưởng kho → ReceiptApprovalController.approve(id)
  ReceiptApprovalController → ReceiptApprovalService.approveReceipt(id, actor)
    ReceiptApprovalService → receiptRepository.findById(id)     → Receipt (must be QC_COMPLETED)
    ReceiptApprovalService → receipt.setStatus(APPROVED)
    ReceiptApprovalService → receiptRepository.save(receipt)
    ReceiptApprovalService → auditLogService.log(RECEIPT_APPROVE, before, after)
  ReceiptApprovalService --> ReceiptApprovalController : ReceiptActionResponse
ReceiptApprovalController --> Trưởng kho : 200 OK

Thủ kho → ReceiptApprovalController.complete(id, putawayRequest)
  ReceiptApprovalController → ReceiptApprovalService.completePutaway(id, request, actor)
    ReceiptApprovalService → receiptRepository.findById(id)     → Receipt (must be APPROVED)
    loop each putaway item
      ReceiptApprovalService → warehouseLocationRepository.findById(locationId)  [is_quarantine=false, capacity check]
      ReceiptApprovalService → batchRepository.findOrCreate(product, receipt, receivedDate)
      ReceiptApprovalService → inventoryRepository.increaseTotalQty(warehouse, product, batch, location, version)
    end
    ReceiptApprovalService → auditLogService.log(RECEIPT_PUTAWAY_COMPLETE, before, after)
  ReceiptApprovalService --> ReceiptApprovalController : ReceiptActionResponse
ReceiptApprovalController --> Thủ kho : 200 OK
```

### d. Database Queries

```sql
-- 1/ Duyệt phiếu nhập
UPDATE receipts SET status = 'APPROVED', approved_by = ?, approved_at = NOW()
WHERE id = ? AND status = 'QC_COMPLETED';

-- 2/ Từ chối phiếu nhập
UPDATE receipts SET status = 'RETURN_TO_SUPPLIER_PENDING', reject_reason = ?
WHERE id = ? AND status = 'QC_COMPLETED';

-- 3/ Kiểm tra sức chứa Bin trước putaway
SELECT wl.id, wl.capacity, wl.is_quarantine,
       COALESCE(SUM(i.total_qty), 0) AS current_qty
FROM warehouse_locations wl
LEFT JOIN inventories i ON i.location_id = wl.id
WHERE wl.id = ? AND wl.is_quarantine = false
GROUP BY wl.id, wl.capacity, wl.is_quarantine;

-- 4/ Tìm hoặc tạo batch theo product + receipt + received_date
SELECT id FROM batches WHERE product_id = ? AND receipt_id = ? AND received_date = ?;
INSERT INTO batches (product_id, receipt_id, received_date, created_at)
VALUES (?, ?, ?, NOW()) RETURNING id;

-- 5/ Cộng tồn kho sau putaway (optimistic locking qua version)
UPDATE inventories
SET total_qty = total_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND batch_id = ? AND location_id = ? AND version = ?;
```

---

## 4. Quarantine & Return-to-Vendor (RTV)

### a. Class Diagram

```
QuarantineController ──uses──► (read-only quarantine listing)
QuarantineRtvController ──uses──► QuarantineRtvService
                                        │
                                        ├──uses──► ReceiptRepository
                                        ├──uses──► ReceiptItemRepository
                                        ├──uses──► AdjustmentRepository
                                        ├──uses──► DebitNoteRepository
                                        ├──uses──► InventoryRepository
                                        ├──uses──► QuarantineRecordRepository
                                        ├──uses──► PriceHistoryRepository (định giá Debit Note)
                                        ├──uses──► ReceiptValidationService
                                        └──uses──► AuditLogService
```

### b. Class Specifications

#### QuarantineController Class

| No  | Method                    | Description                                                                                                                                                                                                 |
| --- | ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getQuarantineItems(...)` | `GET /api/v1/quarantine/items` — role `WAREHOUSE_STAFF`/`STOREKEEPER`/`WAREHOUSE_MANAGER`/`ADMIN`. Trả danh sách hàng đang trong Quarantine theo kho, kèm `origin` (`RECEIPT_QC_FAIL`/`INTERNAL_TRANSFER`). |

#### QuarantineRtvController Class

| No  | Method                                                 | Description                                                                                    |
| --- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| 01  | `createRtv(Long receiptId, ReceiptRtvCreateRequest)`   | `POST /api/v1/receipts/{id}/rtv` — role Trưởng kho. Ủy quyền `QuarantineRtvService.createRtv`. |
| 02  | `confirmRtv(Long receiptId, ReceiptRtvConfirmRequest)` | `PUT /api/v1/receipts/{id}/rtv/confirm` — role Thủ kho. Ủy quyền `confirmRtv`.                 |

#### QuarantineRtvService Class

| No  | Method                                                              | Description                                                                                                                                                                                                                                                                                                                                           |
| --- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createRtv(Long receiptId, ReceiptRtvCreateRequest request, ...)`   | **Input**: `receiptId` (phải có `QC_FAILED` items còn trong Quarantine), actor (Trưởng kho). **Xử lý**: tạo `debit_notes` cho supplier (định giá theo `price_history` hiệu lực); tạo `adjustments` `type = RETURN_TO_VENDOR` ở trạng thái pending; KHÔNG trừ tồn Quarantine ngay; ghi audit `QUARANTINE_RTV_CREATE`. **Output**: `RtvActionResponse`. |
| 02  | `confirmRtv(Long receiptId, ReceiptRtvConfirmRequest request, ...)` | **Input**: `receiptId`, actor (Thủ kho), số lượng xác nhận bàn giao (phải bằng ĐÚNG số lượng RTV — không cho một phần, trả 422 nếu lệch). **Xử lý**: trừ `inventories`/`quarantine_records` theo `receiptId`; giữ `receipts.status = QC_FAILED`; ghi audit `QUARANTINE_RTV_CONFIRM`. **Output**: `RtvActionResponse`.                                 |
| 03  | `getQuarantineItems(Long warehouseId, User actor)`                  | Trả `List<QuarantineItemResponse>` cho kho, kiểm tra warehouse-scope của actor.                                                                                                                                                                                                                                                                       |

### c. Sequence Diagram(s)

```
Trưởng kho → QuarantineRtvController.createRtv(receiptId, request)
  QuarantineRtvController → QuarantineRtvService.createRtv(receiptId, request, actor)
    QuarantineRtvService → receiptRepository.findById(receiptId)      → Receipt (must have QC_FAILED items)
    QuarantineRtvService → priceHistoryRepository.findEffectivePrice(productId, date)
    QuarantineRtvService → debitNoteRepository.save(debitNote)
    QuarantineRtvService → adjustmentRepository.save(adjustment{type=RETURN_TO_VENDOR, status=PENDING})
    QuarantineRtvService → auditLogService.log(QUARANTINE_RTV_CREATE, before, after)
  QuarantineRtvService --> QuarantineRtvController : RtvActionResponse
QuarantineRtvController --> Trưởng kho : 201 Created

Thủ kho → QuarantineRtvController.confirmRtv(receiptId, request)
  QuarantineRtvController → QuarantineRtvService.confirmRtv(receiptId, request, actor)
    QuarantineRtvService → validate confirmedQty == rtvQty (else 422)
    QuarantineRtvService → inventoryRepository.decreaseQuarantineQty(...)
    QuarantineRtvService → quarantineRecordRepository.markCleared(...)
    QuarantineRtvService → auditLogService.log(QUARANTINE_RTV_CONFIRM, before, after)
  QuarantineRtvService --> QuarantineRtvController : RtvActionResponse
QuarantineRtvController --> Thủ kho : 200 OK
```

### d. Database Queries

```sql
-- 1/ Danh sách hàng trong Quarantine theo kho
SELECT qr.id, qr.product_id, qr.quantity, qr.origin, r.receipt_number
FROM quarantine_records qr
JOIN receipts r ON r.id = qr.receipt_id
WHERE qr.warehouse_id = ? AND qr.quantity > 0;

-- 2/ Tạo Debit Note + Adjustment (RTV) — pending, chưa trừ tồn
INSERT INTO debit_notes (receipt_id, supplier_id, amount, created_at)
VALUES (?, ?, ?, NOW()) RETURNING id;

INSERT INTO adjustments (receipt_id, type, quantity, status, created_at)
VALUES (?, 'RETURN_TO_VENDOR', ?, 'PENDING', NOW()) RETURNING id;

-- 3/ Xác nhận bàn giao đủ — trừ tồn Quarantine
UPDATE quarantine_records
SET quantity = quantity - ?
WHERE receipt_id = ? AND product_id = ? AND quantity >= ?;

UPDATE adjustments SET status = 'CONFIRMED', confirmed_at = NOW()
WHERE id = ?;
```

---

## Ghi chú mở rộng tài liệu

Class/method liệt kê trên lấy trực tiếp từ `backend/src/main/java/com/wms/{controller,service}/` tương ứng Spec 003. Khi mở rộng SDS cho các nhóm nghiệp vụ khác, tham chiếu bảng "Current Backend Flows" trong `backend/CLAUDE.md` để xác định class thực tế trước khi viết Class Specifications (VD Outbound → `DeliveryOrderController`/`DeliveryOrderService`, `AutoInvoiceService`, `DriverDeliveryServiceImpl`; Finance → `InvoiceServiceImpl`, `PaymentReceiptServiceImpl`, `AccountingPeriodServiceImpl`; Reports → `ReportServiceImpl`, `StockAlertServiceImpl`), giữ cùng cấu trúc mục a/b/c/d như trên.
