# KIẾN TRÚC PHÂN TẦNG CÁC ACTORS - HỆ THỐNG WMS PHÚC ANH

# Phiên bản: 2.1 | Cập nhật: 2026-07-15

# Ghi chú: Hệ thống dùng xe nội bộ Phúc Anh. KHÔNG có quản lý sản xuất, HRM, Barcode/QR, cổng B2B/B2C. Nguồn nghiệp vụ chuẩn là `.sdd/specs/001`–`010`; Spec 011–012 bổ sung vai trò chất lượng kỹ thuật, không thay đổi RBAC vận hành 10 actor.

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
| **Tầng 2: Quản lý **          | Trưởng kho                   | Checker                                                         | Phê duyệt phiếu nhập/xuất/điều chuyển, xử lý chênh lệch kiểm kê; Phê duyệt biên bản xử lý hàng lỗi, quyết định tiêu hủy/trả NCC |
|                               | Kế toán trưởng               | Checker                                                         | Duyệt bảng giá, chốt sổ kế toán, xem P&L và Aging Report, thiết lập Credit Limit |
| **Tầng 3: Nghiệp vụ (Maker)** | Planner (Người nhận đơn)     | Maker                                                           | Tiếp nhận yêu cầu xuất/nhập kho từ Công ty mẹ hoặc bên thứ ba, nhập yêu cầu lên hệ thống, nhập phiếu điều chuyển thủ công theo lệnh ngoài |
|                               | Dispatcher (Người điều phối) | Maker                                                           | Lập chuyến xe nội bộ, gán tài xế, tối ưu lộ trình giao hàng                      |
|                               | Thủ kho kiêm QC              | Maker                                                           | Quản lý SKU/danh mục sản phẩm, tiếp nhận hàng thực tế, kiểm QC inbound/outbound, soạn hàng, kiểm kê, cất hàng vào Bin Location |
|                               | Nhân viên kho (Bốc xếp)      | Maker                                                           | Bốc xếp, di chuyển hàng hóa, hỗ trợ Thủ kho cất hàng và chuyển hàng lỗi vào Quarantine theo chỉ dẫn |
|                               | Kế toán viên                 | Maker                                                           | Quản lý hồ sơ Nhà cung cấp, lập hóa đơn, ghi nhận thanh toán, cấn trừ công nợ, nhập bảng giá |
|                               | Tài xế                       | Maker                                                           | Nhận lệnh giao hàng, cập nhật trạng thái, xác nhận POD                           |

---

## NGHIỆP VỤ CHI TIẾT TỪNG ACTOR

### 1. CEO

**Tầng:** Quản trị — Checker cấp cao

**Nghiệp vụ:**

- Xem Dashboard quản trị cấp cao:
  - _"Tồn kho tổng hiện tại là bao nhiêu tiền?"_
  - _"Đại lý nào đang nợ quá hạn và nợ nhiều nhất?"_
  - _"Tỷ lệ hàng lỗi QC tháng này có tăng không?"_
  - _"Hiệu suất giao hàng đúng hạn (On-Time Delivery) có đảm bảo SLA không?"_
- Phê duyệt các thay đổi cấu hình hệ thống quan trọng (thêm kho mới, thay đổi hạn mức công nợ Đại lý VIP).
- Phê duyệt hoặc từ chối yêu cầu điều chuyển liên kho do Trưởng kho đề xuất; CEO approval chỉ tạo cơ sở cho Planner lập `TRF-*`, không giữ chỗ hoặc dịch chuyển inventory.

**User Stories liên quan:** US-WMS-01, US-WMS-11A, US-WMS-18
``
---

### 2. System Admin

**Tầng:** Quản trị — Admin

**Nghiệp vụ:**

- Tạo, vô hiệu hóa tài khoản người dùng.
- Phân quyền theo Vai trò (Role) và Chi nhánh Kho (RBAC): Đảm bảo nhân viên Kho Hải Phòng không xem được dữ liệu Kho Hà Nội; nhân viên kho không xem được báo cáo tài chính của Kế toán.
- Cấu hình tham số hệ thống: Hạn mức công nợ mặc định, Tồn kho tối thiểu mặc định, Kỳ hạn thanh toán mặc định, Ngày khóa kỳ kế toán hàng tháng.

