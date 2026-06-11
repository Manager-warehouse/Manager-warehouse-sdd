# USER STORIES - HỆ THỐNG QUẢN LÝ KHO (WMS) - CÔNG TY PHÚC ANH
# Phiên bản: 2.0 | Cập nhật: 2026-05-27
# Ghi chú: Hệ thống sử dụng toàn bộ XE NỘI BỘ của Phúc Anh — KHÔNG phát sinh chi phí 3PL, KHÔNG có luồng Duyệt chi vận tải.

---

## NHÓM 1: QUẢN TRỊ HỆ THỐNG & CẤU HÌNH (ADMIN & CEO)

### US-WMS-01: Cấu hình Tham số Hệ thống (Priority: P1)

**Mô tả:** Là System Admin / CEO, tôi muốn thiết lập các tham số vận hành để hệ thống áp dụng các quy tắc kinh doanh thống nhất.

**Tiêu chí nghiệm thu:**

1. Admin cấu hình được các tham số hệ thống: Hạn mức công nợ mặc định cho Đại lý, Tồn kho tối thiểu theo SKU/Kho, Kỳ hạn thanh toán mặc định (Net 30 / Net 60), Ngày khóa kỳ kế toán hàng tháng, và Ngưỡng cảnh báo tồn kho tối thiểu mặc định.

2. Mọi thao tác thay đổi tham số phải ghi Audit Log đầy đủ: ai sửa, giá trị cũ, giá trị mới, thời điểm.

---

## NHÓM 2: QUY TRÌNH NHẬP HÀNG (INBOUND & QC)

### US-WMS-02: Tiếp nhận thông tin & Lập Lệnh nhập kho (Priority: P1)

**Mô tả:** Là Planner, tôi muốn tiếp nhận thông tin hàng hóa từ Công ty mẹ (qua Zalo/Email) và lập Lệnh nhập kho thủ công vào hệ thống.

**Tiêu chí nghiệm thu:**

1. Planner nhập: Mã đơn hàng nguồn, danh mục sản phẩm (SKU + số lượng dự kiến), kho nhận, người liên hệ và nguồn tiếp nhận (Zalo/Email).
2. Hệ thống tự động sinh Mã Lệnh nhập duy nhất và ghi Log: người tạo, thời gian, nguồn tiếp nhận.
3. Trạng thái Lệnh nhập ban đầu: **Pending Receipt (Chờ tiếp nhận hàng thực tế)**.

---

### US-WMS-03: Kiểm hàng thực tế & Kiểm QC Inbound (Priority: P1)

**Mô tả:** Là Thủ kho kiêm QC, tôi muốn đếm số lượng thực tế và kiểm tra chất lượng hàng nhập để phân loại Đạt/Lỗi vào hệ thống.

**Tiêu chí nghiệm thu:**

1. Thủ kho đếm hàng thực tế và nhập số lượng vào phiếu nhập nháp trên hệ thống.
2. Thủ kho kiểm tra ngoại quan từng lô hàng (kiểm QC) và nhập kết quả: **Đạt** hoặc **Lỗi** kèm lý do chi tiết.
3. Hàng **Đạt** → Thủ kho chỉ định vị trí kệ (Bin Location); Nhân viên kho hỗ trợ cất hàng theo chỉ dẫn. Hệ thống kiểm tra sức chứa Bin trước khi cho phép.
4. Hàng **Lỗi** → Hệ thống **bắt buộc** tự động chuyển sang **Kho cách ly (Quarantine Zone)**, không tính vào tồn kho khả dụng, không được phép xuất bán.

---

### US-WMS-04: Phê duyệt hàng lỗi & Ra quyết định xử lý (Priority: P1)

**Mô tả:** Là Trưởng kho (Checker), tôi muốn phê duyệt biên bản xử lý hàng lỗi trong Quarantine Zone và ra quyết định Tiêu hủy hoặc Trả về Nhà cung cấp.

**Tiêu chí nghiệm thu:**

