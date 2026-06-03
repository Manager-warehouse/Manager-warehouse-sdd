# Tasks: Master Data — Warehouses & Fleet/Drivers

**Input**: Design artifacts from `.sdd/specs/002-master-data-management/`

**Prerequisites**: plan.md, spec.md, features/, research.md, data-model.md, contracts/

**Backend**: Spring Boot 3.4.5 + Java 21 + Maven | **Frontend**: React 18 + Tailwind CSS

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ensure backend/frontend project structures are ready

- [X] T001 Create backend entity package `com.wms.entity` with base JPA auditing (if not exists)
- [X] T002 Create backend repository package `com.wms.repository` base interfaces
- [X] T003 Create backend DTO package structure: `com.wms.dto.request`, `com.wms.dto.response`
- [X] T004 [P] Create backend mapper package `com.wms.mapper` with MapStruct config

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that ALL user stories depend on

- [X] T005 Verify Flyway master data tables structure against spec.md (already defined in V1 and aligned in V3)
- [X] T006 [P] Create `AuditService` in `com.wms.audit` for logging warehouse operations (actor, action, entity_type, entity_id, old/new values)
- [X] T007 [P] Create `GlobalExceptionHandler` with error codes: BIN_OVER_CAPACITY (422), LOCATION_HAS_STOCK (400), DUPLICATE_PLATE_NUMBER (409), DUPLICATE_LICENSE_NUMBER (409), DUPLICATE_DRIVER_USER (409)
- [X] T008 [P] Create `MasterDataMapper` using MapStruct for all Warehouse/WarehouseLocation/Vehicle/Driver entity ↔ DTO conversions
- [ ] T009 [P] Create frontend shared components: `DataTable.jsx`, `SearchFilter.jsx`, `StatusBadge.jsx`, `ConfirmDialog.jsx` in `frontend/src/components/master-data/`
- [ ] T010 [P] Create base API service configuration in `frontend/src/services/api.js` with auth headers and error interceptors

---

## Phase 3: US-WMS-20 — Warehouses & Bin Locations (Priority: P2)

**Goal**: CRUD for 3 physical warehouses + 1 in-transit virtual warehouse, Zone→Bin hierarchy, capacity tracking

**Independent Test**: Create a warehouse → add Zone → add Bin → verify GET listing, capacity endpoint, deactivation block

### Backend — Entities & Repositories

- [X] T011 [US20] Create `Warehouse.java` entity in `com.wms.entity` with all fields from data-model.md, `@PrePersist` for code auto-generation, `@PreUpdate` for timestamps
- [X] T012 [US20] Create `WarehouseLocation.java` entity in `com.wms.entity` with all fields, `@PrePersist` to auto-generate code in format `{warehouse_code}.{zone_code}` (Zone) or `{warehouse_code}.{zone_code}.{bin}` (Bin)
- [X] T013 [P] [US20] Create `WarehouseRepository.java` in `com.wms.repository` extending `JpaRepository` with `findByIsActive`, `existsByCode`, `countByIsActiveAndId`
- [X] T014 [P] [US20] Create `WarehouseLocationRepository.java` with `findByWarehouseId`, `findByWarehouseIdAndType`, `findByParentId`, `countByParentIdAndIsActiveTrue`, `existsByWarehouseIdAndCode`

### Backend — Services

- [X] T015 [US20] Implement `WarehouseService.java` in `com.wms.service`:
  - `createWarehouse()`: validate unique code, check CEO/Trưởng kho role, create IN_TRANSIT auto-creates default Zone+Bin; audit log
  - `updateWarehouse()`: check manager_id permission before allowing update; audit log
  - `deactivateWarehouse()`: check no active inventory before deactivation; audit log

- [X] T016 [US20] Implement `WarehouseLocationService.java` in `com.wms.service`:
  - `createLocation()`: validate parent zone same warehouse, auto-gen code, reject non ZONE→BIN hierarchy
  - `updateLocation()`: same validations; audit log
  - `deactivateLocation()`: check occupied volume/weight = 0 before deactivation; audit log
  - `getCapacity()`: return available m3/kg

### Backend — Controllers