**User Stories liên quan:** US-WMS-01, US-WMS-21

---

### 3. Trưởng kho

**Tầng:** Quản lý — Checker

**Nghiệp vụ:**

- Phê duyệt Phiếu nhập kho sau khi đối chiếu kết quả QC từ Thủ kho → mở khóa putaway; hệ thống chỉ cộng tồn kho sau khi Thủ kho cất hàng vào Bin.
- Khi kho mình thiếu hàng, xem tồn khả dụng liên kho ở chế độ read-only và tạo yêu cầu điều chuyển gửi CEO duyệt.
- Phê duyệt Phiếu điều chuyển kho (kho nguồn): Kiểm tra tồn khả dụng trước khi duyệt.
- Xác nhận nhận hàng điều chuyển (kho đích): Kiểm tra số lượng thực tế, ghi nhận chênh lệch nếu có.
- Với gửi nhầm SKU còn nguyên, Trưởng kho đích duyệt hoặc từ chối xe quay về kho nguồn dựa trên report line-level expected SKU/actual SKU/số lượng/lý do/ảnh nếu có; hàng vẫn ở In-Transit cho tới khi tài xế hoàn tất return departure/source arrival/handover và kho nguồn xác nhận nhận lại.
- Duyệt chênh lệch kiểm kê và phê duyệt điều chỉnh tồn kho thực tế.
- Phê duyệt biên bản hàng lỗi tại Quarantine Zone, quyết định phương án xử lý (tiêu hủy hoặc trả hàng cho nhà cung cấp - NCC).
- Phê duyệt phiếu xuất hủy hàng lỗi.

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

- Nhận lệnh điều chuyển từ Công ty mẹ/bộ phận điều phối trung tâm hoặc yêu cầu điều chuyển đã được CEO duyệt → Tạo Phiếu điều chuyển kho nội bộ thủ công (`TRF-*`) trên màn Điều chuyển nội bộ.

**User Stories liên quan:** US-WMS-02, US-WMS-06, US-WMS-11, US-WMS-12, US-WMS-26

---

### 7. Dispatcher (Người điều phối hàng và vận tải)

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

- Tạo Chuyến xe (Trip Log): Chọn xe nội bộ Phúc Anh từ danh mục, gán Tài xế.
- Gom các Đơn xuất hàng (ở trạng thái Ready to Ship) vào một Chuyến xe; sắp xếp thứ tự giao hàng (Stop Order) để tối ưu lộ trình.
- Lập một chuyến xe nội bộ riêng cho từng Phiếu điều chuyển kho; không gom nhiều Phiếu điều chuyển vào cùng một chuyến trong Sprint 1.
- Kiểm tra tải trọng/thể tích xe trước khi xác nhận chuyến; hệ thống chặn nếu vượt tải.
- Với điều chuyển, tài xế/xe phải thuộc phạm vi kho nguồn, không bị trùng lịch; kiểm tra cân nặng luôn áp dụng và thể tích chỉ áp dụng khi xe có cấu hình thể tích.
- Với điều chuyển, Dispatcher chỉ được đổi xe/tài xế/lịch trước khi tài xế departure; sau departure trip bị khóa.

**Lưu ý quan trọng:** Hệ thống **CHỈ dùng xe nội bộ** của Phúc Anh. KHÔNG phát sinh chi phí vận chuyển 3PL trong luồng xuất hàng thông thường → KHÔNG có quy trình Duyệt chi vận tải.

**User Stories liên quan:** US-WMS-08, US-WMS-23

---

### 8. Thủ kho kiêm QC

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Danh mục sản phẩm:**

- Tạo mới, cập nhật và quản lý SKU/danh mục sản phẩm để đồng bộ thông tin hàng hóa phục vụ nhập, xuất, kiểm kê.
- Quản lý thông tin quy cách đóng gói, đơn vị tính, khối lượng và thể tích của SKU; hàng gia dụng Sprint 1 không quản lý serial từng sản phẩm hoặc hạn sử dụng.

