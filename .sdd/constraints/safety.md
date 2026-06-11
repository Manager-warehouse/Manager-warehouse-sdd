# Safety Constraints — WMS Phúc Anh

> Ràng buộc an toàn bắt buộc. Vi phạm = reject ngay lập tức.
> Nguồn: AGENTS.md · constitution.md · AGENTS.md forbidden patterns

## 1. Secrets & Credentials

| # | Rule | Mức độ |
|---|---|---|
| SEC-01 | KHÔNG lưu password, API keys, JWT secrets trong source code | MUST |
| SEC-02 | KHÔNG commit file `.env` chứa secrets | MUST |
| SEC-03 | KHÔNG log password, tokens, hay thông tin nhạy cảm | MUST |
| SEC-04 | bcrypt cost factor ≥ 12 cho mọi password | MUST |
| SEC-05 | JWT secret phải đủ mạnh (≥ 256-bit random) | MUST |
| SEC-06 | Database connection string không chứa credentials trong source code | MUST |

## 2. Inventory Safety

| # | Rule | Mức độ |
|---|---|---|
| INV-SAFE-01 | KHÔNG cho phép tồn kho âm dưới mọi hình thức | MUST |
| INV-SAFE-02 | DB constraints for `total_qty >= 0`, `reserved_qty >= 0`, and `total_qty - reserved_qty >= 0` + application validation | MUST |
| INV-SAFE-03 | KHÔNG UPDATE inventory trực tiếp — chỉ qua các flow chuẩn | MUST |
| INV-SAFE-04 | Optimistic locking (`@Version`) bắt buộc trên bảng inventories | MUST |
| INV-SAFE-05 | Version conflict → HTTP 409 — không im lặng ghi đè | MUST |

## 3. QC Safety

| # | Rule | Mức độ |
|---|---|---|
| QC-SAFE-01 | KHÔNG bỏ qua QC check trước khi nhập kho | MUST |
| QC-SAFE-02 | Hàng fail QC → bắt buộc vào Quarantine | MUST |
| QC-SAFE-03 | Hàng Quarantine KHÔNG được tính vào available inventory | MUST |
| QC-SAFE-04 | Hàng Quarantine KHÔNG được xuất bán | MUST |

## 4. Data Integrity

| # | Rule | Mức độ |
|---|---|---|
| DAT-01 | KHÔNG xóa vĩnh viễn dữ liệu nghiệp vụ | MUST |
| DAT-02 | Master data → soft-delete bằng `is_active = false` | MUST |
| DAT-03 | Transaction data → cancel bằng `status = CANCELLED` | MUST |
| DAT-04 | KHÔNG làm mất foreign key integrity | MUST |
| DAT-05 | KHÔNG sửa/xóa dữ liệu kỳ kế toán đã chốt | MUST |

## 5. Input Validation

| # | Rule | Mức độ |
|---|---|---|
| VAL-01 | Jakarta Validation annotations trên mọi POST/PUT DTO | MUST |
| VAL-02 | Kiểm tra số dương cho quantity, price, limit | MUST |
| VAL-03 | Sanitize input — chống XSS, SQL injection | MUST |
| VAL-04 | Validate enum values — reject giá trị không hợp lệ | MUST |

## 6. Audit & Traceability

| # | Rule | Mức độ |
|---|---|---|
| AUD-SAFE-01 | KHÔNG bỏ qua audit logging cho thao tác kho | MUST |
| AUD-SAFE-02 | Audit log là bất biến — không sửa/xóa sau khi ghi | MUST |
| AUD-SAFE-03 | Mọi thay đổi phân quyền đều ghi audit | MUST |

## 7. Concurrency Safety

| # | Rule | Mức độ |
|---|---|---|
| CON-01 | `@Transactional` trên mọi service method ghi | MUST |
| CON-02 | `@Version` trên inventory + batch | MUST |
| CON-03 | Reserved quantity check trước release | MUST |
| CON-04 | Không để race condition trên inventory updates | MUST |

## 8. Code Safety

| # | Rule | Mức độ |
|---|---|---|
| CODE-SAFE-01 | KHÔNG `System.out.println` trong production code | MUST |
| CODE-SAFE-02 | KHÔNG `console.log` trong frontend production code | MUST |
| CODE-SAFE-03 | KHÔNG TODO comments trong completed task | MUST |
| CODE-SAFE-04 | KHÔNG hardcode warehouse IDs, role assumptions | MUST |
| CODE-SAFE-05 | KHÔNG commit trực tiếp vào `main` / `production` | MUST |
| CODE-SAFE-06 | KHÔNG dùng field injection — ưu tiên constructor injection | MUST |

## 9. API Safety

| # | Rule | Mức độ |
|---|---|---|
| API-SAFE-01 | Mọi endpoint (trừ auth) MUST check JWT | MUST |
| API-SAFE-02 | Mọi write endpoint MUST check quyền (role + warehouse scope) | MUST |
| API-SAFE-03 | Error response KHÔNG leak stack trace ra client | MUST |
| API-SAFE-04 | Rate limiting trên auth endpoints | SHOULD |

## 10. Financial Safety

| # | Rule | Mức độ |
|---|---|---|
| FIN-01 | KHÔNG cho phép xuất hàng khi Dealer đang CREDIT_HOLD | MUST |
| FIN-02 | KHÔNG cho phép chốt sổ khi còn chứng từ tồn đọng | MUST |
| FIN-03 | Mọi adjustment > 5M phải có approval | MUST |
| FIN-04 | COGS không thể âm | MUST |
