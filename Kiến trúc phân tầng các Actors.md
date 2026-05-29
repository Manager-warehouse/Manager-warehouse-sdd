# KIẾN TRÚC PHÂN TẦNG CÁC ACTORS - HỆ THỐNG WMS PHÚC ANH

# Phiên bản: 2.0 | Cập nhật: 2026-05-27

# Ghi chú: Hệ thống dùng xe nội bộ Phúc Anh. KHÔNG có quản lý sản xuất, HRM, Barcode/QR, cổng B2B/B2C.

---

## MỤC TIÊU DỰ ÁN

**Mục tiêu chiến lược:** Xây dựng giải pháp phần mềm quản lý kho tập trung cho Công ty Phúc Anh, thay thế các phương thức thủ công (giấy tờ, Excel) bằng quy trình kỹ thuật số thống nhất tại 3 kho: Hải Phòng, Hà Nội và TP.HCM.

**Phạm vi hệ thống:**

- Bao gồm: Quản lý kho (WMS), Kế toán nội bộ kho, Điều phối vận tải nội bộ, Kinh doanh (công nợ Đại lý).
- **Không bao gồm:** Quản lý sản xuất, HR/HRM, Barcode/QR Scanner, cổng B2B/B2C Portal, tích hợp hệ thống bên ngoài.

---

## BẢNG PHÂN TẦNG ACTORS

| **Tầng**                      | **Actor**                    | **Loại**                                                        | **Vai trò tóm tắt**                                                              |
| :---------------------------- | :--------------------------- | :-------------------------------------------------------------- | :------------------------------------------------------------------------------- |
| **Tầng 1: Quản trị**          | CEO                          | Checker cấp cao                                                 | Duyệt chi vượt định mức, xem Dashboard chiến lược, phê duyệt cấu hình hệ thống   |
|                               | System Admin                 | Admin                                                           | Quản trị tài khoản, phân quyền RBAC, cấu hình tham số hệ thống                   |
| **Tầng 2: Quản lý **          | Trưởng kho kiêm Trưởng QC    | Checker                                                         | Phê duyệt phiếu nhập/xuất/điều chuyển, xử lý chênh lệch kiểm kê; Phê duyệt biên bản hàng lỗi, quyết định tiêu hủy/trả NCC |
|                               | Kế toán trưởng               | Checker                                                         | Duyệt bảng giá, chốt sổ kế toán, xem P&L và Aging Report, thiết lập Credit Limit |
| **Tầng 3: Nghiệp vụ (Maker)** | Planner (Người nhận đơn)     | Maker                                                           | Tiếp nhận đơn từ Công ty mẹ, lập lệnh nhập/đơn xuất, quản lý Planning Dashboard  |
|                               | Dispatcher (Người điều phối) | Maker                                                           | Lập chuyến xe nội bộ, gán tài xế, tối ưu lộ trình giao hàng                      |
|                               | Thủ kho                      | Maker                                                           | Tiếp nhận hàng thực tế, soạn hàng, kiểm kê, cất hàng vào Bin Location            |
|                               | Nhân viên kho (Bốc xếp & QC) | Maker                                                           | Bốc xếp, di chuyển hàng hóa, hỗ trợ thủ kho; kiểm tra chất lượng hàng nhập/xuất, nhập kết quả QC Đạt/Lỗi |
|                               | Kế toán viên                 | Maker                                                           | Lập hóa đơn, ghi nhận thanh toán, cấn trừ công nợ, nhập bảng giá                 |
|                               | Tài xế                       | Maker                                                           | Nhận lệnh giao hàng, cập nhật trạng thái, xác nhận POD                           |

---

## NGHIỆP VỤ CHI TIẾT TỪNG ACTOR

### 1. CEO

**Tầng:** Quản trị — Checker cấp cao

**Nghiệp vụ:**

- Phê duyệt các khoản chi/điều chỉnh tồn kho vượt định mức thẩm quyền Trưởng kho/Kế toán trưởng (> 100 triệu VNĐ).
- Xem Dashboard quản trị cấp cao:
  - _"Tồn kho tổng hiện tại là bao nhiêu tiền?"_
  - _"Đại lý nào đang nợ quá hạn và nợ nhiều nhất?"_
  - _"Tỷ lệ hàng lỗi QC tháng này có tăng không?"_
  - _"Hiệu suất giao hàng đúng hạn (On-Time Delivery) có đảm bảo SLA không?"_
- Phê duyệt các thay đổi cấu hình hệ thống quan trọng (thêm kho mới, thay đổi hạn mức công nợ Đại lý VIP).

