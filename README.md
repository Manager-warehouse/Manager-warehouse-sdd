# backend
# Hệ Thống Quản Lý Kho (Warehouse Management System)

**Feature Branch**: `001-warehouse-management-system`

**Created**: 2026-05-23

**Status**: Draft

---

## Mục Tiêu Dự Án

Xây dựng hệ thống quản lý kho phục vụ doanh nghiệp thương mại với 3 kho: Hải Phòng, Hà Nội, Hồ Chí Minh. Hệ thống quản lý nhập-xuất-điều chuyển-tồn kho, tích hợp Kế toán, HRM, Sale. Không bao gồm Sản xuất.

### Quy trình đặt hàng

```
Đại lý liên hệ Sale
       ↓
Sale tạo đơn hàng
       ↓
Kế toán duyệt + chụp ảnh hợp đồng
       ↓
Quản lý Kho duyệt
       ↓
Xuất kho → Đồng bộ công nợ sang Kế toán
```

### Phạm vi

| Có | Không |
|----|----|
| Nhập kho, xuất kho, điều chuyển | Module Sản xuất |
| Tồn kho theo thời gian thực | Module CRM |
| Quản lý lô hàng (FEFO/FIFO) | Máy quét Barcode/QR |
| QC kiểm tra chất lượng | Module Kế toán, HRM (chỉ tích hợp) |
| Báo cáo đầy đủ | Giá do Kho tự quyết định |
| Phân quyền theo kho & chức danh | |

---

## Priority Levels

| Priority | Ý nghĩa | Mô tả |
|----------|---------|-------|
| **P1 - Bắt buộc** | Must Have | Nghiệp vụ cốt lõi |
| **P2 - Nên có** | Should Have | Tăng hiệu quả vận hành |
| **P3 - Nếu có** | Nice to Have | Cải thiện trải nghiệm |

---

## Tổng Quan User Stories

### P1 - Bắt buộc

| ID | Tên | Mô tả |
|----|-----|-------|
| FR-WH-01 | Quản lý Danh mục Hàng hóa & Tồn kho | Quản lý sản phẩm, đơn vị tính, tồn kho real-time |
| FR-WH-02 | Nhập kho từ Nhà cung cấp & QC | Nhập hàng, kiểm tra QC, phân loại lưu kho |
| FR-WH-03 | Điều chuyển Nội bộ Giữa Các Kho | Chuyển hàng giữa 3 kho, theo dõi hàng đi đường |
| FR-WH-04 | Xuất kho cho Đại lý & Quản lý Đơn hàng Sale | Sale tạo → Kế toán duyệt → Kho duyệt → Xuất |
| FR-WH-05 | Báo cáo & Kiểm soát Hệ thống | Dashboard, báo cáo tổng hợp, cảnh báo tồn kho |
| FR-WH-06 | Kiểm kê & Điều chỉnh Tồn kho | Kiểm kê định kỳ, điều chỉnh chênh lệch |
| FR-WH-07 | Quản lý Trạng thái Vận chuyển | Theo dõi vận đơn đến khi giao hàng |
| FR-WH-23 | Báo cáo chi tiết Kho | Report templates đầy đủ |

### P2 - Nên có

| ID | Tên | Mô tả |
|----|-----|-------|
| US-WH-10 | Hoàn hàng từ Đại lý | Xử lý hàng hoàn, QC, credit note |
| US-WH-13 | Quản lý Lô Sản phẩm | Batch number, expiry date, Grade, FIFO |
| US-WH-14 | Quản lý Vị trí Kho | Zone/Rack/Shelf/Bin, sức chứa |
| US-WH-15 | Liên thông Kho ↔ Kế toán | Tính giá vốn, COGS, inventory value |
| US-WH-17 | Liên thông Kho ↔ Sale | Đơn từ Sale, cập nhật xuất kho |
| US-WH-21 | Kiểm kê tháng & Báo cáo Tồn kho | Monthly stock taking, variance report |
| US-WH-22 | Phân quyền theo Kho & Chức danh | Role-based access control |