1. Trưởng kho xem danh sách hàng lỗi đang chờ xử lý trong Quarantine Zone và phê duyệt biên bản xử lý.
2. **Nếu chọn Trả về NCC (Return to Vendor — RTV):**
   - Hệ thống tạo **Phiếu trả hàng NCC** kèm lý do lỗi chi tiết và tự tạo **Debit Note**; số lượng hàng trong Quarantine Zone chưa bị trừ tại bước này.
   - Thủ kho đóng gói hàng lỗi và xác nhận đã giao trả **toàn bộ** số lượng Quarantine của receipt cho NCC trên hệ thống.
   - Khi Thủ kho xác nhận trả đủ, hệ thống mới trừ tồn Quarantine. Nếu xác nhận thiếu hoặc dư, hệ thống từ chối và không trừ tồn.
   - **Kế toán viên** theo dõi **Debit Note** (Phiếu đòi bồi hoàn NCC): ghi rõ số lượng, giá trị hàng lỗi, yêu cầu NCC hoàn tiền hoặc giao hàng thay thế.
   - Debit Note được lưu vào hồ sơ giao dịch với NCC, làm căn cứ theo dõi và đối chiếu khi NCC phản hồi.
3. **Nếu chọn Tiêu hủy:** Hệ thống tự động tạo Phiếu xuất hủy và được phê duyệt bởi Trưởng kho.

**Ghi chú mapping Sprint 1:** Trong Spec 003 inbound receipt QC, màn hình xử lý Quarantine chỉ hiển thị nút **Trả NCC**. Hệ thống tự tạo Debit Note khi Trưởng kho tạo RTV request; Thủ kho xác nhận đã giao trả đủ toàn bộ số lượng Quarantine thì hệ thống mới trừ tồn Quarantine. Luồng **Tiêu hủy** được tách sang Spec 009 để áp dụng approval thresholds riêng.
   
---

### US-WMS-05: Ký duyệt Nhập kho chính thức (Priority: P1)

**Mô tả:** Là Trưởng kho (Checker), tôi muốn đối chiếu biên bản QC và phê duyệt Phiếu nhập kho để mở khóa putaway; tồn kho chỉ tăng sau khi Thủ kho cất hàng vào Bin.

**Tiêu chí nghiệm thu:**

1. Trưởng kho đối chiếu số lượng thực tế (do Thủ kho nhập) với kết quả QC Đạt → Nhấn "Duyệt nhập".
2. Hệ thống chuyển phiếu sang `APPROVED`, tạo/resolved batch và mở khóa putaway nhưng **chưa cộng tồn kho khả dụng**.
3. Khi Thủ kho hoàn tất putaway vào Bin thường, hệ thống mới cộng số lượng đã duyệt vào tồn kho.
4. Nếu Trưởng kho từ chối phiếu `QC_COMPLETED`, hệ thống chuyển sang `RETURN_TO_SUPPLIER_PENDING`; khi xe NCC tới lấy, Thủ kho xác nhận bàn giao và phiếu chuyển sang `RETURNED_TO_SUPPLIER`.
5. Hệ thống ghi Audit Log đầy đủ cho duyệt/từ chối, putaway và xác nhận bàn giao trả NCC.

---

## NHÓM 3: XUẤT HÀNG & GIAO HÀNG (OUTBOUND & DELIVERY)

### US-WMS-06: Tiếp nhận yêu cầu & Lập Đơn xuất hàng (Priority: P1)

**Mô tả:** Là Planner, tôi muốn tiếp nhận yêu cầu xuất hàng từ Công ty mẹ, kiểm tra tồn kho và công nợ Đại lý để lập Đơn xuất hàng (Delivery Order).

**Tiêu chí nghiệm thu:**

1. Planner nhập: Đại lý nhận hàng, mã sản phẩm, số lượng xuất, kho xuất.
2. **Kiểm tra công nợ (Credit Check — bắt buộc, tự động):** Hệ thống kiểm tra TRƯỚC KHI cho phép tạo đơn:
   - **Điều kiện khóa (HOẶC):**
     - `Công nợ hiện tại + Giá trị đơn mới > Hạn mức tín dụng` (bằng hạn mức vẫn cho phép tạo đơn)
     - `Đại lý có ít nhất 1 hóa đơn quá hạn > 30 ngày`
   - Nếu vi phạm → Hệ thống **chặn cứng**, hiển thị popup cảnh báo rõ lý do (công nợ hiện tại, hạn mức, số tiền vượt), không cho tạo đơn.
3. **Kiểm tra tồn kho khả dụng (Available Quantity):** Hệ thống tính:
   - `available_qty = total_qty - reserved_qty`
   - Nếu `available_qty < số_lượng_yêu_cầu` → Hiển thị cảnh báo thiếu hàng và gợi ý kho khác còn hàng.
