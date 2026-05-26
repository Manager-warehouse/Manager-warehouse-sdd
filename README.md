# Hệ Thống Quản Lý Kho (Warehouse Management System)

**Feature Branch**: `001-warehouse-management-system`

**Created**: 2026-05-26

**Status**: Draft

---

## Mục Tiêu Dự Án

Xây dựng hệ thống quản lý kho độc lập phục vụ doanh nghiệp thương mại với 3 kho: Hải Phòng, Hà Nội, Hồ Chí Minh, chuyên quản lý chủ yếu các mặt hàng sản phẩm gia dụng (nồi, xoong, chảo,...). Hệ thống quản lý nhập-xuất-điều chuyển-tồn kho; hỗ trợ các quy trình nội bộ của Dispatcher (nhận đơn) và Kế toán (duyệt đơn và xem báo cáo tài chính kho) trực tiếp trên hệ thống; hỗ trợ xuất dữ liệu báo cáo cho các bộ phận Kế toán và HRM bên ngoài. Dự án hoạt động hoàn toàn độc lập và không liên quan hay tích hợp với ERP. Không bao gồm Sản xuất.

### Quy trình đặt hàng

```
Đại lý gửi yêu cầu đặt hàng
       ↓
Dispatcher lập đơn điều phối
       ↓
Kế toán duyệt + chụp ảnh hợp đồng
       ↓
Quản lý Kho duyệt
       ↓
Xuất kho → Cập nhật công nợ trong WMS (Kế toán theo dõi trực tiếp trên hệ thống)
```

### Phạm vi

| Có                              | Không                                                                          |
| ------------------------------- | ------------------------------------------------------------------------------ |
| Nhập kho, xuất kho, điều chuyển | Module Sản xuất                                                                |
| Tồn kho theo thời gian thực     | Module CRM                                                                     |
| Quản lý lô hàng (FEFO/FIFO)     | Nhập liệu thủ công, không dùng quét Barcode/QR                                 |
| QC kiểm tra chất lượng          | Module Kế toán, HRM chuyên sâu (chỉ hỗ trợ vai trò nội bộ và kết xuất dữ liệu) |
| Báo cáo đầy đủ                  | Giá do Kho tự quyết định                                                       |
| Phân quyền theo kho & chức danh |                                                                                |

---

## Priority Levels

| Priority          | Ý nghĩa      | Mô tả                  |
| ----------------- | ------------ | ---------------------- |
| **P1 - Bắt buộc** | Must Have    | Nghiệp vụ cốt lõi      |
| **P2 - Nên có**   | Should Have  | Tăng hiệu quả vận hành |
| **P3 - Nếu có**   | Nice to Have | Cải thiện trải nghiệm  |

---

## Tổng Quan User Stories

### P1 - Bắt buộc

| ID       | Tên                                    | Mô tả                                                           |
| -------- | -------------------------------------- | --------------------------------------------------------------- |
| US-WH-01 | Quản lý Danh mục Hàng hóa & Tồn kho    | Quản lý sản phẩm, đơn vị tính, tồn kho real-time                |
| US-WH-02 | Nhập kho từ Nhà cung cấp & QC          | Nhập hàng, kiểm tra QC, phân loại lưu kho                       |
| US-WH-03 | Điều chuyển Nội bộ Giữa Các Kho        | Chuyển hàng giữa 3 kho, theo dõi hàng đi đường                  |
| US-WH-04 | Xuất kho cho Đại lý & Quản lý Đơn hàng | Dispatcher lập đơn điều phối → Kế toán duyệt → Kho duyệt → Xuất |
| US-WH-05 | Báo cáo & Kiểm soát Hệ thống           | Dashboard, báo cáo tổng hợp, cảnh báo tồn kho                   |
| US-WH-06 | Kiểm kê & Điều chỉnh Tồn kho           | Kiểm kê định kỳ, điều chỉnh chênh lệch                          |
| US-WH-07 | Quản lý Trạng thái Vận chuyển          | Theo dõi vận đơn đến khi giao hàng                              |
| US-WH-23 | Báo cáo chi tiết Kho                   | Report templates đầy đủ                                         |

### P2 - Nên có

| ID       | Tên                             | Mô tả                                  |
| -------- | ------------------------------- | -------------------------------------- |
| US-WH-10 | Hoàn hàng từ Đại lý             | Xử lý hàng hoàn, QC, credit note       |
| US-WH-13 | Quản lý Lô Sản phẩm             | Batch number, expiry date, Grade, FIFO |
| US-WH-14 | Quản lý Vị trí Kho              | Zone/Rack/Shelf/Bin, sức chứa          |
| US-WH-15 | Liên thông Kho ↔ Kế toán        | Tính giá vốn, COGS, inventory value    |
| US-WH-17 | Liên thông Kho ↔ Dispatcher     | Đơn từ Dispatcher, cập nhật xuất kho   |
| US-WH-21 | Kiểm kê tháng & Báo cáo Tồn kho | Monthly stock taking, variance report  |
| US-WH-22 | Phân quyền theo Kho & Chức danh | Role-based access control              |

