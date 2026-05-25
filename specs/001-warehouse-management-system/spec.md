# Feature Specification: Hệ Thống Quản Lý Kho (Warehouse Management System)

**Feature Branch**: `001-warehouse-management-system`

**Created**: 2026-05-23

**Status**: Draft

**Input**: User description: "Hệ thống Quản Lý Kho với các User Stories bao gồm: Quản lý Danh mục Hàng hóa, Nhập kho, Xuất kho, Điều chuyển, Báo cáo, Kiểm kê, Vận chuyển, Barcode/QR, Hoàn hàng, Quản lý giá, Lô sản phẩm, Vị trí kho, Liên thông Kế toán/HRM/Sale, Kiểm soát thất thoát, Bằng chứng giao hàng, Xe vận chuyển, Phân quyền. Không làm về Sản xuất. Quy trình đặt hàng: bên khác liên hệ Sale, Sale điền thông tin vào hệ thống, Kho tự động nhận đơn xử lý."

## Mục Tiêu Dự Án

Xây dựng hệ thống quản lý kho hoàn chỉnh phục vụ cho doanh nghiệp thương mại với 3 kho: Kho Hải Phòng, Kho Hà Nội, Kho Hồ Chí Minh. Hệ thống quản lý toàn bộ quy trình nhập-xuất-điều chuyển-tồn kho, tích hợp với các phòng ban khác (Kế toán, HRM, Sale). Không bao gồm module Sản xuất.

Quy trình đặt hàng: Khi bên khác (đại lý, khách hàng) có nhu cầu đặt hàng, họ liên hệ trực tiếp với bộ phận Sale. Sale sẽ điền thông tin đơn hàng vào hệ thống. Hệ thống Kho tự động nhận đơn hàng từ Sale và tiến hành xử lý: chuẩn bị hàng, xuất kho, giao hàng.

---

## User Scenarios & Testing

### Tổng Quan User Stories

| ID       | Tên User Story                              | Priority | Mô Tả Ngắn                                                 |
| -------- | ------------------------------------------- | -------- | ---------------------------------------------------------- |
| FR-WH-01 | Quản lý Danh mục Hàng hóa & Tồn kho         | P1       | Quản lý sản phẩm, đơn vị tính, tồn kho theo thời gian thực |
| FR-WH-02 | Nhập kho từ Nhà cung cấp & QC               | P1       | Nhập hàng từ NCC, kiểm tra QC, phân loại lưu kho           |
| FR-WH-03 | Điều chuyển Nội bộ Giữa Các Kho             | P1       | Chuyển hàng giữa 3 kho                                     |
| FR-WH-04 | Xuất kho cho Đại lý & Quản lý Đơn hàng Sale | P1       | Xuất cho đại lý từ đơn hàng Sale, theo dõi công nợ         |
| FR-WH-05 | Báo cáo & Kiểm soát Hệ thống                | P1       | Dashboard, báo cáo tổng hợp                                |
| FR-WH-06 | Kiểm kê & Điều chỉnh Tồn kho                | P1       | Kiểm kê định kỳ, điều chỉnh                                |
| FR-WH-07 | Quản lý Trạng thái Vận chuyển               | P1       | Theo dõi vận đơn, trạng thái giao hàng                     |
| US-WH-08 | Hoàn hàng từ Đại lý (Return Management)     | P2       | Xử lý hàng hoàn lại, credit note                           |
| US-WH-09 | Quản lý Giá vốn & Giá bán                   | P2       | Cấu hình giá cost/retail/dealer, khuyến mãi                |
| US-WH-10 | Quản lý Lô Sản phẩm (Batch Management)      | P2       | Batch number, expiry date, Grade, FIFO                     |
| US-WH-11 | Quản lý Vị trí Kho (Bin Location)           | P2       | Zone/Rack/Shelf, mapping sản phẩm                          |
| US-WH-12 | Liên thông Kho ↔ Kế toán                    | P2       | Tính giá vốn, COGS, inventory value                        |
| US-WH-13 | Liên thông Kho ↔ HRM                        | P3       | Theo dõi công nhân kho, sản lượng                          |
| US-WH-14 | Liên thông Kho ↔ Sale                       | P2       | Đơn đặt hàng từ Sale, cập nhật xuất kho                    |
| US-WH-15 | Kiểm soát Thất thoát & Hư hỏng              | P3       | Lost items, damage report                                  |
| US-WH-16 | Quản lý Bằng chứng Giao hàng                | P3       | Chữ ký, hình ảnh, video giao hàng                          |
| US-WH-17 | Quản lý Xe vận chuyển & Tài xế              | P3       | Fleet management, driver assignment                        |
| US-WH-18 | Kiểm kê tháng & Báo cáo Tồn kho             | P2       | Monthly stock taking, variance report                      |
| US-WH-19 | Phân quyền theo Kho & Chức danh             | P2       | Role-based access control                                  |
| US-WH-20 | Báo cáo chi tiết Kho                        | P1       | Report templates đầy đủ                                    |

---

### User Story chi tiết

#### FR-WH-01: Quản lý Danh mục Hàng hóa & Tồn kho (Priority: P1)

**Mô tả**: Người quản lý kho có thể quản lý danh mục sản phẩm, đơn vị tính, và theo dõi tồn kho theo thời gian thực cho 3 kho (Hải Phòng, Hà Nội, Hồ Chí Minh).

**Acceptance Scenarios**:

1. **Given** người dùng đã đăng nhập với quyền QL Kho, **When** thêm sản phẩm mới với mã, tên, đơn vị tính, **Then** sản phẩm xuất hiện trong danh mục và có thể sử dụng cho các nghiệp vụ khác.

