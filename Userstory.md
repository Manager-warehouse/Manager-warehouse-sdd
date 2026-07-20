# USER STORIES - HỆ THỐNG QUẢN LÝ KHO (WMS) - CÔNG TY PHÚC ANH
# Phiên bản: 2.1 | Cập nhật: 2026-07-15
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
   - `available_qty = sum(tồn hợp lệ - reserved tại bin) - reserved cấp kho`
   - Nếu `available_qty < số_lượng_yêu_cầu` → Hiển thị cảnh báo thiếu hàng và gợi ý kho khác còn hàng.
4. Đơn hàng tạo thành công → Trạng thái: **Mới (New)**.
5. **Hệ thống tự động giữ chỗ (Reserve):** Ngay khi đơn được tạo thành công:
   - `reserved_qty += số_lượng_đơn` ở cấp kho/sản phẩm; Thủ kho sẽ gán sang một hoặc nhiều batch/bin cụ thể khi lập kế hoạch lấy hàng.
   - Tồn kho khả dụng hiển thị cho các đơn tiếp theo đã được trừ phần đã giữ chỗ.
6. **Giải phóng Reserved Quantity** trong các trường hợp:
   - Đơn chuyển sang **In-Transit** → Trừ tồn kho kho xuất, giải phóng `reserved_qty`, và cộng số lượng đang đi đường vào Kho ảo In-Transit.
   - Đơn bị **Hủy (Cancelled)** → Giải phóng `reserved_qty` ngay lập tức.

---

### US-WMS-07: Soạn hàng & Kiểm QC đóng gói (Priority: P1)

**Mô tả:** Là Thủ kho, tôi muốn lập kế hoạch lấy hàng từ một hoặc nhiều batch/bin/zone FIFO trong kho được gán; sau đó Nhân viên kho lấy hàng thực tế và kiểm tra chất lượng đóng gói QC trước khi xuất kho.

**Tiêu chí nghiệm thu:**

1. Thủ kho nhận lệnh xuất ở trạng thái **New**, chọn hàng từ một hoặc nhiều batch/bin/zone hợp lệ trong kho được gán theo FIFO; tổng số lượng đã chọn cho từng dòng phải bằng số lượng yêu cầu trên phiếu xuất → Trạng thái đơn: **Chờ lấy hàng (Waiting Picking)**.
2. Nhân viên kho lấy hàng theo kế hoạch và nhập kết quả lấy hàng/QC đúng 1 lần theo từng item/allocation/batch/bin/zone cho toàn bộ kế hoạch hiện tại ngay khi phiếu ở **Waiting Picking**; hệ thống không dùng trạng thái **Picking** riêng → Trạng thái đơn: **QC Pending Approval**, kể cả khi số lượng đạt QC chưa đủ.
3. Nếu sửa kế hoạch sau khi đã có kết quả lấy/QC, hệ thống chỉ yêu cầu xác nhận trả hàng về bin gốc cho allocation đã pick và bị remove/reduce; allocation đã pick nhưng giữ nguyên không cần trả. Mỗi lần trả hàng ghi audit riêng.
4. Nhân viên kho kiểm tra QC: đúng SKU, đúng số lượng, đóng thùng chống sốc. Hàng fail QC bắt buộc chuyển vào Quarantine, tạo phiếu điều chỉnh tồn kho, trừ khỏi tồn kho hợp lệ và xóa khỏi reserved của allocation gốc. Nếu cần hàng thay thế, Thủ kho lập lại kế hoạch từ trạng thái **QC Pending Approval** và phiếu quay về **Waiting Picking** để Nhân viên kho lấy/QC phần thay thế.
5. Thủ kho xác nhận đủ hàng đạt QC → Trạng thái đơn: **QC Completed**.
6. Trưởng kho phê duyệt xuất kho sau QC → Trạng thái đơn: **Warehouse Approved**.

---

### US-WMS-08: Lập Chuyến xe & Điều phối Vận tải Nội bộ (Priority: P1)

