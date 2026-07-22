# Warehouse Management System (WMS) - Phúc Anh

WMS Phúc Anh là hệ thống quản lý kho và vận hành nội bộ dành cho doanh nghiệp thương mại có nhiều kho vật lý. Hệ thống giúp số hóa toàn bộ quy trình từ lúc hàng về kho, kiểm tra chất lượng, cất hàng vào vị trí, xuất hàng cho đại lý, điều phối xe nội bộ, điều chuyển giữa các kho, kiểm kê tồn kho, đến theo dõi hóa đơn và công nợ.

Dự án được thiết kế cho mô hình vận hành của Công ty Phúc Anh với 3 kho tại Hải Phòng, Hà Nội và TP.HCM. Mục tiêu chính là thay thế cách làm thủ công bằng giấy tờ/Excel bằng một hệ thống tập trung, có phân quyền rõ ràng, kiểm soát tồn kho chặt chẽ và ghi nhận lịch sử thao tác đầy đủ.

## Hệ Thống Làm Gì?

WMS quản lý các nghiệp vụ cốt lõi của kho trong một luồng khép kín:

- Nhận thông tin nhập/xuất hàng từ Công ty mẹ hoặc bộ phận điều phối.
- Tạo phiếu nhập, kiểm đếm thực tế và kiểm QC inbound.
- Cất hàng đạt QC vào Bin Location; đưa hàng lỗi vào Quarantine.
- Tạo đơn xuất hàng, kiểm tra tồn khả dụng và công nợ đại lý.
- Lập kế hoạch picking theo FIFO, kiểm QC outbound trước khi xuất kho.
- Điều phối chuyến xe nội bộ, gán tài xế, xác nhận giao hàng bằng POD và OTP.
- Điều chuyển hàng giữa 3 kho thông qua kho ảo In-Transit.
- Kiểm kê định kỳ, ghi nhận chênh lệch và điều chỉnh tồn kho sau phê duyệt.
- Quản lý bảng giá, giá vốn, hóa đơn, thanh toán và công nợ đại lý.
- Hiển thị dashboard quản trị, cảnh báo tồn kho thấp và báo cáo vận hành.

## Điểm Nổi Bật

- Quản lý tập trung 3 kho vật lý: Hải Phòng, Hà Nội, TP.HCM.
- FIFO theo ngày nhận hàng là nguyên tắc xuất kho mặc định.
- Không cho phép tồn kho âm, không bỏ qua reserved quantity.
- QC bắt buộc trước khi nhập kho chính thức và trước khi xuất hàng.
- Hàng lỗi được tách vào Quarantine, không tính vào tồn khả dụng.
- Điều chuyển liên kho đi qua In-Transit để không mất dấu hàng đang vận chuyển.
- RBAC kiểm tra cả vai trò người dùng và phạm vi kho được gán.
- Maker-Checker rõ ràng cho các nghiệp vụ cần phê duyệt.
- Audit log cho mọi thao tác nghiệp vụ quan trọng.
- Giao diện mobile-friendly cho tài xế xác nhận chuyến xe, POD và OTP.
- Kế toán nội bộ, bảng giá, COGS, hóa đơn và công nợ chạy chung dữ liệu với WMS.
- Dashboard cho CEO/Kế toán trưởng và cảnh báo tồn kho thấp cho Trưởng kho/Planner.

## Các Phân Hệ Chính

| Phân hệ | Mục đích |
| --- | --- |
| Quản trị & phân quyền | Đăng nhập JWT, quản lý user, role, warehouse scope, cấu hình hệ thống và audit log |
| Danh mục nền tảng | Quản lý sản phẩm/SKU, kho, zone/bin, đại lý, nhà cung cấp, xe tải và tài xế |
| Nhập hàng & QC | Lập lệnh nhập, đếm hàng, QC inbound, quarantine, RTV và putaway |
| Xuất hàng & giao hàng | Delivery Order, credit check, reserve tồn kho, picking FIFO, outbound QC, trip, POD/OTP |
| Điều chuyển liên kho | Transfer request, CEO approval, phiếu TRF, công nhân xếp/báo số lượng, QC xuất, chuyến TTR, In-Transit, nhận hàng kho đích |
| Kiểm kê & điều chỉnh | Stocktake, variance, phê duyệt điều chỉnh tồn kho |
| Giá vốn & tài chính | Bảng giá, price history, COGS snapshot, invoice, payment, aging report, period closing |
| Hàng hoàn & tiêu hủy | Nhận hàng hoàn từ đại lý, credit note, xử lý hàng lỗi trong quarantine |
| Báo cáo & cảnh báo | CEO dashboard, productivity report, low-stock alert |

## Actor Chính

Hệ thống có 10 actor vận hành theo mô hình Maker-Checker:

- CEO
- System Admin
- Trưởng kho
- Kế toán trưởng
- Planner
- Dispatcher
- Thủ kho kiêm QC
- Nhân viên kho
- Kế toán viên
- Tài xế nội bộ

Chi tiết nghiệp vụ từng actor xem tại [docs/overview/actors.md](docs/overview/actors.md).

## Phạm Vi Sprint 1

Bao gồm:

- Quản lý kho, nhập/xuất, điều chuyển, kiểm kê, tồn kho.
- Kế toán nội bộ kho, giá vốn, hóa đơn, thanh toán và công nợ đại lý.
- Điều phối xe/tài xế nội bộ của Phúc Anh.
- Dashboard, báo cáo và cảnh báo tồn kho thấp.

Không bao gồm:

- Sản xuất, HR/HRM.
- Barcode/QR scanner automation.
- Cổng B2B/B2C.
- Tích hợp hệ thống ngoài.
- Dịch vụ vận chuyển 3PL.
- Tracking serial từng sản phẩm, hạn sử dụng hoặc phân cấp chất lượng bán lại.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Spring Boot 3.4.5, Java 21, Maven |
| Frontend | React 18, JavaScript, Vite |
| Database | PostgreSQL 18 |
| ORM | Spring Data JPA / Hibernate |
| Authentication | JWT, bcrypt |
| API Documentation | OpenAPI / Swagger |
| Testing | JUnit 5, Mockito, Jest/Vitest |
| Styling | Tailwind CSS 3.x |

## Chạy Local

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

URL mặc định:

- Frontend: `http://127.0.0.1:3001`
- Backend: `http://127.0.0.1:8080`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui/index.html`

## Tài Liệu Chi Tiết

| Nội dung | Đường dẫn |
| --- | --- |
| Actor và phân quyền nghiệp vụ | [docs/overview/actors.md](docs/overview/actors.md) |
| User stories | [docs/overview/user-stories.md](docs/overview/user-stories.md) |
| Tổng hợp feature và domain rules | [docs/overview/features-summary.md](docs/overview/features-summary.md) |
| Cấu trúc dự án | [docs/overview/project-structure.md](docs/overview/project-structure.md) |
| Design system frontend | [docs/architecture/design-system.md](docs/architecture/design-system.md) |
| Kế hoạch dọn backend theo module | [docs/architecture/backend-module-refactor-plan.md](docs/architecture/backend-module-refactor-plan.md) |
| Deployment guide | [docs/deployment/deployment-guide.md](docs/deployment/deployment-guide.md) |
| RDS/SDS và diagram | [.sdd/docs](.sdd/docs) |
| Feature specs chi tiết | [.sdd/specs](.sdd/specs) |
