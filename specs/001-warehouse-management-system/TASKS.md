# Tasks: Hệ Thống Quản Lý Kho (Warehouse Management System)

**Input**: Design documents from \/specs/001-warehouse-management-system/\

**Prerequisites**: [plan.md](./PLAN.md) (required), [spec.md](./spec.md) (required for user stories), [data-model.md](./data-model.md), [contracts/api-contracts.md](./contracts/api-contracts.md)

## Format: \[ID] [P?] [Story] Description\

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 [P] Create project structure per plan.md (wms-backend/, wms-frontend/)
- [ ] T002 [P] Initialize Maven project with Spring Boot 3.4.5 dependencies
- [ ] T003 [P] Initialize npm project with React 18 + TypeScript dependencies
- [ ] T004 [P] Configure linting (ESLint + Prettier) and formatting tools
- [ ] T005 [P] Setup database schema and migrations framework (Flyway)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 [P] Create base entities: Warehouse, Product, Unit (đơn vị tính)
- [ ] T007 [P] Create Dealer entity and related repositories
- [ ] T008 Setup Spring Security with JWT authentication
- [ ] T009 Configure error handling and centralized exception handler
- [ ] T010 [P] Setup API routing and middleware structure
- [ ] T011 Configure SLF4J logging infrastructure with audit trail
- [ ] T012 Setup environment configuration management (.env, application.yml)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - FR-WH-01: Quản lý Danh mục Hàng hóa & Tồn kho (Priority: P1) 🎯 MVP

**Goal**: Quản lý sản phẩm, đơn vị tính, theo dõi tồn kho theo thời gian thực

**Independent Test**: CRUD sản phẩm và xem tồn kho hoạt động độc lập

### Tests for User Story 1

- [ ] T013 [P] [US1] Contract test for \GET /api/products\, \POST /api/products\
- [ ] T014 [P] [US1] Contract test for \GET /api/inventory\, \GET /api/inventory/warehouse/{id}\

### Implementation for User Story 1

- [ ] T015 [P] [US1] Create Product entity in \src/main/java/com/wms/entity/Product.java\
- [ ] T016 [P] [US1] Create Inventory entity in \src/main/java/com/wms/entity/Inventory.java\
- [ ] T017 [P] [US1] Create Unit entity in \src/main/java/com/wms/entity/Unit.java\
- [ ] T018 [US1] Create ProductRepository and InventoryRepository
- [ ] T019 [US1] Implement ProductService in \src/main/java/com/wms/service/ProductService.java\
- [ ] T020 [US1] Implement InventoryService in \src/main/java/com/wms/service/InventoryService.java\
- [ ] T021 [US1] Implement ProductController in \src/main/java/com/wms/controller/ProductController.java\
- [ ] T022 [US1] Implement InventoryController in \src/main/java/com/wms/controller/InventoryController.java\
- [ ] T023 [US1] Add validation and error handling for negative inventory prevention
- [ ] T024 [US1] Add audit logging for inventory changes

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - FR-WH-02: Nhập kho từ Nhà cung cấp & QC (Priority: P1)

**Goal**: Quản lý PO từ mua hàng, tiếp nhận hàng, kiểm tra QC và phân loại lưu kho

**Independent Test**: Tạo receipt từ PO, QC pass/fail, nhập vào kho hoặc quarantine

### Tests for User Story 2

- [ ] T025 [P] [US2] Contract test for \POST /api/receipts\, \PUT /api/receipts/{id}/qc\
- [ ] T026 [P] [US2] Integration test for receipt creation flow

### Implementation for User Story 2

- [ ] T027 [P] [US2] Create Receipt entity in \src/main/java/com/wms/entity/Receipt.java\
- [ ] T028 [P] [US2] Create ReceiptItem entity in \src/main/java/com/wms/entity/ReceiptItem.java\
- [ ] T029 [P] [US2] Create PurchaseOrder entity (external reference)
- [ ] T030 [US2] Create WarehouseZone entity for quarantine zone per warehouse
- [ ] T031 [US2] Implement ReceiptService with QC logic
- [ ] T032 [US2] Implement ReceiptController
- [ ] T033 [US2] Add validation: cannot receive negative quantity
- [ ] T034 [US2] Add audit logging for receipt operations
- [ ] T035 [US2] Send event to Accounting on receipt completion

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - FR-WH-03: Điều chuyển Nội bộ Giữa Các Kho (Priority: P1)

**Goal**: Di chuyển hàng giữa 3 kho với In-Transit tracking

**Independent Test**: Tạo transfer, xác nhận nhận, kiểm tra inventory thay đổi đúng

### Tests for User Story 3

- [ ] T036 [P] [US3] Contract test for \POST /api/transfers\, \PUT /api/transfers/{id}/receive\
- [ ] T037 [P] [US3] Integration test for transfer flow

### Implementation for User Story 3

- [ ] T038 [P] [US3] Create Transfer entity in \src/main/java/com/wms/entity/Transfer.java\
- [ ] T039 [P] [US3] Create TransferItem entity in \src/main/java/com/wms/entity/TransferItem.java\
- [ ] T040 [US3] Create virtual "In-Transit" warehouse logic
- [ ] T041 [US3] Implement TransferService with status management
- [ ] T042 [US3] Implement TransferController
- [ ] T043 [US3] Add validation: cannot transfer more than available inventory
- [ ] T044 [US3] Add audit logging for transfer operations

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: User Story 4 - FR-WH-04: Xuất kho cho Đại lý & Quản lý Đơn hàng Sale (Priority: P1)