### P3 - Nếu có

| ID | Tên | Mô tả |
|----|-----|-------|
| US-WH-16 | Liên thông Kho ↔ HRM | Theo dõi công nhân kho, sản lượng |
| US-WH-18 | Kiểm soát Thất thoát & Hư hỏng | Lost items, damage report |
| US-WH-19 | Quản lý Bằng chứng Giao hàng | Chữ ký, hình ảnh, video |
| US-WH-20 | Quản lý Xe vận chuyển & Tài xế | Fleet, driver assignment |

---

## User Stories Chi tiết

### FR-WH-01: Quản lý Danh mục Hàng hóa & Tồn kho (P1)

**Mô tả**: Quản lý sản phẩm, đơn vị tính, theo dõi tồn kho real-time cho 3 kho.

**Acceptance Scenarios**:

1. **Given** đăng nhập với quyền QL Kho, **When** thêm sản phẩm (mã, tên, đơn vị), **Then** sản phẩm xuất hiện trong danh mục.

2. **Given** sản phẩm đã có, **When** nhập kho 100 cái, **Then** tồn kho tăng 100, ghi nhận thời gian và người thực hiện.

3. **Given** tồn kho 50 cái, **When** xuất kho 30 cái, **Then** tồn kho còn 20 cái, không cho xuất quá tồn.

4. **Given** xem báo cáo tồn kho, **When** chọn kho Hải Phòng, **Then** hiển thị chính xác tồn kho mỗi sản phẩm.

---

### FR-WH-02: Nhập kho từ Nhà cung cấp & QC (P1)

**Mô tả**: Quản lý PO từ Mua hàng, tiếp nhận, QC và phân loại: hàng đạt nhập kho thường, không đạt vào Quarantine Zone.

**Acceptance Scenarios**:

1. **Given** Mua hàng tạo PO #PO-001 với 1000kg từ NCC Việt Á, **When** hàng về kho, **Then** tạo phiếu nhận, kiểm tra số lượng.

2. **Given** hàng đang chờ QC, **When** QC kết luận "Đạt", **Then** cho phép nhập kho thường.

3. **Given** QC phát hiện 100kg "Không đạt", **When** ghi nhận, **Then** hàng lỗi vào Quarantine Location, không tính tồn khả dụng, tự động tạo khiếu nại NCC.

---

### FR-WH-03: Điều chuyển Nội bộ Giữa Các Kho (P1)

**Mô tả**: Di chuyển hàng giữa 3 kho với đầy đủ chứng từ, theo dõi qua Kho ảo In-Transit.

**Acceptance Scenarios**:

1. **Given** Kho Hải Phòng có 200 sản phẩm A, Kho Hà Nội có 0, **When** điều chuyển 50 sản phẩm A sang Hà Nội, **Then** Kho HP giảm 50, In-Transit tăng 50, Kho Hà Nội giữ nguyên.

2. **Given** phiếu điều chuyển đang "Đang vận chuyển", **When** Kho Hà Nội xác nhận nhận 50 cái, **Then** In-Transit giảm 50, Kho Hà Nội tăng 50, phiếu "Hoàn thành".

3. **Given** Kho Hải Phòng chỉ có 30 cái, **When** tạo phiếu điều chuyển 50 cái, **Then** hệ thống từ chối: "Tồn kho không đủ".

---

### FR-WH-04: Xuất kho cho Đại lý & Quản lý Đơn hàng Sale (P1)

**Mô tả**: Sale tạo đơn → Kế toán duyệt (chụp ảnh hợp đồng) → Kho duyệt → Xuất kho → Đồng bộ công nợ.