**Nhập hàng:**

- Nhận lệnh nhập từ Planner → Đếm hàng thực tế → Nhập số lượng vào phiếu nhập nháp.
- Kiểm tra QC inbound, nhập kết quả Đạt/Lỗi kèm lý do chi tiết.
- Hướng dẫn Nhân viên kho cất hàng Đạt vào Bin Location hoặc chuyển hàng lỗi vào Quarantine (hệ thống kiểm tra sức chứa trước khi cho phép).

**Xuất hàng:**

- Nhận Đơn xuất → Soạn hàng từ Bin Location → Cập nhật trạng thái Picking.
- Kiểm tra QC outbound, xác nhận đóng gói đạt và hoàn tất soạn hàng → Trạng thái Ready to Ship.

**Điều chuyển:**

- Ở kho nguồn: công nhân/Nhân viên kho nguồn xếp hàng và báo số lượng thực xếp trước; Thủ kho outbound QC bằng mắt/đối chiếu phiếu trên số đã xếp, chụp ảnh xác nhận, QC đạt mới chốt xuất/bàn giao tài xế, QC thất bại thì quay lại công nhân hạ/đổi/xếp lại và báo cáo lại.
- Ở kho đích: kiểm tra lại blind count của công nhân, chốt QC, kiểm tra sức chứa Bin, chọn vị trí nhập kho cho hàng đạt, duyệt receive-check.
- Khi phát hiện wrong-SKU còn nguyên: báo cáo theo line gồm SKU kỳ vọng, SKU thực tế, số lượng ảnh hưởng, lý do và ảnh nếu có.

**Kiểm kê:**

- Tạo Phiếu kiểm kê định kỳ → Đếm thực tế → Nhập số lượng vào hệ thống.

**User Stories liên quan:** US-WMS-03, US-WMS-04, US-WMS-07, US-WMS-12, US-WMS-13, US-WMS-19, US-WMS-20, US-WMS-24, US-WMS-25

---

### 9. Nhân viên kho (Bốc xếp)

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Bốc xếp và di chuyển hàng hóa:**

- Bốc xếp hàng hóa lên/xuống xe tải vận chuyển nội bộ của Phúc Anh.
- Di chuyển hàng đạt QC vào đúng Bin Location theo chỉ dẫn của Thủ kho.
- Di chuyển hàng lỗi vào Quarantine Zone (Khu cách ly) theo chỉ dẫn của Thủ kho.

**User Stories liên quan:** US-WMS-03, US-WMS-07, US-WMS-24, US-WMS-25

---

### 10. Kế toán viên

**Tầng:** Nghiệp vụ — Maker

**Nghiệp vụ:**

**Danh mục Nhà cung cấp:**

- Tạo mới, cập nhật và vô hiệu hóa hồ sơ Nhà cung cấp (NCC) để phục vụ nghiệp vụ nhập hàng, trả hàng NCC và Debit Note.

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

**User Stories liên quan:** US-WMS-04, US-WMS-10, US-WMS-14, US-WMS-15, US-WMS-22, US-WMS-24

---

### 11. Tài xế

**Tầng:** Nghiệp vụ — Maker

**Giao diện:** Web Responsive (Mobile-friendly) — Truy cập bằng smartphone.

**Nghiệp vụ:**

- Đăng nhập → Xem danh sách đơn hàng trong Chuyến xe của mình.
- Tại điểm giao: Đại lý ký tên trực tiếp trên màn hình cảm ứng; Tài xế chụp ảnh hàng hóa bàn giao.
- Nhấn "Xác nhận đã giao" → Hệ thống lưu POD (Hình ảnh + Chữ ký + Timestamp) trên delivery attempt hiện tại, xác thực OTP qua `delivery_otp_attempts` và chuyển trạng thái đơn sang **Delivered**.
- Nếu giao thất bại → Chọn lý do (Đại lý vắng mặt / Từ chối nhận / Sai địa chỉ) → Hệ thống đóng delivery attempt hiện tại là **Failed** và ghi nhận DO **Returned**; hàng vẫn ở Kho ảo In-Transit cho đến khi luồng hoàn hàng riêng tiếp nhận và phân loại.

**User Stories liên quan:** US-WMS-09, US-WMS-25

---

---

## CÁC QUY TRÌNH CHÍNH

### Quy trình Nhập hàng (Inbound)

```
Công ty mẹ gửi thông tin (Zalo/Email)
    → Planner lập Lệnh nhập [Pending Receipt]
    → Thủ kho đếm hàng thực tế và kiểm tra QC → Đạt/Lỗi
        ├── Hàng Lỗi → Quarantine Zone → Trưởng kho tạo RTV [Trả NCC] trong Spec 003 → Thủ kho xác nhận trả đủ NCC rồi mới trừ Quarantine
        └── Hàng Đạt → Trưởng kho Duyệt nhập [Approved] hoặc Từ chối [Return To Supplier Pending]
            └── Nếu bị từ chối → Thủ kho bàn giao xe NCC [Returned To Supplier]
    → Trưởng kho Duyệt nhập [Approved] → mở khóa putaway
    → Thủ kho cất vào Bin Location sau duyệt
    → Hệ thống cộng tồn kho khả dụng sau putaway
```

Luồng tiêu hủy hàng lỗi không nằm trong feature inbound 003; xử lý theo Spec 009.

### Quy trình Xuất hàng & Giao hàng (Outbound & Delivery)

```
Công ty mẹ gửi yêu cầu xuất hàng
    → Planner kiểm tra [Credit Check + Tồn kho]
        ├── Vi phạm Credit → Chặn cứng, hiển thị lý do
        └── Hợp lệ → Lập Đơn xuất [New]
    → Thủ kho soạn hàng [Picking]
    → Thủ kho kiểm tra QC & đóng gói đạt
    → Thủ kho xác nhận xong [Ready to Ship]
    → Dispatcher lập Chuyến xe nội bộ, gán Tài xế
    → Tài xế xác nhận nhận hàng → Xe rời kho [In-Transit] → Hệ thống trừ tồn kho và tạo delivery attempt hiện tại
    → Tài xế giao hàng → Đại lý ký POD + xác thực OTP [Delivered]
    → Kế toán viên xử lý invoice candidates → Lập Hóa đơn (Invoice) → Cộng công nợ Đại lý [Completed]
    → Đại lý thanh toán → Kế toán viên tạo Phiếu thu → Cấn trừ công nợ [Closed]
```

### Quy trình Điều chuyển Kho Nội bộ (Internal Transfer)

```
Trưởng kho kho thiếu hàng có thể xem tồn liên kho read-only
    → Tạo yêu cầu điều chuyển gửi CEO nếu cần
    → CEO duyệt/từ chối; nếu duyệt, Planner kho nguồn/trung tâm nhận mẫu đã duyệt
Planner nhận lệnh điều chuyển ngoài (external instruction code) hoặc transfer request đã được CEO duyệt
    → Planner tạo Phiếu điều chuyển `TRF-*` [Mới] trên màn Điều chuyển nội bộ
    → Trưởng kho nguồn kiểm tra tồn khả dụng FIFO eligible → Duyệt và khóa hàng [Đã duyệt]
    → Dispatcher kho nguồn lập chuyến xe `TTR-*` riêng, gán xe và tài xế thuộc phạm vi kho nguồn, kiểm tra tải trọng/thể tích/trùng lịch
    → Thủ kho nguồn outbound QC bằng mắt/đối chiếu phiếu, chụp ảnh xác nhận, ghi nhận số gửi, bốc xếp lên xe nội bộ và chụp ảnh handover cho tài xế
    → Tài xế xác nhận nhận hàng, xe rời kho
        → Hệ thống: Trừ tồn Kho nguồn, Cộng Kho ảo In-Transit [Đang vận chuyển]
    → Tài xế ghi nhận đến kho nhận và kho nhận ghi handover
    → Công nhân/Nhân viên kho đích blind count số nhận thực tế ban đầu
    → Thủ kho đích kiểm tra lại số lượng, chốt QC, kiểm tra Bin capacity, chọn vị trí nhập kho cho hàng đạt
    → Trưởng kho đích xác nhận cuối cùng
        ├── Khớp + QC đạt → Hệ thống: Trừ In-Transit, Cộng tồn Kho đích [Hoàn thành]
        ├── Thiếu → Ghi lý do + incident/discrepancy + Tạo Phiếu điều chỉnh bù trừ; không tạo Quarantine cho phần thiếu
        ├── QC lỗi/hư hỏng → Phần lỗi vào Quarantine origin INTERNAL_TRANSFER, chỉ đi luồng tiêu hủy Spec 009
        ├── Gửi nhầm SKU còn nguyên → Thủ kho đích báo cáo line expected/actual SKU + số lượng + lý do/ảnh nếu có, Trưởng kho đích duyệt quay về kho nguồn, tài xế ghi return departure/source arrival/handover
        ├── Trip quá hạn → Chặn nhận ở kho đích, kích hoạt Return to Source theo thẩm quyền với lý do, kèm ảnh nếu có
        └── Nhận thừa → Chặn nhập regular inventory và ghi discrepancy hold/incident
```

### Quy trình Công nợ & Kế toán (Finance Cycle)

```
[Phát sinh nợ]
Giao hàng Delivered → Kế toán lấy invoice candidates → Lập Invoice (Net 30/60) → current_balance += giá trị đơn → DO [Completed]
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
| **Sẵn sàng xuất (Ready to Ship)**     | Soạn xong, Thủ kho đã xác nhận đóng gói QC đạt                | Thủ kho |
| **Đang vận chuyển (In-Transit)**      | Tài xế đã nhận hàng, xe rời kho — **Tồn kho bị trừ tại đây** | Tài xế (xác nhận nhận hàng) |
| **Đang giao hàng (Out for Delivery)** | Tài xế đang trên đường đến địa chỉ Đại lý                    | Tài xế                      |
| **Đã giao thành công (Delivered)**    | Đại lý đã ký POD và OTP hợp lệ — Kế toán nhận thông báo lập hóa đơn | Tài xế (xác nhận POD + OTP) |
| **Giao thất bại (Returned)**          | Không giao được — Delivery attempt hiện tại là Failed, hàng vẫn ở In-Transit chờ luồng hoàn hàng riêng | Tài xế (ghi lý do)          |
| **Đã hoàn thành (Completed)**         | Kế toán đã lập hóa đơn — Chờ thu tiền                        | Kế toán viên                |
| **Đã đóng (Closed)**                  | Hóa đơn đã được thanh toán đầy đủ                            | Hệ thống (sau Phiếu thu)    |
| **Đã hủy (Cancelled)**                | Đơn bị hủy — ghi rõ lý do                                    | Planner / Trưởng kho        |

---

## CƠ CHẾ CREDIT CHECK (KIỂM SOÁT CÔNG NỢ)

**Điều kiện khóa tín dụng (CREDIT_HOLD) — kích hoạt khi vi phạm BẤT KỲ điều kiện nào:**

1. `current_balance + giá_trị_đơn_mới > credit_limit` _(kiểm tra khi Planner tạo đơn; bằng hạn mức vẫn cho phép)_
2. `current_balance > credit_limit` _(kiểm tra sau khi lập hóa đơn; bằng hạn mức vẫn cho phép)_
3. Đại lý có ít nhất 1 hóa đơn quá ngày Hạn thanh toán > 30 ngày _(Daily Job)_

**Điều kiện mở khóa (ACTIVE):**

- `current_balance < credit_limit × 0.8` _(buffer 20%)_
- Kiểm tra sau mỗi lần Kế toán viên ghi nhận thanh toán.

---

## PHÊ DUYỆT ĐIỀU CHỈNH TỒN KHO & HỦY HÀNG

Tất cả các phiếu điều chỉnh chênh lệch kiểm kê và phiếu xuất hủy hàng lỗi đều được gửi trực tiếp đến Trưởng kho để phê duyệt mà không phân cấp theo giá trị.

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
