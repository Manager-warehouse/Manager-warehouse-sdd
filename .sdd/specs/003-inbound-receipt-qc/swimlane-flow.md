# Swimlane Flow: 003 Inbound Receipt & QC

> **Mục đích**: Tài liệu này mô tả đầy đủ các luồng nghiệp vụ của Spec 003 theo từng actor để phục vụ vẽ biểu đồ swimlane.  
> **Nguồn gốc**: [spec.md](./spec.md) · [quickstart.md](./quickstart.md) · [plan.md](./plan.md)

---

## 1. Các Actor (Làn bơi)

| Làn | Actor | Mô tả vai trò |
|-----|-------|---------------|
| 1 | **PLANNER** | Tạo phiếu nhập hàng, chỉnh sửa khi bị yêu cầu sửa, hủy phiếu |
| 2 | **WH_MANAGER** | Duyệt/từ chối phiếu trước nhận hàng; duyệt/từ chối phiếu sau QC; tạo RTV, xác nhận bàn giao NCC |
| 3 | **WH_STAFF** | Nhập số lượng thực tế và kết quả QC vào màn hình "Nhận hàng & QC đầu vào" |
| 4 | **STOREKEEPER** | Review kết quả đếm/QC của Staff; phê duyệt hoặc yêu cầu đếm lại; chọn bin cất kệ; xác nhận cất kệ |
| 5 | **HỆ THỐNG** | Sinh số phiếu, cập nhật trạng thái, ghi audit log, cập nhật tồn kho |

---

## 2. Trạng thái phiếu nhập (Receipt Status)

| Trạng thái | Mô tả |
|-----------|-------|
| `PENDING_MANAGER_APPROVAL` | Phiếu vừa tạo, chờ WH_MANAGER duyệt trước khi nhận hàng |
| `REVISION_REQUIRED` | WH_MANAGER từ chối, PLANNER phải sửa và gửi lại |
| `PENDING_RECEIPT` | WH_MANAGER đã duyệt, chờ Staff nhận hàng & QC |
| `PENDING_STOREKEEPER_REVIEW` | Staff đã nộp, chờ Storekeeper review |
| `RECOUNT_REQUIRED` | Storekeeper yêu cầu đếm lại |
| `QC_COMPLETED` | Storekeeper đã duyệt, toàn bộ hàng đạt QC |
| `QC_FAILED` | Storekeeper đã duyệt, có hàng lỗi QC |
| `APPROVED` | WH_MANAGER duyệt toàn bộ (từ QC_COMPLETED) |
| `PARTIALLY_APPROVED` | WH_MANAGER duyệt phần đạt (từ QC_FAILED) |
| `PUTAWAY_COMPLETED` | Storekeeper đã cất kệ xong |
| `RETURN_TO_SUPPLIER_PENDING` | WH_MANAGER từ chối toàn bộ phiếu, chờ bàn giao NCC |
| `RETURNED_TO_SUPPLIER` | Đã bàn giao hàng cho NCC |
| `CANCELLED` | Đã hủy phiếu |
| `DRAFT` | Phiếu cũ/mở lại – chỉ dùng cho legacy hoặc reopen |

---

## 3. Luồng chính – Hàng đạt toàn bộ QC

