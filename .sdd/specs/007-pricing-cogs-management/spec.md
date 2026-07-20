# Feature Specification: Bảng giá & Giá vốn (Pricing & COGS)

**Spec ID**: 007-pricing-cogs-management
**Created**: 2026-05-30
**Updated**: 2026-06-17
**Status**: Draft
**Features**: US-WMS-14

---

## 1. Context and Goal

Giá bán và giá vốn hàng hóa thay đổi theo từng kỳ kinh doanh. Hệ thống cần duy trì lịch sử giá (`price_history`) để:

1. Tra cứu giá bán đúng kỳ khi lập Đơn xuất (DO) → tính doanh thu và kiểm tra Credit Check.
2. Ghi nhận giá vốn (COGS) tại ngày xuất hàng → phục vụ hạch toán P&L.
3. Cung cấp lịch sử biến động giá để so sánh, kiểm soát nội bộ.

Kế toán viên nhập bảng giá theo kỳ; Kế toán trưởng duyệt để kích hoạt. Chỉ bản giá đã `APPROVED` mới được phép sử dụng trong giao dịch.

### Features List
* [US-WMS-14a: Kế toán viên Quản lý Bản giá Đơn lẻ](./features/feature-accountant-price-entry-management.md)
* [US-WMS-14b: Kế toán viên Import Bảng giá từ Excel](./features/feature-accountant-price-import-excel.md)
* [US-WMS-14c: Kế toán trưởng Duyệt Bản giá](./features/feature-accountant-manager-price-approval.md)
* [US-WMS-14d: Hệ thống Tra cứu Giá & Snapshot COGS vào Giao dịch](./features/feature-system-cogs-calculation.md)

---

## 2. Clarifications

### Session 2026-06-17

**Q: `end_date` bắt buộc hay tùy chọn khi tạo?**
A: Bắt buộc (`NOT NULL`). Không cho phép tạo khoảng giá vô thời hạn. Phải luôn khai báo cả `effective_date` và `end_date`. Ngày `end_date` là ngày cuối cùng bản giá còn hiệu lực (inclusive). Ví dụ: `effective_date = 2026-06-01`, `end_date = 2026-06-30` → bản giá có hiệu lực từ ngày 1 đến hết ngày 30 tháng 6.

> **Lý do**: Bản giá vô thời hạn gây mơ hồ trong tra cứu và báo cáo. Kỳ kinh doanh của Phúc Anh được xác định rõ ràng (tháng/quý), nên không có nhu cầu giá "không có ngày kết thúc".
>
> **[SUPERSEDED — Session 2026-07-09]**: `end_date` đã bị bỏ. Xem Session 2026-07-09 bên dưới.

**Q: Nếu tạo bản giá mới có ngày trùng/giao với bản giá `APPROVED` đang tồn tại thì xử lý thế nào?**
A: Hệ thống từ chối tạo với lỗi `OVERLAPPING_EFFECTIVE_DATE` (HTTP 409). Không tự động đóng/thu hẹp bản giá cũ. Kế toán viên phải đảm bảo khoảng ngày không overlap với bất kỳ bản giá `APPROVED` nào cho cùng `product_id`.

> Overlap kiểm tra logic: `new_effective_date <= existing_end_date AND new_end_date >= existing_effective_date`. Bản giá `PENDING` hoặc `CANCELLED` không tham gia kiểm tra overlap.
>
> **[SUPERSEDED — Session 2026-07-09]**: Khái niệm "overlap khoảng ngày" không còn áp dụng. Xem Session 2026-07-09 bên dưới.

### Session 2026-07-09

**Q: Giữ nguyên quyết định "`end_date` bắt buộc" (Session 2026-06-17) hay đổi sang mô hình effective-date-only?**
A: Đổi sang mô hình effective-date-only. Bỏ cột `end_date`. Một bản giá `APPROVED` có hiệu lực kể từ `effective_date` cho đến khi có một bản giá `APPROVED` khác của cùng `(product_id, warehouse_id)` mang `effective_date` mới hơn (hoặc vô thời hạn nếu chưa có bản nào mới hơn). Tra cứu giá có hiệu lực tại ngày D = bản giá `APPROVED` của `(product_id, warehouse_id)` có `effective_date <= D`, sắp xếp `effective_date DESC`, lấy dòng đầu tiên.

> **Lý do thay đổi**: Với mô hình `end_date`, sửa một bản giá `APPROVED` bị nhập sai chỉ có hai cách, cả hai đều có rủi ro: (1) API riêng để thu hẹp `end_date` của bản cũ trước khi bản mới được duyệt — nếu hai thao tác không atomic thì vẫn có thể hở ra một khoảng ngày không có giá `APPROVED`; (2) revert bản `APPROVED` sai về `PENDING` để sửa lại — trong lúc chờ Kế toán trưởng duyệt lại, sản phẩm đó **không có bản giá `APPROVED` nào**, và trong khoảng đó `DeliveryOrderServiceImpl` (spec 004) chặn cứng mọi Đơn xuất chứa sản phẩm này với `MISSING_PRICE` (xem `feature-system-cogs-calculation.md` §3.1). Mô hình effective-date-only loại bỏ vấn đề tận gốc: bản giá cũ — dù nhập sai — không bao giờ cần bị sửa hay revert; sửa giá chỉ đơn giản là tạo và duyệt một bản ghi mới với `effective_date = hôm nay`, bản ghi đó tự động thay thế bản cũ ngay khi được duyệt, không có khoảng trống giá và không cần API/logic sửa riêng cho bản `APPROVED`.

