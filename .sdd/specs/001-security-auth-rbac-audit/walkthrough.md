# Walkthrough: Phân Quyền & Cách Ly Kho (User Warehouse Assignment & Isolation)

Tài liệu tóm tắt các thay đổi và kết quả xác thực của tính năng gán kho người dùng và cách ly dữ liệu kho.

---

## 1. Các thay đổi đã thực hiện (Changes Made)

### Backend (Spring Boot 3.4.5 + Java 21)
1. **[UserServiceImpl.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/main/java/com/wms/service/impl/UserServiceImpl.java)**:
   - Thêm phương thức helper `validateWarehouseAssignments` để kiểm tra danh sách kho gán khi tạo hoặc cập nhật tài khoản.
   - Vai trò `ADMIN` và `CEO` tự động bypass bảng gán kho (không lưu bản ghi gán kho).
   - Các vai trò khác bị giới hạn phải được gán tối thiểu và tối đa **chính xác 1 kho**, nếu không sẽ ném lỗi `IllegalArgumentException`.
2. **[WarehouseServiceImpl.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/main/java/com/wms/service/impl/WarehouseServiceImpl.java)**:
   - Cập nhật `getAllWarehouses` để nhận tham số `userId`.
   - Lọc danh sách kho trả về: các vai trò bị giới hạn chỉ thấy duy nhất kho họ được gán, trong khi `ADMIN`, `CEO`, và `WAREHOUSE_MANAGER` có thể xem toàn bộ danh sách kho hoạt động.
3. **[WarehouseController.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/main/java/com/wms/controller/WarehouseController.java)**:
   - Thay đổi API `/api/v1/admin/warehouses` nhận đối tượng `Principal` để lấy actor ID và chuyển tiếp cho Service.
4. **[AuthService.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/main/java/com/wms/service/AuthService.java)**:
   - Bổ sung logic cho `buildWarehouseInfoList` và `buildMeWarehouseInfoList`. Trả về toàn bộ danh sách kho hoạt động khi ADMIN/CEO đăng nhập, và trả về duy nhất kho được gán đối với các vai trò khác.
5. **[GlobalExceptionHandler.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java)**:
   - Đăng ký các mã lỗi dịch chuyển sang tiếng Việt trực quan cho người dùng: `WAREHOUSE_REQUIRED`, `MULTIPLE_WAREHOUSES_NOT_ALLOWED`, `WAREHOUSE_SCOPE_FORBIDDEN`.

### Frontend (React 18)
1. **[Header.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/components/layout/Header.jsx)**:
   - Tích hợp bộ chọn kho toàn cục.
   - `ADMIN` và `CEO` có thể thay đổi kho trên mọi trang.
   - `WAREHOUSE_MANAGER` chỉ có thể thay đổi kho tại trang `/dashboard` và `/inventory-availability`. Ở các trang tác vụ khác, selector bị khóa cứng và ẩn chevron.
   - Các vai trò khác bị ẩn chevron và hoàn toàn bị khóa ở kho được gán duy nhất.
2. **[ProtectedRoute.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/routes/ProtectedRoute.jsx)**:
   - Lắng nghe thay đổi route. Nếu `WAREHOUSE_MANAGER` chuyển từ trang `/dashboard` sang trang tác vụ khác khi đang xem kho khác, hệ thống tự động reset `activeWarehouse` về kho được gán mặc định.
3. **[auth.store.js](file:///d:/swp/Manager-warehouse-sdd/frontend/src/stores/auth.store.js)**:
   - Xác thực logic `hasWarehouseAccess` hoạt động trơn tru.

---

## 2. Kết quả kiểm thử (Verification Results)

### Kiểm thử Backend (JUnit 5 + Mockito)
Đã bổ sung các kịch bản kiểm thử bảo vệ cấu trúc cách ly kho:
* **[UserServiceImplTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/UserServiceImplTest.java)**: Xác thực các ràng buộc khi gán 0 kho, gán nhiều kho cho role bị giới hạn, và bypass cho Admin.
* **[WarehouseServiceTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/WarehouseServiceTest.java)**: Đảm bảo lọc kho trả về đúng theo quyền hạn.
* **[ReportServiceImplTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/service/impl/ReportServiceImplTest.java)**: Kiểm tra bảo vệ chặn Trưởng kho xem báo cáo của kho khác.
* **[WarehouseControllerTest.java](file:///d:/swp/Manager-warehouse-sdd/backend/src/test/java/com/wms/controller/WarehouseControllerTest.java)**: Xác thực API Controller cập nhật tham số mới biên dịch tốt.

**Kết quả chạy**:
```text
[INFO] Running com.wms.service.UserServiceImplTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.wms.controller.WarehouseControllerTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.wms.service.impl.ReportServiceImplTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.wms.service.WarehouseServiceTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0

[INFO] BUILD SUCCESS
```

### Kiểm thử Frontend (Vitest)
Chạy tất cả 85 bài test trên frontend bao gồm:
* **[rbac.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/rbac.test.js)**: Đảm bảo phân quyền truy cập kho đúng vai trò.

**Kết quả chạy**:
```text
 Test Files  7 passed (7)
      Tests  85 passed (85)
   Start at  14:36:53
   Duration  1.95s
```