**User Stories liên quan:** US-WMS-01, US-WMS-04, US-WMS-13, US-WMS-18

---

### 2. System Admin

**Tầng:** Quản trị — Admin

**Nghiệp vụ:**

- Tạo, vô hiệu hóa tài khoản người dùng.
- Phân quyền theo Vai trò (Role) và Chi nhánh Kho (RBAC): Đảm bảo nhân viên Kho Hải Phòng không xem được dữ liệu Kho Hà Nội; nhân viên kho không xem được báo cáo tài chính của Kế toán.
- Cấu hình tham số hệ thống: Bảng định mức phê duyệt, Tồn kho tối thiểu, Kỳ hạn thanh toán mặc định.

**User Stories liên quan:** US-WMS-01, US-WMS-21, US-WMS-22

---

### 3. Trưởng kho kiêm Trưởng QC

**Tầng:** Quản lý — Checker

**Nghiệp vụ:**

- Phê duyệt Phiếu nhập kho sau khi đối chiếu kết quả QC từ Nhân viên kho → Hệ thống tự động cộng tồn kho.
- Phê duyệt Phiếu điều chuyển kho (kho nguồn): Kiểm tra tồn khả dụng trước khi duyệt.
- Xác nhận nhận hàng điều chuyển (kho đích): Kiểm tra số lượng thực tế, ghi nhận chênh lệch nếu có.
- Duyệt chênh lệch kiểm kê giá trị 5 – 100 triệu VNĐ.
- Phê duyệt biên bản hàng lỗi tại Quarantine Zone, quyết định phương án xử lý (tiêu hủy hoặc trả hàng cho nhà cung cấp - NCC).
- Phê duyệt phiếu xuất hủy hàng lỗi 5 – 100 triệu VNĐ.

**User Stories liên quan:** US-WMS-04, US-WMS-05, US-WMS-12, US-WMS-13, US-WMS-24, US-WMS-25, US-WMS-26

---

### 4. Kế toán trưởng

**Tầng:** Quản lý — Checker

**Nghiệp vụ:**

- **Quản lý Bảng giá:** Phê duyệt Bảng giá (giá vốn + giá bán Đại lý) do Kế toán viên trình lên.
- **Thiết lập Credit Limit:** Người duy nhất có quyền thiết lập và điều chỉnh Hạn mức tín dụng (Credit Limit) và Kỳ hạn thanh toán (Net 30/60) cho từng Đại lý.
- **Chốt sổ Kế toán:** Thực hiện chốt sổ hàng tháng sau khi hệ thống xác nhận tất cả chứng từ đã hợp lệ.
- **Aging Report:** Xem báo cáo phân kỳ công nợ (Trong hạn / Quá hạn 1-30 / 31-60 / > 60 ngày).
- **P&L Report:** Xem báo cáo Lãi/Lỗ định kỳ.
- **Tồn kho cuối kỳ (Inventory Valuation):** Xem và xuất báo cáo giá trị tồn kho sau chốt sổ.

**User Stories liên quan:** US-WMS-14, US-WMS-16, US-WMS-17, US-WMS-18, US-WMS-22

---

---

### 6. Planner (Người nhận đơn)

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Nhập hàng:**

- Tiếp nhận thông tin hàng về từ Công ty mẹ (qua Zalo/Email) → Lập Lệnh nhập kho (Pending Receipt) trên hệ thống.

**Xuất hàng:**

- Tiếp nhận yêu cầu xuất hàng từ Công ty mẹ → Kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý → Lập Đơn xuất hàng (Delivery Order).
- Hệ thống tự động chặn nếu Đại lý vi phạm Credit Check (vượt hạn mức hoặc có hóa đơn quá hạn > 30 ngày).

**Điều chuyển:**

- Truy cập Planning Dashboard → Xem gợi ý điều chuyển → Tạo Phiếu điều chuyển kho nội bộ.

**User Stories liên quan:** US-WMS-02, US-WMS-06, US-WMS-11, US-WMS-12, US-WMS-19, US-WMS-26

---

### 7. Dispatcher (Người điều phối hàng và vận tải)

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

- Tạo Chuyến xe (Trip Log): Chọn xe nội bộ Phúc Anh từ danh mục, gán Tài xế.
- Gom các Đơn xuất hàng (ở trạng thái Ready to Ship) vào một Chuyến xe; sắp xếp thứ tự giao hàng (Stop Order) để tối ưu lộ trình.
- Kiểm tra tải trọng xe trước khi xác nhận chuyến (hệ thống cảnh báo nếu vượt tải).