4. Đơn hàng tạo thành công → Trạng thái: **Mới (New)**.
5. **Hệ thống tự động giữ chỗ (Reserve):** Ngay khi đơn được tạo thành công:
   - `reserved_qty += số_lượng_đơn` tại kho xuất → Ngăn Planner khác oversell cùng lô hàng.
   - Tồn kho khả dụng hiển thị cho các đơn tiếp theo đã được trừ phần đã giữ chỗ.
6. **Giải phóng Reserved Quantity** trong các trường hợp:
   - Đơn chuyển sang **In-Transit** → Trừ thẳng vào `total_qty`, giải phóng `reserved_qty`.
   - Đơn bị **Hủy (Cancelled)** → Giải phóng `reserved_qty` ngay lập tức.

---

### US-WMS-07: Soạn hàng & Kiểm QC đóng gói (Priority: P1)

**Mô tả:** Là Thủ kho kiêm QC, tôi muốn soạn hàng từ vị trí kệ và kiểm tra chất lượng đóng gói QC trước khi xuất kho.

**Tiêu chí nghiệm thu:**

1. Thủ kho nhận lệnh xuất → Đi lấy hàng từ các Bin Location → Cập nhật trạng thái đơn: **Đang soạn hàng (Picking)**.
2. Thủ kho kiểm tra QC: đúng SKU, đúng số lượng, đóng thùng chống sốc → Xác nhận đạt trên hệ thống.
3. Thủ kho xác nhận hoàn tất soạn hàng sau khi QC đạt → Trạng thái đơn: **Sẵn sàng xuất (Ready to Ship)**.

---

### US-WMS-08: Lập Chuyến xe & Điều phối Vận tải Nội bộ (Priority: P1)

**Mô tả:** Là Dispatcher, tôi muốn lập chuyến xe từ đội xe nội bộ Phúc Anh, gom các đơn hàng và gán Tài xế để tối ưu hóa tải trọng giao hàng.

**Tiêu chí nghiệm thu:**

1. Dispatcher tạo Chuyến xe (Trip Log) mới: Chọn xe nội bộ (từ danh mục xe Phúc Anh), gán Tài xế, thiết lập ngày giao dự kiến.
2. Gom nhiều Đơn xuất hàng (Delivery Orders) ở trạng thái **Ready to Ship** vào một Chuyến xe; sắp xếp thứ tự giao hàng (Stop Order).
3. Hệ thống kiểm tra tải trọng xe: Nếu tổng khối lượng/thể tích hàng vượt tải trọng xe → Cảnh báo Dispatcher.
4. Tài xế xác nhận nhận hàng, xe rời kho → Trạng thái Chuyến xe: **Đang vận chuyển (In-Transit)** → Hệ thống **tự động trừ tồn kho** tại thời điểm này.

---

### US-WMS-09: Giao diện Web di động cho Tài xế & POD thời gian thực (Priority: P1)

**Mô tả:** Là Tài xế, tôi muốn đăng nhập bằng smartphone vào giao diện Web Responsive của WMS để xem lộ trình giao hàng và ký nhận POD trực tiếp tại điểm giao.

**Tiêu chí nghiệm thu:**

1. Tài xế đăng nhập bằng tài khoản riêng → Xem danh sách đơn hàng cần giao trong chuyến xe của mình.
2. Tại điểm giao: Đại lý ký tên trực tiếp trên màn hình cảm ứng; Tài xế chụp ảnh hàng hóa bàn giao → Hệ thống lưu POD gồm: Hình ảnh + Chữ ký + Timestamp.
3. Tài xế nhấn "Xác nhận đã giao" → Trạng thái đơn: **Đã giao thành công (Delivered)**.
4. Nếu giao thất bại (đại lý vắng mặt, từ chối nhận) → Tài xế chọn lý do → Trạng thái đơn: **Giao thất bại (Returned)** → Hệ thống tự động tạo Phiếu nhập hàng hoàn vào Kho cách ly chờ xử lý.

---

### US-WMS-10: Lập Hóa đơn bán hàng & Ghi nhận Công nợ (Priority: P1)

**Mô tả:** Là Kế toán viên, sau khi đơn hàng chuyển sang trạng thái Delivered, tôi muốn lập Hóa đơn bán hàng kèm kỳ hạn thanh toán để hệ thống theo dõi và tự động cảnh báo nợ quá hạn.