**Mô tả:** Là Dispatcher, tôi muốn lập chuyến xe từ đội xe nội bộ Phúc Anh, gom các đơn hàng và gán Tài xế để tối ưu hóa tải trọng giao hàng.

**Tiêu chí nghiệm thu:**

1. Dispatcher tạo Chuyến xe (Trip Log) mới trong kho được gán: chọn xe nội bộ và Tài xế thuộc cùng kho, thiết lập ngày giao dự kiến.
2. Gom ít nhất một Đơn xuất hàng (Delivery Orders) ở trạng thái **Warehouse Approved** và cùng kho vào một Chuyến xe `trip_type = DELIVERY`; mỗi DO chỉ được nằm trong một trip active, sắp xếp thứ tự giao hàng (Stop Order).
3. Hệ thống kiểm tra tải trọng xe: luôn kiểm tra tổng khối lượng; chỉ kiểm tra tổng thể tích khi xe có cấu hình `max_volume_m3`.
4. Dispatcher được sửa xe, tài xế, ngày dự kiến, stop order và danh sách DO, hoặc hủy trip nếu chuyến xe chưa xuất phát; payload sửa danh sách DO là danh sách cuối cùng sau chỉnh sửa. DO của trip bị hủy giữ trạng thái **Warehouse Approved** để xếp lại chuyến khác, còn trip giữ lịch sử xe/tài xế nhưng không chiếm dụng active assignment.
5. Tài xế được gán xác nhận nhận hàng, xe rời kho → Trạng thái Chuyến xe: **Đang vận chuyển (In-Transit)** → Hệ thống chuyển hàng từ outbound staging sang Kho ảo In-Transit, giải phóng reserved ở staging và tạo delivery attempt hiện tại.
6. Chuyến xe chỉ hoàn tất khi xe quay trở lại kho và mọi DO trong chuyến đã **Completed** hoặc **Returned**; hàng Returned vẫn ở Kho ảo In-Transit cho tới khi luồng hoàn hàng riêng xử lý.

---

### US-WMS-09: Giao diện Web di động cho Tài xế & POD thời gian thực (Priority: P1)

**Mô tả:** Là Tài xế, tôi muốn đăng nhập bằng smartphone vào giao diện Web Responsive của WMS để chỉ xem trip được gán, upload `goodsImage`/`signDocumentImage`, yêu cầu OTP Đại lý và xác nhận giao full Delivery Order tại điểm giao.

**Tiêu chí nghiệm thu:**

1. Tài xế đăng nhập bằng tài khoản riêng → Chỉ xem được danh sách trip và delivery attempt được gán cho driver profile của mình.
   - Màn danh sách của tài xế dùng tiêu đề trung tính **Chuyến xe của tôi**, không dùng wording chỉ dành cho giao đại lý.
   - Mỗi card hiển thị nhãn loại chuyến: **Giao đại lý** cho `trip_type = DELIVERY` và **Điều chuyển nội bộ** cho `trip_type = TRANSFER`.
   - Tài xế có 3 nút filter: **Tất cả**, **Nội bộ**, **Đại lý**. **Nội bộ** chỉ hiển thị `TTR-*`/`TRANSFER`; **Đại lý** chỉ hiển thị `TRIP-*`/`DELIVERY`.
   - Card giao đại lý hiển thị số điểm giao/đại lý; card điều chuyển nội bộ hiển thị tuyến kho nguồn → kho đích và số dòng hàng, không hiển thị wording POD/OTP đại lý trên card.