### P3 - Nếu có

| ID       | Tên                            | Mô tả                             |
| -------- | ------------------------------ | --------------------------------- |
| US-WH-16 | Liên thông Kho ↔ HRM           | Theo dõi công nhân kho, sản lượng |
| US-WH-18 | Kiểm soát Thất thoát & Hư hỏng | Lost items, damage report         |
| US-WH-19 | Quản lý Bằng chứng Giao hàng   | Chữ ký, hình ảnh, video           |
| US-WH-20 | Quản lý Xe vận chuyển & Tài xế | Fleet, driver assignment          |

---

## User Stories Chi tiết

### US-WH-01: Quản lý Danh mục Hàng hóa & Tồn kho (P1)

**Mô tả**: Quản lý sản phẩm, đơn vị tính, theo dõi tồn kho real-time cho 3 kho.

**Acceptance Scenarios**:

1. **Given** đăng nhập với quyền QL Kho, **When** thêm sản phẩm (mã, tên, đơn vị), **Then** sản phẩm xuất hiện trong danh mục.

2. **Given** sản phẩm đã có, **When** nhập kho 100 cái, **Then** tồn kho tăng 100, ghi nhận thời gian và người thực hiện.

3. **Given** tồn kho 50 cái, **When** xuất kho 30 cái, **Then** tồn kho còn 20 cái, không cho xuất quá tồn.

4. **Given** xem báo cáo tồn kho, **When** chọn kho Hải Phòng, **Then** hiển thị chính xác tồn kho mỗi sản phẩm.

---

### US-WH-02: Nhập kho từ Nhà cung cấp & QC (P1)

**Mô tả**: Quản lý PO từ Mua hàng, tiếp nhận, QC và phân loại: hàng đạt nhập kho thường, không đạt vào Quarantine Zone.

**Acceptance Scenarios**:

1. **Given** Mua hàng tạo PO #PO-001 với 1000kg từ NCC Việt Á, **When** hàng về kho, **Then** tạo phiếu nhận, kiểm tra số lượng.

2. **Given** hàng đang chờ QC, **When** QC kết luận "Đạt", **Then** cho phép nhập kho thường.

3. **Given** QC phát hiện 100kg "Không đạt", **When** ghi nhận, **Then** hàng lỗi vào Quarantine Location, không tính tồn khả dụng, tự động tạo khiếu nại NCC.

---

### US-WH-03: Điều chuyển Nội bộ Giữa Các Kho (P1)

**Mô tả**: Di chuyển hàng giữa 3 kho với đầy đủ chứng từ, theo dõi qua Kho ảo In-Transit.

**Acceptance Scenarios**:

1. **Given** Kho Hải Phòng có 200 sản phẩm A, Kho Hà Nội có 0, **When** điều chuyển 50 sản phẩm A sang Hà Nội, **Then** Kho HP giảm 50, In-Transit tăng 50, Kho Hà Nội giữ nguyên.

2. **Given** phiếu điều chuyển đang "Đang vận chuyển", **When** Kho Hà Nội xác nhận nhận 50 cái, **Then** In-Transit giảm 50, Kho Hà Nội tăng 50, phiếu "Hoàn thành".

3. **Given** Kho Hải Phòng chỉ có 30 cái, **When** tạo phiếu điều chuyển 50 cái, **Then** hệ thống từ chối: "Tồn kho không đủ".

---

### US-WH-04: Xuất kho cho Đại lý & Quản lý Đơn hàng (P1)

**Mô tả**: Dispatcher lập đơn điều phối → Kế toán duyệt (chụp ảnh hợp đồng) → Kho duyệt → Xuất kho → Đồng bộ công nợ.

**Quy trình**:

1. Đại lý gửi yêu cầu đặt hàng đến Dispatcher (qua điện thoại, email, Zalo, hoặc cổng portal)
2. Dispatcher lập đơn điều phối: điền khách hàng, sản phẩm, số lượng, ngày giao
3. Dispatcher xác nhận đơn điều phối → trạng thái: **CHỜ_KETOAN_DUYET**
4. **Kế toán duyệt**: bắt buộc chụp ảnh hợp đồng làm bằng chứng
   - Duyệt → **CHỜ_KHO_DUYET**
   - Từ chối → trả lại Dispatcher để điều chỉnh
5. **QL Kho duyệt**:
   - Duyệt → nhân viên chuẩn bị hàng, xuất kho
   - Từ chối → trả lại Dispatcher để điều chỉnh
