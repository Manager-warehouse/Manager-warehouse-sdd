# Plan: Master Data — Warehouses & Fleet/Drivers

**Spec**: `.sdd/specs/002-master-data-management/spec.md`
**Features**: `feature-admin-warehouses.md` (US-WMS-20) + `feature-dispatcher-fleet-drivers.md` (US-WMS-23)
**Branch**: `feat/son-adminWarehouse-dispatcherFleetDrivers`
**Tech**: Spring Boot 3.4.5 + Java 21 / React 18 + Tailwind / PostgreSQL 18

---

## 1. Entities & Data Model

### Warehouse
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL PK | |
| code | VARCHAR(20) UNIQUE NN | HP / HN / HCM / IN_TRANSIT |
| name | VARCHAR(255) NN | |
| address | TEXT | |
| phone | VARCHAR(20) | |
| manager_id | BIGINT FK→users | Must have role Trưởng kho |
| type | VARCHAR(20) | CHECK 'PHYSICAL','IN_TRANSIT' |
| is_active | BOOLEAN DEF true | |
| created_by, updated_by, created_at, updated_at | | |

### WarehouseLocation (Zone/Bin)
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL PK | |
| warehouse_id | BIGINT FK→warehouses NN | |
| code | VARCHAR(50) UNIQUE NN | Auto-gen: `{wh}.{zone}` (Zone) / `{wh}.{zone}.{bin}` (Bin) |
| type | VARCHAR(10) | CHECK 'ZONE','BIN' |
| parent_id | BIGINT FK→self | NULL for ZONE, required for BIN (same warehouse) |
| capacity_m3 | DECIMAL(10,3) | BIN only |
| capacity_kg | DECIMAL(10,2) | BIN only |
| current_volume_m3 | DECIMAL(10,3) DEF 0 | BIN only |
| current_weight_kg | DECIMAL(10,2) DEF 0 | BIN only |
| is_quarantine | BOOLEAN DEF false | |
| is_active | BOOLEAN DEF true | |

### Vehicle
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL PK | |
| plate_number | VARCHAR(20) UNIQUE NN | |
| vehicle_type | VARCHAR(100) NN | |
| max_weight_kg | DECIMAL(10,2) NN | Positive |
| max_volume_m3 | DECIMAL(10,3) | **Nullable**. If provided, positive |
| status | VARCHAR(20) DEF 'AVAILABLE' | 'AVAILABLE','ON_TRIP','MAINTENANCE' |
| is_active | BOOLEAN DEF true | |

### Driver
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK→users UNIQUE NN | User must have role DRIVER |
| full_name | VARCHAR(255) NN | |
| phone | VARCHAR(20) | **Nullable**. Fallback→users.phone |
| license_number | VARCHAR(50) UNIQUE NN | |
| license_expiry | DATE NN | |
| status | VARCHAR(20) DEF 'AVAILABLE' | 'AVAILABLE','ON_TRIP','UNAVAILABLE' |
| is_active | BOOLEAN DEF true | |

### Relationships
```
Warehouse 1──* WarehouseLocation (warehouse_id)
  Location self-ref: ZONE 1──* BIN (parent_id)
Vehicle 1──* Trip (via outbound feature)
Driver 1──* Trip (via outbound feature)
Driver 1──1 User (user_id)
```

### State machines
```
Vehicle: AVAILABLE ↔ ON_TRIP (trip) | AVAILABLE ↔ MAINTENANCE
Driver:  AVAILABLE ↔ ON_TRIP (trip) | (any) → UNAVAILABLE (license expired)
```

---

## 2. Research Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Code auto-gen | `@PrePersist` in entity | Simple, testable |
| In-transit locations | Auto-create Zone + Bin pair, bypass capacity | Fits ZONE→BIN schema |
| max_volume_m3 nullable | Keep nullable as spec | If null, skip volume check in outbound |
| Driver phone fallback | If null → inherit from users.phone | Avoid redundant data |
| Cascade deactivation | DriverService calls UserRepository directly | Deactivates both in one transaction |
| Bin capacity enforcement | Service layer (not DB) | Called by inbound putaway flow |
| Audit logging | Shared AuditService.log() called from each service | Consistent, single impl |

---

## 3. API Contracts

### Warehouses (`/api/v1/warehouses`)
| Method | Endpoint | Auth | Notes |
|--------|----------|------|-------|
| GET | /warehouses | Any | Filter: is_active |
| POST | /warehouses | CEO/Trưởng kho | IN_TRANSIT auto-creates default Zone+Bin |
| PUT | /warehouses/{id} | manager_id check | |
| DELETE | /warehouses/{id} | Any | Soft-delete. Error if stock>0 |

### Bin Locations (`/api/v1/bin-locations`)
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | /bin-locations | Filters: warehouse_id, type, is_quarantine, is_active |
| POST | /bin-locations | Auto-gen code. Validate parent same warehouse |
| PUT | /bin-locations/{id} | |
| DELETE | /bin-locations/{id} | Soft-delete. Error if occupied>0 |
| GET | /bin-locations/{id}/capacity | Returns available m³, kg |

