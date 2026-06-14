# Business Constraints — WMS Phúc Anh

> Ràng buộc nghiệp vụ WMS bắt buộc. Mọi code vi phạm các luật này coi như không hợp lệ.
> Nguồn: AGENTS.md · constitution.md · Userstory.md

## 1. Inventory Rules

| ID | Rule | Mức độ |
|---|---|---|
| INV-01 | `inventory.quantity >= 0` luôn đúng trước và sau mọi thao tác | MUST |
| INV-02 | FIFO mặc định: chọn batch có `received_date` cũ nhất cho domain hàng gia dụng không quản lý hạn dùng | MUST |
| INV-03 | FEFO ngoại lệ: chỉ chọn batch có hạn dùng gần nhất cho sản phẩm có expiry hoặc được cấu hình FEFO | MUST |
| INV-04 | Mọi thay đổi tồn kho MUST qua receipt/issue/transfer/adjustment/stocktake | MUST |
| INV-05 | `@Version` trên inventory — conflict → HTTP 409 + retry | MUST |
| INV-06 | `available = total - reserved >= 0`. Check trước khi xuất | MUST |
| INV-07 | Chênh lệch 5-100M → Trưởng kho duyệt; > 100M → CEO duyệt | MUST |

## 2. Batch Rules

| ID | Rule | Mức độ |
|---|---|---|
| BAT-01 | Mỗi batch chỉ 1 grade (A/B/C). Khác grade → tạo batch mới | MUST |
| BAT-02 | Sản phẩm `has_serial = true` → bắt buộc nhập serial khi nhập/xuất | MUST |
| BAT-03 | Putaway kiểm tra `bin_capacity` trước khi đặt hàng vào bin | MUST |
| BAT-04 | Batch hết hạn không được chọn cho flow xuất kho thông thường | MUST |

## 3. QC & Quarantine Rules

| ID | Rule | Mức độ |
|---|---|---|
| QC-01 | Hàng nhập MUST qua QC Inbound trước khi nhập chính thức | MUST |
| QC-02 | Hàng xuất MUST qua QC Outbound trước khi giao | MUST |
| QC-03 | Hàng fail QC → Quarantine Zone — không tính available inventory | MUST |
| QC-04 | Hàng Quarantine chỉ được xử lý: Trả NCC (RTV) hoặc Tiêu hủy | MUST |
| QC-05 | RTV → tạo Debit Note đòi bồi hoàn NCC | MUST |
| QC-06 | Tiêu hủy → áp dụng bảng định mức phê duyệt (5M/100M) | MUST |

## 4. Transfer Rules

| ID | Rule | Mức độ |
|---|---|---|
| TRF-01 | Điều chuyển MUST qua In-Transit location (kho ảo) | MUST |
| TRF-02 | Kho nguồn giảm → In-Transit tăng → Kho đích nhận → In-Transit giảm → Kho đích tăng | MUST |
| TRF-03 | Chênh lệch `quantity_sent` vs `quantity_received` → adjustment + audit record | MUST |
| TRF-04 | Chỉ cập nhật inventory đích khi kho đích xác nhận | MUST |

## 5. Outbound / Delivery Rules

| ID | Rule | Mức độ |
|---|---|---|
| OUT-01 | Credit Check tự động trước khi tạo Delivery Order | MUST |
| OUT-02 | Reserve inventory ngay khi DO tạo thành công | MUST |
| OUT-03 | Giải phóng reserved khi DO chuyển In-Transit hoặc Cancelled | MUST |
| OUT-04 | Hệ thống chỉ dùng xe nội bộ Phúc Anh — KHÔNG 3PL | MUST |
| OUT-05 | Khi Tài xế xác nhận In-Transit, trừ tồn kho kho xuất và cộng vào kho ảo In-Transit | MUST |
| OUT-06 | POD = ảnh hàng bàn giao + ảnh chữ ký/biên nhận Đại lý + timestamp + OTP email đã xác thực. Bắt buộc khi giao thành công | MUST |
| OUT-07 | Giao thất bại chỉ ghi nhận trạng thái `RETURNED` và lý do; xử lý hàng hoàn đi theo luồng hoàn hàng riêng | MUST |

## 6. Credit & Debt Rules

| ID | Rule | Mức độ |
|---|---|---|
| CRD-01 | CREDIT_HOLD khi: `current_balance + giá_trị_đơn > credit_limit` HOẶC có hóa đơn quá hạn > 30 ngày; nếu bằng hạn mức thì vẫn cho tạo giao dịch | MUST |
| CRD-02 | CREDIT_HOLD → chặn tạo giao dịch mới, hiển thị lý do | MUST |
| CRD-03 | Mở khóa ACTIVE khi: `current_balance < credit_limit * 0.8` | MUST |
| CRD-04 | Top-up payment cần thiết: thông báo số tiền cần trả thêm | MUST |
| CRD-05 | Credit Limit chỉ Kế toán trưởng được thiết lập | MUST |

## 7. Accounting Rules

| ID | Rule | Mức độ |
|---|---|---|
| ACC-01 | Invoice được tạo khi DO chuyển Delivered — tự động cộng công nợ | MUST |
| ACC-02 | Hóa đơn quá hạn → tự động CREDIT_HOLD + cảnh báo Kế toán trưởng | MUST |
| ACC-03 | Chốt sổ: kiểm tra tồn đọng (chưa duyệt/chưa lập hóa đơn) → không cho chốt nếu còn | MUST |
| ACC-04 | Sau chốt: khóa cứng — không tạo/sửa/xóa chứng từ trong kỳ đã chốt | MUST |
| ACC-05 | Sai sót kỳ trước → Adjustment Voucher tại kỳ mở, link tham chiếu | MUST |
| ACC-06 | COGS = giá vốn tại ngày xuất từ price_history | MUST |

## 8. Soft Delete Rules

| ID | Rule | Mức độ |
|---|---|---|
| DEL-01 | Master data → `is_active = false` | MUST |
| DEL-02 | Transaction data → `status = CANCELLED` | MUST |
| DEL-03 | KHÔNG DELETE rows khỏi bảng nghiệp vụ | MUST |
| DEL-04 | Foreign key integrity luôn duy trì | MUST |

## 9. Audit Rules

| ID | Rule | Mức độ |
|---|---|---|
| AUD-01 | Mọi thao tác ghi trên kho MUST có audit log | MUST |
| AUD-02 | Audit log gồm: actor, action, timestamp, entity_type, entity_id, old_value, new_value, warehouse_id | MUST |
| AUD-03 | Phân quyền thay đổi cũng phải ghi audit log | MUST |
| AUD-04 | Report view cũng ghi log (người xem, thời gian, bộ lọc) | MUST |

## 10. Approval Thresholds

| Giá trị / Loại | Người duyệt | Điều kiện |
|---|---|---|
| Chênh lệch < 5M | Tự động | Có QC/kiểm kê xác nhận |
| Chênh lệch 5M - 100M | Trưởng kho | — |
| Chênh lệch > 100M hoặc lỗi do nhân viên | CEO | — |
| Tiêu hủy < 5M | Không bán | — |
| Tiêu hủy 5M - 100M | Trưởng kho | — |
| Tiêu hủy > 100M | CEO | — |
| Credit Limit | Kế toán trưởng | Duy nhất |