2. Tại điểm giao: Tài xế chụp ảnh hàng hóa bàn giao (`goodsImage`) và ảnh chữ ký/biên nhận của Đại lý (`signDocumentImage`); mỗi ảnh phải là file ảnh nhỏ hơn 5MB.
3. Tài xế yêu cầu xác nhận giao hàng → Hệ thống sinh OTP ngẫu nhiên 6 chữ số, gửi qua email Đại lý, chỉ lưu hash/verifier, thời điểm tạo, thời điểm hết hạn, số lần thử và trạng thái trong `delivery_otp_attempts`; OTP có hiệu lực 5 phút và mỗi delivery attempt chỉ có một row OTP.
4. Nếu OTP còn hạn và Tài xế yêu cầu gửi lại, hệ thống trả lỗi và không ghi đè mã cũ. Nếu OTP quá hạn và Tài xế yêu cầu gửi lại, hệ thống dùng `UPDATE` ghi đè OTP hiện tại của delivery attempt bằng mã mới. Nếu nhập sai OTP 3 lần, phải nhờ Admin reset thì mới có mã mới; nếu OTP xác thực thành công, hệ thống đánh dấu OTP đã xác thực và không cho dùng lại.
5. Tài xế nhấn "Xác nhận đã giao" với OTP hợp lệ → Hệ thống bắt buộc giao đủ toàn bộ DO, trừ hàng khỏi Kho ảo In-Transit chỉ cho DO đó, tạo invoice/công nợ cho DO đó, đóng delivery attempt là **Delivered** và chuyển DO thành **Completed**.
6. Nếu Đại lý không nhận hàng → Tài xế bấm chuyển DO sang **Returned**, delivery attempt hiện tại đóng là **Failed**, hàng vẫn ở Kho ảo In-Transit cho đến khi luồng hoàn hàng riêng tiếp nhận.
7. Khi xe quay lại kho và mọi DO trong trip đã **Completed** hoặc **Returned**, Tài xế bấm xác nhận xe đã về kho → Trip chuyển **Completed**.

---

### US-WMS-10: Tự động tạo Hóa đơn bán hàng & Cộng công nợ Đại lý (Priority: P1)

**Mô tả:** Sau khi tài xế xác nhận giao thành công bằng POD + OTP hợp lệ, hệ thống tự động tạo Hóa đơn bán hàng và cộng công nợ cho Đại lý.

**Tiêu chí bổ sung:** Scope của US-WMS-10 dừng ở việc tạo invoice và cộng công nợ tự động. Thông báo kế toán, ghi nhận thanh toán, phê duyệt thanh toán, cấn trừ công nợ và chuyển DO sang `CLOSED` thuộc các luồng riêng.

**Tiêu chí nghiệm thu:**

1. Khi tài xế xác nhận giao full DO thành công bằng POD + OTP, hệ thống tự động tạo Hóa đơn bán hàng (Invoice) gồm:
   - Mã hóa đơn (tự động sinh), Đại lý, Tổng giá trị (tính theo số lượng sản phẩm và `unit_price` đã snapshot trên phiếu xuất kho tại thời điểm Thủ kho soạn/lập picking plan), Ngày xuất hóa đơn theo ngày địa phương thực tế của backend, **Hạn thanh toán = ngày xuất hóa đơn + 30 ngày**.
   - Trạng thái hóa đơn ban đầu: **Chưa thanh toán (Unpaid)**.
2. **Hệ thống tự động** sau khi hóa đơn được tạo:
   - Cộng dồn giá trị hóa đơn vào `current_balance` của Đại lý.
   - Không gửi thông báo trong scope này; thông báo kế toán do luồng riêng xử lý.
   - Chặn tạo trùng invoice cho cùng Delivery Order; retry không được cộng công nợ lần hai.
3. Đơn hàng chuyển trạng thái: **Đã hoàn thành (Completed)**.
4. **Ngoài scope:** Thông báo kế toán, gia hạn ngày thanh toán, xử lý thanh toán, cấn trừ công nợ, mở/khóa tín dụng do thanh toán, cảnh báo quá hạn và đóng DO sau khi thanh toán đầy đủ do các luồng riêng xử lý.

---

## NHÓM 4: ĐIỀU CHUYỂN NỘI BỘ (REPLENISHMENT & TRANSFER)

### US-WMS-11: Planner nhập lệnh điều chuyển thủ công từ Công ty mẹ/bộ phận điều phối (Priority: P2)

