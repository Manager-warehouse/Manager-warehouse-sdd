# Data Model: Admin Role Hierarchy & Warehouse Isolation

Không yêu cầu thay đổi cấu trúc bảng (schema database) hoặc tạo thêm Migration File. Các cấu trúc bảng hiện tại đáp ứng đầy đủ yêu cầu nghiệp vụ thông qua các ràng buộc logic ở tầng ứng dụng (Application Level).

---

## 1. Schema References

### Bảng `users` (Thông tin tài khoản)
Bảng này lưu trữ thông tin cơ bản và vai trò của người dùng.
* **role**: `VARCHAR(50)`, CHECK `role IN ('ADMIN', 'CEO', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'PLANNER', 'DISPATCHER', 'DRIVER')`.

### Bảng `user_warehouse_assignments` (Gán kho người dùng)
Bảng trung gian thiết lập quan hệ Nhiều-Nhiều giữa người dùng và kho.
* **user_id**: `BIGINT (FK)` liên kết đến `users(id)`, `NOT NULL`.
* **warehouse_id**: `BIGINT (FK)` liên kết đến `warehouses(id)`, `NOT NULL`.
* **Ràng buộc duy nhất**: `UNIQUE(user_id, warehouse_id)`.

---

## 2. Logic Constraints & Data Validation

### Ràng buộc về số lượng bản ghi gán kho (Warehouse Assignment Count)
| Vai trò (User Role) | Số bản ghi tối thiểu | Số bản ghi tối đa | Cách lưu trữ trong DB |
| :--- | :---: | :---: | :--- |
| **ADMIN** | 0 | 0 | Không lưu trữ bản ghi nào. Logic code tự động cho phép truy cập tất cả các kho. |
| **CEO** | 0 | 0 | Không lưu trữ bản ghi nào. Logic code tự động cho phép truy cập tất cả các kho. |
| **Mọi vai trò khác** | 1 | 1 | Lưu chính xác 1 bản ghi liên kết đến kho hoạt động của user. |

---

## 3. Migration Plan
* **Flyway**: Không cần tạo file migration mới.
* **Seed Data**: Đảm bảo dữ liệu mẫu (nếu có) tuân thủ đúng quy tắc: các tài khoản demo (Thủ kho, Kế toán...) chỉ được gán duy nhất 1 kho, tài khoản admin/ceo demo không gán kho trong bảng `user_warehouse_assignments`.