- [X] T017 [US20] Create `WarehouseController.java` in `com.wms.controller`:
  - `GET /api/v1/warehouses` — list with is_active filter
  - `POST /api/v1/warehouses` — create (role check: CEO/Trưởng kho)
  - `PUT /api/v1/warehouses/{id}` — update (permission check: manager_id)
  - `DELETE /api/v1/warehouses/{id}` — soft-delete (is_active=false)
  - Proper DTO validation with Jakarta Validation annotations

- [X] T018 [US20] Create `WarehouseLocationController.java`:
  - `GET /api/v1/bin-locations` — list with warehouse_id, type, is_quarantine, is_active filters
  - `POST /api/v1/bin-locations` — create Zone or Bin
  - `PUT /api/v1/bin-locations/{id}` — update
  - `DELETE /api/v1/bin-locations/{id}` — soft-delete
  - `GET /api/v1/bin-locations/{id}/capacity` — capacity query

### Backend — Tests

- [X] T019 [US20] Write `WarehouseServiceTest.java` — test create, update, deactivate, validation errors, audit logging
- [X] T020 [US20] Write `WarehouseLocationServiceTest.java` — test create Zone+BIN, same-warehouse validation, capacity check, deactivation with stock
- [X] T021 [US20] Write `WarehouseControllerIntegrationTest.java` — test all endpoints happy + error paths
- [X] T022 [US20] Write `WarehouseLocationControllerIntegrationTest.java` — test all endpoints

### Frontend — Warehouses Pages

- [ ] T023 [P] [US20] Create `frontend/src/services/warehouseService.js` — API calls for warehouse endpoints
- [ ] T024 [P] [US20] Create `frontend/src/services/binLocationService.js` — API calls for bin-location endpoints
- [ ] T025 [US20] Create `WarehouseListPage.jsx` — table listing warehouses with active/inactive filter, deactivate button
- [ ] T026 [US20] Create `WarehouseFormPage.jsx` — create/edit form with Jakarta Validation-equivalent frontend validation
- [ ] T027 [P] [US20] Create `BinLocationListPage.jsx` — tree or table view of Zone→Bin hierarchy per warehouse
- [ ] T028 [US20] Create `BinLocationFormPage.jsx` — create/edit Zone or Bin with warehouse/zone dropdowns
- [ ] T029 [US20] Wire up React Router routes for all warehouse/bin-location pages under `/warehouses` and `/bin-locations`

---

## Phase 4: US-WMS-23 — Fleet & Drivers (Priority: P2)

**Goal**: CRUD for vehicles and drivers, status management, prevent double booking

**Independent Test**: Create vehicle → create driver → PATCH status → verify GET filters → soft-delete

### Backend — Entities & Repositories

- [X] T030 [P] [US23] Create `Vehicle.java` entity in `com.wms.entity` with all fields, status enum validation
- [X] T031 [P] [US23] Create `Driver.java` entity in `com.wms.entity` with all fields, user_id FK→users
- [X] T032 [P] [US23] Create `VehicleRepository.java` with `findByIsActive`, `findByStatus`, `findByPlateNumber` (for uniqueness check)
- [X] T033 [P] [US23] Create `DriverRepository.java` with `findByIsActive`, `findByStatus`, `findByLicenseNumber` (uniqueness), `findByUserId`

### Backend — Services

- [X] T034 [US23] Implement `VehicleService.java`:
  - `createVehicle()`: validate plate_number uniqueness, positive weight
  - `updateVehicle()`: audit log
  - `updateStatus()`: apply allowed transitions (AVAILABLE→ON_TRIP/MAINTENANCE, ON_TRIP→AVAILABLE, MAINTENANCE→AVAILABLE)
  - `deactivateVehicle()`: soft-delete; audit log

- [X] T035 [US23] Implement `DriverService.java`:
  - `createDriver()`: validate user_id not already linked, user has role DRIVER, license_number unique; phone fallback to users.phone
  - `updateDriver()`: re-validate uniqueness for license_number
  - `updateStatus()`: enforce license_expiry→UNAVAILABLE check
  - `deactivateDriver()`: soft-delete driver AND set `users.is_active=false` for linked user; audit log
  - `checkLicenseExpiry()`: scheduled or on-read check for force UNAVAILABLE

### Backend — Controllers

- [X] T036 [US23] Create `VehicleController.java`:
  - `GET /api/v1/vehicles` — list with status, is_active filters
  - `POST /api/v1/vehicles` — create
  - `PUT /api/v1/vehicles/{id}` — update
  - `PATCH /api/v1/vehicles/{id}/status` — quick status update
  - `DELETE /api/v1/vehicles/{id}` — soft-delete