2. **Given** sản phẩm đã có trong danh mục, **When** thực hiện nhập kho 100 cái, **Then** tồn kho tại kho đó tăng 100, hệ thống ghi nhận thời gian và người thực hiện.

3. **Given** tồn kho hiện tại là 50 cái, **When** xuất kho 30 cái, **Then** tồn kho còn 20 cái, không thể xuất quá tồn kho.

4. **Given** người dùng xem báo cáo tồn kho, **When** chọn kho Hải Phòng và thời gian hiện tại, **Then** hiển thị chính xác số lượng tồn kho của mỗi sản phẩm tại kho đó.

---

#### FR-WH-02: Nhập kho từ Nhà cung cấp & QC (Priority: P1)

**Mô tả**: Quản lý Purchase Order (PO) từ bộ phận Mua hàng, tiếp nhận hàng, kiểm tra chất lượng (QC) và phân loại lưu kho: hàng đạt chất lượng nhập kho thường, hàng không đạt chất lượng chuyển vào Khu cách ly (Quarantine Zone) hoặc làm thủ tục trả lại NCC.

**Acceptance Scenarios**:

1. **Given** Mua hàng tạo PO #PO-001 với 1000 kg nguyên liệu từ NCC Việt Á, **When** hàng về kho, **Then** tạo phiếu nhận hàng, kiểm tra số lượng thực tế.

2. **Given** hàng nhập đang chờ QC, **When** QC kiểm tra và kết luận "Đạt", **Then** cho phép nhập kho đầy đủ vào vị trí kệ thường.

3. **Given** hàng nhập PO-001 về kho và QC phát hiện 100 kg "Không đạt" tiêu chuẩn, **When** hệ thống ghi nhận kết quả QC thất bại cho phần hàng lỗi này, **Then** số hàng lỗi (100 kg) được nhập tạm vào Vị trí cách ly (Quarantine Location) để chờ NCC thu hồi hoặc xử lý hoàn trả, không được tính vào tồn kho khả dụng, đồng thời tự động tạo thông báo khiếu nại NCC.

---

#### FR-WH-03: Điều chuyển Nội bộ Giữa Các Kho (Priority: P1)

**Mô tả**: Di chuyển hàng hóa từ kho này sang kho khác với đầy đủ chứng từ và theo dõi hành trình thông qua Kho ảo Đang đi đường (In-Transit Location) để tránh thất thoát số liệu tồn kho toàn hệ thống.

**Acceptance Scenarios**:

1. **Given** Kho Hải Phòng có 200 sản phẩm A, Kho Hà Nội có 0, **When** thực hiện xuất kho điều chuyển 50 sản phẩm A sang Kho Hà Nội, **Then** Kho Hải Phòng giảm 50, Kho ảo Đang đi đường tăng 50, Kho Hà Nội vẫn giữ nguyên là 0.

2. **Given** phiếu điều chuyển đang ở trạng thái "Đang vận chuyển", **When** Kho Hà Nội xác nhận đã nhận thực tế 50 sản phẩm A, **Then** Kho ảo Đang đi đường giảm 50, Kho Hà Nội tăng 50, phiếu điều chuyển chuyển sang trạng thái "Hoàn thành".

3. **Given** Kho Hải Phòng chỉ có 30 sản phẩm, **When** tạo phiếu điều chuyển 50 sản phẩm, **Then** hệ thống từ chối và báo lỗi "Số lượng tồn kho không đủ".

---

#### FR-WH-04: Xuất kho cho Đại lý & Quản lý Đơn hàng Sale (Priority: P1)

**Mô tả**: Khi đại lý hoặc khách hàng có nhu cầu đặt hàng, họ liên hệ trực tiếp với bộ phận Sale. Sale điền thông tin đơn hàng vào hệ thống (không qua CRM). Hệ thống Kho tự động nhận đơn hàng từ Sale, chuẩn bị hàng và xuất kho. Sau khi xuất kho, hệ thống tự động gửi dữ liệu sang Kế toán để xử lý công nợ.

**Quy trình đặt hàng**:

1. Đại lý/Khách hàng liên hệ Sale qua điện thoại, email, Zalo, hoặc gặp trực tiếp.
2. Sale điền thông tin đơn hàng vào hệ thống: thông tin khách hàng, sản phẩm, số lượng, giá, thời gian giao hàng mong muốn.
3. Sale xác nhận đơn hàng (trạng thái: Đã xác nhận).
4. Hệ thống Kho tự động nhận đơn hàng và hiển thị trong danh sách chờ xử lý.
5. Kho chuẩn bị hàng, xuất kho, cập nhật trạng thái đơn hàng.
6. Hệ thống tự động gửi thông tin xuất kho sang Kế toán để xử lý công nợ.

**Acceptance Scenarios**:

1. **Given** Đại lý Delta liên hệ Sale đặt hàng 20 triệu, Sale đã xác nhận đơn vào hệ thống, **When** kho thực hiện xuất kho thành công, **Then** tồn kho giảm 20 triệu, hệ thống tự động gửi API báo cáo xuất hàng sang hệ thống Kế toán để ghi nhận công nợ.

2. **Given** Đại lý Omega thanh toán tiền hàng, **When** thao tác này thực hiện ở module Kế toán, **Then** hệ thống WMS không cần can thiệp hay lập phiếu thu trực tiếp.

3. **Given** Sale tạo đơn hàng cho đại lý mới, **When** lưu thông tin, **Then** hệ thống cho phép tạo nhanh thông tin đại lý mới nếu chưa tồn tại (tên, SĐT, địa chỉ giao hàng).