6. Hệ thống cập nhật thông tin xuất kho và công nợ kho trực tiếp trong hệ thống để Kế toán theo dõi hoặc kết xuất báo cáo.

**Trạng thái đơn hàng**:
`CHỜ_KETOAN_DUYET` → `CHỜ_KHO_DUYET` → `DA_DUYET` → `DANG_CHUAN_BI` → `DA_XUAT_KHO` → `HOAN_THANH` / `DA_HUY`

**Acceptance Scenarios**:

1. **Given** Dispatcher lập đơn điều phối cho Delta 20 triệu, **When** Kế toán duyệt (chụp ảnh hợp đồng), **Then** đơn chuyển "Chờ Kho duyệt".

2. **Given** Kho duyệt và xuất kho thành công, **When** hoàn tất, **Then** tồn kho giảm, trạng thái đơn hàng và công nợ kho được cập nhật trực tiếp trên hệ thống để Kế toán theo dõi.

3. **Given** Dispatcher lập đơn điều phối cho đại lý mới, **When** lưu, **Then** cho phép tạo nhanh đại lý (tên, SĐT, địa chỉ).

4. **Given** Kế toán từ chối, **When** nhập lý do, **Then** đơn quay lại Dispatcher để điều chỉnh.

5. **Given** Kho từ chối (hết hàng), **When** nhập lý do, **Then** đơn quay lại Dispatcher để điều chỉnh.

---

### US-WH-05: Báo cáo & Kiểm soát Hệ thống (P1)

**Mô tả**: Dashboard tổng quan, báo cáo nhập-xuất-tồn, cảnh báo tồn kho thấp.

**Acceptance Scenarios**:

1. **Given** mở dashboard, **When** xem tổng quan, **Then** hiển thị: tổng tồn 3 kho, giá trị tồn, đơn chờ duyệt, đơn đang vận chuyển.

2. **Given** tồn sản phẩm A tại HCM dưới reorder point, **When** hệ thống phát hiện, **Then** gửi cảnh báo in-app đến Warehouse Manager của kho HCM và Admin.

3. **Given** chạy báo cáo nhập-xuất-tồn tháng 5, **When** xuất Excel/PDF, **Then** file có: ngày, số phiếu, sản phẩm, số lượng, kho, người thực hiện.

---

### US-WH-06: Kiểm kê & Điều chỉnh Tồn kho (P1)

**Mô tả**: Kiểm kê định kỳ hoặc đột xuất, so sánh thực tế với hệ thống, điều chỉnh khi chênh lệch.

**Acceptance Scenarios**:

1. **Given** tồn hệ thống 100 cái, **When** kiểm kê thực tế còn 95 cái, **Then** tạo phiếu điều chỉnh giảm 5 cái kèm lý do.

2. **Given** phiếu điều chỉnh đang chờ, **When** QL Kho duyệt, **Then** tồn kho cập nhật, ghi nhận người duyệt và thời gian.

3. **Given** phát hiện hư hỏng khi kiểm kê, **When** ghi nhận, **Then** tạo báo cáo hư hỏng, không điều chỉnh tồn kho thường.

---

### US-WH-07: Quản lý Trạng thái Vận chuyển (P1)

**Mô tả**: Theo dõi vận đơn từ xuất kho đến giao hàng: Chờ giao → Đang giao → Đã giao → Hoàn thành.

**Acceptance Scenarios**:

1. **Given** đơn đã xuất kho và tạo vận đơn, **When** tài xế cập nhật "Đang giao", **Then** trạng thái thay đổi, Dispatcher nhận thông báo.

2. **Given** giao thành công, **When** tài xế xác nhận và chụp ảnh biên nhận, **Then** vận đơn "Hoàn thành", ghi nhận thời gian giao.

3. **Given** giao thất bại (không nhận), **When** tài xế ghi lý do thất bại, **Then** vận đơn chuyển trạng thái "Giao thất bại", hệ thống tạo tác vụ để Warehouse Manager xác nhận hàng về kho và hoàn trả tồn kho thủ công qua phiếu điều chỉnh.

---

### US-WH-10: Hoàn hàng từ Đại lý (P2)

**Mô tả**: Xử lý hàng hoàn (hỏng, không bán, hết hạn), QC phân loại, đẩy dữ liệu sang Kế toán xử lý credit note.

**Acceptance Scenarios**:

1. **Given** Gamma hoàn 20 sản phẩm B hết hạn, **When** QC xác nhận nhập kho, **Then** tạo phiếu nhập hoàn và tự động tạo bản ghi credit note 20 triệu trong WMS để Kế toán xem xét và xử lý.

2. **Given** hàng hoàn hư hỏng nặng, **When** xác định không tái sử dụng, **Then** nhập Quarantine Location "Chờ thanh lý".