**Q: Đánh đổi (trade-off) khi bỏ `end_date` là gì?**
A: Mất khả năng khai báo một bản giá tự hết hạn trong một hành động duy nhất (ví dụ: giá khuyến mãi tháng 7, tự động quay về giá thường vào tháng 8). Với mô hình mới, Kế toán viên phải tạo thêm một bản giá thứ hai với `effective_date` là ngày muốn khôi phục giá thường; nếu quên tạo bản thứ hai, giá khuyến mãi sẽ tiếp tục có hiệu lực vô thời hạn. Sprint 1 không có nhu cầu dynamic/promotional pricing (xem mục 10 Out of Scope), nên đánh đổi này được chấp nhận.

**Q: Overlap check còn cần thiết không nếu không còn `end_date`?**
A: Còn, nhưng đơn giản hơn nhiều. Không còn khái niệm "giao khoảng ngày" — xung đột chỉ còn xảy ra khi hai bản của cùng `(product_id, warehouse_id)` có **cùng `effective_date`** (không rõ bản nào là giá áp dụng cho ngày đó). Điều kiện chặn: từ chối tạo nếu đã tồn tại một bản `APPROVED` khác cùng `(product_id, warehouse_id, effective_date)`.
>
> **[SUPERSEDED — Session 2026-07-12]**: (1) Không còn re-check tại approval; (2) điều kiện chặn được mở rộng sang cả bản `PENDING`, không chỉ `APPROVED`. Xem Session 2026-07-12 bên dưới.

**Q: Có cần migration/refactor gì ở tầng dữ liệu không?**
A: Có. `price_history.end_date` chuyển từ `NOT NULL` sang bị loại bỏ khỏi schema (migration mới, không sửa migration cũ). `PriceHistoryRepository.findApprovedAtDate` và `findApprovedOverlapping` cần viết lại theo logic "lấy `effective_date` gần nhất không vượt quá ngày tra cứu". `findLatestApproved` không cần đổi vì đã sắp xếp theo `effectiveDate DESC` và không dùng `end_date`. Chi tiết kỹ thuật thuộc phạm vi plan/implementation, không lặp lại ở đây.

**Q: Overlap check áp dụng ở thời điểm tạo hay thời điểm duyệt?**
A: Áp dụng ở **cả hai** thời điểm. Khi tạo (để feedback sớm cho Kế toán viên) và khi duyệt (để đảm bảo tính toàn vẹn — có thể có bản giá `APPROVED` khác được tạo trong khoảng thời gian chờ duyệt).
>
> **[SUPERSEDED — Session 2026-07-12]**: Chỉ còn áp dụng khi tạo. Xem Session 2026-07-12 bên dưới.

### Session 2026-07-12

**Q: Overlap check tại approval (re-check) có nên giữ lại không?**
A: Bỏ. Overlap/conflicting-effective-date chỉ còn được kiểm tra **tại thời điểm tạo/sửa** (`POST /api/v1/price-history` và `PUT /api/v1/price-history/{id}`). `PUT /api/v1/price-history/{id}/approve` không còn re-check.

> **Lý do**: Trường hợp thực tế xảy ra — một bản giá bị nhập sai (`selling_price` lệch nhiều bậc, ví dụ hàng tỷ đồng thay vì hàng trăm nghìn) đã được duyệt và chặn cứng toàn bộ Đơn xuất cho sản phẩm đó (`MISSING_PRICE`/Credit Check chặn do giá trị đơn quá lớn). Việc sửa lại chỉ có một đường: Kế toán viên tạo bản giá đúng, Kế toán trưởng duyệt. Nếu approval có thể bất ngờ thất bại vì một bản khác đã xuất hiện cùng `effective_date` từ lúc tạo đến lúc duyệt (race condition hiếm gặp), đúng lúc cần sửa gấp lại là lúc dễ gặp friction nhất. Với quy mô vận hành hiện tại (workflow chỉ 1 Kế toán viên + 1 Kế toán trưởng, tần suất tạo bản giá thấp), rủi ro race condition thực tế rất nhỏ so với chi phí của một bước duyệt có thể thất bại bất ngờ.

**Q: Điều kiện chặn khi tạo có nên chỉ xét bản `APPROVED`, hay cả `PENDING`?**
A: Cả hai. Mở rộng từ "chỉ chặn nếu trùng với bản `APPROVED`" sang "chặn nếu trùng với bất kỳ bản nào chưa `CANCELLED` (`PENDING` hoặc `APPROVED`)".

