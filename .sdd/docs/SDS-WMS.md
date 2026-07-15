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

---

# II. System Design Specifications (Spec 001–002)

> **Phạm vi phần này:** Class Diagram, Entity Relationships, Database Schema, và SQL queries cho Spec 001 (Auth/RBAC) + Spec 002 (Master Data). Các class lấy từ backend; schema liên kết với Spec 003's receipts/batches/inventories để hỗ trợ warehouse-scoped RBAC.

## 2. Spec 001: Authentication & RBAC Classes

### 2.1 Class Specifications

**Controller Layer:**
- `AuthController.java` – Endpoints: login, refresh, logout, changePassword, forgotPassword, verifyOtp, getMe
- `AdminController.java` – Endpoints: CRUD users, assign warehouses, create/update/delete roles

**Service Layer:**
- `AuthenticationService.java` – Password hashing (bcrypt cost ≥12), token generation/validation, OTP generation/verification
- `JwtTokenProvider.java` – JWT token creation (access: 15m, refresh: 7d), verification, extraction
- `UserService.java` – User CRUD, password hashing, warehouse assignment, audit logging
- `RbacService.java` – Role/permission checks, warehouse scope validation

**Entity Relationships:**
```
users (1) ←─── (many) user_warehouse_assignments ─── (many) warehouses
users (1) ←─── (many) audit_logs
users (1) ←─── (many) role_permissions ─── (many) permissions
```

**Key Database Columns (users table):**
```sql
id (BIGSERIAL PK)
email (VARCHAR, UNIQUE)
username (VARCHAR, UNIQUE)
password_hash (VARCHAR, bcrypt)
full_name (VARCHAR)
phone (VARCHAR)
job_title (VARCHAR)
role (VARCHAR) – FK to roles table or ENUM
is_active (BOOLEAN DEFAULT true)
refresh_token_hash (VARCHAR NULL) – SHA-256 hash
refresh_token_expires_at (TIMESTAMPTZ NULL)
otp_hash (VARCHAR NULL) – SHA-256 hash for password reset
otp_expires_at (TIMESTAMPTZ NULL) – TTL 10 minutes
created_at (TIMESTAMPTZ)
updated_at (TIMESTAMPTZ)
```

**SQL: User Authentication & Token Revocation**

```sql
-- Query user by email + verify password during login
SELECT u.id, u.full_name, u.email, u.role, u.password_hash, u.is_active
FROM users u
WHERE u.email = ? AND u.is_active = true;

-- Store refresh token hash after successful login
UPDATE users
SET refresh_token_hash = ?, refresh_token_expires_at = NOW() + INTERVAL '7 days'
WHERE id = ?;

-- Verify refresh token during token refresh endpoint
SELECT u.id, u.role, u.email
FROM users u
WHERE u.refresh_token_hash = ? AND u.refresh_token_expires_at > NOW();

-- Invalidate token on logout
UPDATE users
SET refresh_token_hash = NULL, refresh_token_expires_at = NULL
WHERE id = ?;

-- Store OTP hash for password reset
UPDATE users
SET otp_hash = ?, otp_expires_at = NOW() + INTERVAL '10 minutes'
WHERE email = ? AND is_active = true;

-- Verify OTP & update password
UPDATE users
SET password_hash = ?, otp_hash = NULL, otp_expires_at = NULL
WHERE email = ? AND otp_hash = ? AND otp_expires_at > NOW();
```

---

### 2.2 Warehouse Assignment & RBAC SQL

**Table: user_warehouse_assignments**
```sql
CREATE TABLE user_warehouse_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, warehouse_id)
);

-- Query assigned warehouses for a user
SELECT w.id, w.code, w.name
FROM warehouses w
INNER JOIN user_warehouse_assignments uwa ON w.id = uwa.warehouse_id
WHERE uwa.user_id = ? AND w.is_active = true;

-- Validate warehouse access (used by every warehouse-scoped operation)
SELECT COUNT(*)
FROM user_warehouse_assignments
WHERE user_id = ? AND warehouse_id = ?;
-- Result > 0 = authorized; 0 = forbidden (403)
```

---