**Mô tả:** Là Planner, tôi muốn nhập phiếu điều chuyển thủ công dựa trên lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm để hệ thống có chứng từ điều chuyển rõ ràng và truy vết được nguồn lệnh.

**Tiêu chí nghiệm thu:**

1. Planner nhập mã lệnh điều chuyển từ Công ty mẹ/bộ phận điều phối (`external_instruction_code`), kho nguồn, kho đích, ngày kế hoạch, ngày chứng từ và các SKU/số lượng cần chuyển.
2. Hệ thống không tự sinh gợi ý điều chuyển và không tự quyết định kho nguồn/kho đích/số lượng trong Sprint 1.
3. Hệ thống bắt buộc mã lệnh ngoài để truy vết sau này.
4. Planner có thể sửa hoặc hủy phiếu khi phiếu còn trạng thái **Mới**.
5. Phiếu điều chuyển nội bộ dùng mã `TRF-*` và được xử lý ở màn Điều chuyển nội bộ, tách riêng khỏi phiếu nhập NCC `RN-*`.

---

### US-WMS-11A: Trưởng kho đề xuất điều chuyển từ tồn kho kho khác và CEO duyệt (Priority: P1)

**Mô tả:** Là Trưởng kho của kho đang thiếu hàng, tôi muốn xem tồn kho khả dụng của các kho khác ở chế độ chỉ đọc và gửi yêu cầu điều chuyển để CEO duyệt trước khi Planner kho nguồn tạo phiếu `TRF-*`.

**Tiêu chí nghiệm thu:**

1. Trưởng kho chỉ được xem tồn kho khả dụng liên kho ở chế độ read-only; số khả dụng tính bằng `total_qty - reserved_qty` và loại trừ hàng Quarantine/In-Transit không available.
2. Trưởng kho chỉ được tạo yêu cầu cho kho mình phụ trách; kho yêu cầu trở thành kho đích, kho còn hàng là kho nguồn đề xuất.
3. Yêu cầu điều chuyển phải có kho nguồn, kho đích, SKU/số lượng, ngày cần hàng, lý do nghiệp vụ, số tồn khả dụng quan sát tại kho nguồn và kho yêu cầu.
4. CEO có thể duyệt hoặc từ chối yêu cầu; từ chối bắt buộc nhập lý do và giữ lịch sử audit.
5. CEO duyệt yêu cầu **không** reserve tồn và **không** tạo/trừ/cộng inventory. Việc giữ chỗ chỉ xảy ra khi Trưởng kho nguồn duyệt phiếu `TRF-*`.
6. Sau khi CEO duyệt, hệ thống tạo/gửi mẫu yêu cầu đã duyệt cho Planner kho nguồn hoặc Planner trung tâm để chuyển thành một phiếu `TRF-*`.
7. Một yêu cầu đã CEO duyệt chỉ được chuyển thành tối đa một phiếu `TRF-*`; chuyển trùng phải bị chặn.

---

### US-WMS-12: Lập, Duyệt & Xác nhận Phiếu Điều chuyển Kho Nội bộ (Priority: P1)

**Mô tả:** Là Planner / Trưởng kho, tôi muốn tạo và thực thi phiếu điều chuyển hàng hóa giữa 3 kho vật lý để cân bằng tồn kho giữa các miền.

**Tiêu chí nghiệm thu:**

1. Planner tạo Phiếu điều chuyển: nhập mã lệnh điều chuyển ngoài, chọn kho nguồn, kho đích, SKU, số lượng → Trạng thái: **Mới**.
2. **Trưởng kho nguồn (Checker)** kiểm tra tồn kho khả dụng:
   - Nếu đủ hàng → Phê duyệt và hệ thống khóa/giữ chỗ số lượng điều chuyển ngay → Trạng thái: **Đã duyệt**.
   - Nếu không duyệt → Trưởng kho nguồn nhập lý do từ chối bắt buộc → Trạng thái: **Từ chối (REJECTED)**.
   - Nếu đã duyệt nhưng cần hủy trước khi xe rời kho → Chỉ Trưởng kho nguồn/manager được hủy và hệ thống giải phóng giữ chỗ.
