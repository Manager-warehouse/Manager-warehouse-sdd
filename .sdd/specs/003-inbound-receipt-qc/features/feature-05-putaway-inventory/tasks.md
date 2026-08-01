# Tasks: Feature 05 Putaway And Inventory

- [ ] F05-T001 Add tests for putaway from `APPROVED`.
- [ ] F05-T002 Add tests for putaway from `PARTIALLY_APPROVED`.
- [ ] F05-T003 Add tests for putaway quantity mismatch.
- [ ] F05-T004 Add tests for invalid location and capacity exceeded.
- [ ] F05-T004A Add tests requiring STOREKEEPER to provide a target bin/location for every putaway allocation.
- [ ] F05-T005 Add tests for duplicate putaway idempotency.
- [ ] F05-T006 Implement allocation validation equals `approved_qty`.
- [ ] F05-T006A Implement required per-allocation target bin/location validation before capacity and inventory mutation.
- [ ] F05-T007 Implement regular inventory increase with optimistic locking.
- [ ] F05-T008 Set `PUTAWAY_COMPLETED` and `putaway_completed_at`.
- [ ] F05-T009 Add `RECEIPT_PUTAWAY_COMPLETE` and `INVENTORY_UPDATE` audit.
- [ ] F05-T010 Add integration tests for putaway validation `400`, stale version `409`, invalid location/capacity/business rules `422`, and duplicate idempotency conflict `409`.