**Quy trình**:
1. Đại lý liên hệ Sale báo giá/hợp đồng (điện thoại, email, Zalo, gặp trực tiếp)
2. Sale điền thông tin đơn hàng: khách hàng, sản phẩm, số lượng, ngày giao
3. Sale xác nhận → trạng thái: **CHỜ_KETOAN_DUYET**
4. **Kế toán duyệt**: bắt buộc chụp ảnh hợp đồng làm bằng chứng
   - Duyệt → **CHỜ_KHO_DUYET**
   - Từ chối → trả lại Sale kèm lý do
5. **QL Kho duyệt**:
   - Duyệt → nhân viên chuẩn bị hàng, xuất kho
   - Từ chối → trả lại Sale kèm lý do
6. Hệ thống tự động gửi xuất kho sang Kế toán xử lý công nợ

**Trạng thái đơn hàng**:
`CHỜ_KETOAN_DUYET` → `CHỜ_KHO_DUYET` → `DA_DUYET` → `DANG_CHUAN_BI` → `DA_XUAT_KHO` → `HOAN_THANH` / `DA_HUY`

**Acceptance Scenarios**:

1. **Given** Sale tạo đơn cho Delta 20 triệu, **When** Kế toán duyệt (chụp ảnh hợp đồng), **Then** đơn chuyển "Chờ Kho duyệt".

2. **Given** Kho duyệt và xuất kho thành công, **When** hoàn tất, **Then** tồn kho giảm, tự động gửi API sang Kế toán.

3. **Given** Sale tạo đơn cho đại lý mới, **When** lưu, **Then** cho phép tạo nhanh đại lý (tên, SĐT, địa chỉ).

4. **Given** Kế toán từ chối, **When** nhập lý do, **Then** Sale nhận thông báo từ chối.

5. **Given** Kho từ chối (hết hàng), **When** nhập lý do, **Then** Sale nhận thông báo.

---

### FR-WH-05: Báo cáo & Kiểm soát Hệ thống (P1)

**Mô tả**: Dashboard tổng quan, báo cáo nhập-xuất-tồn, cảnh báo tồn kho thấp.

**Acceptance Scenarios**:

1. **Given** mở dashboard, **When** xem tổng quan, **Then** hiển thị: tổng tồn 3 kho, giá trị tồn, đơn chờ duyệt, đơn đang vận chuyển.

2. **Given** tồn sản phẩm A tại HCM dưới reorder point, **When** phát hiện, **Then** gửi cảnh báo QL Kho và Mua hàng.

3. **Given** chạy báo cáo nhập-xuất-tồn tháng 5, **When** xuất Excel/PDF, **Then** file có: ngày, số phiếu, sản phẩm, số lượng, kho, người thực hiện.

---

### FR-WH-06: Kiểm kê & Điều chỉnh Tồn kho (P1)

**Mô tả**: Kiểm kê định kỳ hoặc đột xuất, so sánh thực tế với hệ thống, điều chỉnh khi chênh lệch.

**Acceptance Scenarios**:

1. **Given** tồn hệ thống 100 cái, **When** kiểm kê thực tế còn 95 cái, **Then** tạo phiếu điều chỉnh giảm 5 cái kèm lý do.

2. **Given** phiếu điều chỉnh đang chờ, **When** QL Kho duyệt, **Then** tồn kho cập nhật, ghi nhận người duyệt và thời gian.

3. **Given** phát hiện hư hỏng khi kiểm kê, **When** ghi nhận, **Then** tạo báo cáo hư hỏng, không điều chỉnh tồn kho thường.

---

### FR-WH-07: Quản lý Trạng thái Vận chuyển (P1)

**Mô tả**: Theo dõi vận đơn từ xuất kho đến giao hàng: Chờ giao → Đang giao → Đã giao → Hoàn thành.

**Acceptance Scenarios**:

1. **Given** đơn đã xuất kho và tạo vận đơn, **When** tài xế cập nhật "Đang giao", **Then** trạng thái thay đổi, Sale nhận thông báo.

2. **Given** giao thành công, **When** tài xế xác nhận và chụp ảnh biên nhận, **Then** vận đơn "Hoàn thành", ghi nhận thời gian giao.

