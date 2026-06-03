# Báo cáo Kiểm thử — Feature: Nhật ký Hoạt động Hệ thống (Audit Log)

**Ngày thực hiện:** 2026-06-03  
**Nhánh:** `product-merge`  
**Người thực hiện:** Đặng Đức Dương  
**Kết quả tổng thể:** ✅ 48/48 PASSED

---

## 1. Phạm vi kiểm thử

| File test | Loại | Số test |
|-----------|------|---------|
| `AuditLogServiceTest.java` | Unit Test (Mockito) | 18 |
| `AuditLogControllerTest.java` | Integration Test (MockMvc) | 13 |
| `AuditLogUtilTest.java` | Unit Test (pure) | 17 |

---

## 2. Thay đổi thực hiện trước khi test

### 2.1 Sửa lỗi `SecurityConfig` — thiếu `@EnableMethodSecurity`
- **Vấn đề:** `SecurityConfig` có `@EnableWebSecurity` nhưng thiếu `@EnableMethodSecurity`, khiến annotation `@PreAuthorize("hasRole('ADMIN')")` trên controller không có tác dụng. Non-admin users không bị chặn ở layer method security.
- **Fix:** Thêm `@EnableMethodSecurity` và import tương ứng vào `SecurityConfig.java`.
- **Ảnh hưởng:** CEO, STOREKEEPER, và các role khác giờ bị từ chối đúng với 403 khi gọi endpoint audit log.

---

## 3. Kết quả chi tiết — AuditLogServiceTest (Unit Test)

| # | Tên test | Mô tả | Kết quả |
|---|----------|-------|---------|
| 1 | `log_withExplicitActor_savesAllFields` | Ghi log với actor rõ ràng → lưu đầy đủ actor, role, action, entityType, entityId, description, warehouseId, IP | ✅ PASS |
| 2 | `log_withSecurityContext_resolvesActorFromContext` | Ghi log không truyền actor → tự lấy từ SecurityContext | ✅ PASS |
| 3 | `log_nullActor_throwsIllegalState` | Actor null → throw `IllegalStateException` | ✅ PASS |
| 4 | `log_sensitiveFields_areExcluded` | `passwordHash` bị xóa khỏi oldValue và newValue trước khi lưu | ✅ PASS |
| 5 | `log_tokenFields_areExcluded` | `accessToken`, `refreshToken`, `token` bị xóa khỏi log | ✅ PASS |
| 6 | `log_xForwardedFor_usesFirstIp` | IP lấy từ `X-Forwarded-For` header (phần tử đầu tiên khi qua proxy) | ✅ PASS |
| 7 | `getAuditLogs_noParams_returnsDefaultPage1Size30` | Không truyền params → mặc định trang 1, pageSize 30 | ✅ PASS |
| 8 | `getAuditLogs_pageSizeOver30_isCappedAt30` | pageSize > 30 → bị cap về 30 | ✅ PASS |
| 9 | `getAuditLogs_invalidPage_defaultsToPage1` | page null hoặc < 1 → mặc định trang 1 | ✅ PASS |
| 10 | `getAuditLogs_page51NoFilter_throwsQueryRangeTooLarge` | page > 50 không có filter → 400 `QUERY_RANGE_TOO_LARGE` | ✅ PASS |
| 11 | `getAuditLogs_page51WithFilter_allowed` | page > 50 có filter time → được phép (không throw) | ✅ PASS |
| 12 | `getAuditLogs_page51WithWarehouseFilter_allowed` | page > 50 có filter warehouseId → được phép | ✅ PASS |
| 13 | `getAuditLogs_fromAfterTo_throwsInvalidDateRange` | `from` > `to` → 400 `INVALID_DATE_RANGE` | ✅ PASS |
| 14 | `getAuditLogs_invalidDateFormat_throwsInvalidDateRange` | Định dạng ngày không hợp lệ → 400 `INVALID_DATE_RANGE` | ✅ PASS |
| 15 | `getAuditLogs_withDateFilter_returnsResults` | Filter theo ngày hợp lệ → trả kết quả đúng | ✅ PASS |
| 16 | `getAuditLogById_found_returnsDetail` | Lấy chi tiết log theo ID → trả đầy đủ actorName, action, entityType | ✅ PASS |
| 17 | `getAuditLogById_notFound_throws404` | ID không tồn tại → 404 `AUDIT_LOG_NOT_FOUND` | ✅ PASS |
| 18 | `getAuditLogById_returnsOldAndNewValues` | Chi tiết log có oldValue và newValue đúng | ✅ PASS |

---

## 4. Kết quả chi tiết — AuditLogControllerTest (Integration Test)