### Vehicles (`/api/v1/vehicles`)
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | /vehicles | Filters: status, is_active |
| POST | /vehicles | Validate plate_number unique |
| PUT | /vehicles/{id} | |
| PATCH | /vehicles/{id}/status | Quick status update |
| DELETE | /vehicles/{id} | Soft-delete |

### Drivers (`/api/v1/drivers`)
| Method | Endpoint | Notes |
|--------|----------|-------|
| GET | /drivers | Filters: status, is_active |
| POST | /drivers | Validate: user has role DRIVER, unique license, unique user_id |
| PUT | /drivers/{id} | |
| PATCH | /drivers/{id}/status | Quick status update |
| DELETE | /drivers/{id} | Soft-delete + cascade `users.is_active=false` |

Error codes: `BIN_OVER_CAPACITY` (422), `LOCATION_HAS_STOCK` (400), `DUPLICATE_PLATE_NUMBER` (409), `DUPLICATE_LICENSE_NUMBER` (409), `DUPLICATE_DRIVER_USER` (409)

---

## 4. Validation Rules Summary

- **Warehouse**: code unique, manager user must have role Trưởng kho
- **Zone**: unique code within warehouse; `code` auto-gen `{wh}.{zone}`
- **Bin**: parent Zone must exist + same warehouse; `code` auto-gen `{wh}.{zone}.{bin}`
- **Bin deactivation**: blocked if `current_volume_m3 > 0` OR `current_weight_kg > 0`
- **Warehouse deactivation**: blocked if any active inventory exists
- **Vehicle**: plate_number unique, max_weight_kg > 0
- **Driver**: user_id unique + must have role DRIVER, license_number unique, license_expiry not null
- **Status transitions**: only valid transitions allowed (AVAILABLE↔ON_TRIP, AVAILABLE↔MAINTENANCE)
- **License expiry**: status forced to UNAVAILABLE if expired
- **Double booking**: vehicles/drivers in ON_TRIP or assigned to incomplete trips excluded from dropdown

---

## 5. File Structure (to create)

```
backend/src/main/java/com/wms/
├── entity/Warehouse.java
├── entity/WarehouseLocation.java
├── entity/Vehicle.java
├── entity/Driver.java
├── repository/WarehouseRepository.java
├── repository/WarehouseLocationRepository.java
├── repository/VehicleRepository.java
├── repository/DriverRepository.java
├── dto/request/WarehouseRequest.java
├── dto/request/WarehouseLocationRequest.java
├── dto/request/VehicleRequest.java
├── dto/request/VehicleStatusRequest.java
├── dto/request/DriverRequest.java
├── dto/request/DriverStatusRequest.java
├── dto/response/WarehouseResponse.java
├── dto/response/WarehouseLocationResponse.java
├── dto/response/CapacityResponse.java
├── dto/response/VehicleResponse.java
├── dto/response/DriverResponse.java
├── service/WarehouseService.java
├── service/WarehouseLocationService.java
├── service/VehicleService.java
├── service/DriverService.java
├── controller/WarehouseController.java
├── controller/WarehouseLocationController.java
├── controller/VehicleController.java
├── controller/DriverController.java
├── mapper/MasterDataMapper.java
├── audit/AuditService.java

backend/src/main/resources/db/migration/
(Đã định nghĩa cấu trúc trong V1 và V3, không cần file migration mới)

frontend/src/
├── services/warehouseService.js
├── services/binLocationService.js
├── services/vehicleService.js
├── services/driverService.js
├── pages/warehouses/WarehouseListPage.jsx
├── pages/warehouses/WarehouseFormPage.jsx
├── pages/warehouses/BinLocationListPage.jsx
├── pages/warehouses/BinLocationFormPage.jsx
├── pages/fleet/VehicleListPage.jsx
├── pages/fleet/VehicleFormPage.jsx
├── pages/fleet/DriverListPage.jsx
├── pages/fleet/DriverFormPage.jsx
```

---

## 6. Quick Test Scenarios

1. POST warehouse → POST Zone → POST Bin → GET /bin-locations → verify codes
2. Bin with 80/100m³ → attempt putaway 30m³ → error `BIN_OVER_CAPACITY`
3. Bin with volume>0 → DELETE → error `LOCATION_HAS_STOCK`
4. POST IN_TRANSIT warehouse → verify default Zone + Bin created
5. POST vehicle → POST driver → PATCH status → GET with filter
6. Vehicle status=ON_TRIP → not listed in AVAILABLE filter
7. Driver with expired license → status forced UNAVAILABLE
8. DELETE driver → verify driver.is_active=false + user.is_active=false

---

## 7. Audit Logging

Every CREATE/UPDATE/DELETE on warehouses, locations, vehicles, drivers calls `AuditService.log()` with:
`actor_id, actor_role, action, entity_type, entity_id, old_value (JSON), new_value (JSON), timestamp`