3. **Given** giao thất bại (không nhận), **When** tài xế ghi lý do, **Then** vận đơn "Giao thất bại", hàng quay về kho.

---

### US-WH-10: Hoàn hàng từ Đại lý (P2)

**Mô tả**: Xử lý hàng hoàn (hỏng, không bán, hết hạn), QC phân loại, đẩy dữ liệu sang Kế toán xử lý credit note.

**Acceptance Scenarios**:

1. **Given** Gamma hoàn 20 sản phẩm B hết hạn, **When** QC xác nhận nhập kho, **Then** tạo phiếu nhập hoàn, tự động đẩy sang Kế toán xử lý credit note 20 triệu.

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

3. **Given** sản phẩm 2m³/300kg, **When** putaway vào Bin còn 4m³/400kg, **Then** từ chối, gợi ý Bin khác.

---

### US-WH-15: Liên thông Kho ↔ Kế toán (P2)

**Mô tả**: Tự động gửi dữ liệu nhập/xuất sang Kế toán tính giá vốn, COGS, inventory value.

**Acceptance Scenarios**:

1. **Given** xuất kho 100 sản phẩm, giá vốn 80.000đ/cái, **When** hoàn tất, **Then** gửi Kế toán: Nợ công nợ phải thu / Có hàng tồn kho 8.000.000đ.

2. **Given** cuối tháng, **When** chạy báo cáo tồn kho, **Then** tự động gửi Kế toán: tổng giá trị tồn theo giá vốn.

---

### US-WH-16: Liên thông Kho ↔ HRM (P3)

**Mô tả**: Theo dõi công nhân kho, phân ca, ghi nhận sản lượng để tính lương.

**Acceptance Scenarios**:

1. **Given** Tuấn đăng nhập ca sáng Kho HP, **When** vào ca, **Then** ghi nhận: ca sáng, Kho HP, thời gian vào.

2. **Given** Tuấn xuất kho 50 đơn trong ca, **When** kết ca, **Then** tổng hợp: 50 đơn, 200 sản phẩm, gửi HRM tính lương.

---

### US-WH-17: Liên thông Kho ↔ Sale (P2)

**Mô tả**: Sale nhập đơn từ đại lý (không qua CRM). Kho nhận và xử lý. Sale được thông báo cập nhật trạng thái.

**Acceptance Scenarios**:

1. **Given** Delta liên hệ Sale đặt 100 sản phẩm A, **When** Sale xác nhận đơn, **Then** Kho nhận thông báo, bắt đầu chuẩn bị.

2. **Given** Kho xuất hàng cho Delta, **When** hoàn tất, **Then** Sale nhận thông báo "Đã xuất kho", cập nhật trạng thái, gửi Kế toán cập nhật công nợ.

3. **Given** đại lý mới liên hệ Sale, **When** tạo đơn, **Then** cho phép nhập nhanh: tên, SĐT, địa chỉ giao.

---

### US-WH-18: Kiểm soát Thất thoát & Hư hỏng (P3)

**Mô tả**: Ghi nhận mất cắp, hư hỏng, phân tích nguyên nhân và tỷ lệ.

**Acceptance Scenarios**:

1. **Given** kiểm kê thiếu 5 sản phẩm A, **When** lập báo cáo, **Then** ghi "Thất thoát - Chưa xác định", điều chỉnh tồn, thông báo QL Kho.

2. **Given** nhận hàng hoàn, phát hiện 3 sản phẩm vỡ, **When** lập báo cáo, **Then** ghi "Hư hỏng - Vận chuyển", kèm hình ảnh, gửi QL Sale.

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

2. **Given** tạo đơn vận chuyển cho Beta, **When** gán xe #HP-001 và An, **Then** hiển thị đầy đủ trên vận đơn.

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

**Mô tả**: Kiểm soát truy cập theo kho và chức danh: QL Kho chỉ quản kho được gán, Thủ kho chỉ xuất/nhập, Kế toán chỉ duyệt đơn, Sale chỉ tạo đơn.