**Lưu ý quan trọng:** Hệ thống **CHỈ dùng xe nội bộ** của Phúc Anh. KHÔNG phát sinh chi phí vận chuyển 3PL trong luồng xuất hàng thông thường → KHÔNG có quy trình Duyệt chi vận tải.

**User Stories liên quan:** US-WMS-08, US-WMS-23

---

### 8. Thủ kho

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Nhập hàng:**

- Nhận lệnh nhập từ Planner → Đếm hàng thực tế → Nhập số lượng vào phiếu nhập nháp.
- Hướng dẫn Nhân viên kho cất hàng Đạt vào Bin Location (hệ thống kiểm tra sức chứa trước khi cho phép).

**Xuất hàng:**

- Nhận Đơn xuất → Soạn hàng từ Bin Location → Cập nhật trạng thái Picking.
- Xác nhận hoàn tất soạn hàng sau khi Nhân viên kho kiểm tra QC xong → Trạng thái Ready to Ship.

**Điều chuyển:**

- Xác nhận xuất hàng lên xe nội bộ khi Phiếu điều chuyển đã được duyệt.
- Xác nhận nhận hàng điều chuyển đến kho mình (báo cáo số lượng thực tế nhận được).

**Kiểm kê:**

- Tạo Phiếu kiểm kê định kỳ → Đếm thực tế → Nhập số lượng vào hệ thống.

**User Stories liên quan:** US-WMS-03, US-WMS-04, US-WMS-07, US-WMS-12, US-WMS-13, US-WMS-20, US-WMS-24, US-WMS-25

---

### 9. Nhân viên kho (Bốc xếp & QC)

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Bốc xếp và di chuyển hàng hóa:**

- Bốc xếp hàng hóa lên/xuống xe tải vận chuyển nội bộ của Phúc Anh.
- Di chuyển hàng đạt QC vào đúng Bin Location theo chỉ dẫn của Thủ kho.
- Di chuyển hàng lỗi vào Quarantine Zone (Khu cách ly).

**QC Inbound (Nhập hàng):**

- Kiểm tra ngoại quan từng sản phẩm nhập về.
- Nhập kết quả QC lên hệ thống: **Đạt** hoặc **Lỗi** kèm lý do chi tiết (vỡ, móp, sai quy cách, hỏng bao bì,...).
- Di chuyển hàng Lỗi vào Quarantine Zone.

**QC Outbound (Xuất hàng):**

- Kiểm tra hàng đã soạn bởi Thủ kho: Đúng SKU, đúng số lượng, đóng thùng chống sốc.
- Xác nhận QC đạt trên hệ thống để Thủ kho cập nhật trạng thái Ready to Ship.

**User Stories liên quan:** US-WMS-03, US-WMS-07, US-WMS-24, US-WMS-25

---

### 10. Kế toán viên

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Lập Hóa đơn:**

- Nhận thông báo đơn hàng Delivered → Tạo Hóa đơn bán hàng (Invoice) với đầy đủ: Tổng giá trị (theo bảng giá hiệu lực tại ngày giao), Ngày xuất hóa đơn, Hạn thanh toán (Net 30/60 theo hồ sơ Đại lý).
- Hệ thống tự động cộng dồn vào `current_balance` của Đại lý và kiểm tra Credit Limit.

**Ghi nhận Thanh toán:**

- Khi Đại lý trả tiền → Tạo Phiếu thu (Payment Receipt): Chọn Đại lý, nhập số tiền, ngày thu, phương thức (Chuyển khoản/Tiền mặt), chọn Hóa đơn cần cấn trừ.
- Hệ thống tự động trừ `current_balance` và kiểm tra mở khóa tín dụng (ngưỡng 80%).

**Quản lý Bảng giá:**

- Tạo Bảng giá mới (giá vốn + giá bán Đại lý theo kỳ có ngày hiệu lực/hết hạn) → Gửi Kế toán trưởng duyệt.
- Hỗ trợ Import từ file Excel theo mẫu.

**Hàng hoàn trả:**

- Lập Credit Note khi Đại lý hoàn trả hàng → Hệ thống trừ `current_balance` tương ứng.

**User Stories liên quan:** US-WMS-04, US-WMS-10, US-WMS-14, US-WMS-15, US-WMS-19, US-WMS-24

---

### 11. Tài xế

**Tầng:** Nghiệp vụ — Maker

**Giao diện:** Web Responsive (Mobile-friendly) — Truy cập bằng smartphone.

**Nghiệp vụ:**

