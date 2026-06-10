# Báo cáo Kiểm thử — Feature: Cấu hình Hệ thống (System Config)

**Ngày thực hiện:** 2026-06-03  
**Nhánh:** `product-merge`  
**Người thực hiện:** Đặng Đức Dương  
**Kết quả tổng thể:** ✅ 39/39 PASSED

---

## 1. Phạm vi kiểm thử

| File test | Loại | Số test |
|-----------|------|---------|
| `SystemConfigServiceTest.java` | Unit Test (Mockito) | 27 |
| `SystemConfigControllerTest.java` | Integration Test (MockMvc) | 12 |

---

## 2. Thay đổi thực hiện trước khi test

### 2.1 Tạo mới `SystemConfigControllerTest.java`
- **Lý do:** File này chưa tồn tại — cần bổ sung để kiểm tra HTTP layer.
- **Nội dung:** 12 test case cho `GET /api/v1/admin/system-config` và `PUT /api/v1/admin/system-config/{configKey}`.
- **Import:** `@WebMvcTest`, `@Import(SecurityConfig, JwtAuthFilter, GlobalExceptionHandler)`, `@WithMockUser`.

### 2.2 Không thay đổi file hiện có
- `SystemConfigServiceTest.java` (27 tests) giữ nguyên, không chỉnh sửa.
- `SystemConfigServiceImpl.java`, `SystemConfigController.java` không thay đổi.

---

## 3. Kết quả chi tiết — SystemConfigServiceTest (Unit Test)

| # | Tên test | Mô tả | Kết quả |
|---|----------|-------|---------|
| 1 | `getAllConfigs_returnsAllConfigs` | Trả về danh sách 2 config đúng | ✅ PASS |
| 2 | `getAllConfigs_empty_returnsEmptyList` | Không có config → trả về list rỗng | ✅ PASS |
| 3 | `updateConfig_creditLimit_valid_savesAndLogsAudit` | DEFAULT_CREDIT_LIMIT hợp lệ → lưu + ghi audit log đúng old/new value | ✅ PASS |
| 4 | `updateConfig_creditLimit_zero_throws` | DEFAULT_CREDIT_LIMIT = 0 → 400 "must be > 0" | ✅ PASS |
| 5 | `updateConfig_creditLimit_negative_throws` | DEFAULT_CREDIT_LIMIT âm → 400 "must be > 0" | ✅ PASS |
| 6 | `updateConfig_paymentTermDays_valid` | DEFAULT_PAYMENT_TERM_DAYS = 30 → hợp lệ | ✅ PASS |
| 7 | `updateConfig_paymentTermDays_zero_throws` | DEFAULT_PAYMENT_TERM_DAYS = 0 → 400 | ✅ PASS |
| 8 | `updateConfig_creditHoldOverdueDays_valid` | CREDIT_HOLD_OVERDUE_DAYS = 15 → hợp lệ | ✅ PASS |
| 9 | `updateConfig_creditHoldOverdueDays_negative_throws` | CREDIT_HOLD_OVERDUE_DAYS âm → 400 | ✅ PASS |
| 10 | `updateConfig_creditUnlockBufferPct_valid` | CREDIT_UNLOCK_BUFFER_PCT = 0.8 → hợp lệ | ✅ PASS |
| 11 | `updateConfig_creditUnlockBufferPct_exactlyOne_valid` | CREDIT_UNLOCK_BUFFER_PCT = 1.0 → hợp lệ (boundary max) | ✅ PASS |
| 12 | `updateConfig_creditUnlockBufferPct_zero_throws` | CREDIT_UNLOCK_BUFFER_PCT = 0.0 → 400 "must be between (0, 1]" | ✅ PASS |
| 13 | `updateConfig_creditUnlockBufferPct_greaterThan1_throws` | CREDIT_UNLOCK_BUFFER_PCT = 1.5 → 400 | ✅ PASS |
| 14 | `updateConfig_monthlyClosingDay_valid` | MONTHLY_CLOSING_DAY = 25 → hợp lệ | ✅ PASS |
| 15 | `updateConfig_monthlyClosingDay_boundaryMin_valid` | MONTHLY_CLOSING_DAY = 1 → hợp lệ (boundary min) | ✅ PASS |
| 16 | `updateConfig_monthlyClosingDay_boundaryMax_valid` | MONTHLY_CLOSING_DAY = 31 → hợp lệ (boundary max) | ✅ PASS |
| 17 | `updateConfig_monthlyClosingDay_zero_throws` | MONTHLY_CLOSING_DAY = 0 → 400 "must be between 1 and 31" | ✅ PASS |
| 18 | `updateConfig_monthlyClosingDay_greaterThan31_throws` | MONTHLY_CLOSING_DAY = 32 → 400 | ✅ PASS |
| 19 | `updateConfig_minInventoryThreshold_valid` | MIN_INVENTORY_WARNING_THRESHOLD = 5 → hợp lệ | ✅ PASS |
| 20 | `updateConfig_minInventoryThreshold_zero_valid` | MIN_INVENTORY_WARNING_THRESHOLD = 0 → hợp lệ (>= 0) | ✅ PASS |
| 21 | `updateConfig_minInventoryThreshold_negative_throws` | MIN_INVENTORY_WARNING_THRESHOLD âm → 400 "must be >= 0" | ✅ PASS |
| 22 | `updateConfig_blankValue_throws` | Giá trị trắng (blank) → 400 "Value cannot be empty" | ✅ PASS |
| 23 | `updateConfig_notANumber_throws` | Giá trị không phải số → 400 "Invalid number format for key" | ✅ PASS |
| 24 | `updateConfig_configKeyNotFoundInDb_throws404` | configKey không tồn tại trong DB → 404 ResourceNotFoundException | ✅ PASS |
| 25 | `updateConfig_invalidEnumKey_throwsUnknown` | configKey không có trong enum → 400 "Unknown config key" | ✅ PASS |
| 26 | `updateConfig_adminUserNotFound_throws404` | Admin user không tồn tại → 404 "User not found" | ✅ PASS |
| 27 | `updateConfig_auditLog_hasCorrectEntityTypeAndAction` | Audit log ghi đúng entityType=SystemConfig, action=UPDATE, actorRole=ADMIN | ✅ PASS |