---

#### FR-WH-05: Báo cáo & Kiểm soát Hệ thống (Priority: P1)

**Mô tả**: Dashboard tổng quan, báo cáo tồn kho, báo cáo nhập-xuất-điều chuyển, cảnh báo khi tồn kho thấp hoặc hết hàng.

**Acceptance Scenarios**:

1. **Given** người dùng mở dashboard, **When** xem tổng quan, **Then** hiển thị: tổng tồn kho 3 kho, tổng giá trị tồn kho, số đơn xuất chờ duyệt, số đơn đang vận chuyển.

2. **Given** tồn kho sản phẩm A tại Kho HCM giảm xuống dưới mức tối thiểu (reorder point), **When** hệ thống phát hiện, **Then** gửi cảnh báo cho QL Kho và bộ phận mua hàng.

3. **Given** QL Kho chạy báo cáo nhập-xuất-tồn tháng 5/2026, **When** xuất Excel/PDF, **Then** file chứa đầy đủ chi tiết: ngày, số phiếu, sản phẩm, số lượng, kho, người thực hiện.

---

#### FR-WH-06: Kiểm kê & Điều chỉnh Tồn kho (Priority: P1)

**Mô tả**: Thực hiện kiểm kê định kỳ hoặc đột xuất, so sánh tồn kho thực tế với số liệu trên hệ thống, điều chỉnh khi có chênh lệch.

**Acceptance Scenarios**:

1. **Given** tồn kho hệ thống của sản phẩm A là 100 cái, **When** kiểm kê thực tế chỉ còn 95 cái, **Then** tạo phiếu điều chỉnh giảm 5 cái kèm lý do.

2. **Given** có phiếu điều chỉnh đang chờ phê duyệt, **When** QL Kho duyệt, **Then** tồn kho được cập nhật, ghi nhận người duyệt và thời gian.

3. **Given** sản phẩm bị phát hiện hư hỏng khi kiểm kê, **When** ghi nhận vào phiếu kiểm kê, **Then** tạo báo cáo hư hỏng, không điều chỉnh vào tồn kho thường.

---

#### FR-WH-07: Quản lý Trạng thái Vận chuyển (Priority: P1)

**Mô tả**: Theo dõi vận đơn từ khi xuất kho đến khi giao hàng thành công, cập nhật trạng thái: Chờ giao → Đang giao → Đã giao → Hoàn thành.

**Acceptance Scenarios**:

1. **Given** đơn hàng đã xuất kho và tạo vận đơn, **When** tài xế cập nhật "Đang giao", **Then** trạng thái vận đơn thay đổi, Sale nhận thông báo.

2. **Given** giao hàng thành công, **When** tài xế xác nhận và chụp ảnh biên nhận, **Then** vận đơn chuyển "Hoàn thành", hệ thống ghi nhận thời gian giao.

3. **Given** giao hàng thất bại (khách không nhận), **When** tài xế ghi nhận lý do, **Then** vận đơn chuyển "Giao thất bại", hàng quay về kho.

---

#### US-WH-09: Quét Barcode/QR Code (Priority: P2)

**Mô tả**: Sử dụng máy quét barcode/QR để nhập liệu nhanh và chính xác trong các thao tác: nhập kho, xuất kho, điều chuyển, kiểm kê.

**Acceptance Scenarios**:

1. **Given** sản phẩm đã có barcode in trên vỏ, **When** quét barcode khi nhập kho, **Then** hệ thống tự điền mã sản phẩm, số lượng mặc định 1 (có thể sửa).

2. **Given** thực hiện kiểm kê với máy quét, **When** quét barcode sản phẩm A 10 lần, **Then** hệ thống ghi nhận đã kiểm đếm 10 cái sản phẩm A.

3. **Given** quét barcode không tồn tại trong hệ thống, **When** hệ thống nhận diện mã lạ, **Then** cảnh báo và không cho phép xử lý.

---

#### US-WH-10: Hoàn hàng từ Đại lý (Return Management) (Priority: P2)

**Mô tả**: Xử lý hàng đại lý hoàn lại (hỏng, không bán được, hết hạn), tiến hành QC phân loại hàng: hàng đạt chuẩn nhập lại kho thường, hàng lỗi chuyển Khu cách ly, đồng thời tự động đẩy dữ liệu sang Kế toán để xử lý khấu trừ công nợ (Credit Note).

**Acceptance Scenarios**:

1. **Given** Đại lý Gamma hoàn lại 20 sản phẩm B do hết hạn, **When** kho tiếp nhận, kiểm tra QC và xác nhận nhập kho, **Then** tạo phiếu nhập hoàn thành công và tự động đẩy dữ liệu hàng hoàn sang hệ thống Kế toán để Kế toán tự xử lý cấp credit note 20 triệu cho Gamma.

2. **Given** hàng hoàn được kiểm tra và phát hiện hư hỏng nặng, **When** xác định không thể tái sử dụng, **Then** nhập vào Vị trí cách ly (Quarantine Location) với trạng thái "Chờ thanh lý", ghi nhận lý do hư hỏng.

3. **Given** hàng hoàn đạt chất lượng tốt, **When** nhập lại kho thường, **Then** tồn kho khả dụng tăng, sẵn sàng xuất cho đơn hàng khác.

---

#### US-WH-12: Quản lý Giá vốn & Giá bán (Priority: P2)

**Mô tả**: Cấu hình và quản lý các loại giá: giá vốn (cost), giá bán lẻ (retail), giá đại lý (dealer), áp dụng khuyến mãi/giảm giá.

**Acceptance Scenarios**:

1. **Given** sản phẩm A có giá vốn 80.000đ, giá bán lẻ 120.000đ, giá đại lý 100.000đ, **When** xuất cho đại lý, **Then** hệ thống tự động áp dụng giá đại lý.

2. **Given** có chương trình khuyến mãi giảm 10% cho sản phẩm B từ 1-15/6, **When** đại lý đặt hàng ngày 5/6, **Then** áp dụng giá khuyến mãi tự động.

3. **Given** cần xem lịch sử giá của sản phẩm, **When** tra cứu, **Then** hiển thị các mốc thời gian thay đổi giá và người thay đổi.

---

#### US-WH-13: Quản lý Lô Sản phẩm (Batch Management) (Priority: P2)

**Mô tả**: Theo dõi hàng hóa theo lô nhập (Batch Number), ngày nhập kho, hạn sử dụng (EXP) nếu có. Mỗi số lô gắn liền với một hạng chất lượng duy nhất (Grade A/B/C). Hệ thống tự động áp dụng thuật toán FEFO cho sản phẩm có hạn sử dụng, và FIFO cho sản phẩm không có hạn sử dụng.

**Acceptance Scenarios**:

1. **Given** nhập kho 500 sản phẩm từ Lô #LSP-2026-001-A, nhập ngày 1/5, hạn 1 năm, chất lượng Grade A, **When** lưu, **Then** hệ thống ghi nhận batch number kèm Grade A, ngày nhập, EXP date đồng bộ.

2. **Given** có 2 lô sản phẩm A trong kho: Lô 1 (hạn dùng 1/7) và Lô 2 (hạn dùng 1/10), **When** xuất kho sản phẩm A, **Then** hệ thống tự động ưu tiên gợi ý xuất hàng từ Lô 1 trước vì có ngày hết hạn gần hơn (thuật toán FEFO).

3. **Given** một lô nhập hàng có sản phẩm đạt chất lượng Grade A và Grade B, **When** nhập kho, **Then** hệ thống bắt buộc tạo thành 2 mã lô riêng biệt để quản lý tồn kho và giá trị chính xác.

---

#### US-WH-14: Quản lý Vị trí Kho & Sức chứa (Bin Location Capacity) (Priority: P2)

**Mô tả**: Cấu hình cấu trúc kho phân cấp (Zone/Rack/Shelf/Bin) kèm theo giới hạn sức chứa: thể tích (m³), khối lượng chịu tải (kg), để hệ thống tự động kiểm tra khi Putaway.

**Acceptance Scenarios**:

1. **Given** cấu hình Kho Hải Phòng: Zone A, Rack A-01, Shelf 1, Bin 01 có sức chứa 5m³ và tải trọng 500kg, **When** kiểm tra, **Then** hệ thống hiển thị đầy đủ cấu trúc phân cấp.

2. **Given** sản phẩm có kích thước 1m³ và khối lượng 200kg, **When** thực hiện Putaway vào Bin có sức chứa 5m³ và tải trọng 500kg (đã chứa 4m³/400kg), **Then** hệ thống cho phép xếp hàng.

3. **Given** sản phẩm có kích thước 2m³ và khối lượng 300kg, **When** thực hiện Putaway vào Bin có sức chứa 5m³ và tải trọng 500kg (đã chứa 4m³/400kg), **Then** hệ thống từ chối và gợi ý các Bin khác còn trống.

---

#### US-WH-15: Liên thông Kho ↔ Kế toán (Priority: P2)

**Mô tả**: Tự động gửi dữ liệu nhập/xuất kho sang Kế toán để tính giá vốn, doanh thu, COGS, inventory value cuối kỳ.

**Acceptance Scenarios**:

1. **Given** xuất kho 100 sản phẩm cho đại lý với giá vốn 80.000đ/cái, **When** hoàn thành xuất kho, **Then** hệ thống tự tạo bút toán gửi Kế toán: Nợ công nợ phải thu / Có hàng tồn kho 8.000.000đ.

2. **Given** cuối tháng, **When** chạy báo cáo tồn kho cuối kỳ, **Then** hệ thống tự động gửi cho Kế toán: tổng giá trị tồn kho theo giá vốn.

---

#### US-WH-16: Liên thông Kho ↔ HRM (Priority: P3)

**Mô tả**: Theo dõi công nhân kho, phân ca làm việc, ghi nhận sản lượng của từng nhân viên để tính lương theo sản phẩm.

**Acceptance Scenarios**:

1. **Given** Nhân viên Kho Tuấn làm ca sáng tại Kho Hải Phòng, **When** đăng nhập ca, **Then** hệ thống ghi nhận ca làm việc: ca sáng, Kho HP, thời gian vào.

2. **Given** Tuấn xuất kho 50 đơn trong ca, **When** kết ca, **Then** hệ thống tổng hợp: 50 đơn xuất, 200 sản phẩm kiện hàng, gửi HRM để tính lương.

---

#### US-WH-17: Liên thông Kho ↔ Sale (Priority: P2)

**Mô tả**: Sale tiếp nhận yêu cầu đặt hàng từ đại lý/khách hàng (qua điện thoại, email, Zalo, gặp trực tiếp), điền thông tin vào hệ thống. Kho tự động nhận đơn hàng và xử lý. Khi Kho xuất hàng, Sale được thông báo để cập nhật trạng thái cho khách hàng.

**Điểm khác biệt so với CRM**: Không có module CRM để đại lý đặt hàng online. Sale là đầu mối tiếp nhận và nhập đơn hàng thủ công vào hệ thống.

**Acceptance Scenarios**:

1. **Given** Đại lý Delta liên hệ Sale qua Zalo đặt 100 sản phẩm A, **When** Sale điền thông tin và xác nhận đơn hàng, **Then** Kho nhận thông báo và bắt đầu chuẩn bị hàng.

2. **Given** Kho xuất hàng cho Delta, **When** hoàn thành, **Then** Sale nhận thông báo xuất kho thành công, cập nhật trạng thái đơn hàng "Đã xuất kho", và gửi thông tin cho Kế toán để cập nhật công nợ.

3. **Given** đại lý mới liên hệ Sale lần đầu, **When** Sale tạo đơn hàng, **Then** hệ thống cho phép nhập nhanh thông tin đại lý mới: tên, SĐT, địa chỉ giao hàng.

---

#### US-WH-18: Kiểm soát Thất thoát & Hư hỏng (Priority: P3)

**Mô tả**: Ghi nhận hàng thất thoát (mất cắp), hư hỏng (do vận chuyển, bảo quản), phân tích nguyên nhân và tỷ lệ.

**Acceptance Scenarios**:

1. **Given** kiểm kê phát hiện thiếu 5 sản phẩm A (không rõ nguyên nhân), **When** lập báo cáo, **Then** ghi nhận "Thất thoát - Chưa xác định", điều chỉnh tồn kho, thông báo QL Kho.

2. **Given** nhận hàng hoàn từ đại lý, phát hiện 3 sản phẩm vỡ do đóng gói kém, **When** lập báo cáo, **Then** ghi nhận "Hư hỏng - Vận chuyển", kèm hình ảnh, gửi báo cáo cho QL Sale.

---

#### US-WH-19: Quản lý Bằng chứng Giao hàng (Priority: P3)

**Mô tả**: Ghi nhận hình ảnh/video giao hàng, thời gian và địa điểm giao làm bằng chứng.

**Acceptance Scenarios**:

1. **Given** tài xế giao hàng cho Đại lý Epsilon, **When** Epsilon ký xác nhận trên thiết bị di động, **Then** lưu chữ ký số, gắn với vận đơn.

2. **Given** giao hàng xong, **When** tài xế chụp ảnh hàng giao và biên nhận, **Then** lưu ảnh vào hệ thống, gắn timestamp và GPS location.

---

#### US-WH-20: Quản lý Xe vận chuyển & Tài xế (Priority: P3)

**Mô tả**: Quản lý danh sách xe vận chuyển và tài xế nội bộ để phục vụ điều phối các chuyến giao hàng cho Đại lý hoặc điều chuyển kho. Hệ thống chỉ tập trung vào gán xe, gán tài xế vào chuyến đi và ghi nhận trạng thái hoàn thành đơn. Lược bỏ đo lường xăng xe, km, chi tiết vận hành TMS.

**Acceptance Scenarios**:

1. **Given** cần cấu hình đội xe vận chuyển của kho, **When** thêm xe Toyota #HP-001 (5 tấn) và tài xế An, **Then** lưu thông tin xe và tài xế khả dụng.

2. **Given** tạo đơn vận chuyển cho Đại lý Beta, **When** gán xe #HP-001 và tài xế An, **Then** hiển thị đầy đủ thông tin vận tải trên vận đơn giao hàng.

3. **Given** tài xế An hoàn thành chuyến đi và cập nhật trạng thái "Giao hàng thành công", **When** hệ thống xác nhận, **Then** ghi nhận chuyến đi hoàn tất, cập nhật trạng thái phiếu xuất liên quan.

---

#### US-WH-21: Kiểm kê tháng & Báo cáo Tồn kho (Priority: P2)

**Mô tả**: Thực hiện kiểm kê định kỳ hàng tháng, so sánh với tồn kho hệ thống, báo cáo chênh lệch, gửi cho Kế toán.

**Acceptance Scenarios**:

1. **Given** ngày 25 hàng tháng, **When** QL Kho tạo phiếu kiểm kê tháng 5, **Then** hệ thống tạo danh sách đầy đủ sản phẩm cần kiểm.

2. **Given** kiểm kê xong tất cả sản phẩm, **When** so sánh với hệ thống, **Then** hiển thị báo cáo Variance: sản phẩm A chênh -5 cái, sản phẩm B chênh +3 cái.

3. **Given** variance report hoàn thành và được duyệt, **When** kết thúc tháng, **Then** hệ thống tự động gửi báo cáo tồn kho cuối tháng cho Kế toán.

---

#### US-WH-22: Phân quyền theo Kho & Chức danh (Priority: P2)

**Mô tả**: Kiểm soát truy cập dựa trên kho và chức danh: QL Kho chỉ quản lý kho được gán, Thủ kho chỉ xuất/nhập, Kế toán chỉ xem báo cáo, Sale chỉ tạo đơn hàng và xem trạng thái.

**Acceptance Scenarios**:

1. **Given** Nhân viên Minh có vai trò "QL Kho Hải Phòng", **When** đăng nhập, **Then** chỉ thấy và thao tác trên Kho Hải Phòng.

2. **Given** Nhân viên Lan có vai trò "Thủ kho", **When** đăng nhập, **Then** có quyền xuất/nhập kho nhưng không có quyền phê duyệt điều chuyển.

3. **Given** Kế toán Trang đăng nhập, **When** vào module kho, **Then** chỉ xem được báo cáo, không thể tạo phiếu xuất/nhập.

4. **Given** Sale Hùng đăng nhập, **When** vào module kho, **Then** có quyền tạo đơn hàng, xem trạng thái đơn hàng, không có quyền xuất/nhập kho trực tiếp.

---

#### US-WH-23: Báo cáo chi tiết Kho (Priority: P1)

