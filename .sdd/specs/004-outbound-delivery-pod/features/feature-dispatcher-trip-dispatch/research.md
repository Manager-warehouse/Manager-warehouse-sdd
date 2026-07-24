# Research: Dispatcher Trip Dispatch

## Decision: Keep trip workflow in a dedicated Trip service instead of extending DeliveryOrderService

**Rationale**: The repository already has dedicated `Trip`, `TripDeliveryOrder`, and `Delivery` entities, so trip planning and departure are a separate lifecycle aggregate from Delivery Order authoring and warehouse QC. A dedicated service isolates vehicle/driver assignment, active-trip conflict validation, and delivery-attempt initialization while still coordinating Delivery Order status updates through repositories.

**Alternatives considered**: Extending `DeliveryOrderServiceImpl` was rejected because trip planning spans multiple Delivery Orders plus resource assignment, which would overload the current outbound order service.

## Decision: Treat active-trip conflicts as membership in `PLANNED` or `IN_TRANSIT` trips only

**Rationale**: The spec explicitly allows reassignment after trip cancellation and after operational completion. Therefore vehicle, driver, and Delivery Order availability checks should ignore `CANCELLED` and `COMPLETED` trips and only block assignments that belong to another active trip.

**Alternatives considered**: Blocking reuse across every historical trip was rejected because it would prevent valid re-planning and contradict the acceptance criteria.

## Decision: Model trip update as replacement of the full Delivery Order list

**Rationale**: The update spec says `deliveryOrders[]` replaces the current list when provided. Using the final revised list makes stop order uniqueness, capacity recalculation, and add/remove validation deterministic and avoids patch-order ambiguity.

**Alternatives considered**: Incremental add/remove operations were rejected because they make active-trip conflict and final capacity validation harder to reason about.

## Decision: Run departure inventory movement by aggregating staged QC-pass stock per product and batch

**Rationale**: Departure dispatches already QC-approved goods from outbound staging to virtual `IN_TRANSIT`. Aggregating the staged rows by product, batch, and warehouse keeps the movement aligned with current inventory modeling while still allowing item-level `issued_qty` and delivery-attempt creation per Delivery Order.

**Alternatives considered**: Moving stock directly from Delivery Order items without referencing concrete staging inventory was rejected because inventory integrity in this codebase is enforced on actual inventory rows.

## Decision: Initialize delivery attempts only at departure

**Rationale**: The feature spec states a planned trip is only a dispatch plan; the physical delivery attempt starts once the driver confirms goods received and leaves the warehouse. Creating `Delivery` rows at departure avoids cleanup complexity for cancelled or edited planned trips.

**Alternatives considered**: Pre-creating delivery attempts during trip creation was rejected because it would create inactive rows for trips that may still be revised or cancelled.

## Decision: Complete trips only from `IN_TRANSIT` after all Delivery Orders are terminal

**Rationale**: A trip is an operational route. The feature should not complete until the vehicle is back and every assigned Delivery Order is either `COMPLETED` or `RETURNED`. Returned goods remain in virtual `IN_TRANSIT` for the separate return flow, so trip completion must not touch regular inventory or change returned Delivery Orders to `DELIVERY_FAILED`. That later closure requires staff count/QC, Storekeeper quantity/quality approval, Storekeeper putaway planning, and staff putaway confirmation.

**Alternatives considered**: Completing a trip when the last delivery is marked terminal was rejected because the spec requires explicit driver return confirmation and resource release at the warehouse.

## Decision: Capacity validation always enforces weight and conditionally enforces volume

**Rationale**: Vehicle entities already store `maxWeightKg` and optional `maxVolumeM3`. The dispatch workflow should always enforce weight, and only apply volume validation when the vehicle has a configured volume limit, matching the feature spec exactly.

**Alternatives considered**: Requiring volume for every vehicle was rejected because some vehicles intentionally omit `maxVolumeM3`.