---

## 4. Kết quả chi tiết — SystemConfigControllerTest (Integration Test)

| # | Endpoint | Tên test | Mô tả | HTTP | Kết quả |
|---|----------|----------|-------|------|---------|
| 1 | `GET /admin/system-config` | `getAllConfigs_admin_returns200` | ADMIN truy cập → 200 + danh sách config đầy đủ | 200 | ✅ PASS |
| 2 | `GET /admin/system-config` | `getAllConfigs_ceo_returns403` | CEO truy cập → 403 | 403 | ✅ PASS |
| 3 | `GET /admin/system-config` | `getAllConfigs_storekeeper_returns403` | STOREKEEPER truy cập → 403 | 403 | ✅ PASS |
| 4 | `GET /admin/system-config` | `getAllConfigs_unauthenticated_returns403` | Không có token → 403 | 403 | ✅ PASS |
| 5 | `GET /admin/system-config` | `getAllConfigs_emptyList_returns200` | Danh sách rỗng khi chưa có config → 200 + [] | 200 | ✅ PASS |
| 6 | `PUT /admin/system-config/{key}` | `updateConfig_admin_validValue_returns200` | ADMIN cập nhật giá trị hợp lệ → 200 + response đúng | 200 | ✅ PASS |
| 7 | `PUT /admin/system-config/{key}` | `updateConfig_ceo_returns403` | CEO cập nhật → 403 | 403 | ✅ PASS |
| 8 | `PUT /admin/system-config/{key}` | `updateConfig_unauthenticated_returns403` | Không có token → 403 | 403 | ✅ PASS |
| 9 | `PUT /admin/system-config/{key}` | `updateConfig_invalidValue_returns400` | Giá trị âm → service throw IllegalArgumentException → 400 | 400 | ✅ PASS |
| 10 | `PUT /admin/system-config/{key}` | `updateConfig_keyNotFound_returns404` | configKey không tồn tại → 404 | 404 | ✅ PASS |
| 11 | `PUT /admin/system-config/{key}` | `updateConfig_nonNumericValue_returns400` | Format số không hợp lệ → 400 | 400 | ✅ PASS |
| 12 | `PUT /admin/system-config/MONTHLY_CLOSING_DAY` | `updateConfig_monthlyClosingDay_valid_returns200` | Cập nhật MONTHLY_CLOSING_DAY = 25 → 200 | 200 | ✅ PASS |

---

## 5. Lưu ý kỹ thuật

| Vấn đề | Giải thích |
|--------|-----------|
| Cả 2 endpoint đều trả 403 cho non-ADMIN | Nhờ `@PreAuthorize("hasRole('ADMIN')")` hoạt động đúng vì `SecurityConfig` đã có `@EnableMethodSecurity` (đã fix ở sprint trước). |
| `ResourceNotFoundException` phải có constructor 1 tham số | Constructor hiện tại chỉ nhận `String message`. Test dùng `new ResourceNotFoundException("...")` là đúng. |
| `GlobalExceptionHandler` map `IllegalArgumentException` → 400 | Đảm bảo validation errors từ service layer được trả về HTTP 400 đúng chuẩn. |
| Audit log ghi đủ thông tin | `entityType=SystemConfig`, `action=UPDATE`, `actorRole=ADMIN`, `oldValue` và `newValue` chứa giá trị trước/sau khi thay đổi. |

---

## 6. Tóm tắt

```
SystemConfigServiceTest    (Unit):       27 passed, 0 failed
SystemConfigControllerTest (MockMvc):    12 passed, 0 failed
──────────────────────────────────────────────────────────────
TOTAL (System Config):                   39 passed, 0 failed  ✅

TỔNG CỘNG (Auth + Audit Log + System Config): 122 passed, 0 failed  ✅
```
