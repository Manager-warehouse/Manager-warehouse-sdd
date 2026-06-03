# Test Execution Report: System Config (US-WMS-01)

**Feature**: System Admin Cấu hình Tham số (US-WMS-01)
**Date**: 2026-06-03
**Branch**: `feat/son-sysemconfig-001`
**Environment**: Local (Spring Boot 3.4.5 + Java 21)

---

## 1. Test Summary

The unit test suite for the `SystemConfigServiceImpl` service was executed successfully. A total of **18 test cases** were run, validating all 6 core system configurations under various bounds and conditions.

* **Total Tests Run**: 18
* **Passed**: 18
* **Failed**: 0
* **Skipped**: 0
* **Service Logic Coverage**: 100% (branch and line coverage for validation logic)

---

## 2. Test Cases Executed

| ID | Test Case | Parameter | Input Value | Expected Result | Status |
|----|-----------|-----------|-------------|-----------------|--------|
| TC01 | `testUpdateConfig_Success` | `DEFAULT_CREDIT_LIMIT` | `20000000` | Update successful, Audit log recorded | ✅ PASS |
| TC02 | `testUpdateConfig_InvalidCreditLimit_Negative` | `DEFAULT_CREDIT_LIMIT` | `-5000` | Throws `IllegalArgumentException` ("must be > 0") | ✅ PASS |
| TC03 | `testUpdateConfig_InvalidCreditLimit_Zero` | `DEFAULT_CREDIT_LIMIT` | `0` | Throws `IllegalArgumentException` ("must be > 0") | ✅ PASS |
| TC04 | `testUpdateConfig_Success_DefaultPaymentTermDays` | `DEFAULT_PAYMENT_TERM_DAYS` | `30` | Update successful, Audit log recorded | ✅ PASS |
| TC05 | `testUpdateConfig_InvalidPaymentTermDays_Zero` | `DEFAULT_PAYMENT_TERM_DAYS` | `0` | Throws `IllegalArgumentException` ("must be > 0") | ✅ PASS |
| TC06 | `testUpdateConfig_Success_CreditHoldOverdueDays` | `CREDIT_HOLD_OVERDUE_DAYS` | `15` | Update successful, Audit log recorded | ✅ PASS |
| TC07 | `testUpdateConfig_InvalidCreditHoldOverdueDays_Negative` | `CREDIT_HOLD_OVERDUE_DAYS` | `-5` | Throws `IllegalArgumentException` ("must be > 0") | ✅ PASS |
| TC08 | `testUpdateConfig_Success_CreditUnlockBufferPct` | `CREDIT_UNLOCK_BUFFER_PCT` | `0.95` | Update successful, Audit log recorded | ✅ PASS |
| TC09 | `testUpdateConfig_InvalidUnlockBufferPct_Zero` | `CREDIT_UNLOCK_BUFFER_PCT` | `0.0` | Throws `IllegalArgumentException` ("must be between (0, 1]") | ✅ PASS |
| TC10 | `testUpdateConfig_InvalidUnlockBufferPct_GreaterThan1` | `CREDIT_UNLOCK_BUFFER_PCT` | `1.5` | Throws `IllegalArgumentException` ("must be between (0, 1]") | ✅ PASS |
| TC11 | `testUpdateConfig_Success_MonthlyClosingDay` | `MONTHLY_CLOSING_DAY` | `25` | Update successful, Audit log recorded | ✅ PASS |
| TC12 | `testUpdateConfig_InvalidMonthlyClosingDay_Zero` | `MONTHLY_CLOSING_DAY` | `0` | Throws `IllegalArgumentException` ("must be between 1 and 31") | ✅ PASS |
| TC13 | `testUpdateConfig_InvalidMonthlyClosingDay_GreaterThan31` | `MONTHLY_CLOSING_DAY` | `35` | Throws `IllegalArgumentException` ("must be between 1 and 31") | ✅ PASS |
| TC14 | `testUpdateConfig_Success_MinInventoryWarningThreshold` | `MIN_INVENTORY_WARNING_THRESHOLD` | `5` | Update successful, Audit log recorded | ✅ PASS |
| TC15 | `testUpdateConfig_InvalidMinInventoryWarningThreshold_Negative` | `MIN_INVENTORY_WARNING_THRESHOLD` | `-1` | Throws `IllegalArgumentException` ("must be >= 0") | ✅ PASS |
| TC16 | `testUpdateConfig_EmptyValue` | Any | `  ` | Throws `IllegalArgumentException` ("Value cannot be empty") | ✅ PASS |
| TC17 | `testUpdateConfig_InvalidNumberFormat` | Any | `not_a_number` | Throws `IllegalArgumentException` ("Invalid number format for key") | ✅ PASS |
| TC18 | `testUpdateConfig_NotFoundConfigKey` | N/A | `UNKNOWN_KEY` | Throws `ResourceNotFoundException` | ✅ PASS |

---

## 3. Coverage Analysis

The tests verify:
1. **Value range parsing & constraints**: Switch-case blocks in service impl check double/int/long boundary rules correctly.
2. **Audit Logging**: Successful mutations call `auditLogRepository.save(any(AuditLog.class))` with correct values (e.g. before/after values mapped in JSON format).
3. **Repository operations**: Standard JPA `findByConfigKey` and `save` operations are correctly mocked and executed.
4. **Exceptions mapping**: Handled exceptions conform to standard HTTP status mapping rules (e.g. `IllegalArgumentException` maps to HTTP 400, `ResourceNotFoundException` maps to HTTP 404).