```
PLANNER                  WH_MANAGER               WH_STAFF                 STOREKEEPER              HỆ THỐNG
   |                         |                        |                         |                       |
   |-- Tạo phiếu nhập ------>|                        |                        |                       |
   |                         |                        |                        |                       |-- Sinh PO-YYYYMMDD-SEQ
   |                         |                        |                        |                       |-- Status: PENDING_MANAGER_APPROVAL
   |                         |                        |                        |                       |-- Ghi audit
   |                         |                        |                        |                       |
   |                  [Duyệt trước nhận hàng]         |                        |                       |
   |                         |-- Phê duyệt ---------->|                        |                       |
   |                         |                        |                        |                       |-- Status: PENDING_RECEIPT
   |                         |                        |                        |                       |-- Ghi audit RECEIPT_PRE_RECEIVE_APPROVE
   |                         |                        |                        |                       |
   |                         |           [Nhận hàng & QC đầu vào]              |                       |
   |                         |                        |-- Nhập actual_qty      |                       |
   |                         |                        |-- Nhập passed/failed qty                        |
   |                         |                        |-- Nhập lý do lỗi QC   |                       |
   |                         |                        |-- Nộp kết quả -------->|                       |
   |                         |                        |                        |                       |-- Validate: passed+failed=actual
   |                         |                        |                        |                       |-- Status: PENDING_STOREKEEPER_REVIEW
   |                         |                        |                        |                       |-- Ghi audit Staff actor
   |                         |                        |                        |                       |
   |                         |                        |             [Storekeeper Review]                |
   |                         |                        |                        |-- Xem kết quả đếm/QC  |
   |                         |                        |                        |-- Phê duyệt review -->|
   |                         |                        |                        |                       |-- Status: QC_COMPLETED (all pass)
   |                         |                        |                        |                       |-- Ghi audit RECEIPT_STOREKEEPER_REVIEW_APPROVE
   |                         |                        |                        |                       |
   |                  [WH_MANAGER quyết định]         |                        |                       |
   |                         |-- Phê duyệt toàn bộ   |                        |                       |
   |                         |                        |                        |                       |-- Status: APPROVED
   |                         |                        |                        |                       |-- Tồn kho chưa tăng
   |                         |                        |                        |                       |-- Ghi audit
   |                         |                        |                        |                       |
   |                         |                        |                [Cất kệ - Putaway]               |
   |                         |                        |                        |-- Chọn bin/vị trí     |
   |                         |                        |                        |-- Xác nhận cất kệ --> |
   |                         |                        |                        |                       |-- Status: PUTAWAY_COMPLETED
   |                         |                        |                        |                       |-- Tồn kho tăng (approved_qty)
   |                         |                        |                        |                       |-- Sinh batch_code
   |                         |                        |                        |                       |-- Ghi audit
```

---

## 4. Luồng – Hàng có lỗi QC (Partially Approved + RTV)

```
PLANNER                  WH_MANAGER               WH_STAFF                 STOREKEEPER              HỆ THỐNG
   |                         |                        |                         |                       |
   |                         |                        |-- Nhập: passed_qty < actual_qty                 |
   |                         |                        |-- Nhập: failed_qty > 0  |                       |
   |                         |                        |-- Nhập lý do lỗi       |                       |
   |                         |                        |-- Nộp kết quả -------->|                       |
   |                         |                        |                        |                       |-- Status: PENDING_STOREKEEPER_REVIEW
   |                         |                        |                        |                       |
   |                         |                        |                        |-- Phê duyệt review -->|
   |                         |                        |                        |                       |-- Status: QC_FAILED
   |                         |                        |                        |                       |-- failed_qty → Quarantine (readiness)
   |                         |                        |                        |                       |-- Ghi audit
   |                         |                        |                        |                       |
   |                  [WH_MANAGER duyệt phần đạt]     |                        |                       |
   |                         |-- Phê duyệt passed qty |                        |                       |
   |                         |                        |                        |                       |-- Status: PARTIALLY_APPROVED
   |                         |                        |                        |                       |-- failed_qty → Quarantine chính thức
   |                         |                        |                        |                       |-- Ghi audit
   |                         |                        |                        |                       |
   |                         |                [Cất kệ - Putaway]               |                       |
   |                         |                        |                        |-- Chọn bin            |
   |                         |                        |                        |-- Xác nhận ---------->|
   |                         |                        |                        |                       |-- Status: PUTAWAY_COMPLETED
   |                         |                        |                        |                       |-- Tồn kho tăng (passed_qty only)
   |                         |                        |                        |                       |
   |                  [Xử lý Quarantine - RTV]        |                        |                       |
   |                         |-- Tạo phiếu RTV        |                        |                       |
   |                         |   (failed_qty)         |                        |                       |-- Sinh Adjustment RETURN_TO_VENDOR
   |                         |                        |                        |                       |-- Sinh Debit Note
   |                         |                        |                        |                       |-- Quarantine tồn chưa trừ
   |                         |                        |                        |                       |
   |                         |-- Xác nhận bàn giao NCC|                        |                       |
   |                         |                        |                        |                       |-- Quarantine tồn giảm đúng failed_qty
   |                         |                        |                        |                       |-- Ghi audit QUARANTINE_RTV_CONFIRM
```

---

## 5. Luồng – Storekeeper yêu cầu đếm lại