- [X] T037 [US23] Create `DriverController.java`:
  - `GET /api/v1/drivers` — list with status, is_active filters
  - `POST /api/v1/drivers` — create (link to user)
  - `PUT /api/v1/drivers/{id}` — update
  - `PATCH /api/v1/drivers/{id}/status` — quick status update
  - `DELETE /api/v1/drivers/{id}` — soft-delete + cascade user deactivation

### Backend — Tests

- [X] T038 [US23] Write `VehicleServiceTest.java` — test create, update, status transitions, soft-delete, duplicate plate
- [X] T039 [US23] Write `DriverServiceTest.java` — test create, license expiry check, cascade user deactivation, duplicate license, duplicate user_id
- [X] T040 [US23] Write `VehicleControllerIntegrationTest.java` — test all endpoints
- [X] T041 [US23] Write `DriverControllerIntegrationTest.java` — test all endpoints

### Frontend — Fleet Pages

- [ ] T042 [P] [US23] Create `frontend/src/services/vehicleService.js` — API calls
- [ ] T043 [P] [US23] Create `frontend/src/services/driverService.js` — API calls
- [ ] T044 [US23] Create `VehicleListPage.jsx` — vehicles table with status badge, status filter, quick PATCH status button
- [ ] T045 [US23] Create `VehicleFormPage.jsx` — create/edit form
- [ ] T046 [P] [US23] Create `DriverListPage.jsx` — drivers table with status, license expiry warning
- [ ] T047 [US23] Create `DriverFormPage.jsx` — create/edit with user selection dropdown
- [ ] T048 [US23] Wire up React Router routes for all vehicle/driver pages under `/fleet/vehicles` and `/fleet/drivers`

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Finalize, document, verify

- [ ] T049 [P] Add OpenAPI/Swagger annotations to all controller endpoints
- [X] T050 [P] Verify seed data for 3 physical warehouses + 1 in-transit warehouse is present in database (already seeded in V3)
- [X] T051 Run `mvn compile` and fix all compilation errors
- [ ] T052 Run `npm run build` and fix all build errors
- [X] T053 Run full test suite: `mvn test` — verify all tests pass
- [ ] T054 Verify quickstart.md scenarios end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Phase 1
- **US-WMS-20 (Phase 3)**: Depends on Phase 1+2
- **US-WMS-23 (Phase 4)**: Depends on Phase 1+2
- **Polish (Phase 5)**: Depends on Phase 3+4

### User Story Dependencies
- **US-WMS-20** and **US-WMS-23**: INDEPENDENT — no shared entities or services
- Both can be implemented in parallel after Phase 2

### Within Each User Story
- Entities → Repositories → Services → Controllers → Tests → Frontend
- Tests for each story are independent and test only that story's contracts

### Parallel Opportunities
All [P] markers indicate tasks that can run in parallel (different files, no dependencies).
Major parallel splits:
- T006 (AuditService) + T007 (ExceptionHandler) + T008 (Mapper) + T009 (Shared UI) in Phase 2
- T013 + T014 (Repositories) in Phase 3
- T023 + T024 (Frontend services) in Phase 3
- T030 + T031 + T032 + T033 (Entities+Repos) in Phase 4
- T042 + T043 (Frontend services) in Phase 4
- Phases 3 and 4 can run fully in parallel

---

## Implementation Strategy

### MVP Scope
Both US-WMS-20 and US-WMS-23 are P2 but both are needed for Sprint 1 core operations:
- US-WMS-20 is prerequisite for inbound putaway (US-WMS-03), outbound picking (US-WMS-07), and transfer (US-WMS-12)
- US-WMS-23 is prerequisite for trip dispatching (US-WMS-08)

### Priority Order
1. **Backend entities + services** (T011-T016, T030-T035) — core data + business rules
2. **Backend controllers + integration tests** (T017-T022, T036-T041) — API layer
3. **Frontend pages** (T023-T029, T042-T048) — UI layer
4. **Polish** (T049-T054) — final checks

### Total Task Count
- Phase 1: 4 tasks
- Phase 2: 6 tasks
- Phase 3 (US-WMS-20): 19 tasks
- Phase 4 (US-WMS-23): 19 tasks
- Phase 5: 6 tasks
- **Total: 54 tasks**