## 3. Spec 002: Master Data Classes & Schema

### 3.1 Product Management Classes

**Service Layer:**
- `ProductService.java` – CRUD products, SKU uniqueness, soft delete (is_active)
- `ProductHistoryService.java` – Track price history for COGS calculation

**Entity Relationships:**
```
products (1) ←─── (many) product_price_history
products (1) ←─── (many) batches
products (1) ←─── (many) receipt_items
products (1) ←─── (many) delivery_order_items
```

**Key Database Columns (products table):**
```sql
id (BIGSERIAL PK)
sku (VARCHAR, UNIQUE) – Cannot be modified after creation
name (VARCHAR)
unit (VARCHAR) – e.g., "cái", "thùng"
description (TEXT NULL)
weight_kg (DECIMAL(10,2) NULL)
volume_m3 (DECIMAL(10,2) NULL)
unit_per_pack (INTEGER NULL) – Conversion factor (e.g., 1 thùng = 12 cái)
reorder_point (INTEGER NULL) – Low stock alert threshold
is_active (BOOLEAN DEFAULT true)
created_at (TIMESTAMPTZ)
updated_at (TIMESTAMPTZ)
```

**SQL: Product Queries**

```sql
-- List active products with search
SELECT p.id, p.sku, p.name, p.unit, p.weight_kg, p.volume_m3
FROM products p
WHERE p.is_active = true AND (p.sku ILIKE ? OR p.name ILIKE ?)
ORDER BY p.created_at DESC;

-- Check SKU uniqueness
SELECT COUNT(*) FROM products WHERE sku = ? AND id <> ?;

-- Prevent transactions for inactive products
SELECT p.id FROM products p
WHERE p.id = ? AND p.is_active = false;
-- If result > 0: reject receipt/issue/transfer
```

---

### 3.2 Warehouse & Bin Location Classes

**Service Layer:**
- `WarehouseService.java` – CRUD warehouses, manager assignment, soft delete
- `BinLocationService.java` – CRUD zones/bins, capacity validation, quarantine setup
- `LocationLockService.java` – Lock locations during stocktake, prevent concurrent transactions

**Entity Relationships:**
```
warehouses (1) ←─── (many) warehouse_locations (zones/bins)
warehouse_locations (1) ←─── (many) inventories
warehouse_locations (1) ←─── (many) stock_take_items (locked during stocktake)
```

**Key Database Columns:**

**warehouses table:**
```sql
id (BIGSERIAL PK)
code (VARCHAR, UNIQUE) – e.g., HP, HN, HCM, IN_TRANSIT
name (VARCHAR)
address (TEXT NULL)
phone (VARCHAR NULL)
manager_id (BIGINT REFERENCES users(id)) – Must have MANAGER role
type (VARCHAR) – PHYSICAL or IN_TRANSIT
is_active (BOOLEAN DEFAULT true)
created_at (TIMESTAMPTZ)
updated_at (TIMESTAMPTZ)
```

**warehouse_locations table:**
```sql
id (BIGSERIAL PK)
warehouse_id (BIGINT NOT NULL REFERENCES warehouses(id))
code (VARCHAR, UNIQUE) – e.g., HP.A (zone) or HP.A.01.1.01 (bin)
name (VARCHAR)
type (VARCHAR) – ZONE or BIN
parent_id (BIGINT REFERENCES warehouse_locations(id) NULL) – Parent zone for bins
capacity_m3 (DECIMAL(10,2) NULL)
capacity_kg (DECIMAL(10,2) NULL)
is_quarantine (BOOLEAN DEFAULT false)
is_locked (BOOLEAN DEFAULT false) – Locked during stocktake
is_active (BOOLEAN DEFAULT true)
created_at (TIMESTAMPTZ)
updated_at (TIMESTAMPTZ)
```

**SQL: Warehouse & Bin Capacity**

```sql
-- List active warehouses with manager info
SELECT w.id, w.code, w.name, w.type, u.full_name AS manager_name
FROM warehouses w
INNER JOIN users u ON w.manager_id = u.id
WHERE w.is_active = true
ORDER BY w.code;

-- Get bin capacity & current usage
SELECT wl.id, wl.code, wl.capacity_m3, wl.capacity_kg,
       COALESCE(SUM(i.total_qty * p.volume_m3), 0) AS current_volume_m3,
       COALESCE(SUM(i.total_qty * p.weight_kg), 0) AS current_weight_kg
FROM warehouse_locations wl
LEFT JOIN inventories i ON i.location_id = wl.id
LEFT JOIN products p ON p.id = i.product_id
WHERE wl.id = ? AND wl.type = 'BIN'
GROUP BY wl.id, wl.capacity_m3, wl.capacity_kg;

-- Prevent putting away goods if bin over capacity
-- Application code: 
-- IF (current_volume + incoming_volume > capacity_m3) OR 
--    (current_weight + incoming_weight > capacity_kg)
-- THEN reject with error BIN_OVER_CAPACITY

-- Lock all locations during stocktake
UPDATE warehouse_locations
SET is_locked = true, updated_at = NOW()
WHERE warehouse_id = ? AND type = 'BIN' AND is_quarantine = false;

-- Prevent transactions on locked locations
SELECT wl.id FROM warehouse_locations wl
WHERE wl.id = ? AND wl.is_locked = true;
-- If result > 0: reject receipt/issue/transfer with LOCATION_LOCKED

-- Unlock locations after stocktake approval/rejection
UPDATE warehouse_locations
SET is_locked = false, updated_at = NOW()
WHERE warehouse_id = ? AND type = 'BIN' AND is_locked = true;
```

---

### 3.3 Audit Log for RBAC/Config Changes

**Table: audit_logs (shared with all specs)**
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    actor_name VARCHAR,
    action VARCHAR – e.g., USER_CREATED, WAREHOUSE_CREATED, PRODUCT_UPDATED, PASSWORD_RESET_REQUEST
    entity_type VARCHAR – e.g., USER, WAREHOUSE, PRODUCT, CONFIG
    entity_id BIGINT
    warehouse_id BIGINT REFERENCES warehouses(id) NULL
    old_value TEXT NULL – JSON serialized before state
    new_value TEXT NULL – JSON serialized after state
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Example: User creation audit
INSERT INTO audit_logs (actor_id, actor_name, action, entity_type, entity_id, new_value)
VALUES (?, ?, 'USER_CREATED', 'USER', ?, '{"email":"...","role":"STOREKEEPER","warehouses":[1,2]}');

-- Example: Password reset request
INSERT INTO audit_logs (actor_id, action, entity_type, entity_id)
VALUES (NULL, 'PASSWORD_RESET_REQUEST', 'USER', ?); -- NULL actor for unauthenticated flow
```

---

## 4. Spec 004+005: Outbound, Delivery & Transfer Classes

### 4.1 Outbound/Delivery Core Services

**Service Layer:**
- `DeliveryOrderService.java` – CRUD DO, Credit Check, reserve + release, status transitions
- `PickingPlanService.java` – FIFO batch selection, allocation strategy, snapshot unit_price
- `OutboundQCService.java` – Record QC pass/fail, quarantine + adjustments for failures
- `TripDispatchService.java` – Create trips, assign vehicle/driver, capacity validation (weight/volume)
- `DriverDeliveryService.java` – Mobile API for driver: accept trip, depart, deliver, POD, OTP
- `DeliveryOtpService.java` – Generate OTP (6-digit, TTL 5min), hash storage, verification (max 3 fail)
- `AutoInvoiceService.java` – Event listener: WHEN delivery attempt = DELIVERED → create invoice + receivable

**Key SQL: DO + Credit Check + Reservation**

```sql
-- Credit Check before DO create
SELECT SUM(invoices.total_amount - COALESCE(payment_receipts.amount_paid, 0)) AS current_balance
FROM invoices
LEFT JOIN payment_receipts ON invoices.id = payment_receipts.invoice_id
WHERE invoices.dealer_id = ? AND invoices.status NOT IN ('CANCELLED','CLOSED')
GROUP BY invoices.dealer_id;
-- IF current_balance + DO_value > credit_limit → BLOCK with CREDIT_HOLD

-- Reserve warehouse-level on DO create
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ?
  AND version = ? -- Optimistic locking
