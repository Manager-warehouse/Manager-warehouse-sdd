# CLAUDE.md — WMS Frontend (React 18 + Vite + Tailwind CSS)

## Giao diện Quản Lý Kho Vận (WMS) v1.0

---

## TL;DR (Đọc nhanh — 60 giây)

> **Đây là phân hệ Frontend của hệ thống Warehouse Management System (WMS)**
>
> **Stack**: React 18 + Tailwind CSS 3.x
> **Auth & RBAC**: JWT Authentication + Authorization theo cả **Role** và **Warehouse Assignment**.
> **Routing**: React Router DOM v6 với cơ chế Lazy Loading và Route Guard.
> **Design**: Apple / Shopifi design system (Phẳng, bo tròn Pill, tối giản, thanh lịch, phông chữ Inter/Neue Haas Grotesk).
>
> **Lệnh thông dụng**:
> * Khởi chạy dev server: `npm run dev`
> * Xây dựng production: `npm run build`
> * Kiểm tra linting: `npm run lint`

---

## KIẾN TRÚC FRONTEND & DATA FLOW

### Sơ đồ luồng dữ liệu (Data Flow)

```
                     ┌────────────────────────┐
                     │   React Components     │
                     │  (Pages / UI Elements) │
                     └──────┬──────────▲──────┘
                            │          │
         1. Gọi Actions     │          │ 4. Re-render khi State đổi
         (e.g., login())    │          │ (Selectors)
                            ▼          │
                     ┌────────────────────────┐
                     │         Stores         │
                     │  (authStore, uiStore)  │
                     └──────┬──────────▲──────┘
                            │          │
         2. Gọi API         │          │ 3. Cập nhật State
         (authService)      │          │ (set({ user }))
                            ▼          │
                     ┌────────────────────────┐
                     │      Axios Client      │
                     │     (api.client.js)    │
                     └──────────┬─────────────┘
                                │ HTTP Requests (Bearer JWT)
                                ▼
                     ┌────────────────────────┐
                     │    REST API Backend    │
                     │       (/api/v1)        │
                     └────────────────────────┘
```

### Các Store Toàn Cục (Zustand)

Hệ thống quản lý State bằng **Zustand** nhằm tối ưu hóa hiệu năng render:

1. **`auth.store.js`** ([auth.store.js](file:///d:/swp/Manager-warehouse-sdd/frontend/src/stores/auth.store.js)):
   * **State**: `user`, `token`, `activeWarehouse` (kho làm việc hiện tại).
   * **Actions**:
     * `login(user, token)`: Lưu token, user vào `localStorage`, tính toán và thiết lập `activeWarehouse` mặc định (kho đầu tiên được gán, hoặc HP-01 đối với Admin/CEO).
     * `logout()`: Xóa sạch session trong `localStorage` và reset state.
     * `setActiveWarehouse(warehouse)`: Thay đổi kho làm việc hiện tại và lưu vào cache.
   * **Helpers**:
     * `hasRole(role)`: Kiểm tra vai trò của người dùng hiện tại.
     * `hasWarehouseAccess(warehouseId)`: Kiểm tra người dùng có quyền thao tác trên kho cụ thể hay không (Admin/CEO mặc định được truy cập tất cả).

2. **`ui.store.js`** ([ui.store.js](file:///d:/swp/Manager-warehouse-sdd/frontend/src/stores/ui.store.js)):
   * **State**: `sidebarOpen`, `toasts` (danh sách thông báo), `loading` (trạng thái loading toàn màn hình).
   * **Actions**:
     * `toggleSidebar()` / `setSidebarOpen(open)`
     * `setLoading(loading)`
     * `addToast(message, type)`: Hiển thị thông báo đẩy (success, error, warning) và tự động ẩn sau 3 giây.
     * `removeToast(id)`: Tắt thông báo thủ công.

---

## ROUTING & PHÂN QUYỀN (RBAC)

Hệ thống điều hướng sử dụng **React Router DOM v6** tại [AppRoutes.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/routes/AppRoutes.jsx) kết hợp với Route Guard tại [ProtectedRoute.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/routes/ProtectedRoute.jsx).

* **Public Routes**: `/login`
* **Protected Routes** (Yêu cầu đăng nhập): `/dashboard`, `/profile`
* **Role-based Routes** (Yêu cầu Role cụ thể): Ví dụ `/admin/users` chỉ cho phép `ROLES.ADMIN`.
* **Cơ chế Layout**: Khi đi qua `ProtectedRoute`, hệ thống tự động bọc nội dung trang vào cấu trúc layout chuẩn gồm: `Header` (navbar trên, cho phép chuyển kho), `Sidebar` (menu bên trái lọc theo phân quyền), và `Footer` ở cuối trang.

---

## KẾT NỐI API & MOCK MODE

### API Client (`api.client.js`)

* **Base URL**: Tự động nhận từ `.env.development` / `.env.production` qua biến `VITE_API_BASE_URL` (mặc định `/api/v1`).
* **Interceptors**:
  * **Request Interceptor**: Tự động gắn header `Authorization: Bearer <token>` nếu có token trong `localStorage`.
  * **Response Interceptor**: Lắng nghe mã lỗi `401 Unauthorized` để thực hiện làm mới token tự động qua endpoint `/api/v1/auth/refresh`. Nếu thất bại, xóa session và đẩy người dùng về trang `/login`.

### Mock Mode

* Kích hoạt bằng biến môi trường `VITE_USE_MOCK=true` để chạy thử nghiệm frontend độc lập khi chưa có API backend.

---

## THIẾT KẾ GIAO DIỆN & STYLING (DESIGN SYSTEM)

Quy chuẩn thiết kế được định nghĩa trong [DESIGN.md](file:///d:/swp/Manager-warehouse-sdd/DESIGN.md) và cấu hình Tailwind tại [tailwind.config.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tailwind.config.js):

### 1. Phông chữ & Typography
* Phông chữ hiển thị tiêu đề (Display): **Neue Haas Grotesk Display** (hoặc Helvetica Neue/Arial), mặc định ở độ dày mỏng **weight 330** (`font-display`).
* Phông chữ giao diện (Body/Form): **Inter** (`font-sans`).
* Kích hoạt tính năng OpenType **ss03** trên toàn trang để đồng bộ ký tự dạng hình học tối giản.

### 2. Bảng Màu (Colors)
* **Canvas Cream** (`#fbfbf5`): Màu nền mặc định cho giao diện ánh sáng (Light canvas).
* **Canvas Night** (`#000000`): Màu đen sâu dùng cho các trang chế độ tối hoặc Hero section.
* **Ink** (`#000000`): Màu chữ chính trên nền sáng.
* **OnPrimary** (`#ffffff`): Màu chữ trên nền tối/nền nút bấm.
* **Aloe 10** (`#c1fbd4`) & **Pistachio 10** (`#d4f9e0`): Tông xanh mint đại diện cho sự phát triển, thành công giao dịch, dùng làm điểm nhấn (Accent) trên nền sáng.

### 3. Nút & Hộp Chứa (Buttons & Cards)
* Tất cả nút bấm bắt buộc phải có dạng viên thuốc (**Pill shape** - bo tròn tối đa `rounded-pill`).
* Sử dụng các CSS Components đã định nghĩa sẵn trong [globals.css](file:///d:/swp/Manager-warehouse-sdd/frontend/src/styles/globals.css):
  * `.btn-pill`: Lớp cơ sở cho nút pill.
  * `.btn-pill-primary`: Nút chính (Nền đen chữ trắng).
  * `.btn-pill-outline-light`: Nút viền đen nền trắng.
  * `.btn-pill-aloe`: Nút màu xanh mint nổi bật.
  * `.card-premium`: Hộp chứa thông tin chuẩn với đổ bóng mờ xếp lớp (Level 3 Shadow).

---

## CẤU TRÚC THƯ MỤC CHI TIẾT & PHÂN HỆ NGHIỆP VỤ

Ánh xạ chi tiết từ 10 phân hệ đặc tả (Spec 001 - Spec 010) vào cấu trúc frontend thực tế:

```text
src/
├── components/                 # Các component React tái sử dụng
│   ├── common/                 # UI Components dùng chung (Atoms & Molecules)
│   │   ├── Button.jsx          # Nút bấm pill-shape
│   │   ├── Input.jsx           # Form input có validate
│   │   ├── Table.jsx           # Bảng dữ liệu (phân trang, sắp xếp, lọc)
│   │   ├── Select.jsx          # Dropdown phân trang/lazy search
│   │   ├── Modal.jsx           # Dialog popup
│   │   ├── Badge.jsx           # Nhãn trạng thái (QC Pass, Đang chuyển, Đã duyệt...)
│   │   ├── Toast.jsx           # Thông báo trượt
│   │   └── BarcodeScanner.jsx  # Quét mã vạch camera/thiết bị chuyên dụng
│   │
│   ├── layout/                 # Khung giao diện
│   │   ├── Header.jsx          # Header điều khiển và chọn Kho làm việc
│   │   ├── Sidebar.jsx         # Menu điều hướng theo phân quyền Role
│   │   └── Footer.jsx          # Footer thông tin hệ thống
│   │
│   └── warehouse/              # Component nghiệp vụ kho đặc thù
│       ├── BatchSelector.jsx   # Chọn lô hàng (gợi ý FEFO/FIFO)
│       ├── BinCapacityIndicator.jsx # Thanh hiển thị sức chứa Bin trước khi Putaway
│       ├── SerialNumberList.jsx # Nhập/quản lý Serial (nếu has_serial = true)
│       └── InTransitTracker.jsx # Theo dõi lộ trình hàng đang đi đường
│
├── pages/                      # Các màn hình chính theo phân hệ
│   ├── Auth/
│   │   └── Login.jsx           # Đăng nhập (Spec 001)
│   ├── Admin/
│   │   ├── UserManagement.jsx  # Quản lý User, Role, phân Kho (Spec 001)
│   │   ├── SystemConfig.jsx    # Cài đặt tham số hệ thống, khóa sổ (Spec 001)
│   │   └── AuditLogs.jsx       # Truy vết thao tác (Audit Trail) (Spec 001)
│   ├── MasterData/
│   │   ├── ProductList.jsx     # Quản lý SKU, sản phẩm, has_serial (Spec 002)
│   │   ├── WarehouseList.jsx   # Quản lý Kho, Vùng (Zones), Ô kệ (Bins) (Spec 002)
│   │   ├── Partners.jsx        # Đại lý (Dealers) & Nhà cung cấp (Suppliers) (Spec 002)
│   │   └── FleetManagement.jsx # Đội xe & Tài xế (Spec 002)
│   ├── Inbound/
│   │   ├── ReceiptList.jsx     # Phiếu nhập kho (Spec 003)
│   │   ├── QCInbound.jsx       # Kiểm định chất lượng đầu vào, phân Grade (Spec 003)
│   │   └── PutawayPlan.jsx     # Kế hoạch xếp dỡ hàng vào Bin (Spec 003)
│   ├── Outbound/
│   │   ├── DeliveryOrders.jsx  # Lệnh xuất kho DO & Picking (Spec 004)
│   │   ├── QCOutbound.jsx     # QC đầu ra trước khi giao hàng (Spec 004)
│   │   └── TripPlanning.jsx    # Gom chuyến xe & Ký nhận POD (Spec 004)
│   ├── InternalTransfer/
│   │   ├── TransferRequest.jsx # Lệnh điều chuyển giữa các kho Hải Phòng/HN/HCM (Spec 005)
│   │   ├── InTransitStock.jsx  # Theo dõi hàng đang đi đường (Virtual location) (Spec 005)
│   │   └── TransferReceipt.jsx # Nhận hàng điều chuyển & xử lý lệch (Spec 005)
│   ├── Stocktake/
│   │   ├── StocktakeSession.jsx # Tạo đợt kiểm kê theo Zone/Bin (Spec 006)
│   │   └── VarianceApproval.jsx # Phê duyệt chênh lệch kiểm kê (Spec 006)
│   ├── Finance/
│   │   ├── PriceListManagement.jsx # Bảng giá & Lịch sử giá bán đại lý (Spec 007)
│   │   ├── DealerDebtInvoice.jsx # Quản lý hóa đơn công nợ đại lý (Spec 008)
│   │   ├── Payments.jsx        # Ghi nhận thanh toán thu/chi công nợ (Spec 008)
│   │   └── ReturnsDisposal.jsx # Trả hàng (Credit Note) & Tiêu hủy hàng hỏng (Spec 009)
│   ├── Reports/
│   │   ├── InventoryReport.jsx # Báo cáo tồn kho tức thời & Giá vốn COGS (Spec 010)
│   │   └── LowStockAlerts.jsx  # Cảnh báo tồn kho thấp (Spec 010)
│   └── Dashboard.jsx           # Báo cáo KPI trực quan (Spec 010)
```

---

## PHÂN HỆ NGHIỆP VỤ HÀNG HOÀN TRẢ & TIÊU HỦY (SPEC 009)

Quản lý luồng hàng hoàn trả từ đại lý (Customer Returns) và tiêu hủy hàng hỏng từ Quarantine (Scrap Disposal).

### 1. Phân hệ Customer Returns (Trả hàng)
- **Tạo phiếu trả**: `STOREKEEPER` soạn nháp phiếu trả chọn từ DO gốc có trạng thái `DELIVERED`.
- **QC kiểm đếm**: Storekeeper nhập số lượng thực tế, tách thành `qc_passed_qty` (Bin thường) và `qc_failed_qty` (Bin cách ly). Tổng số lượng phải `<= issued_qty` của DO gốc.
- **Phê duyệt**: `WAREHOUSE_MANAGER` phê duyệt phiếu trả.
- **Putaway**: Storekeeper phân bổ hàng đạt vào Bin thường và hàng hỏng vào Bin cách ly (Quarantine).
- **Credit Note**: `ACCOUNTANT` tạo Credit Note cấn trừ công nợ `current_balance` của đại lý. Số tiền hoàn bằng `actual_qty * unit_price` từ DO gốc.

### 2. Phân hệ Scrap Disposal (Tiêu hủy)
- **Đề xuất tiêu hủy**: `WAREHOUSE_MANAGER` chọn hàng từ Quarantine đề xuất tiêu hủy, tạo `damage_reports` và `adjustments` chờ duyệt.
- **Phê duyệt hạn mức**:
  - Giá trị đề xuất `≤ 100,000,000` VND: `WAREHOUSE_MANAGER` duyệt trực tiếp.
  - Giá trị đề xuất `> 100,000,000` VND: Chỉ `CEO` được duyệt.
- **Trừ tồn kho**: Tồn kho Quarantine bị trừ ngay khi đề xuất được duyệt.

### 3. Các File Frontend Liên Quan
- Màn hình giao diện: [ReturnsDisposal.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Finance/ReturnsDisposal.jsx)
- API Service: [finance.service.js](file:///d:/swp/Manager-warehouse-sdd/frontend/src/services/finance.service.js)

---

## NGUYÊN TẮC PHÁT TRIỂN & VIẾT CODE

### ✅ LUÔN LUÔN LÀM (ALWAYS DO)
1. **Phân quyền chặt chẽ trên UI**: Sử dụng helper `hasRole` và `hasWarehouseAccess` từ `auth.store` để ẩn/hiện hoặc chặn thao tác của người dùng không có quyền.
2. **Kiểm soát nghiệp vụ nghiêm ngặt**:
   * Không cho phép nhập số lượng âm ở bất kỳ form nghiệp vụ nào.
   * Khi thực hiện xuất/điều chuyển hàng, bắt buộc giao diện phải hiển thị cảnh báo nếu số khả dụng không đủ: `available = total - reserved < quantity_requested`.
   * Giao diện nhập kho bắt buộc phải bắt qua luồng **QC Inbound** trước khi hiển thị nút "Putaway".
   * Giao diện chọn lô hàng xuất kho phải tự động sắp xếp và gợi ý lô theo quy tắc **FEFO** (sản phẩm có hạn dùng) hoặc **FIFO** (sản phẩm không có hạn dùng).
3. **Quy chuẩn đặt tên**:
   * Tên Component React: PascalCase (ví dụ: `BatchSelector.jsx`, `InTransitStock.jsx`).
   * Tên Hook / Tiện ích / File Logic: camelCase (ví dụ: `useAuth.js`, `format.js`).
   * Tên Route: kebab-case (ví dụ: `/admin/users`, `/internal-transfer/in-transit`).
4. **Hiệu năng & Tối ưu hóa**:
   * Sử dụng `React.lazy` kết hợp `Suspense` cho tất cả các trang lớn trong bộ định tuyến `AppRoutes.jsx`.
   * Sử dụng selector khi lấy dữ liệu từ Zustand store để tránh re-render không cần thiết (Ví dụ: `const user = useAuthStore(state => state.user)`).
   * Sử dụng custom hook `useDebounce` khi xây dựng bộ lọc tìm kiếm nhanh đối với SKU hoặc đối tác.

### ❌ KHÔNG ĐƯỢC LÀM (NEVER DO)
1. **Không code cứng (Hardcode) dữ liệu hệ thống**: Không khai báo cứng mã kho, vai trò, hoặc danh mục trạng thái trong component. Sử dụng hằng số tập trung tại [constants.js](file:///d:/swp/Manager-warehouse-sdd/frontend/src/utils/constants.js).
2. **Không bỏ qua việc validate form**: Tất cả biểu mẫu nhập liệu (đặc biệt là tạo phiếu, điều chuyển, điều chỉnh tồn kho) phải sử dụng bộ kiểm tra dữ liệu từ `utils/validators.js` trước khi gửi lên API.
3. **Không dùng console.log trong code hoàn thiện**: Xóa toàn bộ lệnh log trước khi hoàn thành task. Sử dụng hệ thống log tập trung hoặc báo lỗi qua Toast.
4. **Không viết file quá dài**: Giới hạn tối đa **300 dòng** cho một file component. Nếu vượt quá, hãy chia nhỏ thành các component con hoặc tách logic ra custom hooks.
5. **Không sửa đổi trực tiếp tồn kho**: Mọi hành động điều chỉnh tồn kho trên UI phải đi qua màn hình tạo phiếu điều chỉnh (Adjustment) hoặc kiểm kê (Stocktake), không bao giờ có nút thay đổi trực tiếp số lượng tồn kho của Bin.