3. **Given** hàng hoàn đạt chất lượng, **When** nhập kho thường, **Then** tồn khả dụng tăng, sẵn sàng xuất.

---

### US-WH-13: Quản lý Lô Sản phẩm (P2)

**Mô tả**: Theo dõi lô nhập (Batch Number), ngày nhập, hạn sử dụng. Mỗi lô có một Grade duy nhất (A/B/C). Áp dụng FEFO cho sản phẩm có hạn, FIFO cho không có hạn.

**Acceptance Scenarios**:

1. **Given** nhập kho 500 sản phẩm Lô #LSP-2026-001-A, ngày 1/5, hạn 1 năm, Grade A, **When** lưu, **Then** ghi nhận batch, Grade, ngày nhập, EXP.

2. **Given** có 2 lô sản phẩm A: Lô 1 (hạn 1/7) và Lô 2 (hạn 1/10), **When** xuất kho, **Then** ưu tiên gợi ý xuất Lô 1 trước (FEFO).

3. **Given** lô nhập có Grade A và B, **When** nhập kho, **Then** bắt buộc tạo 2 mã lô riêng.

---

### US-WH-14: Quản lý Vị trí Kho (P2)

**Mô tả**: Cấu hình cấu trúc kho Zone/Rack/Shelf/Bin kèm sức chứa (m³, kg). Tự động kiểm tra khi putaway.

**Acceptance Scenarios**:

1. **Given** cấu hình Kho HP: Zone A, Rack A-01, Shelf 1, Bin 01 (5m³, 500kg), **When** kiểm tra, **Then** hiển thị đầy đủ cấu trúc.

2. **Given** sản phẩm 1m³/200kg, **When** putaway vào Bin còn 4m³/400kg, **Then** cho phép xếp.

3. **Given** sản phẩm 2m³/300kg, **When** putaway vào Bin chỉ còn 1m³/250kg, **Then** hệ thống từ chối do vượt sức chứa, tự động gợi ý Bin khác còn đủ dung tích.

---

### US-WH-15: Liên thông Kho ↔ Kế toán (P2)

**Mô tả**: Hỗ trợ Kế toán theo dõi và xuất báo cáo tài chính kho (giá vốn, COGS, giá trị tồn kho) ngay trên hệ thống.

**Acceptance Scenarios**:

1. **Given** xuất kho 100 sản phẩm, giá vốn 80.000đ/cái, **When** hoàn tất, **Then** hệ thống cập nhật báo cáo giá vốn và công nợ kho trị giá 8.000.000đ trực tiếp trong hệ thống để Kế toán theo dõi.

2. **Given** cuối tháng, **When** chạy báo cáo tồn kho, **Then** hiển thị và hỗ trợ xuất báo cáo tổng giá trị tồn kho theo giá vốn.

---

### US-WH-16: Liên thông Kho ↔ HRM (P3)

**Mô tả**: Ghi nhận hoạt động và sản lượng xử lý của nhân viên kho để làm căn cứ tính lương.

**Acceptance Scenarios**:

1. **Given** Tuấn đăng nhập ca sáng Kho HP, **When** vào ca, **Then** ghi nhận: ca sáng, Kho HP, thời gian vào.

2. **Given** Tuấn xuất kho 50 đơn trong ca, **When** kết ca, **Then** tổng hợp: 50 đơn, 200 sản phẩm, hỗ trợ xuất báo cáo hiệu suất phục vụ HRM tính lương.

---

### US-WH-17: Liên thông Kho ↔ Dispatcher (P2)

**Mô tả**: Dispatcher thao tác trực tiếp trên hệ thống WMS để nhận và điều phối đơn hàng từ đại lý và theo dõi trạng thái đơn hàng (không thông qua CRM bên ngoài).

**Acceptance Scenarios**:

1. **Given** Delta gửi yêu cầu đặt 100 sản phẩm A, **When** Dispatcher lập đơn điều phối trực tiếp trên hệ thống WMS, **Then** đơn chuyển trạng thái và Kho nhận thông báo để tiếp nhận xử lý.

2. **Given** Kho xuất hàng cho Delta, **When** hoàn tất, **Then** hệ thống cập nhật trạng thái "Đã xuất kho", cập nhật công nợ kho trực tiếp trong hệ thống để Dispatcher và Kế toán theo dõi.

3. **Given** đại lý mới gửi yêu cầu đặt hàng, **When** lập đơn điều phối, **Then** cho phép nhập nhanh: tên, SĐT, địa chỉ giao.

---

### US-WH-18: Kiểm soát Thất thoát & Hư hỏng (P3)

**Mô tả**: Ghi nhận mất cắp, hư hỏng, phân tích nguyên nhân và tỷ lệ.

**Acceptance Scenarios**:

1. **Given** kiểm kê thiếu 5 sản phẩm A, **When** lập báo cáo, **Then** ghi "Thất thoát - Chưa xác định", điều chỉnh tồn, thông báo QL Kho.