RETURNING reserved_qty;

-- Create allocation on picking plan (FIFO)
INSERT INTO delivery_order_item_allocations 
  (do_item_id, inventory_id, batch_id, location_id, zone_id, planned_qty)
SELECT ?, i.id, i.batch_id, i.location_id, wl_zone.id, ?
FROM inventories i
INNER JOIN batches b ON i.batch_id = b.id
INNER JOIN warehouse_locations wl_zone ON wl_zone.id = wl.zone_id
WHERE i.warehouse_id = ? AND i.product_id = ? 
  AND i.total_qty - i.reserved_qty > 0
  AND wl.is_quarantine = false
  AND wl.type = 'BIN'
ORDER BY b.received_date ASC -- FIFO
LIMIT 1;
```

**SQL: Delivery & OTP**

```sql
-- Record OTP for delivery attempt (hash only, never plain)
INSERT INTO delivery_otp_attempts 
  (delivery_attempt_id, recipient_email, otp_hash, expires_at, status)
VALUES (?, ?, SHA256(?), NOW() + INTERVAL '5 minutes', 'PENDING');

-- Verify OTP (max 3 attempts)
SELECT otp_hash, expires_at, attempt_count
FROM delivery_otp_attempts
WHERE delivery_attempt_id = ? AND status = 'PENDING'
  AND attempt_count < 3;

-- Mark OTP used after successful verification
UPDATE delivery_otp_attempts
SET status = 'VERIFIED', consumed_at = NOW()
WHERE delivery_attempt_id = ? AND otp_hash = ?;

-- Auto-create invoice on delivery DELIVERED
INSERT INTO invoices (dealer_id, total_amount, issue_date, due_date, status)
SELECT do.dealer_id, 
       SUM(doi.qc_pass_qty * doi.unit_price),
       DATE(NOW()),
       DATE(NOW()) + INTERVAL '30 days',
       'UNPAID'
FROM delivery_attempts da
INNER JOIN delivery_orders do ON da.do_id = do.id
INNER JOIN delivery_order_items doi ON do.id = doi.do_id
WHERE da.status = 'DELIVERED' AND da.id = ?
GROUP BY do.dealer_id;
```

---

### 4.2 Transfer Core Services

**Service Layer:**
- `InterWarehouseTransferService.java` – CRUD transfer, reserve FIFO, status transitions
- `TransferShipmentService.java` – Outbound: pick + QC + handover
- `TransferReceiveService.java` – Inbound: blind count + receive QC + bin-capacity + discrepancy
- `TransferLocationLockService.java` – Lock/unlock during stocktake
- `TransferDiscrepancyService.java` – Calculate discrepancy, create adjustment records

**Key SQL: Transfer + Discrepancy**

```sql
-- Reserve FIFO for transfer on approval (source warehouse)
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ?;

-- Move qty to In-Transit on departure
UPDATE inventories
SET total_qty = total_qty - ?
WHERE warehouse_id = ? AND product_id = ? AND version = ?;

INSERT INTO inventories (warehouse_id, product_id, batch_id, location_id, total_qty)
SELECT ?, ?, batch_id, in_transit_location_id, ?
FROM inventories
WHERE warehouse_id = ? AND product_id = ?;

-- Calculate discrepancy on dest receive
SELECT 
  trf_items.planned_qty,
  reception_records.received_qty,
  (reception_records.received_qty - trf_items.planned_qty) AS variance_qty
FROM transfer_items trf_items
INNER JOIN reception_records ON trf_items.id = reception_records.transfer_item_id
WHERE trf_items.transfer_id = ?;

-- Create TRANSFER_DISCREPANCY adjustment if variance ≠ 0
INSERT INTO adjustments 
  (transfer_id, product_id, type, quantity_adjustment, variance_value)
VALUES (?, ?, 'TRANSFER_DISCREPANCY', ?, ? * cost_price);
```

---

## 5. Spec 006+007: Stocktake & Pricing Classes

**Services:** `StocktakeService`, `PricingService` (price_history lookup for COGS on invoice create)

**SQL: Stocktake + Location Lock**

```sql
-- Lock locations during IN_PROGRESS
UPDATE warehouse_locations
SET is_locked = true WHERE warehouse_id = ? AND type = 'BIN' AND is_quarantine = false;