> **Lý do**: Bản giá `PENDING` đã có sẵn hành động sửa (`PUT /api/v1/price-history/{id}`, Session 2026-07-09/14a). Nếu vẫn cho phép tạo một bản `PENDING` thứ hai trùng `effective_date` với một bản `PENDING` đang tồn tại, hệ thống có hai đường song song để "sửa" cùng một thứ — sửa bản cũ, hoặc tạo bản mới đè lên — dẫn đến các cặp bản ghi trùng ngày, trùng SKU nằm cạnh nhau trong danh sách chờ duyệt mà không ai chủ động dọn, dễ gây nhầm lẫn cho người duyệt (bản nào là "đúng"?). Không có lý do nghiệp vụ nào cần hai bản `PENDING` cùng ngày cùng tồn tại — nếu bản `PENDING` hiện tại sai, sửa nó; nếu cần đổi hẳn phương án, hủy (`CANCELLED`) rồi tạo lại.

**Q: Việc mở rộng điều kiện chặn sang cả `PENDING` có xung đột với việc bỏ re-check tại approval không?**
A: Không — hai quyết định này bổ trợ nhau. Vì tạo/sửa giờ đã chặn trùng `effective_date` với cả `PENDING` lẫn `APPROVED`, việc hai bản `PENDING` cùng ngày cùng tồn tại (kịch bản duy nhất từng khiến re-check tại approval có ý nghĩa) không còn xảy ra được trong luồng bình thường nữa. Nói cách khác: mở rộng điều kiện chặn khi tạo giải quyết tận gốc race condition mà re-check tại approval từng cố chặn — nên bỏ re-check tại approval không mở ra lỗ hổng nghiệp vụ mới, chỉ còn một khe hở race condition kỹ thuật thuần túy (xem câu hỏi tiếp theo).

**Q: Hai bản `APPROVED` cùng `effective_date` còn có thể xảy ra không, và nếu có thì giá nào được dùng khi tra cứu?**
A: Về mặt lý thuyết vẫn còn một khe hở race condition tạo/tạo (hai request `POST /price-history` gửi gần như đồng thời, cả hai cùng vượt qua check `findConflictingActive` trước khi bản còn lại kịp ghi vào DB) — nhưng khe hở này đã được chặn cứng ở tầng database (xem câu hỏi tiếp theo), nên trên thực tế hai bản không-`CANCELLED` trùng ngày không còn tồn tại được nữa, kể cả trong race condition. Cơ chế tie-break `ORDER BY effective_date DESC, approved_at DESC` vẫn được giữ lại như một lớp phòng thủ bổ sung (defense-in-depth) phòng trường hợp có dữ liệu cũ trùng ngày còn sót lại từ trước khi ràng buộc DB được thêm vào.

**Q: Tại sao lại thêm ràng buộc UNIQUE INDEX ở tầng database thay vì chỉ dựa vào check ở tầng service?**
A: Vì `findConflictingActive` + tạo bản ghi mới là hai lệnh DB tách rời (SELECT rồi INSERT), không atomic — hai transaction chạy đồng thời có thể cùng SELECT ra kết quả rỗng trước khi transaction còn lại kịp commit INSERT, khiến cả hai đều "vượt qua" check dù đang tạo trùng ngày. Đây là pattern check-then-act kinh điển, và cách khắc phục chuẩn trong các hệ thống thực tế là đưa ràng buộc xuống tầng database — nơi duy nhất có thể đảm bảo tính atomic giữa các transaction đồng thời. `UNIQUE INDEX ... WHERE status <> 'CANCELLED'` (migration `V57__price_history_active_effective_date_unique.sql`) là một partial/filtered unique index — pattern phổ biến cho các ràng buộc kiểu "duy nhất trong số các bản ghi đang active" (ví dụ: email duy nhất trong số user chưa xóa, không được đặt trùng lịch một tài nguyên cho cùng một ngày). Service layer bắt `DataIntegrityViolationException` khi ràng buộc này bị vi phạm và dịch lại thành `OVERLAPPING_EFFECTIVE_DATE` (409) — cùng dạng lỗi với check ở tầng service — để người dùng không bao giờ thấy một exception SQL thô.

**Q: Việc này có ảnh hưởng gì đến tính hợp lệ của `price_history` như một sổ lịch sử không?**
A: Không thay đổi nguyên tắc cốt lõi: một dòng `price_history` đã `APPROVED` **không bao giờ bị sửa giá trị** (xem thảo luận về in-place edit — quyết định giữ nguyên, không áp dụng). Việc mở rộng điều kiện chặn khi tạo và bỏ re-check tại approval chỉ ảnh hưởng đến *có bao nhiêu dòng* có thể tồn tại song song cho cùng `effective_date` (giờ gần như luôn là một, trừ race condition hiếm gặp ở trên), không ảnh hưởng đến việc dữ liệu trong một dòng có bị thay đổi sau khi duyệt hay không. Lịch sử vẫn đầy đủ và trung thực.