- Đăng nhập → Xem danh sách đơn hàng trong Chuyến xe của mình.
- Tại điểm giao: Đại lý ký tên trực tiếp trên màn hình cảm ứng; Tài xế chụp ảnh hàng hóa bàn giao.
- Nhấn "Xác nhận đã giao" → Hệ thống lưu POD (Hình ảnh + Chữ ký + Timestamp) và chuyển trạng thái đơn sang **Delivered**.
- Nếu giao thất bại → Chọn lý do (Đại lý vắng mặt / Từ chối nhận / Sai địa chỉ) → Hệ thống tạo Phiếu nhập hàng hoàn vào Kho cách ly.

**User Stories liên quan:** US-WMS-09, US-WMS-25

---

---

## CÁC QUY TRÌNH CHÍNH

### Quy trình Nhập hàng (Inbound)

```
Công ty mẹ gửi thông tin (Zalo/Email)
    → Planner lập Lệnh nhập [Pending Receipt]
    → Thủ kho đếm hàng thực tế
    → Nhân viên kho kiểm tra QC → Đạt/Lỗi
        ├── Hàng Lỗi → Quarantine Zone → Trưởng kho kiêm Trưởng QC quyết định (Trả NCC / Tiêu hủy)
        └── Hàng Đạt → Nhân viên kho di chuyển & cất vào Bin Location
    → Trưởng kho Duyệt nhập [Approved]
    → Hệ thống cộng tồn kho khả dụng
```

### Quy trình Xuất hàng & Giao hàng (Outbound & Delivery)

```
Công ty mẹ gửi yêu cầu xuất hàng
    → Planner kiểm tra [Credit Check + Tồn kho]
        ├── Vi phạm Credit → Chặn cứng, hiển thị lý do
        └── Hợp lệ → Lập Đơn xuất [New]
    → Thủ kho soạn hàng [Picking]
    → Nhân viên kho kiểm tra QC & đóng gói
    → Thủ kho xác nhận xong [Ready to Ship]
    → Dispatcher lập Chuyến xe nội bộ, gán Tài xế
    → Tài xế xác nhận nhận hàng → Xe rời kho [In-Transit] → Hệ thống trừ tồn kho
    → Tài xế giao hàng → Đại lý ký POD [Delivered]
    → Kế toán viên lập Hóa đơn (Invoice) → Cộng công nợ Đại lý [Completed]
    → Đại lý thanh toán → Kế toán viên tạo Phiếu thu → Cấn trừ công nợ [Closed]
```

### Quy trình Điều chuyển Kho Nội bộ (Internal Transfer)

```
Planner xem Planning Dashboard → Nhận gợi ý điều chuyển
    → Planner tạo Phiếu điều chuyển [Mới]
    → Trưởng kho nguồn kiểm tra tồn khả dụng → Duyệt [Đã duyệt]
    → Thủ kho nguồn xuất hàng lên xe nội bộ
        → Hệ thống: Trừ tồn Kho nguồn, Cộng Kho ảo In-Transit [Đang vận chuyển]
    → Trưởng kho đích nhận hàng, kiểm tra số lượng thực tế
        ├── Khớp → Hệ thống: Trừ In-Transit, Cộng tồn Kho đích [Hoàn thành]
        └── Lệch → Ghi lý do + Tạo Phiếu điều chỉnh bù trừ
```

### Quy trình Công nợ & Kế toán (Finance Cycle)

```
[Phát sinh nợ]
Giao hàng Delivered → Kế toán lập Invoice (Net 30/60) → current_balance += giá trị đơn
    → IF current_balance >= credit_limit → CREDIT_HOLD (chặn đơn mới)
    → Daily Job: IF invoice quá hạn > 30 ngày → CREDIT_HOLD + cảnh báo Kế toán trưởng

[Thu nợ]
Đại lý trả tiền → Kế toán tạo Phiếu thu → current_balance -= số tiền thu
    → IF current_balance < credit_limit * 0.8 → ACTIVE (mở khóa)
    → IF current_balance >= credit_limit * 0.8 → Vẫn CREDIT_HOLD, thông báo số tiền cần trả thêm

[Chốt sổ - Cuối tháng]
Kế toán trưởng kiểm tra điều kiện → Chốt sổ kỳ T → CLOSED
    → Khóa cứng mọi chứng từ có transaction_date trong kỳ T
    → Chứng từ trễ hạn → Hạch toán vào kỳ hiện tại (document_date giữ nguyên)
    → Sai sót phát hiện sau → Tạo Adjustment Voucher tại kỳ hiện tại (có link tham chiếu)
```

---

## LUỒNG TRẠNG THÁI ĐƠN HÀNG XUẤT

