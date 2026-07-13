# Feature Specification: Kiểm thử Nghiệp vụ Kho bãi (WMS Business Test Suite)

**Feature Branch**: `feat/backend-test-sonarqube`
**Created**: 2026-07-01
**Status**: Draft

---

## 1. Context and Goal

Các nghiệp vụ cốt lõi của WMS bao gồm Nhập kho, Xuất kho, Điều chuyển, Kiểm kê và Điều chỉnh tồn kho có các luật nghiệp vụ (Invariants) cực kỳ nghiêm ngặt như: không được tồn kho âm, phải giữ chỗ (reserved quantity) trước khi xuất, phải tuân thủ FIFO theo ngày nhận hàng, và kiểm tra phiên bản `@Version` tránh ghi đè cạnh tranh. Các luật này cần được bảo vệ tuyệt đối bằng unit test và integration test.

**Goal:** Triển khai các bộ test suite (Unit & Integration) bao phủ các dịch vụ nghiệp vụ chính, kiểm tra tính đúng đắn của các ràng buộc tồn kho trước/sau mọi giao dịch kho bãi.

---

## 2. Actors

| Actor | Vai trò | Trách nhiệm |
|-------|---------|-------------|
| Developer | Maker | Viết mã nguồn kiểm thử dịch vụ nghiệp vụ, mock repository, kiểm tra database state |
| QA Engineer | QA Controller | Chạy các kịch bản hồi quy (regression tests) về nghiệp vụ tồn kho trên Staging, nghiệm thu QA Sign-off cho các nghiệp vụ kho bãi |
| Warehouse Manager | Viewer/Auditor | Đảm bảo các luồng kiểm thử phản ánh đúng quy trình vận hành kho thực tế |

---

## 3. User Scenarios & Testing

### User Story 1 - Kiểm thử Ràng buộc Tồn kho và Tránh tồn kho âm (Priority: P1)
**Why this priority:** Invariant quan trọng nhất của WMS: `inventories.total_qty >= 0`, `inventories.reserved_qty >= 0`, và `total_qty - reserved_qty >= 0` luôn đúng.
**Independent Test:** Tạo bài test giả lập xuất kho vượt quá số lượng hàng khả dụng trong kho.
**Acceptance Scenarios:**
1. **Given** Sản phẩm A có `total_qty = 10` và `reserved_qty = 8` (available = 2), **When** Thực hiện xuất kho số lượng 3 sản phẩm A, **Then** Hệ thống ném ra `InsufficientInventoryException` và không cho phép giao dịch.
2. **Given** Quá trình điều chỉnh tồn kho (Adjustment) được thực hiện, **When** Giảm số lượng tồn kho vượt quá số lượng hiện tại, **Then** Giao dịch bị từ chối, trả về lỗi nghiệp vụ hợp lý.

### User Story 2 - Kiểm thử Nguyên tắc FIFO và optimistic locking (Priority: P1)
**Why this priority:** Đảm bảo xuất hàng cũ trước theo đúng hạn ngày nhận (FIFO) và tránh xung đột khi nhiều thủ kho cùng thao tác một lô hàng.
**Independent Test:** Viết test đa luồng hoặc test so sánh ngày nhập kho của lô hàng được xuất.
**Acceptance Scenarios:**
1. **Given** Có 2 lô hàng sản phẩm A: Lô 1 nhập ngày 2026-06-01, Lô 2 nhập ngày 2026-06-05, **When** Có yêu cầu xuất kho sản phẩm A, **Then** Hệ thống ưu tiên chọn xuất từ Lô 1 trước (FIFO).
2. **Given** Một bản ghi tồn kho có `@Version` hiện tại, **When** 2 luồng đồng thời cố gắng cập nhật số lượng tồn kho, **Then** Luồng thứ hai cập nhật sau phải nhận được `ObjectOptimisticLockingFailureException`.

---

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN thực hiện bất kỳ giao dịch kho nào (Nhập, Xuất, Điều chuyển, Điều chỉnh), hệ thống SHALL ghi nhận bản ghi Audit Log đầy đủ.
- **FR-002**: WHILE cập nhật tồn kho, hệ thống SHALL luôn đảm bảo điều kiện `total_qty >= reserved_qty` được duy trì.
- **FR-003**: IF hàng hóa không qua được QC, hệ thống SHALL chuyển hàng hóa đó vào Quarantine Zone và không tính vào số lượng tồn kho khả dụng để xuất.
- **FR-004**: WHERE các giao dịch điều chuyển liên kho được thực thi, hệ thống SHALL ghi nhận trạng thái hàng hóa ở In-Transit location trước khi kho đích xác nhận.

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Business Service Test Coverage | Line coverage tối thiểu 80% cho các class Service nghiệp vụ |
| NFR-002 | Test isolation | Mỗi bài test integration nghiệp vụ phải chạy độc lập trên transaction rollback |
| NFR-003 | QA Sign-off | Mọi thay đổi về nghiệp vụ kho bãi phải đạt 100% kịch bản UAT nghiệp vụ trên môi trường Staging và được QA Engineer ký duyệt |

---

## 6. Target Components to Test

- `com.wms.service.impl.InventoryServiceImpl` (Inventory mutations, FIFO, reserving).
- `com.wms.service.impl.InboundReceiptServiceImpl` (Receipts, QC, Putaway).
- `com.wms.service.impl.OutboundDeliveryServiceImpl` (DO, picking, POD).
- `com.wms.service.impl.TransferServiceImpl` (In-Transit, confirmations).
- `com.wms.service.impl.AdjustmentServiceImpl` (Stocktake variances, approvals).

---

## 7. Error Handling

| Error | HTTP Status | Target Exception |
|-------|-------------|------------------|
| Insufficient Inventory | 400 Bad Request / 422 | `InsufficientInventoryException` |
| Version Conflict | 409 Conflict | `ObjectOptimisticLockingFailureException` |
| Invalid Status Transition | 400 Bad Request | `InvalidStatusTransitionException` |

---

## 8. Success Criteria

- **SC-001**: Các bài test kiểm tra luồng happy path cho Inbound, Outbound, Transfer chạy thành công.
- **SC-002**: Các kịch bản lỗi (thiếu hàng, sai trạng thái, xung đột version) được kiểm thử đầy đủ và trả về Exception mong muốn.
- **SC-003**: Không có bài test nào kết nối hoặc làm ảnh hưởng dữ liệu thật của các kho.
