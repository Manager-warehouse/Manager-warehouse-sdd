# Feature Checklist: System Config

## Functionality
- [x] API `GET /api/v1/admin/system-config` returns a list of all 6 configurations.
- [x] API `PUT /api/v1/admin/system-config/{config_key}` successfully updates a single configuration.
- [x] `DEFAULT_CREDIT_LIMIT` correctly rejects zero and negative numbers.
- [x] `DEFAULT_PAYMENT_TERM_DAYS` correctly rejects zero and negative decimals/integers.
- [x] `CREDIT_HOLD_OVERDUE_DAYS` correctly rejects zero and negative decimals/integers.
- [x] `CREDIT_UNLOCK_BUFFER_PCT` correctly rejects `< 0` and `> 1`.
- [x] `MONTHLY_CLOSING_DAY` correctly rejects `< 1` and `> 31`.
- [x] `MIN_INVENTORY_WARNING_THRESHOLD` correctly rejects negative numbers.
- [x] `ResourceNotFoundException` (404) is thrown for an invalid `config_key`.
- [x] `IllegalArgumentException` (400) is thrown for invalid values.

## Security & Auth
- [x] Endpoints restrict access exclusively to `ADMIN` role (403 Forbidden for others).

## Observability & Audit
- [x] An Audit Log entry is created containing `previous_value`, `new_value`, and `actor_id` upon successful update.

## Code Quality
- [x] Service layer unit test coverage is strictly > 80%.
- [x] No `System.out` or `console.log` present in production code.
- [x] Swagger/OpenAPI documentation is properly populated for the new endpoints.
