# Implementation Tasks: System Config (US-WMS-01)

## Phase 1: Foundation (DTOs & Mappers)
- [ ] 1. Create `SystemConfigUpdateRequest` DTO to accept `configValue` (String). Use Validation annotations.
- [ ] 2. Create `SystemConfigResponse` DTO to return configured keys, values, and descriptions.
- [ ] 3. Create `SystemConfigMapper` (MapStruct or manual) to map `SystemConfig` Entity to `SystemConfigResponse`.

## Phase 2: Service Layer & Validation Logic
- [ ] 4. Define `SystemConfigService` interface with `updateConfig(String configKey, SystemConfigUpdateRequest request, Long adminUserId)` and `getAllConfigs()`.
- [ ] 5. Implement `SystemConfigServiceImpl`:
  - Inject `SystemConfigRepository` and `AuditLogService`.
  - Fetch `SystemConfig` by `configKey`. Throw `ResourceNotFoundException` if not found.
  - Implement validation logic using switch-case for the 6 keys according to spec requirements (Parse numbers, check bounds). Throw `IllegalArgumentException` or `ValidationException` on failure.
  - Call `auditLogService.logAction(ActionType.UPDATE, ...)` to record `old_value` and `new_value`.
  - Update and save the entity.

## Phase 3: Controller Layer
- [ ] 6. Create `SystemConfigController` with `@RestController` and `@RequestMapping("/api/v1/admin/system-config")`.
- [ ] 7. Add `GET /` endpoint to retrieve all configurations.
- [ ] 8. Add `PUT /{config_key}` endpoint to update a configuration.
- [ ] 9. Secure the endpoints with `@PreAuthorize("hasRole('ADMIN')")`.

## Phase 4: Testing & Quality Assurance
- [ ] 10. Write Unit Tests for `SystemConfigServiceImpl` (`SystemConfigServiceTest.java`) ensuring all 6 validation rules (both valid and invalid bounds) and audit logging are strictly verified.
- [ ] 11. Run `mvn clean compile test` to ensure 80% coverage and no compilation issues.