3. Dispatcher lập một chuyến xe nội bộ riêng cho phiếu điều chuyển: gán xe, tài xế và ngày vận chuyển.
   - Dispatcher chỉ được lập chuyến cho phiếu có kho nguồn thuộc phạm vi kho mình.
   - Danh sách tài xế hợp lệ chỉ gồm các tài xế có thể hoạt động tại kho nguồn của phiếu.
   - Hệ thống phải tính tải trọng/thể tích từ dòng hàng, kiểm tra xe/tài xế không bị trùng lịch, kiểm tra tải trọng xe theo khối lượng; thể tích chỉ kiểm tra khi xe có cấu hình thể tích.
   - Chỉ được đổi xe/tài xế/lịch trước khi tài xế departure; sau departure trip bị khóa.
   - Chuyến điều chuyển `TTR-*` xuất hiện trong màn **Chuyến xe của tôi** của tài xế với nhãn **Điều chuyển nội bộ** và filter **Nội bộ**; detail của chuyến này đi theo luồng depart/arrive/handover của điều chuyển, không dùng POD/OTP đại lý.
4. Thủ kho kho nguồn kiểm outbound QC bằng mắt/đối chiếu phiếu, chụp ảnh xác nhận, ghi nhận số lượng xuất, bốc xếp lên xe và chụp ảnh bàn giao cho tài xế; Tài xế xác nhận đã nhận hàng và xe rời kho → Hệ thống **trừ tồn kho nguồn, giải phóng giữ chỗ, cộng vào Kho ảo In-Transit** → Trạng thái: **Đang vận chuyển (In-Transit)**.
   - Thủ kho nguồn phải ghi đúng số lượng đã duyệt; không được xuất thừa hoặc thiếu.
   - Outbound QC và load/handover là bắt buộc trước khi tài xế departure, xác nhận bằng ảnh; hệ thống không yêu cầu Barcode/QR.
   - Nếu đã ghi hàng lên xe nhưng chưa rời kho mà cần hủy, hệ thống bắt buộc hạ hàng/unship trước rồi mới cho Trưởng kho nguồn hủy phiếu và nhả giữ chỗ.
5. Tài xế được gán phải ghi nhận xe đến kho nhận và bàn giao vật lý trước khi kho nhận được kiểm đếm. Công nhân kho đích nhập blind count số lượng thực nhận; nếu số nhận thiếu/thừa so với số gửi thì phải nhập lý do. Thủ kho kho đích kiểm tra lại số lượng, có thể điều chỉnh số xác nhận kèm ghi chú, nhập/chốt QC, kiểm tra sức chứa Bin và chọn vị trí nhập hàng đạt; Trưởng kho đích xác nhận cuối cùng:
   - Nếu khớp và QC đạt → Hệ thống **trừ Kho ảo In-Transit, cộng vào kho đích** → Trạng thái: **Hoàn thành**.
   - Nếu thiếu → Hệ thống **bắt buộc** ghi lý do chênh lệch, tạo hồ sơ incident/discrepancy và tự động tạo Phiếu điều chỉnh bù trừ.
   - Nếu nhận thừa (`received_qty > sent_qty`) → Hệ thống chặn nhập kho thường và ghi nhận discrepancy hold/incident cho phần hàng vật lý thừa.
   - Nếu QC lỗi/hư hỏng → Phần lỗi được đưa vào Quarantine Zone với nguồn `INTERNAL_TRANSFER`, không tính vào tồn kho khả dụng, chỉ xử lý tiêu hủy theo Spec 009 và không tạo trả NCC/Debit Note.
   - Nếu thiếu hàng → Phần thiếu không được tạo Quarantine hoặc disposal candidate vì không có hàng vật lý.
   - Nếu gửi nhầm SKU nhưng hàng còn nguyên → Thủ kho đích báo cáo `WRONG_SKU` theo từng line với SKU kỳ vọng, SKU thực tế, số lượng ảnh hưởng, lý do và ảnh nếu có; Trưởng kho đích duyệt xe quay về kho nguồn, hàng vẫn ở In-Transit, tài xế ghi nhận return departure/source arrival/handover và kho nguồn thực hiện lại count/check/QC/final receive.
   - Nếu trip quá hạn khi phiếu còn `IN_TRANSIT` → Hệ thống đánh dấu quá hạn, chặn receive-count/receive-check tại kho đích và yêu cầu vai trò có thẩm quyền kích hoạt Return to Source với lý do/bằng chứng.
   - Hàng đạt QC chỉ được cộng vào Bin hợp lệ của kho nhận sau khi kiểm tra sức chứa Bin.
