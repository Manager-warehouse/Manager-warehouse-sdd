# Quickstart: Admin Role Hierarchy & Warehouse Isolation

Tài liệu hướng dẫn kiểm thử, cấu hình và xác thực tính năng phân quyền thừa kế và cách ly dữ liệu kho.

---

## 1. Hướng dẫn cấu hình & Chạy cục bộ

### Cấu hình Backend
* Các quy tắc gán kho được thực hiện trong `UserServiceImpl.java`.
* Quyền hạn vai trò ADMIN được định nghĩa tập trung trong `com.wms.config.SecurityConfig` bằng cơ chế `RoleHierarchy` của Spring Security.

### Cấu hình Frontend
* Auth store (`frontend/src/stores/auth.store.js`) lưu thông tin `user` và `activeWarehouse` đăng nhập.
* Header component (`frontend/src/components/layout/Header.jsx`) hiển thị bộ chọn kho.

---

## 2. Các Kịch bản Xác thực Thủ công (Manual Verification)

### Kịch bản 1: Ràng buộc số lượng kho khi tạo/cập nhật user (ADMIN)
1. Đăng nhập tài khoản System Admin.
2. Vào trang **Quản lý tài khoản** (`/admin/users`), nhấn **Tạo mới**.
3. Chọn vai trò `STOREKEEPER` (Thủ kho):
   * Thử lưu mà không chọn kho nào -> Hệ thống hiển thị thông báo lỗi yêu cầu gán kho.
   * Thử gán nhiều hơn 1 kho (nếu giao diện cho phép) -> API backend phải trả về lỗi `400 Bad Request` và không cho phép lưu.
4. Chọn vai trò `ADMIN` hoặc `CEO`:
   * Nhấn Lưu -> Tài khoản được tạo thành công, không có bản ghi liên kết kho nào được tạo trong DB.

### Kịch bản 2: Cách ly kho đối với Thủ kho (STOREKEEPER / WAREHOUSE_STAFF)
1. Đăng nhập bằng tài khoản Thủ kho được gán cho kho **Hà Nội** (ví dụ: `storekeeper_hn@wms.com`).
2. Xác nhận ở Header chỉ hiển thị text kho hoạt động là **Hà Nội**, không có nút dropdown chuyển kho.
3. Thử đổi URL thủ công hoặc gọi API đến kho **Hồ Chí Minh** -> Hệ thống trả về `403 Forbidden`.

### Kịch bản 3: Quyền xem kho khác của Trưởng kho (WAREHOUSE_MANAGER)
1. Đăng nhập bằng tài khoản Trưởng kho được gán cho kho **Hải Phòng** (`manager_hp@wms.com`).
2. Tại trang **Tổng quan (Dashboard)**, xác nhận có bộ chọn kho ở Header:
   * Chuyển kho sang **Hồ Chí Minh**.
   * Dashboard hiển thị số lượng tồn kho khả dụng của kho Hồ Chí Minh ở chế độ Read-only.
   * Nhấp chọn một sản phẩm, nhấn **Xin điều chuyển** -> Hệ thống mở Modal tạo yêu cầu điều chuyển nhanh, tự động điền kho gửi là **Hồ Chí Minh** và kho nhận mặc định là kho **Hải Phòng** của mình (không được sửa kho nhận).
3. Di chuyển sang trang **Nhập hàng** hoặc **Xuất hàng**:
   * Xác nhận bộ chọn kho ở Header biến mất hoặc bị khóa cứng về **Hải Phòng**.
   * Hệ thống tự động đặt lại `activeWarehouse` về **Hải Phòng** để đảm bảo Trưởng kho không thao tác ghi dữ liệu ở kho khác.

---

## 3. Chạy Kiểm thử tự động (Automated Tests)

### Backend Tests
Chạy kiểm thử logic Service và Controller:
```bash
mvn test -Dtest=UserServiceImplTest,WarehouseControllerTest,ReportServiceImplTest
```

### Frontend Tests
Chạy kiểm thử Auth Store và Route Protection:
```bash
npm run test
```
