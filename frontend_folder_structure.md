# Kiến Trúc Cấu Trúc Thư Mục Frontend - ManagerWarehouse

Tài liệu này định nghĩa cấu trúc thư mục chi tiết cho phần **Frontend (React 18 + Tailwind CSS + Vite)** của dự án **Warehouse Management System (WMS) - ManagerWarehouse**. Cấu trúc này được ánh xạ trực tiếp từ **10 phân hệ đặc tả (Spec 001 - Spec 010)** đã xây dựng trong dự án.

---

## 1. Sơ Đồ Cấu Trúc Thư Mục (Directory Tree)

Dưới đây là cấu trúc thư mục được thiết kế theo phong cách phẳng (File-type-based) như ảnh mẫu nhưng mở rộng chi tiết cho nghiệp vụ kho vận phức tạp:

```text
frontend/
├── public/
│   ├── favicon.ico                 # Icon của ứng dụng trên tab trình duyệt
│   └── assets/                     # Các tài nguyên tĩnh (logo Phúc Anh, ảnh minh họa, icons...)
│
├── src/
│   ├── components/                 # Các component React tái sử dụng
│   │   ├── common/                 # UI Components dùng chung (Atoms & Molecules)
│   │   │   ├── Button.jsx          # Nút bấm (hỗ trợ primary, outline, variant colors...)
│   │   │   ├── Input.jsx           # Trường nhập liệu (text, number, date, validation...)
│   │   │   ├── Select.jsx          # Dropdown chọn (phân trang, lazy search đại lý/sản phẩm)
│   │   │   ├── Table.jsx           # Bảng dữ liệu (hỗ trợ pagination, sorting, filtering)
│   │   │   ├── Modal.jsx           # Hộp thoại popup xác nhận, nhập liệu nhanh
│   │   │   ├── Badge.jsx           # Nhãn trạng thái (QC Pass/Fail, Đang chuyển, Đã duyệt...)
│   │   │   ├── Toast.jsx           # Thông báo đẩy góc màn hình (Success, Error, Warning)
│   │   │   └── BarcodeScanner.jsx  # Component hỗ trợ quét mã vạch bằng camera/thiết bị quét chuyên dụng
│   │   │
│   │   ├── layout/                 # Các component khung giao diện (Layout Wrapper)
│   │   │   ├── Header.jsx          # Thanh đầu trang (thông tin user, chọn kho làm việc hiện tại)
│   │   │   ├── Sidebar.jsx         # Thanh menu điều hướng (phân quyền hiển thị theo Role của Actor)
│   │   │   └── Footer.jsx          # Thanh chân trang (thông tin hệ thống, phiên bản)
│   │   │
│   │   └── warehouse/              # Các UI component nghiệp vụ kho đặc thù
│   │       ├── BatchSelector.jsx   # Bộ chọn lô hàng theo FIFO/ngày nhận
│   │       ├── BinCapacityIndicator.jsx # Chỉ báo sức chứa của Bin trước khi Putaway hàng hóa
│   │       ├── QuantityEntry.jsx    # Nhập số lượng theo SKU/batch cho đơn hàng lớn
│   │       └── InTransitTracker.jsx # Trình theo dõi vị trí/trạng thái hàng đang điều chuyển
│   │
│   ├── pages/                      # Các màn hình chính tương ứng với 10 phân hệ nghiệp vụ Spec
│   │   ├── Auth/
│   │   │   └── Login.jsx           # Trang đăng nhập hệ thống (Spec 001)
│   │   │
│   │   ├── Admin/
│   │   │   ├── UserManagement.jsx  # Quản lý tài khoản, gán Vai trò (Role) & Kho làm việc (Spec 001)
│   │   │   ├── SystemConfig.jsx    # Cấu hình tham số hệ thống, chu kỳ đóng sổ kế toán (Spec 001)
│   │   │   └── AuditLogs.jsx       # Truy vết thao tác người dùng (Audit Trail) (Spec 001)
│   │   │
│   │   ├── MasterData/
│   │   │   ├── ProductList.jsx     # Quản lý danh mục sản phẩm, SKU, đơn vị tính, quy cách đóng gói (Spec 002)
│   │   │   ├── WarehouseList.jsx   # Quản lý sơ đồ kho vật lý, khu vực (zones), ô kệ (bins) (Spec 002)
│   │   │   ├── Partners.jsx        # Quản lý Đại lý (Dealers) & Nhà cung cấp (Suppliers) (Spec 002)
│   │   │   └── FleetManagement.jsx # Quản lý đội xe (Vehicles) & Tài xế (Drivers) (Spec 002)
│   │   │
│   │   ├── Inbound/
│   │   │   ├── ReceiptList.jsx     # Danh sách phiếu nhập kho (Mua hàng / Đại lý trả hàng) (Spec 003)
│   │   │   ├── QCInbound.jsx       # Màn hình kiểm định chất lượng đầu vào, kết luận đạt/lỗi (Spec 003)
│   │   │   └── PutawayPlan.jsx     # Kế hoạch điều phối hàng vào Bin dựa trên sức chứa (Spec 003)
│   │   │
│   │   ├── Outbound/
│   │   │   ├── DeliveryOrders.jsx  # Danh sách đơn xuất kho (DO), quản lý danh sách picking (Spec 004)
│   │   │   ├── QCOutbound.jsx     # Kiểm định chất lượng đầu ra trước khi xếp hàng lên xe (Spec 004)
│   │   │   └── TripPlanning.jsx    # Điều phối xe, tài xế, sắp xếp lộ trình & ký nhận giao hàng POD (Spec 004)
│   │   │
│   │   ├── InternalTransfer/
│   │   │   ├── TransferRequest.jsx # Tạo lệnh điều chuyển giữa các kho Hải Phòng/HN/HCM (Spec 005)
│   │   │   ├── InTransitStock.jsx  # Theo dõi tồn kho ảo đang trên đường vận chuyển (Spec 005)
│   │   │   └── TransferReceipt.jsx # Xác nhận nhận hàng và xử lý chênh lệch tại kho đích (Spec 005)
│   │   │
│   │   ├── Stocktake/
│   │   │   ├── StocktakeSession.jsx # Tạo đợt kiểm kê thực tế theo khu vực/bin (Spec 006)
│   │   │   └── VarianceApproval.jsx # Xem chênh lệch tồn kho và gửi phê duyệt điều chỉnh (Spec 006)
│   │   │
│   │   ├── Finance/
│   │   │   ├── PriceListManagement.jsx # Cấu hình bảng giá bán đại lý & lịch sử giá (Spec 007)
│   │   │   ├── DealerDebtInvoice.jsx # Quản lý hóa đơn công nợ đại lý (Spec 008)
│   │   │   ├── Payments.jsx        # Ghi nhận phiếu thu/chi, thanh toán công nợ (Spec 008)
│   │   │   └── ReturnsDisposal.jsx # Xử lý hàng trả lại (Credit Note) & Tiêu hủy hàng hỏng (Spec 009)
│   │   │
│   │   ├── Reports/
│   │   │   ├── InventoryReport.jsx # Báo cáo tồn kho tức thời, giá vốn COGS (Spec 010)
│   │   │   └── LowStockAlerts.jsx  # Cấu hình hạn mức tối thiểu & theo dõi cảnh báo tồn kho thấp (Spec 010)
│   │   │
│   │   ├── Dashboard.jsx           # Bảng điều khiển chính (CEO/Quản lý xem biểu đồ KPI kho vận) (Spec 010)
│   │   └── NotFound.jsx            # Trang lỗi 404 (Không tìm thấy trang)
│   │
│   ├── routes/                     # Cấu hình định tuyến
│   │   ├── AppRoutes.jsx           # Bộ định tuyến chính (hỗ trợ React.lazy load màn hình)
│   │   └── ProtectedRoute.jsx      # Middleware bảo vệ route (kiểm tra JWT & quyền Role/Warehouse)
│   │
│   ├── services/                   # Quản lý các cuộc gọi API (Axios client)
│   │   ├── api.client.js           # Base Axios instance (gắn JWT Interceptor, handle 401/403)
│   │   ├── auth.service.js         # API Auth: login, logout, refresh token, profile (Spec 001)
│   │   ├── admin.service.js        # API Admin: quản lý user, cấu hình, audit logs (Spec 001)
│   │   ├── master.service.js       # API Master Data: products, warehouses, partners, fleet (Spec 002)
│   │   ├── inbound.service.js      # API Inbound: receipts, QC inbound, putaway (Spec 003)
│   │   ├── outbound.service.js     # API Outbound: DO, picking, QC outbound, trips (Spec 004)
│   │   ├── transfer.service.js     # API Transfer: transfer requests, in-transit stock (Spec 005)
│   │   ├── stocktake.service.js    # API Stocktake: stocktake sessions, adjustments (Spec 006)
│   │   └── finance.service.js      # API Finance: prices, COGS, invoices, returns, disposal (Spec 007/008/009)
│   │
│   ├── hooks/                      # Custom React Hooks
│   │   ├── useAuth.js              # Hook truy xuất nhanh thông tin đăng nhập và phân quyền Actor
│   │   ├── useBarcodeReader.js     # Hook lắng nghe và xử lý luồng sự kiện từ thiết bị quét barcode
│   │   └── useDebounce.js          # Hook tối ưu hiệu năng khi tìm kiếm tức thời (SKU, mã phiếu)
│   │
│   ├── context/                    # Global React Context
│   │   └── ThemeContext.jsx        # Quản lý giao diện Sáng/Tối (Light/Dark Mode) và Color Palette
│   │
│   ├── stores/                     # Quản lý State tập trung toàn cục (Zustand)
│   │   ├── auth.store.js           # Lưu thông tin user đăng nhập, token, kho làm việc được gán
│   │   └── ui.store.js             # Lưu các state UI toàn cục (sidebar open/close, global loading...)
│   │
│   ├── utils/                      # Các hàm tiện ích dùng chung
│   │   ├── constants.js            # Khai báo hằng số hệ thống (Roles, Statuses, Warehouses...)
│   │   ├── format.js               # Định dạng tiền tệ VND, số lượng, ngày tháng hiển thị
│   │   └── validators.js           # Các hàm validate biểu mẫu (form validation)
│   │
│   ├── styles/                     # Quản lý style CSS
│   │   ├── globals.css             # Các thiết lập CSS cơ bản, Tailwind base directives
│   │   └── theme.css               # Định nghĩa các biến CSS variables (colors, spacing, shadows)
│   │
│   ├── App.jsx                     # Component gốc của ứng dụng (bọc Context/Zustand providers)
│   └── main.jsx                    # Điểm khởi đầu ứng dụng (React DOM render)
│
├── .env.development                # Biến môi trường chạy local (API URL local)
├── .env.production                 # Biến môi trường chạy production
├── .gitignore                      # Cấu hình bỏ qua các file khi commit lên Git
├── .prettierrc                     # Cấu hình định dạng code đồng nhất cho team
├── package.json                    # Định nghĩa các package phụ thuộc (React, Tailwind, Axios, Zustand)
├── tailwind.config.js              # Cấu hình Tailwind CSS (custom colors, fonts, responsive)
└── vite.config.js                  # Cấu hình build Vite
```