**Q: Bản giá `PENDING` có được sửa không?**
A: Có. Kế toán viên (người tạo) được sửa bất kỳ trường nào của bản giá `PENDING` trước khi Kế toán trưởng duyệt. Không cần xóa và tạo lại.

**Q: Có thể hủy/rút bản giá `PENDING` không?**
A: Có. Kế toán viên (người tạo) được hủy (cancel) bản giá `PENDING`. Khi hủy, `status` chuyển sang `CANCELLED`. Bản giá `CANCELLED` là immutable và không thể khôi phục.

**Q: Người tạo bản giá có thể tự duyệt không?**
A: Không. Áp dụng nguyên tắc Maker-Checker: Kế toán viên (role `ACCOUNTANT`) tạo; Kế toán trưởng (role `ACCOUNTANT_MANAGER`) duyệt. Đây là hai role khác nhau, RBAC tự động đảm bảo tách biệt này.

**Q: "Thông báo Kế toán trưởng" là cơ chế gì?**
A: In-app notification: hệ thống tạo một bản ghi trong bảng `notifications` cho Kế toán trưởng khi bản giá chuyển sang `PENDING`. Không tích hợp email/Zalo trong Sprint 1.

**Q: Giá bán (`selling_price`) có phân biệt theo Đại lý không?**
A: Không. Sprint 1 không có dealer-tier pricing — một bản giá áp dụng cho tất cả Đại lý trong cùng kho.

**Q: Giá có phân biệt theo kho không?**
A: Có. Mỗi bản giá (`price_history`) gắn với một `warehouse_id` cụ thể. Cùng một sản phẩm có thể có giá khác nhau tại Hải Phòng, Hà Nội và Hồ Chí Minh. Kế toán viên phải chọn kho khi tạo/import bản giá. Overlap check áp dụng trên `(product_id, warehouse_id)` — hai bản giá APPROVED cho cùng sản phẩm nhưng khác kho không bị coi là overlap.

**Q: COGS được lấy từ `price_history.cost_price` hay từ `receipt_items.unit_cost`?**
A: Từ `price_history.cost_price` tại ngày tạo DO, lọc theo `warehouse_id` của DO. Sprint 1 dùng **standard cost** (giá vốn kỳ) thay vì actual purchase cost (giá mua từng lô). `receipt_items.unit_cost` là giá mua ghi nhận trên phiếu nhập (để tính DebitNote, đối chiếu NCC) và là dữ liệu riêng biệt, không ảnh hưởng đến COGS trong module này.

> **Lý do**: Cách tiếp cận standard cost đơn giản hơn trong Sprint 1 và đủ cho nhu cầu P&L nội bộ của Phúc Anh.

**Q: Giá và COGS được snapshot ở thời điểm nào trong luồng DO?**
A: Tại thời điểm **tạo DO** (`POST /api/v1/delivery-orders`). Các trường `unit_price` và `unit_cost` được ghi snapshot vào `delivery_order_items` ngay khi DO được tạo. Nếu bảng giá thay đổi sau đó, DO đã tạo không bị ảnh hưởng.

**Q: DO có thể được tạo nếu không có bản giá `APPROVED` nào cho sản phẩm ở ngày hiện tại?**
A: Không. Hệ thống chặn tạo DO với lỗi `MISSING_PRICE` nếu bất kỳ dòng hàng nào trong DO không tìm thấy bản giá `APPROVED` có hiệu lực tại ngày tạo DO.

**Q: Excel import — scope ra sao?**
A: Kế toán viên có thể import nhiều dòng sản phẩm từ file Excel (`.xlsx`) để tạo hàng loạt bản giá `PENDING`. Mỗi dòng Excel tương ứng một bản ghi `price_history` độc lập. Import không tự duyệt. Khuôn dạng file Excel được quy định trong `feature-accountant-price-import-excel.md`. Validation lỗi trả về danh sách lỗi theo dòng, không chặn toàn bộ file nếu một số dòng hợp lệ.

### Session 2026-07-18

**Q: Có ràng buộc nào giữa `selling_price` và `cost_price` không?**
A: Có. Hệ thống từ chối tạo/sửa/import một bản giá nếu `selling_price <= cost_price`, lỗi `SELLING_BELOW_COST` (422). Áp dụng ở cả ba luồng: tạo đơn lẻ, sửa `PENDING`, và mỗi dòng import Excel — cùng vị trí với các validation khác (positive price, overlap).

> **Lý do**: Đây là một bất biến khách quan (không có khái niệm bán dưới giá vốn hợp lệ trong scope Sprint 1 — xem mục 10, không có dynamic/promotional pricing hay giá thanh lý), khác với việc chặn theo biên độ % thay đổi (vốn có thể là biến động chi phí NCC hợp lệ, xem `feature-accountant-manager-price-approval.md`). Bổ sung ràng buộc này sau khi rà soát lại: sự cố giá nhập sai lệch bậc (Session 2026-07-12) lẽ ra có thể được chặn ngay từ bước tạo nếu có kiểm tra này.

