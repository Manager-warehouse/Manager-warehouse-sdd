# Implementation Plan: Kiểm thử Nghiệp vụ Kho bãi (WMS Business Test Suite)

**Branch**: `feat/backend-test-sonarqube` | **Date**: 2026-07-01 | **Spec**: [spec.md](./spec.md)

## Summary

Viết các bài kiểm thử Unit và Integration cho các service nghiệp vụ cốt lõi của WMS: Nhập kho (Inbound), Xuất kho (Outbound), Điều chuyển (Transfer) và Kiểm kê/Điều chỉnh (Stocktake/Adjustment). Đảm bảo các luật bất biến về tồn kho (FIFO, optimistic locking, tồn kho âm) được kiểm thử kỹ lưỡng.

## Technical Context

- **Language/Version**: Java 21 / Spring Boot 3.4.5
- **Primary Dependencies**: Spring Data JPA, Hibernate, Mockito
- **Testing Tools**: JUnit 5, Mockito Extension, `@DataJpaTest` hoặc `@SpringBootTest` kết hợp Transactional rollback.
- **Constraints**: Phải kiểm tra cơ chế `@Version` để ngăn ngừa race condition cập nhật tồn kho.

## Constitution Check
*GATE: Passed*

- [x] Layered architecture preserved.
- [x] Service business rules tested.
- [x] Inventory invariants (total_qty >= 0, available = total - reserved >= 0) verified via test assertions.
- [x] Optimistic locking conflict paths tested.
- [x] Integration tests use Transactional rollback to prevent database pollution.

## Domain Impact

- **Actors/Roles**: Storekeeper, Warehouse Manager, Accountant, QA Engineer (UAT & QA Sign-off validation).
- **State Changes**: Trạng thái Inbound Receipt, Outbound Delivery Order, Transfer Order, Adjustment.
- **Inventory Impact**: Tồn kho biến động tương ứng theo các luồng nghiệp vụ.
- **Audit Actions**: Mọi transaction kho đều sinh Audit Logs tương ứng.

## Data Model / Migration Impact

- Entities/tables touched: `inventories`, `inventory_transactions`, `inbound_receipts`, `delivery_orders`, `transfers`, `adjustments`.
- Flyway plan: Không thay đổi DB schema, sử dụng schema hiện tại.

## API / Contract Impact

- Đảm bảo các nghiệp vụ kho ném ra đúng Exception nghiệp vụ khi vi phạm luật tồn kho:
  - `InsufficientInventoryException` khi xuất quá số lượng khả dụng.
  - `ObjectOptimisticLockingFailureException` khi có xung đột cập nhật đồng thời.

## Test Strategy

- **Inventory Service Unit Tests**:
  - Viết `InventoryServiceTest.java` dùng Mockito mock repository.
  - Test luồng tính toán FIFO: Chọn đúng lô hàng cũ nhất để xuất trước.
  - Test luồng Reserve hàng: Tăng `reserved_qty` khi tạo DO, giảm `total_qty` và `reserved_qty` khi xuất hàng.
- **Optimistic Locking Test**:
  - Viết Integration Test giả lập 2 luồng đồng thời cập nhật tồn kho bản ghi giống nhau, assert luồng thứ hai ném ra lỗi xung đột version.
- **Inbound & Outbound Service Tests**:
  - `InboundReceiptServiceIT.java`: Chạy luồng hoàn chỉnh từ nhận hàng, QC, Putaway lên bin.
  - `DeliveryOrderServiceIT.java`: Chạy luồng từ DO, Picking, Outbound QC, bàn giao tài xế.
- **Transfer & Adjustment Tests**:
  - `TransferServiceIT.java`: Test chuyển hàng từ kho nguồn -> In-Transit location -> kho đích.
  - `AdjustmentServiceIT.java`: Test chênh lệch kiểm kê tạo bản ghi điều chỉnh tồn kho sau khi phê duyệt.

## Project Structure

```text
.sdd/specs/011-backend-test-sonarqube/features/feature-test-wms-operations/
├── spec.md
└── plan.md

backend/src/test/java/com/wms/service/
├── InventoryServiceTest.java
├── InboundReceiptServiceIT.java
├── DeliveryOrderServiceIT.java
├── TransferServiceIT.java
└── AdjustmentServiceIT.java
```

**Structure Decision**: Nhóm các file test service nghiệp vụ trong package `com.wms.service` để dễ đối chiếu với mã nguồn thực tế tương ứng.
