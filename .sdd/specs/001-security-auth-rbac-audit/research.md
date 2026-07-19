# Research: Admin Role Hierarchy & Warehouse Isolation

## Problem Statement

Hệ thống WMS cần hỗ trợ nhiều vai trò người dùng (Roles) vận hành trên 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). System Admin cần phân quyền và cách ly dữ liệu giữa các kho để tránh nhân viên kho này can thiệp kho khác. Trong đó, yêu cầu đặc thù bao gồm:
1. **ADMIN** có toàn quyền và thừa kế quyền của mọi vai trò khác (Role Hierarchy).
2. **ADMIN & CEO** có quyền xem và truy cập toàn bộ các kho mà không cần gán tường minh trong cơ sở dữ liệu (Database Assignment Bypass).
3. **WAREHOUSE_MANAGER (Trưởng kho)** chỉ được gán vào 1 kho duy nhất nhưng được phép xem Dashboard/Tồn kho tổng quan của kho khác (Read-only) để lên kế hoạch xin điều chuyển hàng hóa.
4. **Các vai trò bị giới hạn khác** chỉ được gán vào đúng 1 kho duy nhất và bị khóa cứng (cả Đọc và Ghi) theo kho đó.

---

## Decisions

### 1. Centralized Role Hierarchy in Spring Security (Quyền Admin)
- **Decision**: Cấu hình `RoleHierarchy` bean trong `SecurityConfig.java` để ánh xạ `ROLE_ADMIN` thừa kế tất cả các vai trò khác (e.g. `ADMIN > CEO`, `ADMIN > WAREHOUSE_MANAGER`...).
- **Rationale**: Tránh sửa đổi thủ công `@PreAuthorize` trên hàng chục Controller và tự động áp dụng cho mọi method security.

### 2. Bypass gán kho cho ADMIN và CEO trong Database
- **Decision**: Quyền truy cập mọi kho của ADMIN và CEO được xử lý thông qua logic nghiệp vụ (Service level). Bảng `user_warehouse_assignments` không lưu bản ghi nào của ADMIN/CEO.
- **Rationale**: 
  - Đơn giản hóa việc quản lý dữ liệu.
  - Khi thêm kho vật lý mới, ADMIN/CEO tự động có quyền truy cập mà không cần chạy batch update database.

### 3. Ràng buộc số lượng kho của các vai trò bị giới hạn (Backend Validation)
- **Decision**: Trong `UserServiceImpl.java`, khi tạo hoặc cập nhật tài khoản:
  - Nếu role là `ADMIN` hoặc `CEO`, tự động bỏ qua danh sách kho gửi lên (không lưu vào bảng gán kho).
  * Nếu role khác, kiểm tra: danh sách kho phải có kích thước **chính xác bằng 1**. Nếu không, ném lỗi `IllegalArgumentException` với các mã lỗi tương ứng (`WAREHOUSE_REQUIRED` hoặc `MULTIPLE_WAREHOUSES_NOT_ALLOWED`).
- **Rationale**: Đảm bảo tính toàn vẹn dữ liệu từ tầng gốc, tránh lỗi runtime khi xử lý các giao dịch nhập/xuất kho.

### 4. Phân quyền hiển thị kho của Trưởng kho (WAREHOUSE_MANAGER)
- **Decision**: 
  - Cho phép `WAREHOUSE_MANAGER` xem Dashboard tổng quan và tồn kho khả dụng (Available Qty) ở các kho khác dưới dạng Read-only.
  - Chặn các API chi tiết nội bộ (sơ đồ Bin, chi tiết giao dịch...) và chặn tất cả API ghi dữ liệu (tạo phiếu, duyệt phiếu...) tại kho khác.
- **Rationale**: Hỗ trợ Trưởng kho tự tra cứu lượng hàng ở kho bạn để tạo yêu cầu điều chuyển nhanh mà vẫn đảm bảo tính bảo mật.

### 5. Bộ chọn kho toàn cục (Global Warehouse Selector) trên Frontend
- **Decision**: 
  - **ADMIN & CEO**: Selector luôn hiển thị trên Header, cho phép chuyển đổi giữa "Tất cả các kho" hoặc chọn kho cụ thể trên mọi trang.
  - **WAREHOUSE_MANAGER**: Selector mặc định khóa ở kho được gán. Chỉ riêng tại trang `/dashboard` và `/inventory-availability` (nếu có), selector mới được mở khóa để chọn xem kho khác (chế độ Read-only). Khi rời khỏi các trang này, hệ thống tự động reset `activeWarehouse` về kho được gán của họ.
  - **Các vai trò còn lại**: Ẩn hoặc khóa Selector, chỉ hiển thị text tên kho duy nhất của họ.
- **Rationale**: Tối ưu hóa trải nghiệm người dùng, giúp chuyển đổi kho trực quan mà không vi phạm quy tắc cách ly.

---

## Alternatives Considered

### Alternative: Lưu đầy đủ liên kết của ADMIN/CEO vào bảng `user_warehouse_assignments`
- **Why Rejected**: Làm phình to bảng gán kho và gặp lỗi đồng bộ dữ liệu khi hệ thống mở thêm chi nhánh mới (phải viết code bổ sung để gán kho mới cho tất cả tài khoản ADMIN/CEO hiện tại).