---

## 2. Ánh Xạ Chi Tiết Giữa Cấu Trúc File & 10 Phân Hệ Đặc Tả (Spec)

Để hiểu rõ hơn cách cấu trúc thư mục này giải quyết các yêu cầu nghiệp vụ của dự án WMS, dưới đây là bảng ánh xạ chi tiết:

| Phân hệ Spec | Thư mục/Trang Frontend (`/src/pages/`) | Component nghiệp vụ (`/src/components/`) | API Service (`/src/services/`) | Quy tắc kiểm soát cần lưu ý |
|---|---|---|---|---|
| **Spec 001: System Admin & RBAC** | `pages/Auth/Login.jsx`<br>`pages/Admin/*` | `layout/Sidebar.jsx` (Phân quyền menu)<br>`ProtectedRoute.jsx` (Chặn route) | `services/auth.service.js`<br>`services/admin.service.js` | JWT Authentication, phân quyền thao tác theo cả **Role** và **Warehouse Assignment**. |
| **Spec 002: Master Data** | `pages/MasterData/*` | - | `services/master.service.js` | Quản lý danh mục Sản phẩm, Kho, Bin, Nhà cung cấp, Đại lý, Đội xe & Tài xế. |
| **Spec 003: Inbound (Nhập kho)** | `pages/Inbound/*` | `components/warehouse/BinCapacityIndicator.jsx` | `services/inbound.service.js` | Bắt buộc phải đi qua **QC Inbound** đạt/lỗi và kiểm tra sức chứa Bin (`bin_capacity`) trước khi Putaway. |
| **Spec 004: Outbound (Giao hàng)** | `pages/Outbound/*` | `components/warehouse/BatchSelector.jsx` | `services/outbound.service.js` | Hỗ trợ lập phiếu Picking theo FIFO, QC kiểm định xuất khẩu, gom chuyến xe (Trip) và thu thập chữ ký/ảnh chụp để xác nhận **POD**. |
| **Spec 005: Internal Transfer** | `pages/InternalTransfer/*` | `components/warehouse/InTransitTracker.jsx` | `services/transfer.service.js` | Phải ghi nhận tồn kho tại kho ảo **In-Transit Location** khi đang vận chuyển và xử lý chênh lệch tại kho nhận. |
| **Spec 006: Stocktake & Adjust** | `pages/Stocktake/*` | - | `services/stocktake.service.js` | Hỗ trợ tạo đợt kiểm kê, đối chiếu tồn thực tế - tồn hệ thống, tính toán **Variance** và quy trình duyệt điều chỉnh. |
| **Spec 007: Pricing & COGS** | `pages/Finance/PriceListManagement.jsx` | - | `services/finance.service.js` | Quản lý giá bán đại lý, lịch sử giá và tích hợp tính **COGS** (Giá vốn) phục vụ báo cáo tài chính kho. |
| **Spec 008: Finance & Credit** | `pages/Finance/DealerDebtInvoice.jsx`<br>`pages/Finance/Payments.jsx` | - | `services/finance.service.js` | Quản lý Hóa đơn công nợ Đại lý, các Giao dịch Thanh toán (phiếu thu/chi) và khóa sổ theo kỳ kế toán. |
| **Spec 009: Returns & Disposal** | `pages/Finance/ReturnsDisposal.jsx` | - | `services/finance.service.js` | Xử lý trả hàng từ Đại lý (tự động tạo **Credit Note** giảm trừ công nợ) và quy trình thanh lý/tiêu hủy hàng hỏng. |
| **Spec 010: Reporting & Alerts** | `pages/Dashboard.jsx`<br>`pages/Reports/*` | - | `services/finance.service.js` (COGS)<br>`services/inbound.service.js` | Bảng điều khiển KPI dành cho CEO/Manager, Báo cáo tồn kho, Cảnh báo tồn kho thấp (Low Stock Alerts). |
