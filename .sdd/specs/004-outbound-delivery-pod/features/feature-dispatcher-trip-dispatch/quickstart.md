# Quickstart: Dispatcher Trip Dispatch

## Goal

Implement outbound trip list, create, update, cancel, depart, and complete flows while preserving warehouse scope, Delivery Order readiness, vehicle and driver availability, capacity validation, inventory integrity, and delivery-attempt initialization.

## Suggested implementation order

1. Add or extend repositories and DTOs for trip header, stop-order rows, driver departure, and trip completion requests.
2. Add repository helpers to load trip details with Delivery Orders, validate active-trip conflicts, find the assigned driver by user, and fetch staged inventory rows required for departure movement.
3. Add controller endpoints for:
   - `GET /api/v1/trips`
   - `POST /api/v1/trips`
   - `PUT /api/v1/trips/{id}`
   - `PUT /api/v1/trips/{id}/cancel`
   - `PUT /api/v1/trips/{id}/depart`
   - `PUT /api/v1/trips/{id}/complete`
4. Implement service methods that:
   - validate dispatcher or assigned-driver role plus warehouse or assignment scope
   - validate same-warehouse Delivery Order list, active-trip conflicts, and stop order uniqueness
   - validate vehicle and driver availability
   - calculate total weight and conditional volume against the selected vehicle
   - move staged goods into virtual `IN_TRANSIT` and initialize delivery attempts at departure
   - release vehicle and driver back to `AVAILABLE` at completion
5. Update trip response mapping so the API returns trip header, Delivery Orders, stop order, totals, and current status.

## API walkthrough

### 1. List trips for the dispatch board

```http
GET /api/v1/trips?warehouseId=20&status=PLANNED
Authorization: Bearer <jwt>
```

Expected result:

- Validate the authenticated user is assigned to warehouse 20.
- Apply optional `warehouseId` and `status` filters.
- Return trip headers with assigned Delivery Orders and stop order.
- Order trips newest first for dispatch planning.

### 2. Create a planned trip

Request:

```http
POST /api/v1/trips
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "warehouseId": 20,
  "vehicleId": 301,
  "driverId": 401,
  "plannedDate": "2026-06-22",
  "notes": "Morning route for Hai Phong dealers",
  "deliveryOrders": [
    { "doId": 101, "stopOrder": 1 },
    { "doId": 102, "stopOrder": 2 }
  ]
}
```

Expected result:

- Validate Dispatcher is assigned to warehouse 20.
- Validate all Delivery Orders belong to warehouse 20 and are `WAREHOUSE_APPROVED`.
- Validate none of the Delivery Orders, the vehicle, or the driver belongs to another active trip.
- Validate stop order uniqueness.
- Validate total weight and, when configured, total volume against the selected vehicle.
- Persist trip in `PLANNED` with `tripType = DELIVERY`.
- Keep Delivery Orders in `WAREHOUSE_APPROVED`.
- Write `TRIP_CREATE` audit.

### 3. Update a planned trip

```http
PUT /api/v1/trips/9001
```

```json
{
  "vehicleId": 302,
  "driverId": 402,
  "plannedDate": "2026-06-23",
  "notes": "Use backup truck",
  "deliveryOrders": [
    { "doId": 102, "stopOrder": 1 },
    { "doId": 103, "stopOrder": 2 }
  ]
}
```

Expected result:

- Validate trip is still `PLANNED`.
- Re-run same-warehouse, active-trip, stop order, vehicle/driver availability, and capacity rules against the full revised Delivery Order list.
- Persist the final revised membership and route order.
- Keep removed Delivery Orders in `WAREHOUSE_APPROVED`.
- Write `TRIP_UPDATE` audit.

### 4. Driver departs with staged goods

```http
PUT /api/v1/trips/9001/depart
```

```json
{
  "confirmedAt": "2026-06-22T08:00:00+07:00",
  "notes": "Loaded and sealed"
}
```

Expected result:

- Validate the authenticated driver matches the trip driver.
- Validate trip is `PLANNED`.
- Validate every Delivery Order is still `WAREHOUSE_APPROVED`.
- Validate requested quantity is fully covered by QC-passed staged stock.
- Move staged inventory to virtual `IN_TRANSIT` inventory in one transaction.
- Set each Delivery Order to `IN_TRANSIT`.
- Set each item `issued_qty` to the dispatched quantity.
- Create one current `Delivery` attempt per Delivery Order with status `IN_TRANSIT`.
- Mark trip `IN_TRANSIT` and vehicle/driver `ON_TRIP`.
- Write `TRIP_DEPART` and `DELIVERY_ATTEMPT_CREATE` audit events.

### 5. Complete trip after vehicle returns

```http
PUT /api/v1/trips/9001/complete
```

```json
{
  "returnedAt": "2026-06-22T18:00:00+07:00",
  "notes": "Vehicle returned to source warehouse"
}
```

Expected result:

- Validate the authenticated driver matches the trip driver.
- Validate trip is `IN_TRANSIT`.
- Validate every assigned Delivery Order is `COMPLETED` or `RETURNED`.
- Mark trip `COMPLETED`.
- Mark vehicle and driver `AVAILABLE`.
- Leave any returned goods in virtual `IN_TRANSIT`.
- Keep returned Delivery Orders in `RETURNED` until staff count/QC, Storekeeper approval, Storekeeper putaway planning, and staff putaway completion close them as `DELIVERY_FAILED`.
- Write `COMPLETE_TRIP` audit.

## Required tests

- Service test: create trip rejects cross-warehouse Delivery Orders.
- Service test: create trip rejects unavailable or wrong-warehouse vehicle and driver.
- Service test: create trip rejects duplicate stop order and Delivery Order already assigned to another active trip.
- Service test: update trip revalidates the full revised Delivery Order list and ignores the current trip in active-trip checks.
- Service test: cancel trip only works from `PLANNED` and leaves Delivery Orders in `WAREHOUSE_APPROVED`.
- Service test: departure rejects trip when any Delivery Order is no longer `WAREHOUSE_APPROVED`.
- Service test: departure rejects when staged QC-pass quantity is below requested quantity.
- Service test: departure moves staging stock to virtual `IN_TRANSIT`, sets `issued_qty`, creates delivery attempts, and marks vehicle/driver `ON_TRIP`.
- Service test: complete trip rejects when any assigned Delivery Order is not `COMPLETED` or `RETURNED`.
- Service test: complete trip releases vehicle and driver to `AVAILABLE`.
- Controller integration test: list endpoint returns scoped trips with warehouse and status filters.
- Controller integration test: create and update endpoints return trip detail for happy path.
- Controller integration test: cancel, depart, and complete endpoints return expected business errors and status transitions.

## Definition of done reminders

- Keep all trip endpoints documented in OpenAPI.
- Do not dispatch goods that are not fully QC-passed in staging.
- Preserve non-negative inventory and optimistic locking on departure movement.
- Ensure every trip mutation writes audit logs with before/after context.