**Q: `GET /api/v1/price-history` hỗ trợ filter theo `effective_date` như thế nào?**
A: Hai query param tùy chọn `effectiveDateFrom` và `effectiveDateTo` (inclusive), có thể dùng độc lập hoặc kết hợp với `productId`/`warehouseId`/`status`. Danh sách trả về luôn sắp xếp `created_at ASC` (cùng thứ tự với màn hình duyệt PENDING).

**Q: Endpoint hủy bản giá là `DELETE` hay `PUT`?**
A: `PUT /api/v1/price-history/{id}/cancel`. Bản đặc tả trước đây ghi `DELETE /api/v1/price-history/{id}` — đây là lỗi tài liệu, không phải hành vi dự kiến; `PUT .../cancel` phản ánh đúng ngữ nghĩa REST cho một soft-cancel (tài nguyên không bị xóa, chỉ đổi trạng thái) và là endpoint đã được frontend tích hợp.

**Q: `previous_approved` trong response chi tiết (`GET /api/v1/price-history/{id}`) có giới hạn theo role người xem không?**
A: Không. Trường này được trả cho bất kỳ ai có quyền xem bản giá đó (`ACCOUNTANT`, `ACCOUNTANT_MANAGER`, `ADMIN`, `CEO`), không riêng `ACCOUNTANT_MANAGER`. Đây là thông tin so sánh giá read-only, không có rủi ro bảo mật khi hiển thị cho người tạo tự xem lại trước khi bản giá được duyệt.

**Q: `previous_approved` được xác định là bản giá nào khi bản đang xem có `effective_date` nằm giữa hai bản `APPROVED` khác (trường hợp tạo bản giá bù trừ cho quá khứ)?**
A: Là bản `APPROVED` có `effective_date` lớn nhất nhưng vẫn `<= effective_date` của bản đang xem — tức bản giá mà bản đang xem thực sự sẽ thay thế khi được duyệt — chứ không phải "bản `APPROVED` mới nhất nói chung". Hai giá trị này trùng nhau trong đa số trường hợp (tạo bản giá cho ngày hiện tại/tương lai), nhưng khác nhau khi Kế toán viên tạo một bản giá bù trừ cho một ngày trong quá khứ trong khi đã có bản `APPROVED` mới hơn — trường hợp đó, so sánh với "mới nhất nói chung" sẽ so sánh ngược thời gian và gây hiểu lầm cho người duyệt.

**Q: Khi tra cứu giá cho DO, "ngày hôm nay" là ngày hệ thống (đồng hồ server) hay `document_date` do Planner nhập?**
A: Là `document_date` của DO (trường bắt buộc trong `DeliveryOrderCreateRequest`, spec 004), không phải đồng hồ server. Điều này cho phép tạo DO cho một giao dịch đã xảy ra trong quá khứ gần (ví dụ nhập liệu trễ) mà vẫn tra đúng giá của thời điểm đó.

> **Giới hạn rủi ro**: `document_date` bị ràng buộc bởi kỳ kế toán đang mở (`AccountingPeriodService.resolveOpenPeriod`, spec 008) — không thể tạo DO với `document_date` thuộc kỳ đã `CLOSED`. Planner vẫn có thể chọn bất kỳ ngày nào trong kỳ đang mở (thường là tháng hiện tại), nên về lý thuyết có thể chọn một ngày sớm hơn trong cùng kỳ để lấy giá cũ hơn nếu giá đã đổi giữa kỳ. Rủi ro này được chấp nhận trong Sprint 1 vì kỳ kế toán là tháng và khối lượng giao dịch/độ tin cậy của Planner trong quy mô vận hành hiện tại thấp; nếu phát sinh lạm dụng thực tế, nên xem xét bổ sung giới hạn `document_date` không được sớm hơn N ngày so với ngày tạo DO.

**Q: Kế toán viên/Kế toán trưởng có bị giới hạn theo kho được assign như các actor vận hành kho khác không?**
A: Không. Đây là quyết định thiết kế có chủ đích: `ACCOUNTANT` và `ACCOUNTANT_MANAGER` là chức năng tài chính tập trung, không gắn với một kho vật lý cụ thể như Thủ kho/Dispatcher (LESSON-003 trong `CLAUDE.md` áp dụng cho các actor vận hành kho, không áp dụng cho Kế toán). Một Kế toán viên có thể tạo bản giá cho cả 3 kho; một Kế toán trưởng duyệt bản giá cho cả 3 kho. Điều này phù hợp với quy mô vận hành hiện tại (workflow chỉ 2 người, xem `feature-accountant-manager-price-approval.md` §1).

