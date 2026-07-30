# Tasks: Feature 06 Quarantine RTV

- [ ] F06-T001 Add tests for RTV create with pending adjustment and Debit Note.
- [ ] F06-T002 Add tests proving Quarantine stock is unchanged on RTV create.
- [ ] F06-T003 Add tests for no quarantine items and duplicate RTV.
- [ ] F06-T004 Add tests for exact RTV confirmation and quantity mismatch.
- [ ] F06-T005 Add tests for duplicate confirmation.
- [ ] F06-T006 Implement RTV creation for unresolved failed quantity only.
- [ ] F06-T007 Implement Debit Note creation once.
- [ ] F06-T008 Implement RTV confirmation reducing Quarantine exactly once.
- [ ] F06-T009 Add quarantine RTV audit actions.
- [ ] F06-T010 Add OpenAPI request bodies and integration tests for RTV create/confirm validation `400`, stale version `409`, no finalized quarantine `422`, duplicate `409`, and exact quantity mismatch `422`.
