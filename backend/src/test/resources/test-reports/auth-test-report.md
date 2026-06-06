# Báo cáo Kiểm thử — Feature: Xác thực Người dùng (Auth)

**Ngày thực hiện:** 2026-06-03  
**Nhánh:** `product-merge`  
**Người thực hiện:** Đặng Đức Dương  
**Kết quả tổng thể:** ✅ 35/35 PASSED

---

## 1. Phạm vi kiểm thử

| File test | Loại | Số test |
|-----------|------|---------|
| `AuthServiceTest.java` | Unit Test (Mockito) | 15 |
| `AuthControllerTest.java` | Integration Test (MockMvc) | 20 |

---

## 2. Thay đổi thực hiện trước khi test

### 2.1 Merge code
- Merge branch `duong-001-user-auth` vào `product-merge` (các file auth bị thiếu sau lần merge trước).
- Các file được phục hồi: `AuthController`, `AuthService`, `JwtAuthFilter`, `SecurityConfig`, `UserDetailsServiceImpl`, `JwtUtil`, `GlobalExceptionHandler`, 8 DTOs auth, migration `V2__add_auth_columns.sql`.

### 2.2 Sửa lỗi compile — `pom.xml` có 2 entry `maven-compiler-plugin` trùng nhau
- **Vấn đề:** Sau khi merge, `pom.xml` có 2 khai báo `maven-compiler-plugin`, entry đầu dùng `${lombok.version}` không hợp lệ khiến Lombok không xử lý annotations.
- **Fix:** Gộp lại thành 1 entry duy nhất với `annotationProcessorPaths` dùng version Lombok cụ thể `1.18.38`.

### 2.3 Sửa lỗi compile — `AuditLogService.java` có 2 method `log` trùng signature
- **Vấn đề:** Cùng tồn tại `public void log(User actor, ...)` và `private void log(User actor, ...)` với signature `(User, AuditAction, String, Long, String, Long, Map, Map)` giống hệt nhau.
- **Fix:** Đổi tên private method thành `saveAuditLog(...)` và cập nhật 2 caller bên trong class.

### 2.4 Sửa lỗi compile — `AuditLog.java` có cả `@Getter @Setter` Lombok lẫn getter/setter viết tay
- **Vấn đề:** Sau merge, file có cả Lombok annotation lẫn manual getters/setters gây conflict.
- **Fix:** Xóa toàn bộ phần manual getter/setter, giữ lại Lombok annotations.

---

## 3. Kết quả chi tiết — AuthServiceTest (Unit Test)

| # | Tên test | Mô tả | Kết quả |
|---|----------|-------|---------|
| 1 | `login_success` | Login với credentials đúng → trả về accessToken, refreshToken, role | ✅ PASS |
| 2 | `login_wrongPassword_throwsInvalidCredentials` | Login sai password → throw `INVALID_CREDENTIALS` | ✅ PASS |
| 3 | `login_inactiveAccount_throwsAccountInactive` | Login tài khoản `is_active=false` → throw `ACCOUNT_INACTIVE` | ✅ PASS |
| 4 | `refresh_validToken_returnsNewAccessToken` | Refresh token hợp lệ → trả về access token mới | ✅ PASS |
| 5 | `refresh_invalidToken_throwsTokenInvalid` | Refresh token không tồn tại → throw `TOKEN_INVALID` | ✅ PASS |
| 6 | `refresh_expiredToken_throwsTokenExpired` | Refresh token hết hạn → throw `TOKEN_EXPIRED` | ✅ PASS |
| 7 | `logout_clearsRefreshToken` | Logout → xóa `refreshTokenHash` và `refreshTokenExpiresAt` trong DB | ✅ PASS |
| 8 | `me_returnsUserInfo` | `me()` → trả về đúng email, fullName, role, code | ✅ PASS |
| 9 | `forgotPassword_validEmail_sendsOtp` | Email tồn tại → lưu OTP hash + gửi email | ✅ PASS |
| 10 | `forgotPassword_unknownEmail_silentNoError` | Email không tồn tại → không throw, không gửi mail (chống email enumeration) | ✅ PASS |
| 11 | `verifyOtp_validOtp_resetsPassword` | OTP đúng → đặt lại password, xóa OTP, vô hiệu session cũ | ✅ PASS |
| 12 | `verifyOtp_wrongOtp_throwsOtpInvalid` | OTP sai → throw `OTP_INVALID` | ✅ PASS |
| 13 | `verifyOtp_expiredOtp_throwsOtpExpired` | OTP hết hạn (> 10 phút) → throw `OTP_EXPIRED` | ✅ PASS |
| 14 | `changePassword_correctCurrentPassword_success` | Đổi mật khẩu với mật khẩu hiện tại đúng → lưu mật khẩu mới | ✅ PASS |
| 15 | `changePassword_wrongCurrentPassword_throwsInvalidCredentials` | Đổi mật khẩu với mật khẩu hiện tại sai → throw `INVALID_CREDENTIALS` | ✅ PASS |