**Tiêu chí nghiệm thu:**

1. Kế toán viên nhận thông báo hệ thống: "Đơn hàng #DO-xxx đã giao thành công" → Tạo Hóa đơn bán hàng (Invoice) gồm:
   - Mã hóa đơn (tự động sinh), Đại lý, Tổng giá trị (tính theo bảng giá hiệu lực tại ngày giao), Ngày xuất hóa đơn, **Hạn thanh toán** (Net 30 hoặc Net 60 theo hồ sơ Đại lý).
   - Trạng thái hóa đơn ban đầu: **Chưa thanh toán (Unpaid)**.
2. **Hệ thống tự động** sau khi hóa đơn được tạo:
   - Cộng dồn giá trị hóa đơn vào `current_balance` của Đại lý.
   - Kiểm tra: `IF current_balance > credit_limit THEN customer.status = 'CREDIT_HOLD'`; nếu `current_balance = credit_limit` thì vẫn cho phép theo hạn mức.
3. **Cảnh báo tự động quá hạn (Daily Job):** Hệ thống quét hàng ngày — nếu Hóa đơn đã quá ngày **Hạn thanh toán** mà chưa được thanh toán → Tự động gán `CREDIT_HOLD` cho Đại lý và bắn thông báo đến Kế toán trưởng.
4. Đơn hàng chuyển trạng thái: **Đã hoàn thành (Completed)** — chờ thu tiền.

---

## NHÓM 4: ĐIỀU CHUYỂN NỘI BỘ (REPLENISHMENT & TRANSFER)

### US-WMS-11: Planning Dashboard & Gợi ý điều chuyển kho tự động (Priority: P2)

**Mô tả:** Là Planner, tôi muốn sử dụng Planning Dashboard để hệ thống tự động gợi ý các lệnh điều chuyển hàng hóa tối ưu giữa 3 miền nhằm tránh đứt gãy nguồn cung.

**Tiêu chí nghiệm thu:**

1. Planner truy cập màn hình "Planning Dashboard" → Nhấn "Quét gợi ý" hoặc hệ thống chạy Batch Job định kỳ.
2. Hệ thống so sánh tồn kho khả dụng hiện tại với định mức tồn tối thiểu đã cấu hình tại 3 kho: Hải Phòng, Hà Nội, TP.HCM.
3. Hiển thị danh sách đề xuất điều chuyển hợp lý kèm SKU, kho nguồn, kho đích, số lượng gợi ý, mức ưu tiên và lý do (Ví dụ: Kho HCM hết SP-001 → Gợi ý điều chuyển 200 cái từ Kho Hà Nội đang dư).
4. Planner có thể nhấn "Tạo nhanh Phiếu điều chuyển" trực tiếp từ gợi ý.

---

### US-WMS-12: Lập, Duyệt & Xác nhận Phiếu Điều chuyển Kho Nội bộ (Priority: P1)

**Mô tả:** Là Planner / Trưởng kho, tôi muốn tạo và thực thi phiếu điều chuyển hàng hóa giữa 3 kho vật lý để cân bằng tồn kho giữa các miền.

**Tiêu chí nghiệm thu:**

1. Planner tạo Phiếu điều chuyển: Chọn kho nguồn, kho đích, SKU, số lượng → Trạng thái: **Mới**.
2. **Trưởng kho nguồn (Checker)** kiểm tra tồn kho khả dụng:
   - Nếu đủ hàng → Phê duyệt và hệ thống khóa/giữ chỗ số lượng điều chuyển ngay → Trạng thái: **Đã duyệt**.
   - Nếu không đủ → Hệ thống từ chối, hiển thị lý do rõ ràng.