2. **Given** nhận hàng hoàn, phát hiện 3 sản phẩm vỡ, **When** lập báo cáo, **Then** ghi "Hư hỏng - Vận chuyển", kèm hình ảnh, gửi Dispatcher.

---

### US-WH-19: Quản lý Bằng chứng Giao hàng (P3)

**Mô tả**: Ghi nhận hình ảnh/video giao hàng, thời gian, địa điểm làm bằng chứng.

**Acceptance Scenarios**:

1. **Given** giao hàng cho Epsilon, **When** ký xác nhận trên thiết bị, **Then** lưu chữ ký số, gắn vận đơn.

2. **Given** giao xong, **When** tài xế chụp ảnh hàng và biên nhận, **Then** lưu ảnh, timestamp, GPS.

---

### US-WH-20: Quản lý Xe vận chuyển & Tài xế (P3)

**Mô tả**: Quản lý xe và tài xế để điều phối giao hàng. Chỉ gán xe, gán tài xế, ghi nhận trạng thái. Không đo xăng, km, chi phí TMS.

**Acceptance Scenarios**:

1. **Given** cần cấu hình đội xe, **When** thêm xe Toyota #HP-001 (5 tấn) và tài xế An, **Then** lưu thông tin.

2. **Given** tạo đơn vận chuyển cho Beta, **When** gán xe #HP-001 and An, **Then** hiển thị đầy đủ trên vận đơn.

3. **Given** An hoàn thành, **When** cập nhật "Giao thành công", **Then** ghi nhận hoàn tất, cập nhật phiếu xuất.

---

### US-WH-21: Kiểm kê tháng & Báo cáo Tồn kho (P2)

**Mô tả**: Kiểm kê định kỳ hàng tháng, so sánh hệ thống, báo cáo chênh lệch, gửi Kế toán.

**Acceptance Scenarios**:

1. **Given** ngày 25 hàng tháng, **When** tạo phiếu kiểm kê tháng 5, **Then** tạo danh sách đầy đủ sản phẩm cần kiểm.

2. **Given** kiểm kê xong, **When** so sánh hệ thống, **Then** hiển thị Variance: sản phẩm A chênh -5, B chênh +3.

3. **Given** variance report được duyệt, **When** kết tháng, **Then** tự động gửi báo cáo tồn cuối tháng cho Kế toán.

---

### US-WH-22: Phân quyền theo Kho & Chức danh (P2)

**Mô tả**: Kiểm soát truy cập dựa trên vai trò (Role-Based Access Control) để phân chia nhiệm vụ rõ ràng giữa các bộ phận trong hệ thống quản lý kho độc lập:

- **Admin**: Quản trị hệ thống, phân quyền người dùng.
- **Warehouse Manager**: Trưởng kho, phê duyệt các giao dịch kho, chốt số liệu.
- **Storekeeper**: Thủ kho, lập phiếu và thực hiện các thao tác vật lý trong kho.
- **Accountant**: Kế toán, duyệt đơn điều phối và theo dõi báo cáo tài chính kho.
- **Dispatcher**: Điều phối viên, lập đơn và theo dõi trạng thái giao hàng.
- **Report Viewer**: Người xem báo cáo, chỉ đọc dữ liệu báo cáo kho.

**Acceptance Scenarios**:

1. **Given** đăng nhập là Admin, **When** vào trang cấu hình người dùng, **Then** có thể tạo tài khoản và gán vai trò tương ứng cho nhân viên.

2. **Given** đăng nhập là Trưởng kho (Warehouse Manager), **When** có phiếu xuất/nhập hoặc lệnh điều chuyển nội bộ giữa 3 kho, **Then** có quyền phê duyệt/từ chối hoặc chốt biên bản kiểm kê.

3. **Given** đăng nhập là Thủ kho (Storekeeper), **When** lập phiếu nhập từ NCC hoặc phiếu xuất cho Đại lý, **Then** thực hiện lưu nháp/gửi phê duyệt lên Trưởng kho.

4. **Given** đăng nhập là Kế toán (Accountant), **When** duyệt đơn hàng do Dispatcher lập, **Then** bắt buộc phải chụp ảnh đính kèm hợp đồng mua bán làm bằng chứng pháp lý trước khi đơn được gửi tới Kho để xuất hàng.

5. **Given** đăng nhập là Người điều phối (Dispatcher), **When** tiếp nhận yêu cầu từ Đại lý, **Then** thực hiện lập đơn điều phối trực tiếp trên hệ thống WMS để chuyển tiếp sang bộ phận Kế toán.

---

### US-WH-23: Báo cáo chi tiết Kho (P1)

**Mô tả**: Báo cáo đầy đủ nhập, xuất, tồn, điều chuyển, giá trị theo nhiều chiều.

