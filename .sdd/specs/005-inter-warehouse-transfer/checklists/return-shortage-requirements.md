# Checklist chất lượng đặc tả: Thiếu hàng điều chuyển và quay đầu quá hạn

**Mục đích**: Xác minh độ đầy đủ nghiệp vụ và tính nhất quán giữa các tài liệu trước khi triển khai
**Ngày tạo**: 2026-06-28
**Tính năng**: [Nhận hàng tại kho đích](../features/feature-storekeeper-transfer-receive.md)

## Chất lượng nội dung

- [ ] Không có chi tiết triển khai như ngôn ngữ, framework hoặc API
- [x] Tập trung vào giá trị người dùng và nhu cầu nghiệp vụ
- [ ] Viết cho stakeholder không kỹ thuật
- [x] Hoàn thành mọi mục bắt buộc

## Độ đầy đủ yêu cầu

- [x] Không còn marker `[NEEDS CLARIFICATION]`
- [x] Yêu cầu có thể test và không mơ hồ
- [x] Tiêu chí thành công đo được
- [x] Tiêu chí thành công không phụ thuộc công nghệ
- [x] Acceptance scenario bao phủ định giá shortage và return quá hạn
- [x] Edge case bao gồm phân quyền, hỏng hàng, thiếu hàng và QC khi quay đầu
- [x] Phạm vi được giới hạn rõ
- [x] Dependency và giả định đã xác định

## Mức sẵn sàng của tính năng

- [x] Functional requirement có acceptance criteria
- [x] User scenario bao phủ tài xế quay đầu và kho nguồn nhận lại
- [x] Task bao phủ test, migration, backend, frontend, OpenAPI, audit và valuation
- [ ] Không rò chi tiết triển khai vào specification

## Ghi chú

- Các mục chi tiết kỹ thuật vẫn để mở vì định dạng WMS SDD chuẩn cố ý bao gồm API, data-model và status contract.
- Hành vi nghiệp vụ đã sẵn sàng cho bước lập kế hoạch triển khai; code hiện tại chỉ hỗ trợ quay đầu do quá hạn/`is_returned` và không còn triển khai flow wrong-SKU approval.