**Q: Hai dòng trong cùng một file Excel import nhắm cùng `(product_id, warehouse_id, effective_date)` thì xử lý thế nào?**
A: Dòng đầu hợp lệ được tạo bình thường; dòng thứ hai (và các dòng tiếp theo) bị từ chối với `OVERLAPPING_EFFECTIVE_DATE`, cùng mã lỗi với việc trùng một bản ghi đã có sẵn trong DB — vì về bản chất đây là cùng một loại xung đột, chỉ khác nguồn xung đột (trong cùng file thay vì với dữ liệu đã tồn tại).

**Q: `effective_date` của một bản giá có bị giới hạn theo kỳ kế toán không, giống như `document_date` của Đơn xuất?**
A: Có. Tạo hoặc sửa một bản giá (kể cả từng dòng khi import Excel) bắt buộc `effective_date` phải nằm trong một kỳ kế toán đang `OPEN` — tái sử dụng nguyên `AccountingPeriodService.validateDateInOpenPeriod()` mà `DeliveryOrderServiceImpl` đã dùng cho `document_date` (spec 004/008). Nếu kỳ chứa `effective_date` đã `CLOSED`, hệ thống từ chối với `UNPROCESSABLE_ENTITY` (422, message tiền tố `PERIOD_CLOSED`) khi tạo/sửa đơn lẻ, hoặc lỗi dòng `PERIOD_CLOSED` (không chặn toàn file) khi import Excel.

> **Lý do**: Trước Session 2026-07-18, `price_history.effective_date` hoàn toàn không có ràng buộc kỳ kế toán — có thể tạo và duyệt một bản giá với `effective_date` từ bất kỳ thời điểm nào trong quá khứ, kể cả một kỳ đã `CLOSED` và đã được báo cáo. Việc này không làm sai lệch các giao dịch đã hoàn tất (`unit_price`/`unit_cost` đã snapshot vào `delivery_order_items` là bất biến, xem `feature-system-cogs-calculation.md`), nhưng làm suy yếu vai trò "sổ lịch sử phục vụ kiểm soát nội bộ" của `price_history` (mục 1) — ai đó có thể chèn một bản ghi "giá lịch sử" bất kỳ lúc nào sau này mà không có dấu vết cho biết đó là dữ liệu bổ sung muộn, khác với dữ liệu ghi nhận đồng thời với giao dịch thực tế. Ràng buộc kỳ kế toán đưa `price_history` về cùng mức bảo vệ với các luồng nghiệp vụ khác trong hệ thống (Đơn xuất, Kiểm kê — xem "Monthly Closing" trong `CLAUDE.md`), đồng thời vẫn cho phép tạo bản giá bù trừ cho bất kỳ ngày nào trong kỳ hiện đang mở (đúng như Session 2026-07-09 dự tính khi bỏ `end_date`).
>
> Ràng buộc này không ảnh hưởng đến các luồng đã có trong spec: sửa/hủy một bản `PENDING` (feature 14a) và duyệt (feature 14c) không tạo mới `effective_date` nên không bị chặn lại; Scenario 3b của `feature-accountant-price-entry-management.md` (tạo bản giá mới với `effective_date = hôm nay` để supersede một bản `APPROVED` nhập sai) luôn nằm trong kỳ đang mở nên không bị ảnh hưởng.

---

## 3. Actors

| Actor | Role | Nghiệp vụ liên quan |
|-------|------|---------------------|
| Kế toán viên | Maker | Tạo, sửa, hủy bản giá `PENDING`; import Excel |
| Kế toán trưởng | Checker | Xem lịch sử, so sánh giá cũ/mới, duyệt bản giá (không có luồng từ chối tường minh trong Sprint 1) |
| System | Auto | Tra cứu giá tại ngày giao dịch, snapshot giá vào DO item, block DO khi thiếu giá |

---

## 4. Functional Requirements (EARS)