**Acceptance Scenarios**:

1. **Given** Minh có vai trò "QL Kho Hải Phòng", **When** đăng nhập, **Then** chỉ thấy và thao tác Kho Hải Phòng.

2. **Given** Lan có vai trò "Thủ kho", **When** đăng nhập, **Then** xuất/nhập được, không duyệt điều chuyển.

3. **Given** Kế toán Trang đăng nhập, **When** vào module kho, **Then** chỉ xem báo cáo, không tạo phiếu.

4. **Given** Sale Hùng đăng nhập, **When** vào module kho, **Then** tạo đơn, xem trạng thái, không xuất/nhập trực tiếp.

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
- **FR-A06**: TỰ ĐỘNG nhận đơn từ Sale → chờ Kế toán duyệt (chụp ảnh) → chờ Kho duyệt → xuất kho → đồng bộ Kế toán.
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
- **FR-D04**: CHO PHÉP tạo khuyến mãi: % giảm hoặc số tiền, thời gian, sản phẩm.

### Nhóm E: Liên thông Hệ thống

- **FR-E01**: TỰ ĐỘNG gửi nhập/xuất kho sang Kế toán (giá vốn, COGS).
- **FR-E02**: TỰ ĐỘNG nhận đơn từ Sale, cập nhật trạng thái khi xuất. KHÔNG có CRM.
- **FR-E03**: CHO PHÉP gửi sản lượng nhân viên kho sang HRM tính lương.

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
- **FR-H02**: CHO PHÉP phân quyền theo chức danh: Thủ kho, QL Kho, Kế toán, Sale.
- **FR-H03**: GHI NHẬN audit log: ai, khi nào, làm gì.

---

## Key Entities

- **Kho (Warehouse)**: Mã, tên, địa chỉ, SĐT, người quản lý, Zone/Rack/Shelf/Bin. Bao gồm In-Transit Location và Quarantine Zone.
- **Sản phẩm (Product)**: Mã, tên, đơn vị, mô tả, hình ảnh, giá vốn (Sản xuất cung cấp), giá bán buôn (Sản xuất cung cấp).
- **Lô hàng (Batch)**: Batch number, sản phẩm, kho, ngày nhập, EXP, Grade (A/B/C duy nhất), số lượng tồn.
- **Tồn kho (Inventory)**: Kho, sản phẩm, lô, vị trí, số lượng, sức chứa, giá vốn.
- **Phiếu nhập kho (Receipt)**: Số phiếu, ngày, loại (NCC/Hoàn), kho nhận, người giao, người nhận, chi tiết sản phẩm.
- **Phiếu xuất kho (Issue)**: Số phiếu, ngày, loại (đại lý/buôn/nội bộ/điều chuyển), kho xuất, người nhận, chi tiết.
- **Đơn hàng Sale (SaleOrder)**: Số đơn, ngày, đại lý (tên, SĐT, địa chỉ), sản phẩm, số lượng, đơn giá, ngày giao, trạng thái (`CHỜ_KETOAN_DUYET` → ... → `HOAN_THANH`/`DA_HUY`), người tạo.
- **Duyệt đơn hàng (SaleOrderApproval)**: Mã duyệt, đơn hàng, người duyệt (Kế toán), thời gian, ảnh hợp đồng, ghi chú.
- **Duyệt đơn Kho (SaleOrderWarehouseApproval)**: Mã duyệt, đơn hàng, người duyệt (QL Kho), thời gian, ghi chú.
- **Đại lý (Dealer)**: Mã, tên, SĐT, địa chỉ giao mặc định.
- **Nhà cung cấp (Supplier)**: Mã, tên, SĐT, địa chỉ, người liên hệ.
- **Vận đơn (Delivery)**: Số vận đơn, phiếu xuất, xe, tài xế, trạng thái, POD (chữ ký, hình ảnh).
- **Nhân viên kho (WarehouseStaff)**: Mã, tên, kho, chức danh, vai trò, ca làm việc.
- **Nhân viên Sale (SalesStaff)**: Mã, tên, SĐT, vai trò, khu vực phụ trách.
- **Xe vận chuyển (Vehicle)**: Mã, biển số, loại, tải trọng, trạng thái.
- **Tài xế (Driver)**: Mã, tên, SĐT, GPLX, trạng thái.
- **Purchase Order (PO)**: Mã, nhà cung cấp, ngày tạo, ngày nhận dự kiến, trạng thái, chi tiết.
- **Phiếu điều chuyển (Transfer)**: Mã, kho nguồn, kho đích, ngày tạo, ngày nhận, trạng thái, chi tiết.
- **Phiếu kiểm kê (StockTake)**: Mã, kho, ngày, người thực hiện, trạng thái, chi tiết chênh lệch.
- **Phiếu điều chỉnh (Adjustment)**: Mã, kho, sản phẩm, số lượng điều chỉnh, lý do, người duyệt.
- **Báo cáo hư hỏng (DamageReport)**: Mã, kho, sản phẩm, số lượng, nguyên nhân, hình ảnh.
- **Khuyến mãi (Promotion)**: Mã, tên, loại, giá trị, ngày bắt đầu, ngày kết thúc, sản phẩm.
- **Audit Log**: ID, người dùng, hành động, bảng ảnh hưởng, dữ liệu cũ, dữ liệu mới, thời gian.

