package com.wms.exception;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ReceiptAlreadyDecidedException;
import com.wms.exception.RtvAlreadyExistsException;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleViolationException ex) {
        String msg = ex.getMessage();
        String code = extractBusinessRuleCode(msg);
        HttpStatus status = businessRuleStatus(code);

        return error(status, code, msg, msg, null);
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiErrorResponse> handleUnprocessable(UnprocessableEntityException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, extractLeadingCode(ex.getMessage()), ex.getMessage(), null, null);
    }

    // Many UnprocessableEntityException call sites already write "SOME_CODE: detail message"
    // (e.g. "PERIOD_CLOSED: ...", "ORIGINAL_PERIOD_NOT_CLOSED: ..."). Extracting that prefix
    // lets the specific code reach ApiErrorResponse.code instead of a single generic
    // "UNPROCESSABLE_ENTITY" for every 422 - callers can then branch on `code` the way the
    // API spec documents, without a hardcoded per-code list here (contrast handleBusinessRule,
    // which does need one because BusinessRuleViolationException messages aren't code-prefixed).
    private static final java.util.regex.Pattern LEADING_CODE = java.util.regex.Pattern.compile("^([A-Z][A-Z0-9_]*): .+");
    private static final java.util.regex.Pattern BUSINESS_RULE_CODE =
            java.util.regex.Pattern.compile("^([A-Z][A-Z0-9_]*)(?::.*)?$");

    private String extractBusinessRuleCode(String message) {
        if (message == null) {
            return "BUSINESS_RULE_VIOLATION";
        }
        var matcher = BUSINESS_RULE_CODE.matcher(message);
        return matcher.matches() ? matcher.group(1) : "BUSINESS_RULE_VIOLATION";
    }

    private HttpStatus businessRuleStatus(String code) {
        return switch (code) {
            case "INVENTORY_VERSION_CONFLICT",
                    "RTV_ALREADY_CONFIRMED",
                    "INVOICE_ALREADY_PAID",
                    "SUPPLIER_INVOICE_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "RECEIPT_NOT_APPROVED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    private String extractLeadingCode(String message) {
        if (message == null) {
            return "UNPROCESSABLE_ENTITY";
        }
        var matcher = LEADING_CODE.matcher(message);
        return matcher.matches() ? matcher.group(1) : "UNPROCESSABLE_ENTITY";
    }

    @ExceptionHandler(ReceiptCountException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptCount(ReceiptCountException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(OutboundDeliveryException.class)
    public ResponseEntity<ApiErrorResponse> handleOutboundDelivery(OutboundDeliveryException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), null, ex.getDetails());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticConflict(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "INVENTORY_VERSION_CONFLICT",
                "The resource was changed by another transaction; reload and retry", null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return dataIntegrityError(rootMessage(ex));
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiErrorResponse> handleTransactionSystem(TransactionSystemException ex) {
        return dataIntegrityError(rootMessage(ex));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", "Access denied", null);
    }

    @ExceptionHandler(ForbiddenReceiptWarehouseException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenReceiptWarehouse(
            ForbiddenReceiptWarehouseException ex) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN_RECEIPT_WAREHOUSE",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(ReceiptAlreadyDecidedException.class)
    public ResponseEntity<ApiErrorResponse> handleReceiptAlreadyDecided(
            ReceiptAlreadyDecidedException ex) {
        return error(HttpStatus.CONFLICT, "RECEIPT_ALREADY_DECIDED",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(RtvAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleRtvAlreadyExists(
            RtvAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, "RTV_ALREADY_EXISTS",
                ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(StockTakeException.class)
    public ResponseEntity<ApiErrorResponse> handleStockTake(StockTakeException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(PriceHistoryException.class)
    public ResponseEntity<ApiErrorResponse> handlePriceHistory(PriceHistoryException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errorMsg, errorMsg, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Invalid request body", null, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), ex.getMessage(), null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason();
        return error(HttpStatus.valueOf(ex.getStatusCode().value()),
                reason == null ? "REQUEST_ERROR" : reason, reason, reason, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "INVALID_ARGUMENT";

        if (msg != null) {
            if (msg.contains("INVALID_CREDENTIALS") || msg.contains("TOKEN_INVALID") || msg.contains("TOKEN_EXPIRED")) {
                status = HttpStatus.UNAUTHORIZED;
                code = "UNAUTHORIZED";
            } else if (msg.contains("OTP_LOCKED")) {
                status = HttpStatus.TOO_MANY_REQUESTS;
                code = "OTP_LOCKED";
            } else if (msg.contains("OTP_EXPIRED") || msg.contains("OTP_INVALID")) {
                status = HttpStatus.BAD_REQUEST;
                code = msg;
            } else if (msg.contains("NEW_PASSWORD_SAME_AS_CURRENT")) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                code = "NEW_PASSWORD_SAME_AS_CURRENT";
            } else if (msg.contains("DUPLICATE") || msg.contains("ALREADY_EXISTS")) {
                status = HttpStatus.CONFLICT;
                code = "DUPLICATE_RESOURCE";
            } else if (msg.contains("NOT_FOUND")) {
                status = HttpStatus.NOT_FOUND;
                code = "RESOURCE_NOT_FOUND";
            } else if (msg.contains("CAPACITY") || msg.contains("STOCK") || msg.contains("ACTIVE") || msg.contains("TRIP")) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                code = "BUSINESS_RULE_VIOLATION";
            }
        }

        return error(status, code, msg, msg, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "ILLEGAL_STATE";

        if (msg != null) {
            if (msg.contains("ACCOUNT_INACTIVE")) {
                // Credentials are correct but the account is suspended — that is
                // an authorization problem (403), not an authentication one (401).
                status = HttpStatus.FORBIDDEN;
                code = "ACCOUNT_INACTIVE";
            } else if (msg.contains("MAIL_SEND_FAILED")) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                code = "MAIL_SEND_FAILED";
            }
        }

        return error(status, code, msg, msg, null);
    }

    private String translateMessage(String msg) {
        if (msg == null) return null;
        String code = msg.contains(":") ? msg.substring(0, msg.indexOf(':')).trim() : msg;
        switch (code) {
            case "DELIVERY_ORDER_UPDATE_FORBIDDEN": return "Delivery Order can only be updated by Planner while status is NEW.";
            case "DELIVERY_ORDER_CANCEL_FORBIDDEN": return "Delivery Order cannot be cancelled by this actor in its current status.";
            case "PICKED_GOODS_RETURN_REQUIRED": return "Picked or QC-processed goods must be returned to bin before cancellation.";
            case "RESERVATION_NOT_FOUND": return "Reservation record was not found for this Delivery Order.";
            // ── Trip ──────────────────────────────────────────────────────────────
            case "TRIP_SCHEDULE_INVALID": return "Lịch trình chuyến đi không hợp lệ (thời gian kết thúc phải sau thời gian bắt đầu).";
            case "TRIP_START_IN_PAST": return "Thời gian bắt đầu chuyến đi không được ở quá khứ.";
            case "TRIP_END_IN_PAST": return "Thời gian hạn giao hàng không được ở quá khứ.";
            case "TRIP_RESOURCE_OVERLAP": return "Tài xế hoặc phương tiện đã được gán cho một chuyến đi khác trùng thời gian.";
            case "VEHICLE_SCHEDULE_OVERLAP": return "Phương tiện đã được gán cho một chuyến đi khác trùng thời gian. Vui lòng chọn xe hoặc khung giờ khác.";
            case "DRIVER_SCHEDULE_OVERLAP": return "Tài xế đã được gán cho một chuyến đi khác trùng thời gian. Vui lòng chọn tài xế hoặc khung giờ khác.";
            case "VEHICLE_NOT_AVAILABLE": return "Phương tiện vận tải hiện không khả dụng. Vui lòng chọn xe khác.";
            case "DRIVER_NOT_AVAILABLE": return "Tài xế hiện không khả dụng. Vui lòng chọn tài xế khác.";
            case "TRIP_ALREADY_DEPARTED": return "Chuyến xe đã khởi hành, không thể thay đổi.";
            case "TRIP_CAPACITY_EXCEEDED": return "Tổng hàng hóa vượt tải trọng của xe. Vui lòng chọn xe lớn hơn hoặc giảm số lượng hàng.";
            case "VEHICLE_OVERLOAD": return "Tổng hàng hóa vượt tải trọng của xe. Vui lòng chọn xe phù hợp hơn.";
            case "DRIVER_LICENSE_EXPIRED": return "Tài xế chưa có hồ sơ bằng lái hợp lệ hoặc bằng lái đã hết hạn, không thể gán chuyến.";
            case "VEHICLE_SOURCE_WAREHOUSE_REQUIRED": return "Xe phải được gán vào kho nguồn của phiếu điều chuyển.";
            case "DRIVER_SOURCE_WAREHOUSE_REQUIRED": return "Tài xế phải được gán vào kho nguồn của phiếu điều chuyển.";
            case "TRANSFER_ALREADY_HAS_TRIP": return "Phiếu điều chuyển đã được gán chuyến xe trước đó.";
            case "DRIVER_ON_TRIP_STATUS_SYSTEM_MANAGED": return "Trạng thái đang chạy chuyến của tài xế do hệ thống cập nhật theo luồng vận chuyển.";
            case "VEHICLE_ON_TRIP_STATUS_SYSTEM_MANAGED": return "Trạng thái đang giao hàng của xe do hệ thống cập nhật theo luồng vận chuyển.";
            // ── Transfer Planning ─────────────────────────────────────────────────
            case "SOURCE_DESTINATION_MUST_DIFFER": return "Kho nguồn và kho đích không được trùng nhau.";
            case "SOURCE_WAREHOUSE_MUST_BE_PHYSICAL": return "Kho nguồn phải là kho vật lý, không thể chọn kho IN_TRANSIT.";
            case "DESTINATION_WAREHOUSE_MUST_BE_PHYSICAL": return "Kho đích phải là kho vật lý, không thể chọn kho IN_TRANSIT.";
            case "DUPLICATE_PRODUCT_IN_TRANSFER": return "Có dòng hàng trùng sản phẩm trong phiếu điều chuyển. Mỗi sản phẩm chỉ được xuất hiện một lần.";
            case "TRANSFER_QTY_MUST_BE_WHOLE_NUMBER": return "Số lượng điều chuyển phải là số nguyên, không được có phần thập phân.";
            case "INVALID_TRANSFER_STATUS": return "Trạng thái phiếu không hợp lệ cho thao tác này.";
            case "DUPLICATE_EXTERNAL_INSTRUCTION": return "Mã lệnh điều chuyển này đã tồn tại cho cùng tuyến và ngày chứng từ.";
            case "DOCUMENT_DATE_MUST_NOT_BE_PAST": return "Ngày chứng từ không được ở quá khứ.";
            case "PLANNED_DATE_MUST_NOT_BE_PAST": return "Ngày dự kiến điều chuyển không được ở quá khứ.";
            case "PLANNED_DATE_MUST_NOT_BE_BEFORE_DOCUMENT_DATE": return "Ngày dự kiến không được trước ngày chứng từ.";
            case "INVALID_SOURCE_LOCATION": return "Bin nguồn không hợp lệ: phải thuộc kho nguồn, đang hoạt động và không phải khu cách ly.";
            case "INVALID_DESTINATION_LOCATION": return "Bin đích không hợp lệ: phải thuộc kho đích, đang hoạt động và không phải khu cách ly.";
            case "TRANSFER_CANCEL_NOT_ALLOWED": return "Không thể hủy phiếu điều chuyển ở trạng thái hiện tại.";
            case "UNSHIP_REQUIRED_BEFORE_CANCEL": return "Cần hủy xuất hàng (Unship) trước khi hủy phiếu điều chuyển.";
            case "INSUFFICIENT_AVAILABLE_STOCK": return "Tồn kho khả dụng không đủ để giữ chỗ cho số lượng yêu cầu. Vui lòng kiểm tra lại tồn kho.";
            case "WAREHOUSE_SCOPE_REQUIRED": return "Bạn không thuộc kho liên quan, không thể thực hiện thao tác này.";
            case "WAREHOUSE_MANAGER_ROLE_REQUIRED": return "Chỉ Quản lý kho mới có quyền thực hiện thao tác này.";
            case "PLANNER_ROLE_REQUIRED": return "Chỉ Planner mới có quyền thực hiện thao tác này.";
            case "CEO_ROLE_REQUIRED": return "Chỉ CEO mới có quyền thực hiện thao tác này.";
            case "REJECTION_REASON_REQUIRED": return "Vui lòng nhập lý do từ chối.";
            // ── Source Shipping ───────────────────────────────────────────────────
            case "SOURCE_LOAD_ITEMS_REQUIRED": return "Vui lòng nhập số lượng xếp cho tất cả dòng hàng.";
            case "TRANSFER_ITEM_NOT_FOUND": return "Không tìm thấy dòng hàng trong phiếu điều chuyển.";
            case "SOURCE_LOAD_REPORT_REQUIRED": return "Công nhân cần báo số lượng thực xếp trước khi thủ kho QC.";
            case "SENT_QTY_MISMATCH": return "Số lượng thực xếp chưa khớp kế hoạch. Cần công nhân chỉnh lại trước khi QC đạt.";
            case "SOURCE_LOAD_REWORK_REQUIRED": return "QC xuất kho thất bại. Hàng cần được xử lý lại trước khi QC lại.";
            case "SOURCE_LOAD_REWORK_REASON_REQUIRED": return "Vui lòng nhập lý do xử lý lại khi số lượng thực xếp lệch kế hoạch.";
            case "OUTBOUND_QC_REQUIRED": return "Thủ kho cần hoàn tất kiểm tra Outbound QC trước khi chốt xuất.";
            case "OUTBOUND_QC_NOT_PASSED": return "Outbound QC chưa đạt, không thể tiến hành xuất hàng.";
            case "OUTBOUND_QC_FAILURE_REASON_REQUIRED": return "Vui lòng nhập lý do khi QC xuất kho thất bại.";
            case "LOAD_HANDOVER_REQUIRED": return "Thủ kho cần xác nhận bàn giao hàng lên xe trước khi tài xế rời kho.";
            case "SENT_QTY_REQUIRED": return "Chưa chốt số lượng xuất. Thủ kho cần xác nhận số lượng lên xe.";
            case "IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED": return "Hệ thống chưa cấu hình kho trung chuyển (IN_TRANSIT). Liên hệ quản trị viên.";
            case "IN_TRANSIT_LOCATION_NOT_CONFIGURED": return "Kho trung chuyển chưa có Bin lưu trữ. Liên hệ quản trị viên.";
            case "INVENTORY_INVARIANT_VIOLATED": return "Phát hiện vi phạm bất biến tồn kho (total_qty hoặc reserved_qty không hợp lệ). Thao tác đã bị hủy để bảo vệ dữ liệu. Vui lòng tải lại và thử lại, nếu lỗi vẫn xảy ra liên hệ quản trị viên.";
            // ── Destination Receiving ─────────────────────────────────────────────
            case "DRIVER_ARRIVE_REQUIRED": return "Tài xế cần xác nhận đã đến kho đích trước khi thực hiện bàn giao.";
            case "ARRIVAL_HANDOVER_REQUIRED": return "Thủ kho kho đích cần xác nhận nhận bàn giao xe trước khi công nhân count hàng.";
            case "RETURN_REQUEST_PENDING": return "Đang có yêu cầu quay đầu chờ duyệt, không thể thực hiện thao tác này.";
            case "RECEIVE_COUNT_ITEMS_REQUIRED": return "Vui lòng nhập số lượng thực nhận cho các dòng hàng.";
            case "ISSUE_REASON_REQUIRED": return "Số lượng thực nhận lệch số lượng đã gửi, vui lòng nhập lý do chênh lệch.";
            case "RECEIVE_QC_PHOTO_REQUIRED": return "Cần chụp hoặc chọn ảnh xác nhận QC nhập hàng.";
            case "WORKER_COUNT_REQUIRED": return "Công nhân cần nhập số lượng thực nhận trước khi thủ kho kiểm tra QC.";
            case "DUPLICATE_RECEIVE_COUNT_ITEM": return "Có dòng hàng bị trùng lặp trong báo cáo số lượng nhận.";
            case "RECEIVE_CHECK_ITEMS_REQUIRED": return "Vui lòng nhập kết quả kiểm tra count/QC cho các dòng hàng.";
            case "DUPLICATE_RECEIVE_CHECK_ITEM": return "Có dòng hàng bị trùng lặp trong kiểm tra QC nhập.";
            case "CHECKER_NOTE_REQUIRED": return "Cần nhập ghi chú khi số lượng chốt khác số lượng công nhân đã báo.";
            case "OVER_RECEIPT_BLOCKED": return "Số lượng thực nhận không được lớn hơn số lượng đã gửi đi.";
            case "QC_TOTAL_MUST_MATCH_CONFIRMED_QTY": return "Tổng số lượng QC đạt và QC lỗi phải bằng số lượng thực nhận.";
            case "QC_FAILURE_REASON_REQUIRED": return "Yêu cầu nhập lý do lỗi khi có số lượng QC không đạt.";
            case "QUARANTINE_LOCATION_NOT_CONFIGURED": return "Kho đích chưa có khu vực cách ly (Quarantine). Cần thêm ít nhất một Bin Quarantine trước khi duyệt QC lỗi.";
            case "QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE": return "Hàng đạt QC không thể xếp vào khu vực cách ly. Vui lòng chọn bin lưu trữ thông thường.";
            case "QC_PASSED_BIN_MUST_NOT_BE_STAGING": return "Hàng đạt QC không thể cất vào khu trung chuyển/staging. Vui lòng chọn kệ lưu trữ thường.";
            case "RECEIVE_CHECK_REQUIRED": return "Thủ kho cần hoàn tất kiểm tra count/QC trước khi gửi kế hoạch cất kệ.";
            case "IN_TRANSIT_STOCK_NOT_FOUND": return "Không tìm thấy tồn kho trung chuyển cho mặt hàng này. Liên hệ quản trị viên.";
            // ── Putaway ───────────────────────────────────────────────────────────
            case "PUTAWAY_PLAN_REQUIRED": return "Chưa có kế hoạch cất kệ. Thủ kho cần phân bổ hàng vào các bin trước.";
            case "PUTAWAY_PLAN_INVALID": return "Kế hoạch cất kệ không hợp lệ. Vui lòng kiểm tra lại phân bổ bin.";
            case "WAREHOUSE_MANAGER_APPROVAL_REQUIRED": return "Chỉ Quản lý kho mới có quyền duyệt cất kệ và nhập kho.";
            case "DISCREPANCY_REASON_REQUIRED": return "Có chênh lệch số lượng, vui lòng nhập lý do trước khi xác nhận.";
            case "DISCREPANCY_INCIDENT_NOT_FOUND": return "Không tìm thấy hồ sơ chênh lệch.";
            case "DISCREPANCY_INCIDENT_ACCESS_DENIED": return "Bạn không có quyền xử lý hồ sơ chênh lệch này.";
            case "DISCREPANCY_INCIDENT_NOT_OPEN": return "Hồ sơ chênh lệch đã được chốt, không thể cập nhật lại.";
            case "DISCREPANCY_RESOLUTION_STATUS_REQUIRED": return "Vui lòng chọn hướng xử lý hồ sơ chênh lệch.";
            case "DISCREPANCY_RESOLUTION_STATUS_INVALID": return "Hướng xử lý hồ sơ chênh lệch không hợp lệ.";
            case "DISCREPANCY_RESOLUTION_NOTE_REQUIRED": return "Vui lòng nhập ghi chú quyết định xử lý hồ sơ chênh lệch.";
            case "DISCREPANCY_RESOLUTION_NOTE_TOO_LONG": return "Ghi chú xử lý hồ sơ chênh lệch không được vượt quá 1000 ký tự.";
            case "DISCREPANCY_HOLD_ENTRY_NOT_FOUND": return "Không tìm thấy hàng tạm giữ của hồ sơ chênh lệch.";
            case "DISCREPANCY_HOLD_QUANTITY_MISMATCH": return "Số lượng hàng tạm giữ không khớp với hồ sơ chênh lệch.";
            case "DISCREPANCY_HOLD_ENTRY_INCOMPLETE": return "Hàng tạm giữ thiếu lô hàng hoặc vị trí, không thể nhập tồn.";
            case "SOURCE_STOCK_NOT_ENOUGH_FOR_DISCREPANCY_RESOLUTION": return "Kho nguồn không còn đủ tồn khả dụng để trừ phần hàng thừa.";
            case "DUPLICATE_PUTAWAY_ITEM": return "Có dòng hàng bị trùng lặp trong kế hoạch cất kệ.";
            case "DESTINATION_LOCATION_REQUIRED": return "Yêu cầu chọn vị trí lưu trữ (Bin) cho hàng đạt QC.";
            case "DUPLICATE_PUTAWAY_LOCATION": return "Không được chọn cùng một Bin cho hai dòng phân bổ của cùng mặt hàng.";
            case "PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED": return "Tổng số lượng phân bổ vào các bin phải bằng đúng số lượng QC đạt.";
            case "PUTAWAY_PLAN_EXHAUSTED": return "Kế hoạch cất kệ đã được hoàn thành rồi.";
            case "BIN_CAPACITY_EXCEEDED": return "Bin đã đầy, không còn đủ sức chứa cho số lượng này.";
            // ── Return Flow ───────────────────────────────────────────────────────
            case "SOURCE_RETURN_ONLY_BEFORE_DESTINATION_ARRIVAL": return "Chỉ có thể quay đầu về kho nguồn trước khi tài xế đến kho đích.";
            case "SOURCE_RETURN_DISABLED": return "Không cho phép quản lý kho nguồn tự yêu cầu quay đầu khi xe đang vận chuyển.";
            case "RETURN_ALREADY_IN_PROGRESS": return "Xe đang trong quá trình quay đầu, không thể thực hiện thêm.";
            case "RETURN_REQUEST_ONLY_BEFORE_HANDOVER": return "Chỉ có thể yêu cầu quay đầu trước khi thực hiện bàn giao nhận hàng.";
            case "RETURN_REQUEST_ONLY_BEFORE_COUNT": return "Chỉ có thể yêu cầu quay đầu trước khi công nhân count hàng.";
            case "RETURN_REASON_REQUIRED": return "Vui lòng nhập lý do quay đầu.";
            case "WRONG_SKU_ITEMS_REQUIRED": return "Vui lòng thêm ít nhất 1 dòng hàng sai SKU trước khi gửi yêu cầu quay đầu.";
            case "NO_RETURN_REQUESTED": return "Chưa có yêu cầu quay đầu nào được gửi.";
            case "TRANSFER_NOT_RETURNED_LEG": return "Thao tác này chỉ áp dụng cho hàng đang trong luồng quay đầu.";
            case "RETURN_DEPART_REQUIRED": return "Tài xế cần xác nhận xuất phát quay đầu trước.";
            case "RETURN_ARRIVE_REQUIRED": return "Tài xế cần xác nhận đã về đến kho nguồn trước.";
            case "RETURN_HANDOVER_REQUIRED": return "Thủ kho kho nguồn cần xác nhận nhận bàn giao hàng quay đầu trước.";
            case "RETURN_HANDOVER_STOREKEEPER_REQUIRED": return "Thủ kho kho nguồn cần xác nhận bàn giao hàng quay đầu.";
            case "EXPECTED_PRODUCT_MISMATCH": return "Sản phẩm dự kiến không khớp với dòng hàng đã chọn.";
            case "ACTUAL_PRODUCT_MUST_DIFFER": return "SKU thực tế phải khác với SKU dự kiến.";
            case "AFFECTED_QTY_MUST_BE_POSITIVE": return "Số lượng sai SKU phải lớn hơn 0.";
            case "AFFECTED_QTY_EXCEEDS_SENT_QTY": return "Số lượng sai SKU không được vượt quá số lượng đã gửi.";
            // ── Photo / File ──────────────────────────────────────────────────────
            case "TRANSFER_PHOTO_FILE_INVALID": return "File ảnh không hợp lệ. Chỉ chấp nhận file JPEG/PNG dưới 10MB.";
            case "TRANSFER_PHOTO_STORAGE_FAILED": return "Lỗi lưu trữ ảnh. Vui lòng thử lại, nếu tiếp tục xảy ra hãy liên hệ quản trị viên.";
            // ── Transfer Trip ─────────────────────────────────────────────────────
            case "TRANSFER_TRIP_REQUIRED": return "Chuyến xe chưa được gán hoặc chưa sẵn sàng khởi hành.";
            case "ASSIGNED_DRIVER_REQUIRED": return "Chỉ tài xế được chỉ định mới có quyền xác nhận khởi hành.";
            // ── General ───────────────────────────────────────────────────────────
            case "WAREHOUSE_REQUIRED": return "Yêu cầu gán kho hoạt động cho tài khoản.";
            case "MULTIPLE_WAREHOUSES_NOT_ALLOWED": return "Chỉ được phép gán tối đa 1 kho cho vai trò này.";
            case "WAREHOUSE_SCOPE_FORBIDDEN": return "Bạn không có quyền truy cập phạm vi kho này.";
            case "ONLY_DRAFT_CAN_BE_UPDATED": return "Chỉ yêu cầu điều chuyển ở trạng thái nháp mới được sửa.";
            case "ONLY_DRAFT_CAN_BE_CANCELLED": return "Chỉ yêu cầu điều chuyển ở trạng thái nháp mới được hủy.";
            case "ONLY_DRAFT_CAN_BE_SUBMITTED": return "Chỉ yêu cầu điều chuyển ở trạng thái nháp mới được gửi duyệt.";
            case "ONLY_SUBMITTED_CAN_BE_APPROVED": return "Chỉ yêu cầu điều chuyển đã gửi duyệt mới được CEO phê duyệt.";
            case "ONLY_SUBMITTED_CAN_BE_REJECTED": return "Chỉ yêu cầu điều chuyển đã gửi duyệt mới được CEO từ chối.";
            case "ONLY_APPROVED_CAN_BE_CONVERTED": return "Chỉ yêu cầu điều chuyển đã được CEO duyệt mới được lập phiếu điều chuyển.";
            case "TRANSFER_REQUEST_ALREADY_CONVERTED": return "Yêu cầu điều chuyển này đã được lập phiếu điều chuyển trước đó.";
            case "TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE": return "Tồn kho nguồn không đủ cho số lượng yêu cầu bổ sung. Vui lòng chọn kho nguồn khác hoặc giảm số lượng.";
            case "NEEDED_BY_DATE_MUST_NOT_BE_PAST": return "Ngày cần hàng không được ở quá khứ.";
            default: return msg;
        }
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status,
                                                   String code,
                                                   String message,
                                                   String errorVal,
                                                   Map<String, Object> details) {
        String translatedMsg = translateMessage(message);
        String translatedErr = translateMessage(errorVal);
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .code(code)
                .message(translatedMsg)
                .error(translatedErr)
                .details(details)
                .timestamp(OffsetDateTime.now())
                .build());
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        Throwable root = throwable;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        String message = root == null ? null : root.getMessage();
        return message == null || message.isBlank() ? "Data integrity violation" : message;
    }

    private static boolean contains(String value, String pattern) {
        return value != null && value.toLowerCase().contains(pattern.toLowerCase());
    }

    private ResponseEntity<ApiErrorResponse> dataIntegrityError(String message) {
        String code = "DATA_INTEGRITY_VIOLATION";
        String userMessage = "Dữ liệu không thỏa mãn ràng buộc của hệ thống";

        if (contains(message, "delivery_orders_do_number_key")
                || contains(message, "delivery_orders_do_number")
                || contains(message, "do_number")) {
            code = "DELIVERY_ORDER_NUMBER_CONFLICT";
            userMessage = "Số đơn xuất kho đã tồn tại, vui lòng thử tạo lại";
        } else if (contains(message, "warehouse_product_reservations")
                || (contains(message, "warehouse_id") && contains(message, "product_id"))) {
            code = "WAREHOUSE_PRODUCT_RESERVATION_CONFLICT";
            userMessage = "Tồn giữ chỗ của sản phẩm trong kho vừa được thay đổi, vui lòng tải lại và thử lại";
        }

        return error(HttpStatus.CONFLICT, code, userMessage, message, null);
    }

}