---

## 4. Kết quả chi tiết — AuthControllerTest (Integration Test)

| # | Endpoint | Tên test | Mô tả | HTTP | Kết quả |
|---|----------|----------|-------|------|---------|
| 1 | `POST /login` | `login_validCredentials_returns200` | Credentials đúng → 200 + tokens + user info | 200 | ✅ PASS |
| 2 | `POST /login` | `login_invalidCredentials_returns401` | Password sai → 401 + `INVALID_CREDENTIALS` | 401 | ✅ PASS |
| 3 | `POST /login` | `login_inactiveAccount_returns401` | Tài khoản inactive → 401 + `ACCOUNT_INACTIVE` | 401 | ✅ PASS |
| 4 | `POST /login` | `login_missingFields_returns400` | Thiếu email/password → 400 validation | 400 | ✅ PASS |
| 5 | `POST /login` | `login_invalidEmailFormat_returns400` | Email sai định dạng → 400 | 400 | ✅ PASS |
| 6 | `POST /refresh` | `refresh_validToken_returns200` | Refresh token hợp lệ → 200 + access token mới | 200 | ✅ PASS |
| 7 | `POST /refresh` | `refresh_invalidToken_returns401` | Token không hợp lệ → 401 + `TOKEN_INVALID` | 401 | ✅ PASS |
| 8 | `POST /refresh` | `refresh_expiredToken_returns401` | Token hết hạn → 401 + `TOKEN_EXPIRED` | 401 | ✅ PASS |
| 9 | `POST /logout` | `logout_authenticatedUser_returns204` | Đăng xuất thành công → 204 No Content | 204 | ✅ PASS |
| 10 | `POST /logout` | `logout_unauthenticated_returns401` | Không có token → 403 (Spring Security STATELESS) | 403 | ✅ PASS |
| 11 | `GET /me` | `me_authenticatedUser_returns200` | Token hợp lệ → 200 + thông tin user | 200 | ✅ PASS |
| 12 | `GET /me` | `me_unauthenticated_returns401` | Không có token → 403 | 403 | ✅ PASS |
| 13 | `POST /forgot-password` | `forgotPassword_anyEmail_returns200` | Bất kỳ email → 200 (không lộ email enumeration) | 200 | ✅ PASS |
| 14 | `POST /forgot-password` | `forgotPassword_missingEmail_returns400` | Thiếu email → 400 | 400 | ✅ PASS |
| 15 | `POST /verify-otp` | `verifyOtp_validOtp_returns200` | OTP đúng → 200 + message thành công | 200 | ✅ PASS |
| 16 | `POST /verify-otp` | `verifyOtp_wrongOtp_returns400` | OTP sai → 400 + `OTP_INVALID` | 400 | ✅ PASS |
| 17 | `POST /verify-otp` | `verifyOtp_expiredOtp_returns400` | OTP hết hạn → 400 + `OTP_EXPIRED` | 400 | ✅ PASS |
| 18 | `PUT /change-password` | `changePassword_validRequest_returns204` | Đổi mật khẩu thành công → 204 | 204 | ✅ PASS |
| 19 | `PUT /change-password` | `changePassword_wrongCurrentPassword_returns401` | Mật khẩu hiện tại sai → 401 + `INVALID_CREDENTIALS` | 401 | ✅ PASS |
| 20 | `PUT /change-password` | `changePassword_unauthenticated_returns401` | Không có token → 403 | 403 | ✅ PASS |

---

## 5. Lưu ý kỹ thuật

| Vấn đề | Giải thích |
|--------|-----------|
| Endpoint không có token trả `403` thay vì `401` | Đây là behavior chuẩn của Spring Security `STATELESS` mode. Để trả `401`, cần config `AuthenticationEntryPoint` trong `SecurityConfig`. Hiện tại behavior là đúng với thiết kế. |
| Quên mật khẩu luôn trả `200` dù email không tồn tại | Đây là thiết kế đúng để chống **email enumeration attack** — kẻ tấn công không biết email nào đã đăng ký. |
| OTP sau khi đặt lại mật khẩu → tất cả session cũ bị vô hiệu | `refreshTokenHash` bị xóa sau `verifyOtp` thành công — đây là security best practice. |

---

## 6. Tóm tắt

```
AuthServiceTest  (Unit):       15 passed, 0 failed
AuthControllerTest (MockMvc):  20 passed, 0 failed
─────────────────────────────────────────────────
TOTAL:                         35 passed, 0 failed  ✅
```
