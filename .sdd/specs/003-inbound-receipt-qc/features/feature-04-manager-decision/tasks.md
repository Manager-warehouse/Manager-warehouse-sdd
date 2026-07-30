# Tasks: Feature 04 Manager Decision

- [ ] F04-T001 Add/verify `PARTIALLY_APPROVED` receipt status.
- [ ] F04-T002 Add tests for full approval from `QC_COMPLETED`.
- [ ] F04-T003 Add tests for partial approval from `QC_FAILED`.
- [ ] F04-T004 Add tests for no passed quantity, approved quantity mismatch, and missing unit cost.
- [ ] F04-T005 Add tests for rejection with and without reason.
- [ ] F04-T006 Implement full approval without regular inventory mutation.
- [ ] F04-T007 Implement partial approval, `approved_qty`, and finalization of failed quantity from Quarantine readiness into Quarantine stock.
- [ ] F04-T008 Implement whole receipt rejection with reason.
- [ ] F04-T009 Implement supplier handover confirmation.
- [ ] F04-T010 Add manager decision audit actions.
- [ ] F04-T011 Add approve request body with item unit costs and integration tests for missing unit cost `400`/`422`, stale version `409`, warehouse scope `403`, and invalid decision state `422`.