```
WH_STAFF                                    STOREKEEPER                           HỆ THỐNG
   |                                              |                                    |
   |-- Nộp count & QC --------------------------->|                                    |
   |                                              |                                    |-- Status: PENDING_STOREKEEPER_REVIEW
   |                                              |                                    |
   |                                              |-- Xem kết quả                      |
   |                                              |-- YÊU CẦU ĐẾM LẠI (có lý do) ---->|
   |                                              |                                    |-- Status: RECOUNT_REQUIRED
   |                                              |                                    |-- Xóa QC readiness chưa finalize
   |                                              |                                    |-- Ghi audit RECEIPT_STOREKEEPER_RECOUNT_REQUEST
   |                                              |                                    |
   |<-- Hệ thống thông báo yêu cầu đếm lại -------|                                    |
   |                                              |                                    |
   |-- Sửa và nộp lại count & QC ---------------->|                                    |
   |                                              |                                    |-- Status: PENDING_STOREKEEPER_REVIEW
   |                                              |                                    |
   |                                              |-- Phê duyệt review ----------------->
   |                                              |                                    |-- Status: QC_COMPLETED hoặc QC_FAILED
```

---

## 6. Luồng – WH_MANAGER từ chối trước khi nhận hàng (Revision Required)

```
PLANNER                              WH_MANAGER                            HỆ THỐNG
   |                                      |                                    |
   |-- Tạo phiếu nhập ------------------->|                                    |
   |                                      |                                    |-- Status: PENDING_MANAGER_APPROVAL
   |                                      |                                    |
   |                                      |-- Từ chối (có lý do) ------------->|
   |                                      |                                    |-- Status: REVISION_REQUIRED
   |                                      |                                    |-- Ghi audit RECEIPT_PRE_RECEIVE_REJECT
   |                                      |                                    |-- WH_STAFF bị chặn không được nhận hàng
   |                                      |                                    |
   |<-- Nhận thông báo cần sửa phiếu -----|                                    |
   |                                      |                                    |
   |-- Sửa thông tin và gửi lại ---------->|                                   |
   |                                      |                                    |-- Status: PENDING_MANAGER_APPROVAL
   |                                      |                                    |
   |                                      |-- Phê duyệt ---------------------->|
   |                                      |                                    |-- Status: PENDING_RECEIPT
   |                                      |                                    |-- WH_STAFF được phép nhận hàng
```

---

## 7. Luồng – WH_MANAGER từ chối toàn bộ phiếu sau QC (Return to Supplier)

```
WH_MANAGER                                                                    HỆ THỐNG
   |                                                                               |
   |   [Phiếu ở QC_COMPLETED hoặc QC_FAILED]                                      |
   |                                                                               |
   |-- Từ chối toàn bộ phiếu (có lý do) ---------------------------------------->|
   |                                                                               |-- Status: RETURN_TO_SUPPLIER_PENDING
   |                                                                               |-- Ghi audit
   |                                                                               |-- Không tạo tồn kho
   |                                                                               |
   |-- Xác nhận bàn giao hàng cho NCC ------------------------------------------>|
   |                                                                               |-- Status: RETURNED_TO_SUPPLIER
   |                                                                               |-- Ghi audit
   |                                                                               |-- Tồn kho không tăng
```

---

## 8. Luồng – Hủy phiếu (Cancel)

Phiếu có thể bị hủy tại các trạng thái sau (trước khi có tác động tồn kho):

| Trạng thái có thể hủy | Actor thực hiện |
|-----------------------|-----------------|
| `PENDING_MANAGER_APPROVAL` | PLANNER hoặc WH_MANAGER |
| `REVISION_REQUIRED` | PLANNER hoặc WH_MANAGER |
| `PENDING_RECEIPT` | Hủy trước khi Staff đếm hàng |
| `PENDING_STOREKEEPER_REVIEW` | Hủy trước khi Storekeeper review |
| `RECOUNT_REQUIRED` | Hủy trước khi Staff nộp lại |
| `DRAFT` | WH_MANAGER hoặc PLANNER |

Khi hủy:
- Status chuyển sang `CANCELLED`
- Không xóa vật lý dữ liệu
- Ghi audit với lý do hủy

---

## 9. Ràng buộc nghiệp vụ quan trọng cần thể hiện trong Swimlane

