# Feature Specification: Bảng giá & Giá vốn (Pricing & COGS)

**Spec ID**: 007-pricing-cogs-management
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-14

---

## 1. Context and Goal

Giá bán và giá vốn hàng hóa thay đổi theo thời gian. Mỗi kỳ kinh doanh có bảng giá riêng với ngày hiệu lực và hết hạn. Hệ thống cần lưu lịch sử giá (`price_history`) để tính COGS chính xác tại ngày xuất hàng và phục vụ báo cáo P&L.

### Features List
* [US-WMS-14: Thiết lập Bảng giá & Lịch sử Biến động giá](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-accountant-pricing-management.md)
* [Hệ thống Tự động Tính Giá vốn COGS & Áp giá Giao dịch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Kế toán viên | Maker | Thiết lập và nhập bảng giá vốn + giá bán theo kỳ hiệu lực cho từng sản phẩm (hỗ trợ Excel import), trình duyệt |
| Kế toán trưởng | Checker | Xem chi tiết, đối chiếu so sánh giá cũ/mới và phê duyệt giá chính thức |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Pricing Management](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-accountant-pricing-management.md#3-functional-requirements-ears)
* [EARS - COGS Calculation](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Price lookup at transaction time | ≤ 100ms |
| NFR-002 | Price history query for a product | ≤ 500ms |
| NFR-003 | Excel import for 1000 SKUs | ≤ 5s |

## 5. Data Model

### price_history
- `id` (BIGSERIAL, PK)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `effective_date` (DATE, NOT NULL)
- `end_date` (DATE) -- NULL = đang hiệu lực vô thời hạn hoặc cho tới khi có giá mới
- `cost_price` (DECIMAL(18,2), NOT NULL)
- `selling_price` (DECIMAL(18,2), NOT NULL)
- `status` (VARCHAR(20), DEFAULT 'PENDING', CHECK IN ('PENDING','APPROVED'), NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Pricing Management](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-accountant-pricing-management.md#4-api-endpoints)
* [APIs - COGS Calculation](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| OVERLAPPING_EFFECTIVE_DATE | 409 | New price overlaps existing active period |
| PRICE_LIST_ALREADY_APPROVED | 409 | Duplicate approval |
| MISSING_PRICE | 400 | Product has no active price at transaction date |
| PRICE_LIST_PENDING | 400 | Cannot use price list not yet approved |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Pricing Management](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-accountant-pricing-management.md#5-acceptance-criteria)
* [Acceptance - COGS Calculation](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md#5-acceptance-criteria)

## 9. Out of Scope

- Automated price optimization / dynamic pricing
- Multi-currency pricing
- Promotional/discount pricing rules
- Bulk pricing tiers by dealer segment