-- Update inventory on approval + optimistic locking
UPDATE inventories SET total_qty = ?, version = version + 1
WHERE id = ? AND version = ?;

-- Price history COGS lookup (at issue_date)
SELECT cost_price FROM product_price_history
WHERE product_id = ? AND effective_date <= ? AND (end_date IS NULL OR end_date > ?)
ORDER BY effective_date DESC LIMIT 1;
```

---

## 6. Spec 008+009+010: Finance, Returns & Reporting Classes

**Services:**
- `InvoiceService` (auto-create on delivery DELIVERED)
- `AccountingPeriodService` (close, lock, late-doc handling)
- `ReturnService` (RTV Debit Note for supplier; Credit Note for customer)
- `ReportService` (Dashboard KPI, Aging, P&L)

**SQL: Auto-Invoice & Accounting Period**

```sql
-- Auto-create invoice trigger (after delivery DELIVERED)
INSERT INTO invoices (dealer_id, total_amount, issue_date, due_date, status, document_reference)
SELECT do.dealer_id, SUM(doi.qc_pass_qty * doi.unit_price), 
       CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'UNPAID', 'DO-' || do.do_number
FROM deliveries d
INNER JOIN delivery_orders do ON d.do_id = do.id
INNER JOIN delivery_order_items doi ON do.id = doi.do_id
WHERE d.status = 'DELIVERED' AND d.id = ?
GROUP BY do.dealer_id;

-- Monthly closing lock
UPDATE accounting_periods SET status = 'CLOSED' WHERE id = ?;
-- Block new transactions in closed period; allow Correction Vouchers in open period

-- Aging Report
SELECT dealer_id, due_date,
       CASE WHEN due_date < CURRENT_DATE AND CURRENT_DATE - due_date > 60 THEN '>60 days'
            WHEN due_date < CURRENT_DATE AND CURRENT_DATE - due_date > 30 THEN '31-60 days'
            WHEN due_date < CURRENT_DATE THEN '1-30 days'
            ELSE 'Not due' END AS aging_bucket,
       SUM(total_amount - COALESCE(paid_amount, 0)) AS balance
FROM invoices
WHERE status != 'CLOSED'
GROUP BY dealer_id, aging_bucket;
```

---

## Schema Reference Summary

**Core Tables (All Specs):**
```
users ←→ user_warehouse_assignments ←→ warehouses
warehouse_locations (zones/bins with is_locked, is_quarantine)
products (SKU, unit, no serial/expiry/grade)
batches (product + received_date + source receipt)
inventories (warehouse + product + batch + location; optimistic locking via version)

receipts, receipt_items, batches ← Spec 003
delivery_orders, delivery_order_items, delivery_order_item_allocations, deliveries ← Spec 004
transfers, transfer_items, reception_records ← Spec 005
stock_takes, stock_take_items ← Spec 006
product_price_history ← Spec 007
invoices, payment_receipts, accounting_periods ← Spec 008
quarantine_records, damage_reports, adjustments (type in STOCK_TAKE, TRANSFER_DISCREPANCY, DISPOSAL, RTV...) ← Spec 009
audit_logs (append-only, action = *, entity_type = *, REPORT_VIEW for dashboards) ← All specs
```

**Constraint Summary:**
- Inventory: total_qty ≥ 0, reserved_qty ≥ 0, total_qty - reserved_qty ≥ 0 (CHECK + app validation)
- Warehouse: unique code, manager_id FK to user with MANAGER role
- Location: unique code per warehouse, parent_id for hierarchy (ZONE → BIN only)
- Product: unique SKU, no serial/expiry/grade for household goods
- DO/Transfer: credit check, FIFO allocation, optimistic locking, soft-cancel (status = CANCELLED)
- Stocktake/Adjustment: location locking, flat approval (Trưởng kho)
- Invoice: auto-create on delivery DELIVERED, unit_price snapshot from picking plan
- Audit: append-only, ghi all mutations + REPORT_VIEW for confidential reports