| # | Ràng buộc | Vị trí trong luồng |
|---|-----------|-------------------|
| 1 | WH_STAFF **không được** nhận hàng khi phiếu ở `PENDING_MANAGER_APPROVAL` hoặc `REVISION_REQUIRED` | Cổng điều kiện sau khi PLANNER tạo phiếu |
| 2 | WH_MANAGER **không được** quyết định trước khi Storekeeper review xong | Cổng điều kiện sau Staff nộp |
| 3 | Tồn kho thông thường chỉ tăng tại bước **PUTAWAY_COMPLETED** | Cuối luồng cất kệ |
| 4 | Hàng lỗi QC **không bao giờ** đưa vào tồn kho thông thường | Mọi nhánh có `failed_qty` |
| 5 | Quarantine chính thức chỉ khi WH_MANAGER duyệt `PARTIALLY_APPROVED` hoặc từ chối toàn bộ | Sau quyết định WH_MANAGER |
| 6 | RTV tạo ra Debit Note nhưng **chưa trừ** Quarantine | Bước tạo RTV |
| 7 | Quarantine chỉ trừ khi **Storekeeper xác nhận bàn giao RTV** | Bước xác nhận RTV |
| 8 | Mỗi phiếu chỉ có **1 RTV** (duplicate trả 409) | Bước tạo RTV |
| 9 | `passed_qty + failed_qty` **phải bằng** `actual_qty` | Validate khi Staff nộp |
| 10 | Mọi mutation cần `expectedVersion` (trừ tạo phiếu lần đầu) | Tất cả các bước ghi |

---

## 10. Màn hình & Actor tương ứng

| Màn hình (Screen) | Actor thao tác |
|------------------|---------------|
| Receipt List | Tất cả (view only cho CEO, ACCT_MANAGER) |
| **Receipt Create** | PLANNER |
| **Receipt Pre-Receive Approval** | WH_MANAGER |
| **Nhận hàng & QC đầu vào** | WH_STAFF (nhập), STOREKEEPER (review) |
| **Putaway Plan** | STOREKEEPER |
| **Quarantine Workspace** | WH_MANAGER, STOREKEEPER (CEO view only) |
| **Returns Workspace / Trả hàng NCC** | WH_MANAGER, STOREKEEPER, ACCT_MANAGER (WH_STAFF và CEO view only) |

---

## 11. Tóm tắt chuyển trạng thái theo Actor

### PLANNER
- Tạo phiếu → `PENDING_MANAGER_APPROVAL`
- Sửa và gửi lại → `REVISION_REQUIRED` → `PENDING_MANAGER_APPROVAL`
- Hủy phiếu → `CANCELLED`

### WH_MANAGER
- Duyệt trước nhận hàng → `PENDING_MANAGER_APPROVAL` → `PENDING_RECEIPT`
- Từ chối trước nhận hàng → `PENDING_MANAGER_APPROVAL` → `REVISION_REQUIRED`
- Duyệt toàn bộ sau QC → `QC_COMPLETED` → `APPROVED`
- Duyệt phần đạt sau QC → `QC_FAILED` → `PARTIALLY_APPROVED`
- Từ chối toàn bộ sau QC → `QC_COMPLETED/QC_FAILED` → `RETURN_TO_SUPPLIER_PENDING`
- Tạo RTV → Sinh Adjustment + Debit Note (trạng thái phiếu không đổi)
- Xác nhận bàn giao NCC → `RETURN_TO_SUPPLIER_PENDING` → `RETURNED_TO_SUPPLIER`

### WH_STAFF
- Nộp count & QC → `PENDING_RECEIPT` → `PENDING_STOREKEEPER_REVIEW`
- Nộp lại sau đếm lại → `RECOUNT_REQUIRED` → `PENDING_STOREKEEPER_REVIEW`

### STOREKEEPER
- Phê duyệt review (all pass) → `PENDING_STOREKEEPER_REVIEW` → `QC_COMPLETED`
- Phê duyệt review (có lỗi) → `PENDING_STOREKEEPER_REVIEW` → `QC_FAILED`
- Yêu cầu đếm lại → `PENDING_STOREKEEPER_REVIEW` → `RECOUNT_REQUIRED`
- Cất kệ → `APPROVED/PARTIALLY_APPROVED` → `PUTAWAY_COMPLETED`
- Xác nhận RTV → Quarantine tồn giảm (trạng thái phiếu không đổi)