**Mô tả**: Báo cáo đầy đủ các nghiệp vụ kho: nhập, xuất, tồn, điều chuyển, giá trị, theo nhiều chiều.

**Acceptance Scenarios**:

1. **Given** cần báo cáo nhập kho tháng 5, **When** chọn loại báo cáo và thời gian, **Then** hiển thị: tổng số phiếu nhập, tổng số lượng, tổng giá trị, chi tiết từng phiếu.

2. **Given** cần báo cáo tồn kho theo sản phẩm, **When** chạy báo cáo, **Then** hiển thị: mã SP, tên SP, đơn vị, tồn kho mỗi kho, tổng tồn, giá vốn, giá trị.

3. **Given** cần báo cáo điều chuyển giữa các kho, **When** chọn kho nguồn, kho đích, **Then** hiển thị danh sách đầy đủ các phiếu điều chuyển.

---

### Edge Cases

- **Khi tồn kho âm**: Hệ thống không cho phép tồn kho âm, phải điều chỉnh bằng phiếu kiểm kê.
- **Khi đồng thời nhiều người thao tác**: Sử dụng locking hoặc optimistic concurrency để tránh sai lệch số liệu tồn kho.
- **Khi mất kết nối giữa các module liên thông**: Queue messages để đồng bộ sau, có cơ chế tự động thử lại (retry mechanism).
- **Khi hàng hóa có nhiều lô với hạn khác nhau**: Bắt buộc áp dụng FEFO - ưu tiên xuất lô có hạn sử dụng gần nhất.
- **Khi đại lý thanh toán vượt công nợ hoặc trả hàng hoàn tiền**: Việc thanh toán và xử lý tài chính do module Kế toán quản trị độc lập, WMS chỉ đồng bộ sự kiện nhập/xuất để ghi nhận định lượng.
- **Khi barcode không đọc được**: Cho phép nhập tay mã sản phẩm (SKU) hoặc số lô (Batch) làm phương án dự phòng.
- **Khi Sale nhập đơn hàng cho đại lý mới**: Hệ thống cho phép tạo nhanh thông tin đại lý mới ngay trong form tạo đơn hàng.
- **Khi đơn hàng Sale bị hủy sau khi Kho đã chuẩn bị**: Kho nhận thông báo hủy đơn, kiểm tra trạng thái chuẩn bị để quyết định hủy hay giữ hàng.

---

## Requirements

### Functional Requirements

#### Nhóm A: Nghiệp vụ Cốt lõi (Core Operations)

- **FR-A01**: Hệ thống CHO PHÉP quản lý danh mục sản phẩm với các thuộc tính: mã SP, tên SP, đơn vị tính, mô tả, barcode, hình ảnh.
- **FR-A02**: Hệ thống CHO PHÉP theo dõi tồn kho theo thời gian thực tại mỗi kho (Hải Phòng, Hà Nội, Hồ Chí Minh) và trạng thái hàng đi đường trong Kho ảo Đang đi đường (In-Transit Location).
- **FR-A03**: Hệ thống KHÔNG CHO PHÉP xuất kho vượt số lượng tồn kho hiện có (trừ khi được QL Kho duyệt đặc biệt).
- **FR-A04**: Hệ thống CHO PHÉP tạo phiếu nhập kho với các loại: từ nhà cung cấp, hoàn hàng đại lý.
- **FR-A05**: Hệ thống CHO PHÉP tạo phiếu xuất kho với các loại: cho đại lý, bán lẻ, xuất nội bộ, điều chuyển kho.
- **FR-A06**: Hệ thống TỰ ĐỘNG nhận đơn hàng từ Sale (không qua CRM), chuẩn bị hàng và xuất kho. Sau khi xuất kho, TỰ ĐỘNG đồng bộ thông tin sang Kế toán để cập nhật công nợ.
- **FR-A07**: Hệ thống CHO PHÉP tạo và theo dõi vận đơn với các trạng thái: Chờ giao, Đang giao, Đã giao, Hoàn thành, Giao thất bại.

#### Nhóm B: Quản lý Chất lượng & Lô hàng

- **FR-B01**: Hệ thống CHO PHÉP kiểm tra chất lượng (QC) trước khi nhập kho từ nhà cung cấp, hàng lỗi tự động cách ly vào Vị trí cách ly (Quarantine Location).
- **FR-B02**: Hệ thống CHO PHÉP quản lý theo lô hàng nhập: batch number, ngày nhập, hạn sử dụng (EXP) nếu có.
- **FR-B03**: Hệ thống quy định mỗi Lô hàng chỉ gán với một Hạng chất lượng (Grade A, B, C) duy nhất để tránh xung đột dữ liệu.
- **FR-B04**: Hệ thống ÁP DỤNG FEFO khi xuất hàng có hạn sử dụng, và FIFO khi xuất hàng không có hạn sử dụng.

#### Nhóm C: Barcode & Vị trí

- **FR-C01**: Hệ thống CHO PHÉP thiết lập và in barcode cho sản phẩm.
- **FR-C02**: Hệ thống CHO PHÉP quét barcode trong các thao tác: nhập, xuất, điều chuyển, kiểm kê.
- **FR-C03**: Hệ thống CHO PHÉP cấu hình cấu trúc kho: Zone/Rack/Shelf/Bin với giới hạn sức chứa cụ thể về thể tích (m³) và khối lượng chịu tải tối đa (kg).
- **FR-C04**: Hệ thống CHO PHÉP gán sản phẩm vào vị trí cụ thể và tự động kiểm tra dung tích, tải trọng của ô kệ trước khi Putaway.

