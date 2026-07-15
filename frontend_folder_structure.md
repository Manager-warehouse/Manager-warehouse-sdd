# Cấu trúc Frontend — ManagerWarehouse

> Đồng bộ: 2026-07-15. Tài liệu mô tả cấu trúc đang có trong repo; nguồn yêu cầu là `.sdd/specs/001`–`012`.

Frontend dùng React 18, Vite, Tailwind CSS 3.x, Zustand, Axios, Capacitor 8 và Vitest + React Testing Library. Không có Barcode/QR scanner trong Sprint 1.

## Cấu trúc hiện hành

```text
frontend/
├── android/                         # Capacitor Android project
├── ios/                             # Capacitor iOS project
├── src/
│   ├── components/
│   │   ├── common/                  # Button, Input, Table, Modal, Toast, PhotoCaptureInput
│   │   ├── layout/                  # Header, Sidebar, Footer, BillingNotificationMenu
│   │   └── warehouse/               # Picking, OTP, MobileLayout, capacity/credit helpers
│   ├── hooks/                       # useDebounce, useMediaQuery
│   ├── pages/
│   │   ├── Admin/                   # 001–002: users, config, audit, products, warehouse, partners, fleet
│   │   ├── Auth/                    # 001: Login, ForgotPassword
│   │   ├── Finance/                 # 007–008: price, invoice/debt, payment
│   │   ├── Inbound/                 # 003 + 009: receipt, QC, putaway, quarantine, return
│   │   ├── InterWarehouseTransfer/  # 005: request/workspace/actions/status
│   │   ├── Outbound/                # 004: DO, picking, QC, trip, driver
│   │   ├── Reports/                 # 010: CEO dashboard, valuation, alerts, productivity
│   │   ├── Stocktake/               # 006: list, form, detail
│   │   ├── Forbidden/               # RBAC denial state
│   │   ├── Profile/                 # current user profile
│   │   └── Dashboard.jsx
│   ├── routes/                      # AppRoutes, ProtectedRoute
│   ├── services/                    # api.client + domain API services
│   ├── stores/                      # auth.store, ui.store
│   ├── styles/                      # globals.css
│   ├── utils/                       # format, constants, transfer status
│   ├── App.jsx
│   └── main.jsx
├── tests/                           # Spec 012 tests, setup and domain suites
├── package.json                     # npm scripts: build, lint, test, test:coverage
└── capacitor.config.*               # Capacitor configuration when present
```

## Ánh xạ spec

| Specs | Khu vực frontend | Ghi chú |
| --- | --- | --- |
| 001–002 | `pages/Auth`, `pages/Admin`, `routes`, `stores/auth.store.js` | JWT, RBAC + warehouse scope và master data. |
| 003, 009 | `pages/Inbound` | Receipt, inbound QC, putaway, quarantine, return. |
| 004 | `pages/Outbound`, `components/warehouse` | DO, FIFO picking, outbound QC, trip, mobile POD/OTP. |
| 005 | `pages/InterWarehouseTransfer` | Transfer request, ship/receive workflow và trạng thái. |
| 006 | `pages/Stocktake` | Count, variance, approval. |
| 007–008 | `pages/Finance`, `services/finance.service.js`, `services/pricing.service.js` | Pricing, COGS, invoice/debt/payment. |
| 010 | `pages/Reports`, `pages/Dashboard.jsx`, `services/report.service.js` | Dashboard, valuation, alert, productivity. |
| 011 | CI/backend concern | Không tạo page frontend. |
| 012 | `tests/`, `src/**/*.test.js`, `vitest` scripts | Vitest + RTL; utility, store, admin và transfer tests. |

## Quy ước bắt buộc

- Component dùng PascalCase; hook, service và utility dùng camelCase.
- Mọi route nghiệp vụ đi qua `ProtectedRoute`; UI chỉ hỗ trợ RBAC, backend vẫn là nơi quyết định quyền cuối cùng.
- Không gọi API trực tiếp trong component nếu đã có domain service.
- Không dùng `console.log` trong production code; không thêm Barcode/QR, serial, expiry hay grade tracking.
- `npm run lint`, `npm run build` và `npm test` là các kiểm tra frontend chuẩn.