3. Dispatcher lập một chuyến xe nội bộ riêng cho phiếu điều chuyển: gán xe, tài xế và ngày vận chuyển.
4. Thủ kho kho nguồn ghi nhận số lượng xuất và bốc xếp lên xe; Tài xế xác nhận đã nhận hàng và xe rời kho → Hệ thống **trừ tồn kho nguồn, giải phóng giữ chỗ, cộng vào Kho ảo In-Transit** → Trạng thái: **Đang vận chuyển (In-Transit)**.
5. Thủ kho kho đích nhập số lượng thực nhận và kiểm QC số lượng/chất lượng; Trưởng kho đích xác nhận cuối cùng:
   - Nếu khớp và QC đạt → Hệ thống **trừ Kho ảo In-Transit, cộng vào kho đích** → Trạng thái: **Hoàn thành**.
   - Nếu thiếu → Hệ thống **bắt buộc** ghi lý do chênh lệch và tự động tạo Phiếu điều chỉnh bù trừ.
   - Nếu nhận thừa (`received_qty > sent_qty`) → Hệ thống chặn, không cho xác nhận.
   - Nếu QC lỗi → Phần lỗi được đưa vào Quarantine Zone, không tính vào tồn kho khả dụng.
6. Hệ thống không hỗ trợ hủy phiếu điều chuyển sau khi trạng thái đã là **Đang vận chuyển (In-Transit)**.

---

## NHÓM 5: KIỂM KÊ & QUẢN LÝ GIÁ (INVENTORY, PRICE & AUDIT)

### US-WMS-13: Kiểm kê kho định kỳ & Điều chỉnh chênh lệch (Priority: P1)

**Mô tả:** Là Thủ kho và Trưởng kho, chúng tôi muốn lập phiếu kiểm kê định kỳ và điều chỉnh số dư tồn kho trên hệ thống khi có chênh lệch thực đếm.

**Tiêu chí nghiệm thu:**

1. Thủ kho tạo Phiếu kiểm kê, khóa sổ kiểm kê tạm thời → Thực hiện đếm thực tế và nhập số lượng.
2. Hệ thống tự động tính toán chênh lệch: `Variance = Thực tế - Hệ thống`.
3. Mọi chênh lệch kiểm kê sau khi xác nhận sẽ được Trưởng kho phê duyệt để điều chỉnh tồn kho chính thức.
4. Sau khi được duyệt → Hệ thống cập nhật tồn kho và ghi Audit Log đầy đủ.

---

### US-WMS-14: Thiết lập Bảng giá & Lịch sử biến động giá (Priority: P1)

**Mô tả:** Là Kế toán viên và Kế toán trưởng, chúng tôi muốn nhập, duyệt bảng giá theo kỳ và lưu vết lịch sử thay đổi giá để tính COGS chính xác.

**Tiêu chí nghiệm thu:**

1. Kế toán viên (Maker) tạo Bảng giá mới: Tên bảng giá, **Ngày hiệu lực (effective_date)**, **Ngày hết hạn (end_date)**, nhập Giá vốn và Giá bán cho từng SKU. Hỗ trợ Import từ file Excel theo mẫu.
2. Kế toán viên bấm "Gửi duyệt" → Trạng thái bảng giá: **Chờ duyệt (Pending)**.
3. Kế toán trưởng (Checker) xem chi tiết, so sánh với Bảng giá cũ → Bấm "Phê duyệt" → Trạng thái: **Đã duyệt (Approved)**.
4. Hệ thống **bắt buộc** lưu lịch sử vào bảng `price_history` với đầy đủ: `product_id`, `effective_date`, `end_date`, `cost_price`, `selling_price`, `created_by`, `approved_by`, `approved_at`.
5. Khi tính toán giá trị Đơn xuất hoặc COGS, hệ thống **tra cứu** giá vốn đúng tại ngày xuất hàng từ `price_history`.

---

## NHÓM 6: TÀI CHÍNH & KỲ KẾ TOÁN (FINANCE & CLOSING)

### US-WMS-15: Ghi nhận Thanh toán & Quản lý Vòng đời Công nợ Đại lý (Priority: P1)

**Mô tả:** Là Kế toán viên, tôi muốn ghi nhận các khoản thanh toán từ Đại lý để cấn trừ công nợ và tự động mở khóa hạn mức tín dụng cho các đơn hàng tiếp theo.

**Tiêu chí nghiệm thu:**

1. Kế toán viên tạo Phiếu thu tiền (Payment Receipt): Chọn Đại lý, nhập số tiền đã thu, ngày thu, phương thức thanh toán (Chuyển khoản / Tiền mặt), và **chọn Hóa đơn tương ứng** cần cấn trừ.
2. Hệ thống tự động:
   - Cập nhật trạng thái Hóa đơn: **Đã thanh toán (Paid)**.
   - Trừ công nợ: `current_balance = current_balance - số_tiền_thu`.
   - Kiểm tra mở khóa: `IF current_balance < credit_limit * 0.8 THEN customer.status = 'ACTIVE'` (buffer 20% để tránh dao động).
