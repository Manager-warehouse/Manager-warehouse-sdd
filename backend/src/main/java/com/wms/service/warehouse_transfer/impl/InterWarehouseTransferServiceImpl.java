package com.wms.service.warehouse_transfer.impl;


import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferFinalReceiveRequest;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCheckRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCountRequest;
import com.wms.dto.request.InterWarehouseTransferRejectRequest;
import com.wms.dto.request.InterWarehouseTransferTripAssignRequest;
import com.wms.dto.request.InterWarehouseTransferUpdateRequest;
import com.wms.dto.request.LoadHandoverRequest;
import com.wms.dto.request.OutboundQcRequest;
import com.wms.dto.request.ReceivingHandoverRequest;
import com.wms.dto.request.SourceLoadReportRequest;
import com.wms.dto.request.TransferReturnRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.dto.response.SourceLoadPickCandidatesResponse;
import com.wms.dto.response.TransferPhotoUploadResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.service.warehouse_transfer.InterWarehouseTransferService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service cửa vào cho luồng điều chuyển nội bộ.
 * Controller gọi vào class này; class này chuyển tiếp sang các service chuyên xử lý từng giai đoạn.
 */
@Service
@RequiredArgsConstructor
public class InterWarehouseTransferServiceImpl implements InterWarehouseTransferService {

    private static final long MAX_TRANSFER_PHOTO_BYTES = 5L * 1024L * 1024L;

    /*
     * SERVICE ĐIỀU PHỐI:
     * - Các hàm public trong file này là hàm chính theo API, nhưng đa số chỉ chuyển tiếp sang service chuyên trách.
     * - Các hàm private cuối file chỉ hỗ trợ upload ảnh bằng chứng.
     *
     * Service tổng của Spec 005. Controller gọi vào đây, còn nghiệp vụ thật được tách theo giai đoạn:
     * lập phiếu = tạo/sửa/hủy, duyệt phiếu = giữ hàng trong kho, xuất-vận chuyển = gán xe/xuất/rời kho,
     * nhận hàng = đếm/QC/nhập vị trí/cách ly/chênh lệch/quay đầu. Helper giữ các quy tắc dùng chung.
     */
    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferHelper helper;
    private final InterWarehouseTransferPlanningService planningService;
    private final InterWarehouseTransferApprovalService approvalService;
    private final InterWarehouseTransferShippingService shippingService;
    private final InterWarehouseTransferReceivingService receivingService;

    @Override
    @Transactional
    public List<InterWarehouseTransferResponse> getAllTransfers(User actor) {
        // HÀM CHÍNH: API lấy danh sách phiếu, lọc theo quyền xem.
        // Danh sách phiếu được lọc theo vai trò và kho phụ trách trước khi trả về giao diện.
        List<Long> actorWarehouseIds = helper.loadWarehouseIds(actor);
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(transfer -> helper.canViewTransfer(actor, actorWarehouseIds, transfer))
                .map(transfer -> helper.toResponseEager(transfer))
                .toList();
    }

    @Override
    @Transactional
    public InterWarehouseTransferResponse getTransferById(Long id, User actor) {
        // HÀM CHÍNH: API lấy chi tiết phiếu, kiểm quyền theo kho hoặc tài xế được gán.
        // Chi tiết phiếu cũng kiểm quyền như danh sách; CEO/Admin thấy toàn bộ, nhân sự kho chỉ thấy phiếu liên quan kho mình.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        if (!helper.canViewTransfer(actor, transfer)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
        return helper.toResponse(transfer);
    }

    @Override
    public InterWarehouseTransferResponse createTransfer(InterWarehouseTransferCreateRequest request, User actor) {
        // HÀM CHÍNH: API tạo phiếu, chuyển tiếp sang PlanningService.
        // Planner tạo phiếu mới; service lập phiếu kiểm tra kho, ngày, dòng hàng và mã lệnh ngoài.
        return planningService.createTransfer(request, actor);
    }

    @Override
    public InterWarehouseTransferResponse createTransferFromApprovedRequest(InterWarehouseTransferCreateRequest request,
            User actor) {
        // Yêu cầu điều chuyển đã duyệt có thể sinh phiếu nếu người tạo thuộc kho nguồn hoặc kho đích liên quan.
        return planningService.createTransferFromApprovedRequest(request, actor);
    }