**Acceptance Scenarios**:

1. **Given** cần báo cáo nhập kho tháng 5, **When** chọn loại và thời gian, **Then** hiển thị: tổng phiếu, tổng số lượng, tổng giá trị, chi tiết.

2. **Given** cần báo cáo tồn theo sản phẩm, **When** chạy báo cáo, **Then** hiển thị: mã, tên, đơn vị, tồn mỗi kho, giá vốn, giá trị.

3. **Given** cần báo cáo điều chuyển, **When** chọn kho nguồn, kho đích, **Then** hiển thị danh sách đầy đủ phiếu điều chuyển.

---

### Edge Cases

- **Tồn kho âm**: Không cho phép, phải điều chỉnh bằng phiếu kiểm kê.
- **Đồng thời nhiều người thao tác**: Dùng locking hoặc optimistic concurrency.
- **Mất kết nối liên thông**: Queue messages để đồng bộ sau, tự động retry.
- **Nhiều lô với hạn khác nhau**: Bắt buộc FEFO - ưu tiên lô gần hạn nhất.
- **Thanh toán vượt công nợ**: Kế toán xử lý độc lập, WMS chỉ ghi nhận định lượng.
- **Tạo đơn cho đại lý mới**: Cho phép tạo nhanh ngay trong form đơn hàng.
- **Đơn hàng bị hủy sau khi chuẩn bị**: Kho kiểm tra trạng thái, quyết định hủy hay giữ hàng.

---

## Requirements

### Nhóm A: Nghiệp vụ Cốt lõi

- **FR-A01**: CHO PHÉP quản lý sản phẩm: mã, tên, đơn vị, mô tả, hình ảnh.
- **FR-A02**: CHO PHÉP theo dõi tồn kho real-time tại 3 kho và In-Transit Location.
- **FR-A03**: KHÔNG CHO PHÉP xuất kho vượt tồn (trừ QL Kho duyệt đặc biệt).
- **FR-A04**: CHO PHÉP tạo phiếu nhập kho: từ NCC, hoàn hàng đại lý.
- **FR-A05**: CHO PHÉP tạo phiếu xuất kho: cho đại lý, bán buôn, nội bộ, điều chuyển.
- **FR-A06**: TỰ ĐỘNG nhận đơn từ Dispatcher → chờ Kế toán duyệt (chụp ảnh) → chờ Kho duyệt → xuất kho → cập nhật công nợ trong WMS để Kế toán theo dõi.
- **FR-A07**: CHO PHÉP tạo và theo dõi vận đơn: Chờ giao → Đang giao → Đã giao → Hoàn thành → Giao thất bại.

### Nhóm B: Quản lý Chất lượng & Lô hàng

- **FR-B01**: CHO PHÉP QC trước nhập kho từ NCC, hàng lỗi vào Quarantine Location.
- **FR-B02**: CHO PHÉP quản lý lô: batch number, ngày nhập, EXP.
- **FR-B03**: Mỗi lô chỉ gán một Grade (A/B/C) duy nhất.
- **FR-B04**: ÁP DỤNG FEFO cho sản phẩm có hạn, FIFO cho sản phẩm không có hạn.

### Nhóm C: Vị trí Kho

- **FR-C01**: CHO PHÉP cấu hình Zone/Rack/Shelf/Bin với sức chứa (m³, kg).
- **FR-C02**: CHO PHÉP gán sản phẩm vào vị trí, tự động kiểm tra dung tích và tải trọng.

### Nhóm D: Quản lý Giá

- **FR-D01**: CHO PHÉP lưu giá vốn (do Sản xuất cung cấp).
- **FR-D02**: CHO PHÉP lưu giá bán buôn (do Sản xuất cung cấp).
- **FR-D03**: KHÔNG CHO PHÉP Kho tự ý thay đổi giá.
- **FR-D04**: CHO PHÉP Admin hoặc Warehouse Manager nhập/cập nhật giá vốn và giá bán buôn bằng cách nhập thủ công hoặc import từ file Excel do bộ phận Sản xuất cung cấp.

### Nhóm E: Tương tác nội bộ & Xuất dữ liệu

- **FR-E01**: CHO PHÉP Kế toán theo dõi và xuất báo cáo dữ liệu tài chính kho (giá vốn, COGS) trực tiếp trên hệ thống.
- **FR-E02**: CHO PHÉP Dispatcher nhận đơn và theo dõi trạng thái đơn hàng trực tiếp trên hệ thống.
- **FR-E03**: CHO PHÉP xuất báo cáo sản lượng/năng suất nhân viên kho để hỗ trợ bộ phận nhân sự (HRM) tính lương.

### Nhóm F: Kiểm soát & Báo cáo