3. Nếu sau khi cấn trừ `current_balance >= credit_limit * 0.8` → Đại lý **vẫn bị giữ CREDIT_HOLD**. Hệ thống thông báo số tiền cần trả thêm để mở khóa.
4. Đơn hàng gốc chuyển trạng thái: **Đã đóng (Closed)** khi Hóa đơn được thanh toán đầy đủ.

---

### US-WMS-16: Báo cáo Công nợ Phân kỳ (Aging Report) (Priority: P1)

**Mô tả:** Là Kế toán trưởng, tôi muốn xem báo cáo phân kỳ công nợ của toàn bộ Đại lý để đánh giá rủi ro tín dụng và đưa ra biện pháp xử lý kịp thời.

**Tiêu chí nghiệm thu:**

1. Kế toán trưởng xem báo cáo Aging Report phân loại công nợ theo nhóm:
   - Nợ trong hạn (chưa đến hạn thanh toán).
   - Nợ quá hạn 1 – 30 ngày.
   - Nợ quá hạn 31 – 60 ngày.
   - Nợ quá hạn > 60 ngày.
2. Báo cáo hiển thị: Tên Đại lý, Số lượng hóa đơn chưa thanh toán, Tổng dư nợ, Số ngày nợ quá hạn lớn nhất.
3. Kế toán trưởng có thể lọc theo kho, theo khu vực, theo khoảng thời gian và xuất file Excel.

---

### US-WMS-17: Chốt sổ Kế toán & Khóa cứng kỳ quá khứ (Priority: P1)

**Mô tả:** Là Kế toán trưởng, tôi muốn thực hiện chốt sổ định kỳ hàng tháng để khóa cứng toàn bộ dữ liệu kỳ đã qua, ngăn chặn gian lận và đảm bảo báo cáo tài chính không thay đổi sau khi công bố.

**Tiêu chí nghiệm thu:**

1. Kế toán trưởng mở màn hình "Quản lý Kỳ kế toán" → Chọn kỳ cần chốt.
2. **Hệ thống kiểm tra tự động trước khi cho phép chốt sổ:**
   - Còn Phiếu nhập/xuất nào trong kỳ chưa được duyệt không?
   - Còn Hóa đơn nào chưa được lập cho đơn hàng đã Delivered không?
   - Còn chứng từ nào ở trạng thái "Chờ duyệt" không?
   - Nếu có bất kỳ mục nào → Hiển thị cảnh báo danh sách lỗi, **không cho phép chốt sổ**.
3. Nếu hợp lệ → Kế toán trưởng bấm "Chốt sổ" → Hệ thống cập nhật trạng thái kỳ: **CLOSED**.
4. **Khóa cứng:** Không cho phép tạo mới, sửa, xóa bất kỳ chứng từ nào có `transaction_date` trong kỳ đã chốt.
5. **Xử lý chứng từ trễ hạn:** Chứng từ của kỳ đã chốt nhưng nhập muộn → Hệ thống ghi vào kỳ hiện tại (kỳ mở) với `transaction_date` = ngày nhập thực tế, giữ nguyên `document_date` = ngày nghiệp vụ gốc.
6. **Xử lý sai sót:** Không cho phép sửa chứng từ cũ. Tạo **Phiếu điều chỉnh ngược (Adjustment Voucher)** tại kỳ mở hiện tại, ghi rõ lý do và link tham chiếu đến chứng từ cũ.

---

## NHÓM 7: BÁO CÁO & DASHBOARD (REPORTING)

### US-WMS-18: Dashboard Báo cáo Quản trị cấp cao (Priority: P1)

**Mô tả:** Là CEO và Kế toán trưởng, tôi muốn xem Dashboard trực quan về tình hình kho, công nợ và lãi/lỗ để đưa ra quyết định điều tiết kinh doanh kịp thời.

**Tiêu chí nghiệm thu:**

1. Dashboard CEO hiển thị thời gian thực:
   - Tổng giá trị tồn kho theo giá vốn tại 3 kho miền (Inventory Valuation).
   - Top Đại lý nợ nhiều nhất và số ngày quá hạn.
   - Báo cáo Lãi/Lỗ (P&L): Doanh thu - COGS - Chi phí vận hành = Lợi nhuận ròng.
   - Tỷ lệ hàng lỗi QC trong tháng.
   - Hiệu suất giao hàng đúng hạn (On-Time Delivery Rate).
