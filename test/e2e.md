# End-to-End Tests — WMS Phúc Anh

> Hướng dẫn viết E2E Test cho toàn bộ luồng nghiệp vụ.
> Nguồn: Userstory.md · README.md

## 1. Mục tiêu

- Test luồng nghiệp vụ hoàn chỉnh từ đầu đến cuối (BE + FE + DB)
- Phát hiện lỗi tích hợp giữa các module
- Đảm bảo nghiệp vụ cross-cutting (audit, phân quyền, soft-delete) hoạt động đúng

## 2. Công cụ đề xuất

| Layer | Công cụ |
|---|---|
| Backend API | REST Assured hoặc TestRestTemplate |
| Frontend rendering | Playwright / Puppeteer |
| DB verification | Testcontainers + JDBC |

## 3. Các luồng E2E bắt buộc

### Flow 1: Nhập hàng hoàn chỉnh (US-WMS-02 → US-WMS-05)

```
Planner tạo Lệnh nhập → Thủ kho nhập số lượng thực tế
→ Nhân viên QC kiểm tra → Hàng lỗi vào Quarantine, hàng đạt cất Bin
→ Trưởng kho duyệt → Cộng tồn kho khả dụng
→ Kiểm tra: Inventory tăng đúng, Audit Log ghi đủ
```

### Flow 2: Xuất hàng + Giao hàng (US-WMS-06 → US-WMS-10)

```
Planner tạo DO (kiểm tra credit check) → Reserve inventory
→ Thủ kho soạn hàng → QC đóng gói → Ready to Ship
→ Dispatcher lập Trip → Tài xế xác nhận In-Transit → Trừ tồn kho và tạo delivery attempt
→ Tài xế giao → Ký POD + OTP hợp lệ trên attempt hiện tại → Delivered
→ Kế toán xử lý invoice candidates → Tạo Invoice → Cộng công nợ → Completed
→ Đại lý thanh toán → Cấn trừ công nợ → Closed
```

### Flow 3: Điều chuyển kho (US-WMS-12)

```
Planner tạo Phiếu điều chuyển → Trưởng kho nguồn duyệt
→ Thủ kho xuất hàng → Trừ tồn nguồn, cộng In-Transit
→ Trưởng kho đích nhận → Trừ In-Transit, cộng tồn đích
→ Kiểm tra: Chênh lệch → Tạo adjustment
```

### Flow 4: Kiểm kê + Điều chỉnh (US-WMS-13)

```
Thủ kho tạo phiếu kiểm kê → Khóa sổ tạm → Đếm thực tế
→ Hệ thống tính chênh lệch → Trưởng kho/CEO duyệt
→ Cập nhật tồn kho → Ghi audit log
```

### Flow 5: Credit Hold + Thanh toán (US-WMS-06 + US-WMS-15)

```
Tạo DO khi Dealer còn hạn mức → OK
Tạo DO khi Dealer vượt hạn mức → BLOCKED + popup lý do
Dealer thanh toán → Cấn trừ công nợ → Mở khóa
→ Kiểm tra: DO mới được tạo lại
```

### Flow 6: Chốt sổ cuối tháng (US-WMS-17)

```
Kế toán trưởng chốt sổ kỳ T → Khóa cứng
→ Kiểm tra: Không thể sửa/xóa chứng từ kỳ T
→ Kiểm tra: Chứng từ trễ hạn ghi vào kỳ T+1
```

## 4. Môi trường E2E

```yaml
# application-e2e.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wms_e2e
    username: wms_e2e
    password: wms_e2e
  jpa:
    hibernate:
      ddl-auto: create  # Tạo sạch trước khi test
```

## 5. Kiểm tra sau E2E

- [ ] Tồn kho cuối cùng khớp với mong đợi
- [ ] No negative inventory ở bất kỳ bước nào
- [ ] Audit log được tạo cho mọi thao tác ghi
- [ ] Reserved quantity được giải phóng đúng lúc
- [ ] Dealer status transitions chính xác
- [ ] Không có orphan records trong DB