    @Override
    public InterWarehouseTransferResponse updateTransfer(Long id, InterWarehouseTransferUpdateRequest request,
            User actor) {
        // Chỉ sửa phiếu mới; service lập phiếu thay lại thông tin chính và danh sách hàng sau khi kiểm tra lại.
        return planningService.updateTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse cancelTransfer(Long id, InterWarehouseTransferReasonRequest request,
            User actor) {
        // Hủy phiếu mới hoặc phiếu đã duyệt nhưng chưa xếp hàng; nếu đã duyệt thì trả lại phần hàng đang giữ chỗ.
        return planningService.cancelTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse approveTransfer(Long id, User actor) {
        // Trưởng kho nguồn duyệt phiếu mới: hệ thống giữ hàng trong kho theo nguyên tắc xuất trước rồi chuyển sang đã duyệt.
        return approvalService.approveTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse rejectTransfer(Long id, InterWarehouseTransferReasonRequest request,
            User actor) {
        // Trưởng kho nguồn từ chối phiếu mới; bắt buộc có lý do để lưu lịch sử quyết định.
        return approvalService.rejectTransfer(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse assignTrip(Long id, InterWarehouseTransferTripAssignRequest request,
            User actor) {
        // Điều phối viên gán chuyến xe điều chuyển riêng; service vận chuyển kiểm lịch, tải trọng, xe và tài xế.
        return shippingService.assignTrip(id, request, actor);
    }

    @Override
    public SourceLoadPickCandidatesResponse getSourceLoadPickCandidates(Long id, User actor) {
        // Công nhân xem các kệ/bin đã giữ hàng để chọn đúng vị trí khi bốc hàng.
        return shippingService.getSourceLoadPickCandidates(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse recordSourceLoadReport(Long id, SourceLoadReportRequest request, User actor) {
        // Công nhân kho nguồn báo số lượng thực tế đã xếp; nếu lệch số lượng dự kiến thì bắt xử lý/xếp lại.
        return shippingService.recordSourceLoadReport(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse shipTransfer(Long id, User actor) {
        // Thủ kho nguồn chốt số lượng gửi sau khi báo cáo xếp hàng đủ và QC xuất đạt.
        return shippingService.shipTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse unshipTransfer(Long id, User actor) {
        // Gỡ số lượng đã chốt gửi khi cần quay lại trạng thái đã duyệt nhưng chưa xuất kho.
        return shippingService.unshipTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse departTransfer(Long id, User actor) {
        // Tài xế được gán xác nhận rời kho; hàng chuyển từ kho nguồn sang kho ảo "đang vận chuyển".
        return shippingService.departTransfer(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request,
            User actor) {
        // Công nhân kho nhận đếm số lượng thực tế; thiếu/thừa so với số lượng gửi phải nhập lý do.
        return receivingService.receiveCount(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse receiveCheck(Long id, InterWarehouseTransferReceiveCheckRequest request,
            User actor) {
        // Thủ kho nhận kiểm/QC; ghi số đạt/số lỗi, ảnh QC, lý do lỗi và vị trí dự kiến cho hàng đạt.
        return receivingService.receiveCheck(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse finalReceive(Long id, InterWarehouseTransferFinalReceiveRequest request,
            User actor) {
        // Thủ kho nộp kế hoạch nhập vị trí, trưởng kho/CEO/admin duyệt cuối để ghi tồn và đóng phiếu.
        return receivingService.finalReceive(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse returnToSource(Long id, TransferReturnRequest request, User actor) {
        // Trưởng kho/CEO/admin đánh dấu xe quay đầu trước khi kho đích đã nhận bàn giao.
        return receivingService.returnToSource(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse quarantineReject(Long id, InterWarehouseTransferRejectRequest request,
            User actor) {
        // Từ chối toàn bộ ở điểm nhận; toàn bộ hàng đang vận chuyển được chuyển vào khu cách ly.
        return receivingService.quarantineReject(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse recordOutboundQc(Long id, OutboundQcRequest request, User actor) {
        // QC xuất kho nguồn: nếu không đạt thì bắt buộc có lý do và khóa bước bàn giao/rời kho cho tới khi xử lý lại.
        return shippingService.recordOutboundQc(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse loadHandover(Long id, LoadHandoverRequest request, User actor) {
        // Bàn giao hàng đã QC đạt cho tài xế; bắt buộc có ảnh trước khi tài xế rời kho.
        return shippingService.loadHandover(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse driverArrive(Long id, User actor) {
        // Tài xế xác nhận đến kho đích; sau đó kho đích mới được ghi nhận bàn giao và nhập số lượng.
        return shippingService.driverArrive(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse receivingHandover(Long id, ReceivingHandoverRequest request, User actor) {
        // Kho nhận xác nhận bàn giao khi xe đến; chỉ sau đó mới được nhập số lượng nhận.
        return shippingService.receivingHandover(id, request, actor);
    }

    @Override
    public InterWarehouseTransferResponse returnDepart(Long id, User actor) {
        // Xe quay đầu: tài xế xác nhận rời kho đích để quay về kho nguồn.
        return shippingService.returnDepart(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse returnArrive(Long id, User actor) {
        // Xe quay đầu: tài xế xác nhận đã về lại kho nguồn.
        return shippingService.returnArrive(id, actor);
    }

    @Override
    public InterWarehouseTransferResponse returnHandover(Long id, LoadHandoverRequest request, User actor) {
        // Xe quay đầu: kho nguồn nhận bàn giao ảnh, sau đó mới được đếm và QC hàng quay về.
        return shippingService.returnHandover(id, request, actor);
    }

    @Override
    public TransferPhotoUploadResponse uploadPhotoEvidence(Long id, MultipartFile file, User actor) {
        // HÀM CHÍNH: API upload ảnh bằng chứng dùng chung cho các bước QC/bàn giao.
        // Upload ảnh chỉ lưu file và trả đường dẫn ảnh; các bước QC/bàn giao sẽ gắn đường dẫn đó vào đúng nghiệp vụ.
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        if (!helper.canViewTransfer(actor, transfer)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
        validateTransferPhoto(file);
        return new TransferPhotoUploadResponse(storeTransferPhoto(file, id));
    }

    private void validateTransferPhoto(MultipartFile file) {
        // HÀM HỖ TRỢ: validate file ảnh bằng chứng trước khi lưu.
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_TRANSFER_PHOTO_BYTES
                || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new BusinessRuleViolationException("TRANSFER_PHOTO_FILE_INVALID");
        }
    }

    private String storeTransferPhoto(MultipartFile file, Long transferId) {
        try {
            Files.createDirectories(Path.of("uploads", "transfer"));
            String filename = "trf-" + transferId + "-" + UUID.randomUUID() + extension(file.getOriginalFilename());
            Path target = Path.of("uploads", "transfer", filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/transfer/" + filename;
        } catch (IOException ex) {
            throw new BusinessRuleViolationException("TRANSFER_PHOTO_STORAGE_FAILED");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