- **FR-F01**: CHO PHÉP kiểm kê định kỳ (tháng) và đột xuất.
- **FR-F02**: CHO PHÉP ghi nhận thất thoát, hư hỏng kèm nguyên nhân, tự động khóa vào Quarantine.
- **FR-F03**: CHO PHÉP tạo báo cáo: nhập, xuất, tồn, điều chuyển, giá trị.
- **FR-F04**: GỬI CẢNH BÁO khi tồn dưới reorder point.
- **FR-F05**: CHO PHÉP xuất báo cáo Excel/PDF.

### Nhóm G: Vận chuyển & Giao hàng

- **FR-G01**: CHO PHÉP quản lý fleet: gán xe, tài xế cho chuyến giao.
- **FR-G02**: CHO PHÉP ghi nhận chữ ký, hình ảnh giao hàng (POD).
- **FR-G03**: CHO PHÉP theo dõi trạng thái hoàn thành, thời gian giao thực tế. Không đo xăng, km, chi phí.

### Nhóm H: Phân quyền & Bảo mật

- **FR-H01**: CHO PHÉP phân quyền theo kho: user chỉ thao tác kho được gán.
- FR-H02: CHO PHÉP phân quyền theo vai trò (Role-Based Access Control - RBAC): Admin, Warehouse Manager, Storekeeper, Accountant, Dispatcher, Report Viewer.
- **FR-H03**: GHI NHẬN audit log: ai, khi nào, làm gì, dữ liệu cũ/mới. CHỈ Admin và Warehouse Manager được phép xem audit log của kho mình quản lý. Admin xem được toàn bộ log hệ thống.

## Key Entities

- **Kho (Warehouse)**: Mã, tên, địa chỉ, SĐT, người quản lý, Zone/Rack/Shelf/Bin. Bao gồm In-Transit Location và Quarantine Zone.
- **Sản phẩm (Product)**: Mã, tên, đơn vị, mô tả, hình ảnh, giá vốn (Sản xuất cung cấp), giá bán buôn (Sản xuất cung cấp).
- **Lô hàng (Batch)**: Batch number, sản phẩm, kho, ngày nhập, EXP, Grade (A/B/C duy nhất), số lượng tồn.
- **Tồn kho (Inventory)**: Kho, sản phẩm, lô, vị trí, số lượng, sức chứa, giá vốn.
- **Phiếu nhập kho (Receipt)**: Số phiếu, ngày, loại (NCC/Hoàn), kho nhận, người giao, người nhận, chi tiết sản phẩm.
- **Phiếu xuất kho (Issue)**: Số phiếu, ngày, loại (đại lý/buôn/nội bộ/điều chuyển), kho xuất, người nhận, chi tiết.
- **Đơn hàng Sale (SaleOrder)**: Số đơn, ngày, đại lý (tên, SĐT, địa chỉ), sản phẩm, số lượng, đơn giá, ngày giao, trạng thái (`CHỜ_KETOAN_DUYET` → ... → `HOAN_THANH`/`DA_HUY`), người tạo.
- **Duyệt đơn hàng (SaleOrderApproval)**: Mã duyệt, đơn hàng, người duyệt (Kế toán), thời gian, kết quả (approved/rejected), ảnh hợp đồng (bắt buộc khi approved), lý do từ chối (bắt buộc khi rejected). Record được tạo cho cả hai trường hợp duyệt và từ chối để đảm bảo audit trail đầy đủ.
- **Duyệt đơn Kho (SaleOrderWarehouseApproval)**: Mã duyệt, đơn hàng, người duyệt (QL Kho), thời gian, ghi chú.
- **Đại lý (Dealer)**: Mã, tên, SĐT, địa chỉ giao mặc định.
- **Nhà cung cấp (Supplier)**: Mã, tên, SĐT, địa chỉ, người liên hệ.
- **Vận đơn (Delivery)**: Số vận đơn, phiếu xuất, xe, tài xế, trạng thái, POD (chữ ký, hình ảnh).
- **Nhân viên kho (WarehouseStaff)**: Mã, tên, kho, chức danh, vai trò, ca làm việc.
- **Người điều phối (Dispatcher)**: Mã, tên, SĐT, vai trò, khu vực phụ trách.
- **Xe vận chuyển (Vehicle)**: Mã, biển số, loại, tải trọng, trạng thái.
- **Tài xế (Driver)**: Mã, tên, SĐT, GPLX, trạng thái.
- **Purchase Order (PO)**: Mã, nhà cung cấp, ngày tạo, ngày nhận dự kiến, trạng thái, chi tiết.
- **Phiếu điều chuyển (Transfer)**: Mã, kho nguồn, kho đích, ngày tạo, ngày nhận, trạng thái, chi tiết.
- **Phiếu kiểm kê (StockTake)**: Mã, kho, ngày, người thực hiện, trạng thái, chi tiết chênh lệch.
- **Phiếu điều chỉnh (Adjustment)**: Mã, kho, sản phẩm, số lượng điều chỉnh, lý do, người duyệt.
- **Báo cáo hư hỏng (DamageReport)**: Mã, kho, sản phẩm, số lượng, nguyên nhân, hình ảnh.
- **Audit Log**: ID, người dùng, hành động, bảng ảnh hưởng, dữ liệu cũ, dữ liệu mới, thời gian.