2. Báo cáo Tồn kho cuối kỳ (Inventory Valuation Report) sau khi Chốt sổ: Tổng giá trị tồn kho tại 3 kho theo giá vốn tại thời điểm chốt.
3. Hệ thống ghi log mỗi lần xem báo cáo (thời gian, người xem, bộ lọc) để phục vụ kiểm toán.

---

## NHÓM 8: DANH MỤC NỀN TẢNG (MASTER DATA)

### US-WMS-19: Quản lý Danh mục Sản phẩm & SKU tập trung (Priority: P1)

**Mô tả:** Là Thủ kho, tôi muốn quản lý danh mục sản phẩm để đồng bộ thông tin hàng hóa trên toàn hệ thống 3 kho.

**Tiêu chí nghiệm thu:**

1. Tạo mới, cập nhật sản phẩm với đầy đủ thông số: Mã SKU (duy nhất), Tên sản phẩm, Đơn vị tính, Quy cách đóng gói, Khối lượng (kg), Thể tích (m³).
2. Hỗ trợ quy đổi đơn vị tính (Thùng → Cái) để Thủ kho dễ soạn hàng và kiểm kê.
3. Soft-delete: Vô hiệu hóa SKU (`is_active = false`) → Không cho tạo đơn mới nhưng giữ lịch sử giao dịch cũ.

---

### US-WMS-20: Cấu hình Vị trí kho & Kiểm tra Sức chứa Kệ (Bin Location) (Priority: P2)

**Mô tả:** Là Thủ kho / Trưởng kho, tôi muốn cấu hình sơ đồ kho theo cấu trúc Zone → Bin và thiết lập sức chứa tối đa ở cấp Bin để tối ưu hóa không gian lưu trữ.

**Tiêu chí nghiệm thu:**

1. Thiết lập mã vị trí kệ duy nhất (Ví dụ: WH-HP.A.01.1.01 = Kho Hải Phòng, Khu A, Dãy 1, Kệ 1, Ô 1).
2. Mỗi Bin có thông số sức chứa tối đa: m³ và kg.
3. Khi Thủ kho làm Putaway: Hệ thống tự động tính tổng thể tích/khối lượng hiện tại của Bin. Nếu vượt sức chứa → Chặn và gợi ý các Bin khác còn trống.

---

### US-WMS-21: Phân quyền theo Chi nhánh Kho & Vai trò (RBAC) (Priority: P1)

**Mô tả:** Là System Admin, tôi muốn phân quyền người dùng chi tiết theo Vai trò (Role) và Chi nhánh Kho được gán để bảo mật dữ liệu tuyệt đối.

**Tiêu chí nghiệm thu:**

1. **Phân quyền theo Vai trò:** Nhân viên kho/Thủ kho bị chặn hoàn toàn khỏi các màn hình báo cáo tài chính, giá trị tồn kho, P&L của Kế toán.
2. **Phân quyền theo Chi nhánh:** Nhân viên được gán vào Kho Hải Phòng chỉ nhìn thấy và thao tác dữ liệu của Kho Hải Phòng; không thể xem hoặc can thiệp vào Kho Hà Nội hay TP.HCM.
3. System Admin gán và thu hồi quyền bất kỳ lúc nào; mọi thay đổi phân quyền phải ghi Audit Log.

---

### US-WMS-22: Quản lý Danh mục Đối tác (Đại lý & Nhà cung cấp) (Priority: P1)

**Mô tả:** Là Kế toán viên / Kế toán trưởng, tôi muốn quản lý danh mục Đại lý và Nhà cung cấp làm nền tảng cho toàn bộ các nghiệp vụ xuất/nhập kho.

**Tiêu chí nghiệm thu:**

1. **Đại lý:** Tạo mới, cập nhật hồ sơ: Tên, SĐT, Địa chỉ giao hàng mặc định, Khu vực phụ trách, Kỳ hạn thanh toán mặc định (Net 30 / Net 60).
   - **Kế toán trưởng** là người duy nhất có quyền thiết lập và điều chỉnh **Hạn mức tín dụng tối đa (Credit Limit)** cho từng Đại lý.