**Goal**: Xuất kho cho đại lý từ đơn hàng Sale, theo dõi công nợ

**Independent Test**: Tạo sale order, xuất kho, kiểm tra công nợ đại lý

### Tests for User Story 4

- [ ] T045 [P] [US4] Contract test for \POST /api/sale-orders\, \POST /api/issues\
- [ ] T046 [P] [US4] Integration test for sale order flow

### Implementation for User Story 4

- [ ] T047 [P] [US4] Create SaleOrder entity in \src/main/java/com/wms/entity/SaleOrder.java\
- [ ] T048 [P] [US4] Create SaleOrderItem entity in \src/main/java/com/wms/entity/SaleOrderItem.java\
- [ ] T049 [P] [US4] Create Issue entity in \src/main/java/com/wms/entity/Issue.java\
- [ ] T050 [P] [US4] Create IssueItem entity in \src/main/java/com/wms/entity/IssueItem.java\
- [ ] T051 [US4] Implement SaleOrderService
- [ ] T052 [US4] Implement IssueService (FEFO selection logic)
- [ ] T053 [US4] Implement SaleOrderController
- [ ] T054 [US4] Implement IssueController
- [ ] T055 [US4] Add validation: check inventory before issue
- [ ] T056 [US4] Add audit logging for issue operations
- [ ] T057 [US4] Send event to Accounting on issue completion

**Checkpoint**: Core warehouse operations now complete

---

## Phase 7: User Story 5 - FR-WH-05: Báo cáo & Kiểm soát Hệ thống (Priority: P1)

**Goal**: Dashboard tổng quan, báo cáo tồn kho, cảnh báo reorder point

### Implementation for User Story 5

- [ ] T058 [P] [US5] Create ReportService for dashboard metrics
- [ ] T059 [P] [US5] Create ReportController with \GET /api/reports/dashboard\
- [ ] T060 [US5] Create InventoryAlertService for reorder point monitoring
- [ ] T061 [US5] Create notification system for low stock alerts

---

## Phase 8: User Story 6 - FR-WH-06: Kiểm kê & Điều chỉnh Tồn kho (Priority: P1)

**Goal**: Kiểm kê định kỳ, so sánh thực tế, điều chỉnh khi có chênh lệch

### Implementation for User Story 6

- [ ] T062 [P] [US6] Create StockTake entity in \src/main/java/com/wms/entity/StockTake.java\
- [ ] T063 [P] [US6] Create StockAdjustment entity in \src/main/java/com/wms/entity/StockAdjustment.java\
- [ ] T064 [US6] Implement StockTakeService with approval workflow
- [ ] T065 [US6] Implement StockAdjustmentService
- [ ] T066 [US6] Add audit logging for adjustments

---

## Phase 9: User Story 7 - FR-WH-07: Quản lý Trạng thái Vận chuyển (Priority: P1)

**Goal**: Theo dõi vận đơn từ xuất kho đến giao hàng thành công

### Implementation for User Story 7

- [ ] T067 [P] [US7] Create Delivery entity in \src/main/java/com/wms/entity/Delivery.java\
- [ ] T068 [P] [US7] Create Vehicle entity and Driver entity
- [ ] T069 [US7] Implement DeliveryService with status management
- [ ] T070 [US7] Implement DeliveryController
- [ ] T071 [US7] Add POD (Proof of Delivery) capture

---

## Phase 10: P2 Features

### US-WH-09: Quét Barcode/QR Code (Priority: P2)

- [ ] T072 [P] Barcode scanning service integration
- [ ] T073 [P] BarcodeController for scan operations

### US-WH-10: Hoàn hàng từ Đại lý (Priority: P2)

- [ ] T074 [P] Return entity and ReturnService
- [ ] T075 [P] Credit Note integration with Accounting

### US-WH-12: Quản lý Giá (Priority: P2)

- [ ] T076 [P] Price configuration service
- [ ] T077 [P] Promotion/discount service

### US-WH-13: Quản lý Lô Sản phẩm (Priority: P2)

- [ ] T078 [P] Batch entity with expiry date tracking
- [ ] T079 [P] FEFO/FIFO selection logic

### US-WH-14: Quản lý Vị trí Kho (Priority: P2)

- [ ] T080 [P] WarehouseZone, Rack, Shelf entities
- [ ] T081 [P] Location mapping service

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T082 [P] Frontend: Dashboard page in React
- [ ] T083 [P] Frontend: CRUD pages for Product, Warehouse
- [ ] T084 [P] Frontend: Receipt/Issue/Transfer forms
- [ ] T085 Security hardening
- [ ] T086 Performance optimization
- [ ] T087 Documentation updates

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational - May integrate with US1
- **User Story 3 (P1)**: Can start after Foundational - May integrate with US1/US2
- **User Story 4 (P1)**: Can start after Foundational - Depends on US1 for inventory checks

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence