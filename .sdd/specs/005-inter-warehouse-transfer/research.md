# Nghiên cứu: 005 Điều chuyển nội bộ giữa kho

## Quyết định: Dùng `sent_qty` làm dấu hiệu hàng đã xếp/chưa gỡ xếp

**Lý do**: Trước khi tài xế rời kho, trạng thái nghiệp vụ của phiếu vẫn là `APPROVED`. `sent_qty == null` nghĩa là hàng chưa được chốt xếp; `sent_qty != null` nghĩa là thủ kho nguồn đã chốt hàng xếp và không được hủy phiếu cho đến khi `/unship` xóa số lượng đã gửi. Cách này tránh phải thêm một trạng thái workflow mới.

**Phương án đã cân nhắc**: Thêm trạng thái `LOADED`. Bị loại vì người dùng đã chấp nhận dùng `sent_qty`, và đặc tả giữ trạng thái `APPROVED` cho đến khi tài xế rời kho.

## Quyết định: Đặt endpoint điều chuyển dưới `/api/v1/inter-warehouse-transfers`

**Lý do**: Triển khai hiện tại expose aggregate điều chuyển qua `InterWarehouseTransferController` tại `/api/v1/inter-warehouse-transfers`. Các thao tác điều chuyển là chuyển trạng thái trên cùng một aggregate, nên các action con như `/approve`, `/ship`, `/depart`, `/receive-check`, `/final-receive` là phù hợp.

**Phương án đã cân nhắc**: Tách thành resource riêng như `/api/v1/transfer-shipments`. Bị loại vì sẽ chia một aggregate giao dịch thành nhiều controller rời rạc, làm khó kiểm soát trạng thái và audit.

## Quyết định: Không đưa phân bổ lot nguồn vào Spec 005

**Lý do**: Người dùng đã làm rõ rằng phân bổ lot nguồn không thuộc module điều chuyển này. Tính năng chỉ vận hành theo sản phẩm, kho, vị trí và số lượng tồn tổng hợp.

**Phương án đã cân nhắc**: Thêm phân bổ source-lot ở dòng điều chuyển. Bị loại theo làm rõ nghiệp vụ hiện tại.

## Quyết định: Chỉ chặn trùng mã lệnh ngoài trên các phiếu đang hoạt động

**Lý do**: Tổ hợp `externalInstructionCode + sourceWarehouse + destinationWarehouse + documentDate` ngăn tạo trùng công việc đang hoạt động, nhưng vẫn cho phép nhập lại bản sửa sau khi phiếu trước đó đã `REJECTED` hoặc `CANCELLED`.

**Phương án đã cân nhắc**: Bắt `externalInstructionCode` unique toàn hệ thống. Bị loại vì mã ngoài từ công ty có thể được dùng lại cho chứng từ sửa/hủy.

## Quyết định: Hàng điều chuyển fail QC đi vào quarantine của kho đích

**Lý do**: Constitution yêu cầu hàng fail QC phải vào quarantine và không được tính vào tồn khả dụng. Thủ kho chỉ chọn vị trí kho đích cho số lượng QC đạt; hệ thống tự tìm quarantine location đang hoạt động của kho đích cho phần QC lỗi.

**Phương án đã cân nhắc**: Cho thủ kho chọn vị trí quarantine thủ công. Bị loại để giảm lỗi nhập liệu và khớp rule đã làm rõ.

## Quyết định: Ánh xạ kết quả quarantine của điều chuyển sang Spec 009 theo tình trạng vật lý

**Lý do**: Hàng hỏng trong điều chuyển nội bộ không có quan hệ trả nhà cung cấp. Hàng hỏng vật lý sẽ ở lại kho nơi bị quarantine và xử lý tiêu hủy theo Spec 009. Thiếu hàng không phải tồn vật lý nên chỉ tạo adjustment/discrepancy điều chuyển. Hàng sai SKU nhưng còn nguyên vẹn có thể dùng luồng Return to Source và không phải ứng viên tiêu hủy.

**Phương án đã cân nhắc**: Cho phép RTV với mọi hàng quarantine. Bị loại vì điều chuyển nội bộ không có claim nhà cung cấp. Trả toàn bộ hàng hỏng về kho nguồn cũng bị loại vì chỉ chuyển trách nhiệm nội bộ và tăng rủi ro vận hành. Tiêu hủy mọi ngoại lệ điều chuyển cũng bị loại vì thiếu hàng không phải hàng vật lý và wrong-SKU nguyên vẹn vẫn có thể thu hồi.

## Quyết định: Tính số lượng và giá trị nhập kho đích theo thực nhận

**Lý do**: Nếu gửi 30 đơn vị nhưng chỉ đến vật lý 28 đơn vị, kho đích chỉ nhập và tính giá trị cho 28 đơn vị. 2 đơn vị thiếu còn lại là `TRANSFER_DISCREPANCY` chỉ theo số lượng, không tính vào giá trị nhập kho đích và không đi vào billing thương mại.

**Phương án đã cân nhắc**: Tính cả 30 đơn vị vào giá trị kho đích. Bị loại vì 2 đơn vị không được nhận vật lý. Tính tổn thất tiền ngay trong flow nhận điều chuyển bị loại vì nghiệp vụ quyết định giữ phần thiếu là discrepancy chỉ theo số lượng. Tự động phạt tài xế cho 2 đơn vị bị loại vì trách nhiệm cần điều tra và phê duyệt riêng.

## Quyết định: Wrong-SKU return cần kho đích báo cáo và quản lý duyệt

**Lý do**: Thủ kho kho đích là người kiểm vận hành và phát hiện sai SKU, còn quản lý kho đích chịu trách nhiệm cho phép xe quay đầu. Giữ nguyên cùng transfer, trip, driver và tồn `IN_TRANSIT` giúp tránh tạo nhận kho giả hoặc chứng từ xuất mới trùng. Khi về kho nguồn, nguồn phải lặp lại count, check/QC và final confirmation trước khi hàng nhập lại kho nguồn.

**Phương án đã cân nhắc**: Cho thủ kho tự cho xe quay đầu ngay. Bị loại vì bỏ qua kiểm soát quản lý và audit. Đưa wrong-SKU nguyên vẹn vào quarantine bị loại vì hàng không hỏng. Tạo transfer mới cho chặng quay đầu bị loại trong Sprint 1 vì nhân đôi chứng từ và mất traceability về chuyến gốc.

## Quyết định: Bắt buộc có test cho tính năng này

**Lý do**: Constitution và `AGENTS.md` yêu cầu coverage service, integration test endpoint, test invariant tồn kho và xác minh audit log cho mọi thao tác kho.

**Phương án đã cân nhắc**: Sinh task triển khai trước rồi bổ sung test sau. Bị loại vì điều chuyển chạm vào tồn kho, reservation, phân quyền và audit.

## Quyết định: Mô hình hóa nhu cầu bổ sung hàng của quản lý kho bằng `TransferRequest` trước `TRF`

**Lý do**: Quản lý kho có thể phát hiện thiếu hàng bằng cách xem tồn read-only ở kho khác, nhưng yêu cầu đó vẫn cần CEO duyệt trước khi vận hành kho thực thi. Giữ luồng này trong `transfer_requests` giúp không làm quá tải status của `transfers`, đồng thời giữ flow `TRF` hiện tại: tồn kho chỉ được reserve sau khi quản lý kho nguồn duyệt.

**Phương án đã cân nhắc**: Cho quản lý kho tạo trực tiếp `TRF`. Bị loại vì bỏ qua CEO approval và trách nhiệm của Planner. Tự động tạo `TRF` ngay sau khi CEO duyệt cũng bị loại vì Planner nguồn vẫn cần nhận template đã duyệt và tạo chứng từ vận hành có traceability.