2. **Nhà cung cấp (NCC):** Kế toán viên tạo mới, cập nhật hồ sơ: Tên công ty, Mã số thuế, SĐT, Người liên hệ, Địa chỉ.
3. Soft-delete: Vô hiệu hóa Đại lý/NCC (`is_active = false`) → Không cho tạo đơn mới nhưng giữ toàn bộ lịch sử giao dịch cũ.

---

### US-WMS-23: Quản lý Danh mục Xe tải & Tài xế Nội bộ (Priority: P2)

**Mô tả:** Là Dispatcher, tôi muốn quản lý danh mục phương tiện vận chuyển nội bộ và tài xế của Phúc Anh để phục vụ lên lịch chuyến xe.

**Tiêu chí nghiệm thu:**

1. Lưu trữ thông tin xe: Biển số xe, Loại xe, Tải trọng tối đa (kg), Thể tích thùng xe (m³).
2. Lưu trữ thông tin Tài xế: Họ tên, SĐT, Số giấy phép lái xe, ngày hết hạn GPLX.
3. Cập nhật và hiển thị trạng thái phương tiện (Rảnh / Đang đi chuyến / Bảo trì) và tài xế (Rảnh / Đang đi chuyến / Không khả dụng) để tránh gán trùng lịch khi Dispatcher lập Chuyến xe.

---

## NHÓM 9: QUY TRÌNH PHỤ TRỢ (SUPPORTING PROCESSES)

### US-WMS-24: Xử lý hàng hoàn trả từ Đại lý (Inbound Returns) (Priority: P2)

**Mô tả:** Là Thủ kho và Kế toán viên, chúng tôi muốn tiếp nhận, kiểm QC hàng hoàn trả và ghi nhận bù trừ công nợ (Credit Note) cho Đại lý.

**Tiêu chí nghiệm thu:**

1. Thủ kho lập Phiếu nhập hàng hoàn từ Đại lý, ghi rõ lý do hoàn trả.
2. Thủ kho kiểm tra QC: Hàng còn tốt → Nhập lại kho thường; Hàng lỗi/hư hỏng → Vào Quarantine Zone chờ Trưởng kho quyết định.
3. Kế toán viên ghi nhận giá trị hàng hoàn → Tạo **Credit Note** → Hệ thống tự động trừ `current_balance` của Đại lý tương ứng.

---

### US-WMS-25: Báo cáo Năng suất & Sản lượng Nhân viên Kho (Priority: P3)

**Mô tả:** Là Trưởng kho, tôi muốn xuất báo cáo năng suất làm việc của từng nhân viên để gửi bộ phận HRM bên ngoài làm căn cứ tính lương sản phẩm.

**Tiêu chí nghiệm thu:**

1. Hệ thống ghi nhận sản lượng thực hiện của từng cá nhân: Số đơn bốc xếp/di chuyển hàng (Nhân viên kho), Số đơn soạn hàng và QC hoàn thành (Thủ kho), Số chuyến giao (Tài xế).
2. Trưởng kho xuất file Excel báo cáo sản lượng theo khoảng thời gian tùy chọn.

---

### US-WMS-26: Cảnh báo tự động Tồn kho dưới định mức (Alerting) (Priority: P1)

**Mô tả:** Là Trưởng kho / Planner, tôi muốn nhận cảnh báo tức thời khi tồn kho khả dụng của sản phẩm chạm hoặc giảm xuống dưới mức tối thiểu.

**Tiêu chí nghiệm thu:**

1. Khi tồn kho khả dụng tại một kho cụ thể < Định mức tối thiểu đã cấu hình → Hệ thống tự động bắn thông báo in-app (High Priority) đến Trưởng kho kho đó và Planner.
2. Sản phẩm bị thiếu hụt được đánh dấu đỏ nổi bật trên Dashboard và Planning Dashboard.

---

*Tổng cộng: 26 User Stories bao phủ toàn bộ luồng vận hành WMS của Công ty Phúc Anh.*
*Ghi chú quan trọng: Hệ thống KHÔNG có quản lý sản xuất (Manufacturing), KHÔNG có HR/HRM, KHÔNG có Barcode/QR Scanner, KHÔNG có cổng B2B/B2C, SỬ DỤNG XE NỘI BỘ (không có chi phí 3PL trong luồng xuất hàng thông thường).*