---

## Success Criteria

- **SC-001**: Hoàn thành nhập kho trong 2 phút.
- **SC-002**: Xử lý 50 người đồng thời, response time < 3s.
- **SC-003**: Báo cáo tồn kho chính xác 99.5% sau kiểm kê hàng tháng.
- **SC-004**: Dispatcher lập đơn điều phối → Kho nhận < 1 phút.
- **SC-005**: 100% vận đơn có bằng chứng giao hàng.
- **SC-006**: Phân quyền chính xác 100%.
- **SC-007**: Kho → Kế toán cập nhật trong 1 phút.
- **SC-008**: Thất thoát giảm 30% sau 6 tháng.
- **SC-009**: 95% đơn hàng được Kho xử lý trong ngày.
- **SC-010**: Xuất kho → Đại lý nhận thông báo < 5 phút.

---

## Assumptions

### Người dùng & Môi trường

- Người dùng có kiến thức cơ bản về máy tính và smartphone.
- Kho có internet ổn định.
- Người giao hàng có smartphone cập nhật vận đơn và chụp ảnh.
- Mỗi kho có ít nhất 1 QL Kho.
- Dispatcher, Kế toán sử dụng máy tính/smartphone nhập liệu.

### Phạm vi

- Là hệ thống hoạt động hoàn toàn độc lập (independent system), không có bất kỳ liên quan hay kết nối nào đến hệ thống ERP. Các nghiệp vụ liên quan đến Dispatcher (nhận đơn và lập đơn điều phối) và Kế toán (duyệt đơn, theo dõi công nợ kho) được quản lý trực tiếp bằng phân quyền người dùng (Role-based access) trong ứng dụng.
- Hỗ trợ xuất dữ liệu báo cáo (Excel/PDF) cho Kế toán và HRM, không tích hợp hay trao đổi dữ liệu với các hệ thống ERP bên ngoài.
- Kho phục vụ lưu trữ và quản lý chủ yếu cho các mặt hàng sản phẩm gia dụng (như nồi, xoong, chảo,...).
- KHÔNG có CRM - Dispatcher lập đơn điều phối thủ công.

### Dữ liệu & Tích hợp

- Dữ liệu sản phẩm import từ Excel.
- Đồng tiền: VND.

### Quy mô

- 3 kho, 1000+ sản phẩm, 50+ đại lý, 1000+ transactions/tháng.
- Lưu trữ tối thiểu 5 năm, backup hàng ngày.

### Phân quyền theo Vai trò (Role-Based Access Control)

| Vai trò (Role)                     | Phạm vi & Quyền hạn trên hệ thống                                                                                                                 |
| :--------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Admin** (Quản trị viên)          | Toàn quyền cấu hình hệ thống, quản lý tài khoản người dùng, phân bổ quyền truy cập, sao lưu và khôi phục dữ liệu.                                 |
| **Warehouse Manager** (Trưởng kho) | Quản lý kho được gán, phê duyệt phiếu xuất/nhập, duyệt phiếu điều chuyển kho nội bộ, chốt số liệu kiểm kê và duyệt điều chỉnh.                    |
| **Storekeeper** (Thủ kho)          | Lập phiếu nhập/xuất kho (lưu nháp gửi duyệt), thực hiện các thao tác vật lý (nhận hàng, QC, xếp kệ, soạn hàng), thực hiện đếm kiểm kê.            |
| **Accountant** (Kế toán)           | Duyệt đơn điều phối từ Dispatcher (yêu cầu đính kèm ảnh hợp đồng), theo dõi COGS, giá trị tồn kho, công nợ kho và kết xuất báo cáo tài chính kho. |
| **Dispatcher** (Người điều phối)   | Lập đơn điều phối từ đại lý, tạo nhanh đại lý mới khi lập đơn, gán tài xế/xe vận chuyển và theo dõi trạng thái vận đơn.                           |
| **Report Viewer** (Chỉ xem)        | Chỉ đọc dữ liệu và xuất các báo cáo thống kê kho (tồn kho, hiệu suất, giao nhận), không được phép thực hiện giao dịch hay cấu hình.               |
| **Tác nhân ngoài** (External)      | **Nhà cung cấp (Supplier)** giao hàng tới kho; **Đại lý (Dealer)** gửi yêu cầu đơn hàng và theo dõi trạng thái qua cổng thông tin ngoài.          |