---

## Success Criteria

- **SC-001**: Hoàn thành nhập kho trong 2 phút.
- **SC-002**: Xử lý 50 người đồng thời, response time < 3s.
- **SC-003**: Báo cáo tồn kho chính xác 99.5% sau kiểm kê hàng tháng.
- **SC-004**: Sale tạo đơn → Kho nhận < 1 phút.
- **SC-005**: 100% vận đơn có bằng chứng giao hàng.
- **SC-006**: Phân quyền chính xác 100%.
- **SC-007**: Kho → Kế toán cập nhật trong 1 phút.
- **SC-008**: Thất thoát giảm 30% sau 6 tháng.
- **SC-009**: 95% đơn Sale được Kho xử lý trong ngày.
- **SC-010**: Xuất kho → Đại lý nhận thông báo < 5 phút.

---

## Assumptions

### Người dùng & Môi trường

- Người dùng có kiến thức cơ bản về máy tính và smartphone.
- Kho có internet ổn định.
- Người giao hàng có smartphone cập nhật vận đơn và chụp ảnh.
- Mỗi kho có ít nhất 1 QL Kho.
- Sale, Kế toán sử dụng máy tính/smartphone nhập liệu.

### Phạm vi

- Chỉ tích hợp Kế toán, HRM, Sale, Sản xuất qua API, không phát triển module riêng.
- KHÔNG có CRM - Sale nhập đơn thủ công.
- KHÔNG có Sản xuất - không có Work Order, nhập thành phẩm.
- KHÔNG có máy quét Barcode/QR - nhập thủ công.
- Giá do Sản xuất cung cấp, Kho chỉ lưu trữ.
- Chỉ bán buôn, không bán lẻ.

### Dữ liệu & Tích hợp

- Dữ liệu sản phẩm import từ Excel.
- Đồng tiền: VND.

### Quy mô

- 3 kho, 1000+ sản phẩm, 50+ đại lý, 1000+ transactions/tháng.
- Lưu trữ tối thiểu 5 năm, backup hàng ngày.

### Phân quyền

| Vai trò | Quyền |
|---------|-------|
| Admin | Toàn quyền |
| QL Kho | Quản lý 1-3 kho, duyệt đơn |
| Thủ kho | Nhập/xuất, không duyệt |
| Kế toán | Duyệt đơn, chụp ảnh hợp đồng, chỉ đọc báo cáo |
| Sale | Tạo đơn, xem trạng thái |
| Đọc báo cáo | Chỉ đọc |