#### Nhóm D: Quản lý Giá & Khuyến mãi

- **FR-D01**: Hệ thống CHO PHÉP cấu hình giá vốn (cost price) cho mỗi sản phẩm.
- **FR-D02**: Hệ thống CHO PHÉP cấu hình giá bán lẻ (retail price) và giá đại lý (dealer price) để đồng bộ tính toán tài chính.
- **FR-D03**: Hệ thống CHO PHÉP tạo chương trình khuyến mãi với: % giảm giá hoặc số tiền, thời gian áp dụng, sản phẩm áp dụng.

#### Nhóm E: Liên thông Hệ thống

- **FR-E01**: Hệ thống TỰ ĐỘNG gửi dữ liệu nhập/xuất kho sang Kế toán để tính giá vốn và COGS.
- **FR-E02**: Hệ thống TỰ ĐỘNG nhận đơn đặt hàng từ Sale (do Sale nhập thủ công) và cập nhật trạng thái khi xuất kho. KHÔNG có kết nối CRM.
- **FR-E03**: Hệ thống CHO PHÉP gửi báo cáo sản lượng nhân viên kho sang HRM để tính lương.

#### Nhóm F: Kiểm soát & Báo cáo

- **FR-F01**: Hệ thống CHO PHÉP thực hiện kiểm kê định kỳ (tháng) và đột xuất.
- **FR-F02**: Hệ thống CHO PHÉP ghi nhận hàng thất thoát, hư hỏng với nguyên nhân, người phát hiện và tự động khóa hàng hư hỏng chuyển vào Khu cách ly.
- **FR-F03**: Hệ thống CHO PHÉP tạo báo cáo chi tiết: nhập, xuất, tồn, điều chuyển, giá trị tồn kho.
- **FR-F04**: Hệ thống GỬI CẢNH BÁO khi tồn kho dưới mức tối thiểu (reorder point).
- **FR-F05**: Hệ thống CHO PHÉP xuất báo cáo định dạng Excel/PDF.

#### Nhóm G: Vận chuyển & Giao hàng

- **FR-G01**: Hệ thống CHO PHÉP quản lý fleet: gán xe vận chuyển và tài xế cho chuyến giao nhận.
- **FR-G02**: Hệ thống CHO PHÉP ghi nhận chữ ký, hình ảnh giao hàng làm bằng chứng giao hàng (POD).
- **FR-G03**: Hệ thống CHO PHÉP theo dõi trạng thái hoàn thành chuyến đi và ghi nhận thời gian giao hàng thực tế (lược bỏ đo lường nhiên liệu, km và chi phí vận tải nội bộ).

#### Nhóm H: Phân quyền & Bảo mật

- **FR-H01**: Hệ thống CHO PHÉP phân quyền theo kho: user chỉ thao tác trên kho được gán.
- **FR-H02**: Hệ thống CHO PHÉP phân quyền theo chức danh: Thủ kho, QL Kho, Kế toán, Sale có quyền khác nhau.
- **FR-H03**: Hệ thống GHI NHẬN nhật ký (audit log) cho mọi thao tác: ai làm, khi nào, làm gì.

---

## Key Entities