| # | Endpoint | Tên test | Mô tả | HTTP | Kết quả |
|---|----------|----------|-------|------|---------|
| 1 | `GET /audit-logs` | `getAuditLogs_admin_returns200` | ADMIN truy cập → 200 + data + page + pageSize đúng | 200 | ✅ PASS |
| 2 | `GET /audit-logs` | `getAuditLogs_ceo_returns403` | CEO truy cập → 403 | 403 | ✅ PASS |
| 3 | `GET /audit-logs` | `getAuditLogs_storekeeper_returns403` | STOREKEEPER truy cập → 403 | 403 | ✅ PASS |
| 4 | `GET /audit-logs` | `getAuditLogs_unauthenticated_returns403` | Không có token → 403 | 403 | ✅ PASS |
| 5 | `GET /audit-logs` | `getAuditLogs_page51NoFilter_returns400` | ADMIN, page > 50, không filter → 400 | 400 | ✅ PASS |
| 6 | `GET /audit-logs` | `getAuditLogs_fromAfterTo_returns400` | from > to → 400 | 400 | ✅ PASS |
| 7 | `GET /audit-logs` | `getAuditLogs_withFilters_returns200` | ADMIN với filter from/to/warehouseId → 200 | 200 | ✅ PASS |
| 8 | `GET /audit-logs` | `getAuditLogs_hasNextTrue_returnedCorrectly` | hasNext/hasPrevious đúng trong response | 200 | ✅ PASS |
| 9 | `GET /audit-logs` | `getAuditLogs_listItemHasAllFields` | List item có đủ actorName, actorRole, description, entityId | 200 | ✅ PASS |
| 10 | `GET /audit-logs/{id}` | `getAuditLogById_admin_returns200WithDetail` | ADMIN xem chi tiết → 200 + oldValue + newValue | 200 | ✅ PASS |
| 11 | `GET /audit-logs/{id}` | `getAuditLogById_notFound_returns404` | ID không tồn tại → 404 | 404 | ✅ PASS |
| 12 | `GET /audit-logs/{id}` | `getAuditLogById_ceo_returns403` | CEO xem chi tiết → 403 | 403 | ✅ PASS |
| 13 | `GET /audit-logs/{id}` | `getAuditLogById_unauthenticated_returns403` | Không có token → 403 | 403 | ✅ PASS |

---

## 5. Kết quả chi tiết — AuditLogUtilTest (Unit Test)

| # | Tên test | Mô tả | Kết quả |
|---|----------|-------|---------|
| 1 | `filterSensitiveFields_removesPasswordHash` | `passwordHash` bị xóa, `email` giữ lại | ✅ PASS |
| 2 | `filterSensitiveFields_removesAllSensitiveKeys` | Xóa: `password`, `password_hash`, `accessToken`, `refreshToken`, `token` | ✅ PASS |
| 3 | `filterSensitiveFields_nullInput_returnsNull` | null → null | ✅ PASS |
| 4 | `filterSensitiveFields_emptyMap_returnsEmpty` | Map rỗng → rỗng | ✅ PASS |
| 5 | `filterSensitiveFields_noSensitiveFields_returnsUnchanged` | Map không có sensitive field → nguyên vẹn | ✅ PASS |
| 6 | `buildDiff_oldNull_returnsFullNewValues` | oldValues null (CREATE) → old=null, new=newValues đầy đủ | ✅ PASS |
| 7 | `buildDiff_returnsOnlyChangedFields` | Chỉ trả về field đã thay đổi, bỏ field giữ nguyên | ✅ PASS |
| 8 | `buildDiff_removesSensitiveFieldsFromDiff` | Diff kết quả không chứa sensitive fields | ✅ PASS |
| 9 | `buildDiff_noChanges_returnsEmptyDiff` | Không có thay đổi → diff rỗng cho cả 2 | ✅ PASS |
| 10 | `toJson_serializesMapCorrectly` | Map → JSON string đúng | ✅ PASS |
| 11 | `toJson_nullInput_returnsNull` | null → null | ✅ PASS |
| 12 | `fromJson_deserializesCorrectly` | JSON string → Map đúng | ✅ PASS |
| 13 | `fromJson_nullInput_returnsNull` | null → null | ✅ PASS |
| 14 | `fromJson_blankInput_returnsNull` | blank string → null | ✅ PASS |
| 15 | `generateDescription_correctFormat` | `STATUS_CHANGE Receipt R001` đúng format | ✅ PASS |
| 16 | `generateDescription_loginAction` | `LOGIN User admin@wms.com` đúng | ✅ PASS |
| 17 | `generateDescription_createAction` | `CREATE Product P001` đúng | ✅ PASS |

---

## 6. Lưu ý kỹ thuật

| Vấn đề | Giải thích |
|--------|-----------|
| `@PreAuthorize` không hoạt động nếu thiếu `@EnableMethodSecurity` | Đây là lỗi cấu hình thực tế — CEO có thể đã truy cập được audit log trước khi fix. Đã thêm `@EnableMethodSecurity` vào `SecurityConfig`. |
| Tất cả endpoint audit log trả `403` khi không có token | Behavior đúng của `STATELESS` mode, nhất quán với auth feature. |
| `filterSensitiveFields` không lọc được token nếu key khác tên | Các key hiện tại được filter: `passwordHash`, `password_hash`, `password`, `accessToken`, `refreshToken`, `token`. Cần bổ sung nếu dùng key tên khác. |

---

## 7. Tóm tắt

```
AuditLogServiceTest  (Unit):       18 passed, 0 failed
AuditLogControllerTest (MockMvc):  13 passed, 0 failed
AuditLogUtilTest     (Unit):       17 passed, 0 failed
─────────────────────────────────────────────────────
TOTAL (Audit Log):                 48 passed, 0 failed  ✅

TỔNG CỘNG (Auth + Audit Log):     83 passed, 0 failed  ✅
```