*Vui lòng xem chi tiết tại:*
* [EARS - Price Entry Management](./features/feature-accountant-price-entry-management.md#3-functional-requirements-ears)
* [EARS - Price Import Excel](./features/feature-accountant-price-import-excel.md#3-functional-requirements-ears)
* [EARS - Price Approval](./features/feature-accountant-manager-price-approval.md#3-functional-requirements-ears)
* [EARS - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#3-functional-requirements-ears)

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Price lookup tại thời điểm tạo DO (p95) | ≤ 100ms |
| NFR-002 | Truy vấn lịch sử giá của một sản phẩm | ≤ 500ms |
| NFR-003 | Excel import 1000 SKUs | ≤ 10s |
| NFR-004 | Overlap validation khi tạo/sửa bản giá | ≤ 200ms |

---

## 6. Data Model

### price_history

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGSERIAL (PK) | |
| `product_id` | BIGINT (FK→products, NOT NULL) | |
| `warehouse_id` | BIGINT (FK→warehouses, NOT NULL) | Kho áp dụng bản giá này |
| `effective_date` | DATE (NOT NULL) | Ngày bắt đầu hiệu lực (inclusive). Không còn `end_date` — xem Session 2026-07-09 |
| `cost_price` | DECIMAL(18,2) (NOT NULL, > 0) | Giá vốn kỳ — dùng cho COGS khi xuất hàng |
| `selling_price` | DECIMAL(18,2) (NOT NULL, > 0) | Giá bán — dùng cho invoice và Credit Check |
| `status` | VARCHAR(20) (NOT NULL) | CHECK IN ('PENDING', 'APPROVED', 'CANCELLED'); DEFAULT 'PENDING' |
| `created_by` | BIGINT (FK→users, NOT NULL) | Kế toán viên tạo |
| `approved_by` | BIGINT (FK→users) | Kế toán trưởng duyệt; NULL khi còn PENDING/CANCELLED |
| `approved_at` | TIMESTAMPTZ | Null cho đến khi APPROVED |
| `cancelled_by` | BIGINT (FK→users) | Kế toán viên hủy; NULL nếu không bị hủy |
| `cancelled_at` | TIMESTAMPTZ | Null cho đến khi CANCELLED |
| `notes` | TEXT | Ghi chú tùy chọn của Kế toán viên |
| `created_at` | TIMESTAMPTZ (DEFAULT NOW()) | |
| `updated_at` | TIMESTAMPTZ (DEFAULT NOW()) | |

**Constraints:**
- `CHECK (cost_price > 0 AND selling_price > 0)`
- **Effective-date invariant** (enforced ở cả service layer VÀ database layer — xem Session 2026-07-09, mở rộng và làm cứng ở Session 2026-07-12): không được tồn tại hai bản không-`CANCELLED` cho cùng `(product_id, warehouse_id)` có cùng `effective_date`. Giá có hiệu lực tại ngày D là bản `APPROVED` có `effective_date <= D` gần D nhất (lớn nhất). Hai bản giá cho cùng product nhưng khác `warehouse_id` không xung đột với nhau.
- `UNIQUE INDEX uq_price_history_active_effective_date` (partial, `WHERE status <> 'CANCELLED'`) trên `(product_id, warehouse_id, effective_date)` — bản thân check ở service layer (`findConflictingActive`) là kiểu SELECT-rồi-INSERT, không atomic; index này là nguồn xác thực thật sự, chặn cứng ngay cả khi hai request tạo đồng thời cùng vượt qua check ở tầng service. Vi phạm được service layer bắt lại (`DataIntegrityViolationException` → `OVERLAPPING_EFFECTIVE_DATE`) để trả lỗi 409 gọn thay vì lộ exception thô. Migration `V57` tự dọn dữ liệu trùng phát sinh từ trước khi có ràng buộc (giữ bản `APPROVED` nếu có, hoặc bản mới nhất theo `created_at`; các bản trùng còn lại chuyển `CANCELLED`, không xóa vật lý) trước khi tạo index, vì Postgres không cho tạo unique index trên dữ liệu đang vi phạm.

**Indexes:**
- `idx_price_history_product_warehouse_status` on `(product_id, warehouse_id, status, effective_date)` — tra cứu giá theo kho và ngày giao dịch (lấy `effective_date` lớn nhất `<=` ngày tra cứu).
- `idx_price_history_product_warehouse_created` on `(product_id, warehouse_id, created_at DESC)` — lịch sử giá theo kho và thời gian tạo.
- `uq_price_history_active_effective_date` — xem Constraints ở trên; đây vừa là unique constraint vừa đóng vai trò index hỗ trợ tra cứu theo `(product_id, warehouse_id, effective_date)` khi status khác CANCELLED.

### delivery_order_items (snapshot fields — đã tồn tại trong spec 004)

Hai trường sau được thêm vào `delivery_order_items` tại thời điểm tạo DO, snapshot từ `price_history`:

| Field | Type | Notes |
|-------|------|-------|
| `unit_price` | DECIMAL(18,2) (NOT NULL) | Snapshot từ `price_history.selling_price` tại ngày tạo DO |
| `unit_cost` | DECIMAL(18,2) (NOT NULL) | Snapshot từ `price_history.cost_price` tại ngày tạo DO |

> Nếu spec 004 đã định nghĩa những trường này, spec 007 chỉ xác nhận rằng giá trị phải được điền từ `price_history` (không để trống, không cho phép override thủ công).

> **Session 2026-07-18**: `unit_cost` từng bị bỏ sót trong luồng tạo DO — chỉ `unit_price` được snapshot, `unit_cost` luôn `NULL`, khiến COGS/gross margin (mục 3.4 của `feature-system-cogs-calculation.md`) không thể tính được cho bất kỳ DO nào. Đã sửa ở tầng service để cả hai trường được snapshot cùng lúc từ cùng một lần tra cứu `price_history`. Cột `unit_cost` trong schema DB (migration `V1`) hiện vẫn `NULLABLE`, chưa được nâng lên `NOT NULL` — việc này cần một migration backfill riêng cho các dòng dữ liệu cũ đã tồn tại trước bản sửa, nằm ngoài phạm vi thay đổi lần này; tính đúng đắn `NOT NULL` hiện được đảm bảo ở tầng application code, không phải constraint DB.

### notifications (bảng bổ trợ — nếu chưa tồn tại)

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGSERIAL (PK) | |
| `recipient_id` | BIGINT (FK→users, NOT NULL) | |
| `type` | VARCHAR(50) (NOT NULL) | Ví dụ: `PRICE_PENDING_APPROVAL` |
| `reference_type` | VARCHAR(50) | `price_history` |
| `reference_id` | BIGINT | ID của bản ghi liên quan |
| `message` | TEXT | |
| `is_read` | BOOLEAN (DEFAULT false) | |
| `created_at` | TIMESTAMPTZ (DEFAULT NOW()) | |

---

## 7. API Spec

*Vui lòng xem chi tiết tại:*
* [APIs - Price Entry Management](./features/feature-accountant-price-entry-management.md#4-api-endpoints)
* [APIs - Price Import Excel](./features/feature-accountant-price-import-excel.md#4-api-endpoints)
* [APIs - Price Approval](./features/feature-accountant-manager-price-approval.md#4-api-endpoints)
* [APIs - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#4-api-endpoints)

---

## 8. Error Handling

| Error Code | HTTP | Điều kiện |
|------------|------|-----------|
| `OVERLAPPING_EFFECTIVE_DATE` | 409 | Đã tồn tại bản giá `PENDING` hoặc `APPROVED` khác cùng `(product_id, warehouse_id, effective_date)` — chỉ kiểm tra khi tạo/sửa (Session 2026-07-12), không re-check khi duyệt |
| `PRICE_ALREADY_APPROVED` | 409 | Thao tác sửa/hủy/duyệt trên bản giá đã ở trạng thái `APPROVED` |
| `PRICE_ALREADY_CANCELLED` | 409 | Thao tác sửa/hủy/duyệt trên bản giá đã ở trạng thái `CANCELLED` |
| `MISSING_PRICE` | 422 | Không tìm thấy bản giá `APPROVED` cho sản phẩm tại ngày tạo DO — liệt kê toàn bộ `product_id` bị thiếu giá trong cùng DO, không chỉ dòng đầu tiên (Session 2026-07-18) |
| `PRICE_NOT_FOUND` | 404 | ID bản giá không tồn tại |
| `FORBIDDEN_APPROVE_OWN` | 403 | Người tạo cố tự duyệt (phòng thủ; RBAC đã chặn qua role) |
| `SELLING_BELOW_COST` | 422 | `selling_price <= cost_price` — áp dụng khi tạo, sửa, và mỗi dòng import Excel (Session 2026-07-18) |
| `PERIOD_CLOSED` (dạng `UNPROCESSABLE_ENTITY`, message tiền tố `PERIOD_CLOSED:`) | 422 | `effective_date` nằm trong một kỳ kế toán đã `CLOSED` — áp dụng khi tạo, sửa, và mỗi dòng import Excel (Session 2026-07-18); tái sử dụng `AccountingPeriodService.validateDateInOpenPeriod()` giống spec 004/008 |
| `EXCEL_FORMAT_INVALID` | 400 | File không phải `.xlsx`, không đọc được, hoặc thiếu cột bắt buộc (A–E) |
| `EXCEL_TOO_MANY_ROWS` | 400 | File có hơn 1.000 dòng dữ liệu (không tính dòng trống cuối file) — toàn bộ file bị từ chối |

---

## 9. Acceptance Criteria

*Vui lòng xem chi tiết tại:*
* [Acceptance - Price Entry Management](./features/feature-accountant-price-entry-management.md#5-acceptance-criteria)
* [Acceptance - Price Import Excel](./features/feature-accountant-price-import-excel.md#5-acceptance-criteria)
* [Acceptance - Price Approval](./features/feature-accountant-manager-price-approval.md#5-acceptance-criteria)
* [Acceptance - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#5-acceptance-criteria)

---

## 10. Out of Scope

- Dynamic/promotional pricing hoặc discount tiers theo Đại lý
- Multi-currency pricing
- Dealer-segment giá khác nhau
- Giá theo kênh bán (B2B vs B2C)
- Giá dựa trên batch actual cost (FIFO actual cost COGS) — Sprint 1 dùng standard cost
- Bản giá tự hết hạn (self-expiring) trong một hành động — sau Session 2026-07-09, một bản giá `APPROVED` có hiệu lực vô thời hạn cho đến khi có bản `APPROVED` mới hơn; muốn giới hạn thời gian hiệu lực phải tạo thêm một bản giá kế tiếp
- Approval workflow nhiều bước (hiện tại: 1 Kế toán trưởng duyệt là đủ)
- Email/Zalo notification khi bản giá chờ duyệt
- Price list versioning hoặc audit so sánh diff tự động trong UI