- **Kho (Warehouse)**: Mã kho, tên kho, địa chỉ, số điện thoại, người quản lý, cấu hình Zone/Rack/Shelf/Bin, bao gồm cả Kho ảo Đang đi đường (In-Transit Location) và Khu cách ly (Quarantine Zone).
- **Sản phẩm (Product)**: Mã SP, tên SP, đơn vị tính, barcode, mô tả, hình ảnh, giá vốn, giá bán lẻ, giá đại lý.
- **Lô hàng (Batch)**: Batch number, sản phẩm, kho, ngày nhập, hạn sử dụng (EXP), hạng (Grade duy nhất cho mỗi số lô), số lượng tồn.
- **Tồn kho (Inventory)**: Kho, sản phẩm, lô (nếu có), vị trí (Zone/Rack/Shelf/Bin), số lượng, sức chứa hiện tại, giá vốn.
- **Phiếu nhập kho (Receipt)**: Số phiếu, ngày, loại (từ NCC/Hoàn), kho nhận, người giao, người nhận, danh sách sản phẩm tốt và sản phẩm lỗi cách ly.
- **Phiếu xuất kho (Issue)**: Số phiếu, ngày, loại (đại lý/lẻ/nội bộ/điều chuyển), kho xuất, người nhận, danh sách chi tiết.
- **Đơn hàng Sale (SaleOrder)**: Số đơn, ngày, thông tin đại lý/khách hàng (tên, SĐT, địa chỉ giao), danh sách sản phẩm, số lượng, đơn giá, thời gian giao mong muốn, trạng thái (Chờ xác nhận/Đã xác nhận/Đang chuẩn bị/Đã xuất kho/Hoàn thành/Đã hủy), người tạo (Sale).
- **Đại lý (Dealer)**: Mã đại lý, tên, SĐT, địa chỉ giao hàng mặc định (Lược bỏ trường quản lý công nợ trực tiếp tại WMS - do Kế toán quản lý).
- **Nhà cung cấp (Supplier)**: Mã NCC, tên, SĐT, địa chỉ, người liên hệ.
- **Vận đơn (Delivery)**: Số vận đơn, phiếu xuất kho liên kết, xe, tài xế, trạng thái, chữ ký giao nhận (POD), hình ảnh xác thực.
- **Nhân viên kho (WarehouseStaff)**: Mã NV, tên, kho, chức danh, vai trò (role), ca làm việc.
- **Nhân viên Sale (SalesStaff)**: Mã NV, tên, SĐT, vai trò (role), khu vực phụ trách.
- **Xe vận chuyển (Vehicle)**: Mã xe, biển số, loại xe, tải trọng, trạng thái.
- **Tài xế (Driver)**: Mã TX, tên, SĐT, số GPLX, trạng thái.
- **Purchase Order (PO)**: Mã PO, nhà cung cấp, ngày tạo, ngày nhận dự kiến, trạng thái, danh sách sản phẩm.
- **Phiếu điều chuyển (Transfer)**: Mã điều chuyển, kho nguồn, kho đích, ngày tạo, ngày nhận, trạng thái, danh sách sản phẩm.
- **Phiếu kiểm kê (StockTake)**: Mã kiểm kê, kho, ngày, người thực hiện, trạng thái, danh sách chênh lệch.
- **Phiếu điều chỉnh (Adjustment)**: Mã điều chỉnh, kho, sản phẩm, số lượng điều chỉnh, lý do, người duyệt.
- **Báo cáo hư hỏng (DamageReport)**: Mã báo cáo, kho, sản phẩm, số lượng, nguyên nhân, hình ảnh.
- **Khuyến mãi (Promotion)**: Mã KM, tên, loại (% hoặc số tiền), giá trị, ngày bắt đầu, ngày kết thúc, sản phẩm áp dụng.
- **Audit Log**: ID, người dùng, hành động, bảng ảnh hưởng, dữ liệu cũ, dữ liệu mới, thời gian.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Người dùng có thể hoàn thành nghiệp vụ nhập kho trong vòng 2 phút từ lúc quét barcode đến khi xác nhận hoàn tất.
- **SC-002**: Hệ thống xử lý đồng thời tối thiểu 50 người dùng cùng thao tác mà không giảm hiệu suất (response time < 3 giây).
- **SC-003**: Báo cáo tồn kho chính xác 99.5% so với thực tế sau kiểm kê hàng tháng.
- **SC-004**: Thời gian từ lúc Sale tạo đơn hàng đến khi Kho nhận được thông báo < 1 phút.
- **SC-005**: 100% các vận đơn có bằng chứng giao hàng (chữ ký hoặc hình ảnh).
- **SC-006**: Phân quyền chính xác 100% - không có trường hợp user thao tác ngoài phạm vi được phép.
- **SC-007**: Hệ thống liên thông với Kế toán cập nhật dữ liệu trong vòng 1 phút sau khi hoàn thành nghiệp vụ kho.
- **SC-008**: Tỷ lệ thất thoát hàng hóa giảm 30% sau 6 tháng triển khai so với trước đó.
- **SC-009**: 95% đơn đặt hàng từ Sale được Kho xử lý trong ngày làm việc.
- **SC-010**: Thời gian từ lúc xuất kho đến khi đại lý nhận được thông báo xác nhận < 5 phút.

---

## Assumptions

### Về Người dùng & Môi trường

- Người dùng có kiến thức cơ bản về máy tính và smartphone.
- Kho có internet ổn định để kết nối hệ thống (WiFi hoặc 4G).
- Người giao hàng có smartphone để cập nhật trạng thái vận đơn và chụp ảnh.
- Mỗi kho có ít nhất 1 người có quyền QL Kho.
- Sale sử dụng máy tính hoặc smartphone để nhập đơn hàng vào hệ thống.

### Về Phạm vi Dự án

- Hệ thống Kho hoạt động độc lập, liên thông với các hệ thống khác qua API/message queue.
- Phiên bản đầu tiên (v1) tập trung vào 7 FRs cốt lõi (P1), các USs còn lại (P2, P3) là mở rộng.
- Không phát triển module Kế toán, HRM, CRM, Sản xuất - chỉ tích hợp qua API.
- Mobile app cho tài xế chỉ hỗ trợ cập nhật trạng thái vận đơn và chụp ảnh (không cần full features).
- KHÔNG CÓ module CRM - đại lý/khách hàng không đặt hàng online. Sale là đầu mối tiếp nhận và nhập đơn hàng thủ công.
- KHÔNG CÓ module Sản xuất - không có Work Order, không có nhập thành phẩm từ xưởng, không có Yield Rate.

### Về Dữ liệu & Tích hợp

- Dữ liệu sản phẩm hiện có sẽ được import từ file Excel vào hệ thống mới.
- Các hệ thống liên thông (Kế toán, HRM, Sale) có sẵn API hoặc sẵn sàng phát triển API integration.
- Barcode sử dụng format Code128 hoặc QR Code, được in sẵn hoặc in từ hệ thống.
- Đồng tiền tính toán: VND (Việt Nam Đồng).

### Về Quy mô & Hiệu suất

- Quy mô ước tính: 3 kho, 1000+ sản phẩm, 50+ đại lý, 1000+ transactions/tháng.
- Dữ liệu lịch sử được lưu trữ tối thiểu 5 năm.
- Backup dữ liệu hàng ngày, retention 30 ngày gần nhất.

### Về Phân quyền

- 5 cấp quyền chính: Admin (toàn quyền), QL Kho (quản lý 1-3 kho), Thủ kho (nhập/xuất, không duyệt), Sale (tạo đơn hàng, xem trạng thái, không xuất/nhập kho trực tiếp), Kế toán (chỉ đọc).
- QL Kho Hải Phòng chỉ quản lý Kho Hải Phòng, không thể thao tác trên Kho Hà Nội hoặc Kho HCM.
- Kế toán chỉ có quyền đọc (read-only) trên module kho.
- Sale chỉ có quyền tạo đơn hàng, xem trạng thái đơn hàng, không có quyền thao tác trực tiếp trên kho.
