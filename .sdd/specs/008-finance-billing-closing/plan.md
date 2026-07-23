# Implementation Plan - Spec 008: Tái cấu trúc UX/UI Phân hệ Tài chính & Công nợ (Finance & Credit)

**Spec ID**: `008-finance-billing-closing`  
**Plan Date**: 2026-07-23  
**Status**: Approved  

## 1. Goal & Objectives
Tái cấu trúc kiến trúc giao diện Tài chính & Công nợ (Spec 008) theo quy chuẩn mới đã thống nhất tại `/speckit-clarify`:
1. Phân nhóm menu Sidebar khối Tài chính thành 4 sub-module độc lập.
2. Hợp nhất màn hình Phải thu AR thành Unified AR View 3 Tab đồng bộ 100% với Phải trả AP.
3. Bổ sung 2 màn hình quản trị tài chính: **Kỳ kế toán & Khóa sổ** (`/finance/periods`) và **Báo cáo Phân kỳ Công nợ (Aging Report)** (`/reports/credit-aging`).

## 2. Technical Context & Components

- **Sidebar Menu (`Sidebar.jsx`)**:
  - `financeItems`: Bảng giá, Duyệt bảng giá, Phải thu Đại lý (AR), Phải trả Nhà cung cấp (AP), Kỳ kế toán & Khóa sổ, Báo cáo tuổi nợ Đại lý.
- **Unified AR View (`DealerDebtInvoice.jsx`)**:
  - Route: `/finance/invoices`
  - Tab 1: `Thông báo HĐ Bán` (`billing_notifications`)
  - Tab 2: `Hóa đơn Bán (SINV)` (`invoices`)
  - Tab 3: `Phiếu thu AR & Quét OCR` (`payment_receipts`)
- **Period Closing (`PeriodClosing.jsx`)**:
  - Route: `/finance/periods`
  - API: `GET /api/v1/accounting-periods`, `PUT /api/v1/accounting-periods/{id}/close`
- **Credit Aging Report (`CreditAgingReport.jsx`)**:
  - Route: `/reports/credit-aging`
  - API: `GET /api/v1/credit/aging-report`

## 3. Verification Plan
- Build check: `npm run build` thành công.
- Manual test: Đăng nhập vai trò `ACCOUNTANT` và `ACCOUNTANT_MANAGER` để chuyển các tab, khóa kỳ kế toán và kiểm tra báo cáo phân kỳ công nợ.