| Trạng thái                            | Mô tả                                                        | Người chuyển                |
| :------------------------------------ | :----------------------------------------------------------- | :-------------------------- |
| **Mới (New)**                         | Planner vừa tạo, chưa xử lý                                  | Planner                     |
| **Đang soạn hàng (Picking)**          | Thủ kho đang lấy hàng từ Bin                                 | Thủ kho                     |
| **Sẵn sàng xuất (Ready to Ship)**     | Soạn xong, Nhân viên kho đã xác nhận đóng gói QC              | Thủ kho (sau khi Nhân viên kho xác nhận QC) |
| **Đang vận chuyển (In-Transit)**      | Tài xế đã nhận hàng, xe rời kho — **Tồn kho bị trừ tại đây** | Tài xế (xác nhận nhận hàng) |
| **Đang giao hàng (Out for Delivery)** | Tài xế đang trên đường đến địa chỉ Đại lý                    | Tài xế                      |
| **Đã giao thành công (Delivered)**    | Đại lý đã ký POD — Kế toán nhận thông báo lập hóa đơn        | Tài xế (xác nhận POD)       |
| **Giao thất bại (Returned)**          | Không giao được — Tạo phiếu nhập hàng hoàn Quarantine        | Tài xế (ghi lý do)          |
| **Đã hoàn thành (Completed)**         | Kế toán đã lập hóa đơn — Chờ thu tiền                        | Kế toán viên                |
| **Đã đóng (Closed)**                  | Hóa đơn đã được thanh toán đầy đủ                            | Hệ thống (sau Phiếu thu)    |
| **Đã hủy (Cancelled)**                | Đơn bị hủy — ghi rõ lý do                                    | Planner / Trưởng kho        |

---

## CƠ CHẾ CREDIT CHECK (KIỂM SOÁT CÔNG NỢ)

**Điều kiện khóa tín dụng (CREDIT_HOLD) — kích hoạt khi vi phạm BẤT KỲ điều kiện nào:**

1. `current_balance + giá_trị_đơn_mới >= credit_limit` _(kiểm tra khi Planner tạo đơn)_
2. `current_balance >= credit_limit` _(kiểm tra sau khi lập hóa đơn)_
3. Đại lý có ít nhất 1 hóa đơn quá ngày Hạn thanh toán > 30 ngày _(Daily Job)_

**Điều kiện mở khóa (ACTIVE):**

- `current_balance < credit_limit × 0.8` _(buffer 20%)_
- Kiểm tra sau mỗi lần Kế toán viên ghi nhận thanh toán.

---

## BẢNG ĐỊNH MỨC PHÊ DUYỆT (ĐIỀU CHỈNH TỒN KHO & HỦY HÀNG)

| Giá trị lệch / Giá trị hủy           | Người duyệt             | Xử lý                                              |
| :----------------------------------- | :---------------------- | :------------------------------------------------- |
| 5 – 100 triệu VNĐ                     | Trưởng kho              | Trưởng kho nhận thông báo, duyệt trên hệ thống     |
| >100 triệu VNĐ hoặc lỗi do nhân viên | CEO                     | CEO nhận thông báo, duyệt/từ chối trên hệ thống    |

_Lưu ý: Bảng định mức này áp dụng cho Điều chỉnh kiểm kê và Phiếu xuất hủy hàng lỗi. KHÔNG áp dụng cho vận chuyển (hệ thống dùng xe nội bộ)._

---

## AUDIT LOG — CẤU TRÚC BẮT BUỘC

Mọi thao tác trên dữ liệu nghiệp vụ phải ghi Audit Log với đầy đủ thông tin:

| Trường        | Ý nghĩa                    | Ví dụ                                  |
| :------------ | :------------------------- | :------------------------------------- |
| `timestamp`   | Thời gian chính xác đến ms | `2026-05-27 14:30:05.123`              |
| `actor_id`    | ID người thực hiện         | `ketoan_01`                            |
| `actor_role`  | Vai trò người thực hiện    | `Kế toán viên`                         |
| `action`      | Hành động                  | `Tạo mới / Cập nhật / Phê duyệt / Hủy` |
| `entity_type` | Loại đối tượng             | `Phiếu xuất kho / Hóa đơn / Phiếu thu` |
| `entity_id`   | Mã đối tượng               | `DO-2026-001 / INV-2026-001`           |
| `old_value`   | Giá trị trước thay đổi     | `{quantity: 100}`                      |
| `new_value`   | Giá trị sau thay đổi       | `{quantity: 90}`                       |
