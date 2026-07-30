# Quickstart: Planner Delivery Order

## Prerequisites

- Backend running with Spring Boot profile configured for local PostgreSQL.
- Test data includes active Planner assigned to a warehouse, active Warehouse Manager assigned to the same warehouse, active dealer, active products, and regular quality-valid inventory.

## Happy Path: Create Delivery Order

1. Login as Planner assigned to warehouse HP.
2. Call `POST /api/v1/delivery-orders` with dealer, warehouse, document date, optional expected delivery date, and item list.
3. Backend validates dealer credit, overdue invoices, planner warehouse scope, product availability, and reservation version.
4. Backend creates Delivery Order in `NEW`.
5. Backend increments `warehouse_product_reservations.reserved_qty`.
6. Backend writes `DELIVERY_ORDER_CREATE` audit.
7. Response returns the created Delivery Order with item requested quantities.

## Happy Path: Planner Update Before Picking Plan

1. Login as Planner assigned to the Delivery Order warehouse.
2. Use a Delivery Order currently in `NEW`.
3. Call `PUT /api/v1/delivery-orders/{id}` with the corrected header fields and full item list.
4. Backend validates the order is still `NEW`.
5. Backend re-runs dealer credit, overdue invoice, warehouse scope, product, price, accounting period, and stock availability checks.
6. Backend applies planner-level reservation deltas in `warehouse_product_reservations`.
7. Backend writes `DELIVERY_ORDER_UPDATE` audit.
8. Response returns the updated Delivery Order still in `NEW`.

## Happy Path: Planner Cancel Before Picking Plan

1. Login as Planner assigned to the Delivery Order warehouse.
2. Use a Delivery Order currently in `NEW`.
3. Call `PUT /api/v1/delivery-orders/{id}/cancel` with `cancelReason`.
4. Backend validates the order is still `NEW` for Planner cancellation.
5. Backend releases planner-level reservations.
6. Backend sets status `CANCELLED`.
7. Backend writes `DELIVERY_ORDER_CANCEL` audit.

## Error Path: Planner Update Or Cancel After Picking Plan

1. Use a Delivery Order already moved to `WAITING_PICKING` by Storekeeper picking-plan save.
2. Login as Planner.
3. Call Planner update or cancel endpoint.
4. Expect `422 DELIVERY_ORDER_CANCEL_FORBIDDEN` or equivalent state error.
5. Verify Delivery Order, reservations, and concrete picking allocations are unchanged.

## Error Path: Credit Limit Exceeded

1. Set dealer `current_balance + order_value > credit_limit`.
2. Call create endpoint.
3. Expect `422 CREDIT_HOLD`.
4. Verify no Delivery Order, item, reservation delta, or audit success event is created.

## Error Path: Insufficient Warehouse Stock

1. Set selected warehouse availability below requested quantity after subtracting planner-level reservations.
2. Call create endpoint.
3. Expect `422 INSUFFICIENT_STOCK`.
4. Response explains that selected warehouse stock is insufficient and does not suggest alternative warehouses.
5. Verify no reservation mutation occurred.

## Happy Path: Cancel Before Warehouse Approval

1. Login as Warehouse Manager assigned to the DO warehouse.
2. Call `PUT /api/v1/delivery-orders/{id}/cancel` with `cancelReason`.
3. Backend validates the DO is before `WAREHOUSE_APPROVED`.
4. Backend releases planner-level and any concrete reservations already assigned by picking.
5. Backend sets status `CANCELLED`.
6. Backend writes `DELIVERY_ORDER_CANCEL` audit.

## Required Tests

- Unit: credit limit equality is allowed.
- Unit: `CREDIT_HOLD` dealer is rejected.
- Unit: unpaid invoices overdue beyond the dealer's configured payment term days reject create.
- Unit: planner warehouse scope is enforced.
- Unit: availability subtracts `warehouse_product_reservations.reserved_qty`.
- Unit: quarantine/non-quality inventory is excluded.
- Unit: successful create increments reservation with optimistic version.
- Unit: Planner update in `NEW` applies reservation deltas and keeps status `NEW`.
- Unit: Planner update outside `NEW` is rejected without reservation changes.
- Unit: Planner cancel in `NEW` releases planner-level reservations.
- Unit: Planner cancel outside `NEW` is rejected without reservation changes.
- Unit: cancellation before `WAREHOUSE_APPROVED` releases reservations.
- Unit: cancellation at `WAREHOUSE_APPROVED` or later is rejected.
- Integration: `POST /api/v1/delivery-orders` happy path and major errors.
- Integration: `PUT /api/v1/delivery-orders/{id}` Planner update happy path and forbidden state.
- Integration: `PUT /api/v1/delivery-orders/{id}/cancel` happy path and forbidden states/roles.

## Verification Commands

```powershell
cd backend
mvn test
mvn compile
```