6. Planner chỉ được hủy phiếu khi còn **NEW**; sau khi **APPROVED** Planner không được hủy. Hệ thống không hỗ trợ hủy phiếu điều chuyển sau khi trạng thái đã là **Đang vận chuyển (In-Transit)**.
7. Luồng nhận hàng điều chuyển vẫn ở màn Điều chuyển nội bộ; không gộp vào danh sách phiếu nhập NCC `RN`.
8. Mọi mutation của transfer/request/trip/resource/inventory phải có kiểm soát version/concurrency và audit đủ header, line-item, allocation, QC, wrong-SKU, trip/resource và inventory movement.

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
2. Sản phẩm bị thiếu hụt được đánh dấu đỏ nổi bật trên Dashboard và màn hình cảnh báo tồn kho.

---

---

## NHÓM 10: CHẤT LƯỢNG KỸ THUẬT & KIỂM THỬ

### US-WMS-TEST-01: Hạ tầng kiểm thử Backend & Quality Gate (Priority: P1)

**Mô tả:** Là Developer/QA, tôi muốn CI chạy unit và integration test backend, xuất JaCoCo XML cho SonarQube để kiểm soát chất lượng mã mới.

**Tiêu chí nghiệm thu:** Quality Gate 80% áp dụng cho new code trong PR; DTO/entity/config có thể được loại trừ minh bạch; không commit token/credential; lỗi test hoặc cấu hình phải làm build thất bại.

### US-WMS-TEST-02: Bộ kiểm thử Core Services & Nghiệp vụ Kho (Priority: P1)

**Mô tả:** Là Developer/QA, tôi muốn kiểm thử RBAC và các bất biến FIFO, tồn kho không âm, reserved quantity, QC, transfer, optimistic lock và audit trail.

**Tiêu chí nghiệm thu:** Các dịch vụ mới đạt tối thiểu 80% coverage; test có nhiều bộ dữ liệu dùng JUnit 5 Parameterized Tests; endpoint có happy path và error path phù hợp.

### US-WMS-TEST-03: Kiểm thử giao diện Frontend (Priority: P1)

**Mô tả:** Là Frontend Developer/QA, tôi muốn dùng Vitest + React Testing Library để kiểm thử utility/form validation, route/RBAC, trạng thái component và chống double-submit.

**Tiêu chí nghiệm thu:** Test xác nhận hành vi người dùng, chạy được qua `npm test`, không thay thế integration test của backend và dùng parameterized tests cho dữ liệu biên khi phù hợp.

*Tổng cộng: 30 User Stories: 27 câu chuyện vận hành và 3 câu chuyện chất lượng kỹ thuật. Chi tiết chuẩn: `.sdd/specs/001`–`012`.*
*Ghi chú quan trọng: Hệ thống KHÔNG có quản lý sản xuất (Manufacturing), KHÔNG có HR/HRM, KHÔNG có Barcode/QR Scanner, KHÔNG có cổng B2B/B2C, SỬ DỤNG XE NỘI BỘ (không có chi phí 3PL trong luồng xuất hàng thông thường).*
