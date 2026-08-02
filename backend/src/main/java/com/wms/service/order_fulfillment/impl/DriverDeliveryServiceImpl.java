package com.wms.service.order_fulfillment.impl;


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
import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.response.DeliveryAttemptResponse;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.dto.response.DriverDeliveryOrderResponse;
import com.wms.dto.response.TripDriverViewResponse;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOtpAttempt;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.order_fulfillment.Trip;
import com.wms.entity.order_fulfillment.TripDeliveryOrder;
import com.wms.entity.access_control.User;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOtpStatus;
import com.wms.enums.order_fulfillment.DeliveryStatus;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.order_fulfillment.TripType;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryOtpAttemptRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.repository.TripDeliveryOrderRepository;
import com.wms.repository.TripRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AutoInvoiceService;
import com.wms.service.order_fulfillment.DriverDeliveryService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wms.repository.SplitDeliveryLegRepository;
import com.wms.repository.SplitDeliveryPlanRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.order_fulfillment.PodEvidenceStorageService;
import com.wms.service.order_fulfillment.PodEvidenceStorageService.StoredPodObject;
import com.wms.entity.order_fulfillment.SplitDeliveryPlan;
import com.wms.entity.order_fulfillment.SplitDeliveryLeg;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import com.wms.dto.response.PodEvidenceSignedUrlsResponse;
import com.wms.dto.response.PodEvidenceSignedUrlResponse;
import com.wms.exception.OtpDeliveryFailedException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DriverDeliveryServiceImpl implements DriverDeliveryService {

    /*
     * Service cho màn "Giao hàng của tôi" của tài xế.
     * Tài xế chỉ thao tác trên chuyến được gán: xem chuyến, upload ảnh giao hàng,
     * xin OTP xác nhận, xác nhận giao thành công hoặc báo giao thất bại.
     */
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final long MAX_POD_BYTES = 5L * 1024L * 1024L;
    private static final List<DeliveryStatus> CURRENT_ATTEMPT_STATUSES = List.of(DeliveryStatus.IN_TRANSIT);
    private static final List<DeliveryOrderStatus> TERMINAL_DO_STATUSES =
            List.of(DeliveryOrderStatus.COMPLETED, DeliveryOrderStatus.RETURNED);

    private final TripRepository tripRepository;
    private final TripDeliveryOrderRepository tripDeliveryOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryOtpAttemptRepository otpRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final InterWarehouseTransferRepository interWarehouseTransferRepository;
    private final AutoInvoiceService autoInvoiceService;
    private final AuditLogService auditLogService;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    private final SplitDeliveryLegRepository splitDeliveryLegRepository;
    private final SplitDeliveryPlanRepository splitDeliveryPlanRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final PodEvidenceStorageService podStorageService;

    public DriverDeliveryServiceImpl(TripRepository tripRepository,
                                     TripDeliveryOrderRepository tripDeliveryOrderRepository,
                                     DeliveryRepository deliveryRepository,
                                     DeliveryOtpAttemptRepository otpRepository,
                                     DeliveryOrderRepository deliveryOrderRepository,
                                     DeliveryOrderItemRepository deliveryOrderItemRepository,
                                     InventoryRepository inventoryRepository,
                                     InterWarehouseTransferRepository interWarehouseTransferRepository,
                                     SplitDeliveryLegRepository splitDeliveryLegRepository,
                                     SplitDeliveryPlanRepository splitDeliveryPlanRepository,
                                     UserWarehouseAssignmentRepository userWarehouseAssignmentRepository,
                                     AutoInvoiceService autoInvoiceService,
                                     AuditLogService auditLogService,
                                     JavaMailSender mailSender,
                                     PodEvidenceStorageService podStorageService) {
        this.tripRepository = tripRepository;
        this.tripDeliveryOrderRepository = tripDeliveryOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.otpRepository = otpRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.interWarehouseTransferRepository = interWarehouseTransferRepository;
        this.splitDeliveryLegRepository = splitDeliveryLegRepository;
        this.splitDeliveryPlanRepository = splitDeliveryPlanRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
        this.autoInvoiceService = autoInvoiceService;
        this.auditLogService = auditLogService;
        this.mailSender = mailSender;
        this.podStorageService = podStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public TripDriverViewResponse getAssignedTrip(Long tripId, User actor) {
        // Tài xế chỉ xem được chi tiết chuyến nếu tài khoản của mình đang được gán vào chuyến đó.
        Trip trip = assignedTrip(tripId, actor);
        // Nhánh điều chuyển nội bộ: nếu tripType = TRANSFER thì load thêm phiếu TRF để mobile hiển thị tuyến kho nguồn -> kho đích.
        InterWarehouseTransfer transfer = transferSummaryByTrip(trip);
        return toTripDriverView(trip, transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDriverViewResponse> listMyTrips(User actor) {
        // Danh sách chuyến của tài xế hiện tại, gồm cả chuyến giao đại lý và chuyến điều chuyển nội bộ nếu có.
        List<Trip> trips = tripRepository.findAssignedDriverTrips(actor.getId());
        Map<Long, InterWarehouseTransfer> transfersByTripId = transferSummariesByTripId(trips);
        return trips.stream()
                .map(trip -> toTripDriverView(trip, transfersByTripId.get(trip.getId())))
                .toList();
    }

    @Override
    @Transactional
    public DeliveryAttemptResponse uploadPodEvidence(Long tripId, Long deliveryOrderId,
                                                     MultipartFile goodsImage,
                                                     MultipartFile signDocumentImage,
                                                     String notes,
                                                     User actor) {
        Trip trip = assignedTrip(tripId, actor);
        TripDeliveryOrder row = tripDeliveryOrderRepository.findByTripIdAndDeliveryOrderId(trip.getId(), deliveryOrderId)
                .orElseThrow(() -> new OutboundDeliveryException("DELIVERY_ORDER_NOT_IN_TRIP", HttpStatus.FORBIDDEN, "Delivery order not in trip"));
        
        if (row.getSplitPlan() != null) {
            SplitDeliveryPlan plan = row.getSplitPlan();
            if (plan.getLeadDriver() == null || !plan.getLeadDriver().getId().equals(actor.getId())) {
                throw new OutboundDeliveryException("SPLIT_LEAD_DRIVER_REQUIRED", HttpStatus.FORBIDDEN, "Only lead driver can upload POD");
            }
        }
        
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        java.util.Map<String, Object> before = attemptSnapshot(delivery);

        boolean podChanged = false;
        if (goodsImage != null && !goodsImage.isEmpty()) {
            validatePodFile(goodsImage);
            StoredPodObject obj = podStorageService.upload(delivery.getId(), "GOODS", goodsImage);
            delivery.setGoodsImageObjectKey(obj.objectKey());
            delivery.setPodImageUrl(podStorageService.createSignedUrl(obj.objectKey(), 3600));
            podChanged = true;
        }
        if (signDocumentImage != null && !signDocumentImage.isEmpty()) {
            validatePodFile(signDocumentImage);
            StoredPodObject obj = podStorageService.upload(delivery.getId(), "SIGNED_DOCUMENT", signDocumentImage);
            delivery.setSignedDocumentObjectKey(obj.objectKey());
            delivery.setPodSignatureUrl(podStorageService.createSignedUrl(obj.objectKey(), 3600));
            podChanged = true;
        }
        
        if (podChanged) {
            delivery.setPodTimestamp(java.time.OffsetDateTime.now());
            // Invalidate active OTP
            otpRepository.findByDeliveryId(delivery.getId()).ifPresent(otp -> {
                if (otp.getStatus() == com.wms.enums.order_fulfillment.DeliveryOtpStatus.ACTIVE) {
                    otp.setStatus(com.wms.enums.order_fulfillment.DeliveryOtpStatus.EXPIRED);
                    otpRepository.save(otp);
                }
            });
        }

        Delivery saved = deliveryRepository.save(delivery);
        auditLogService.log(actor, AuditAction.UPLOAD_POD, "DELIVERY", saved.getId(), saved.getDeliveryNumber(), trip.getWarehouse().getId(), before, attemptSnapshot(saved));
        return toAttemptResponse(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public PodEvidenceSignedUrlsResponse getPodEvidenceSignedUrls(Long deliveryOrderId, User actor) {
        Delivery delivery = deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(deliveryOrderId, java.util.List.of(DeliveryStatus.PENDING, DeliveryStatus.IN_TRANSIT))
                .orElseThrow(() -> new OutboundDeliveryException("DELIVERY_NOT_FOUND", HttpStatus.NOT_FOUND, "Delivery not found"));
        Trip trip = delivery.getTrip();
        if (!trip.getDriver().getId().equals(actor.getId())) {
            throw new OutboundDeliveryException("TRIP_DRIVER_MISMATCH", HttpStatus.FORBIDDEN, "Driver mismatch");
        }
        PodEvidenceSignedUrlResponse goods = null;
        if (delivery.getGoodsImageObjectKey() != null) {
            goods = PodEvidenceSignedUrlResponse.builder()
                .signedUrl(podStorageService.createSignedUrl(delivery.getGoodsImageObjectKey(), 3600))
                .build();
        }
        PodEvidenceSignedUrlResponse sign = null;
        if (delivery.getSignedDocumentObjectKey() != null) {
            sign = PodEvidenceSignedUrlResponse.builder()
                .signedUrl(podStorageService.createSignedUrl(delivery.getSignedDocumentObjectKey(), 3600))
                .build();
        }
        return PodEvidenceSignedUrlsResponse.builder()
            .doId(delivery.getDeliveryOrder().getId())
            .deliveryId(delivery.getId())
            .goodsImage(goods)
            .signDocumentImage(sign)
            .build();
    }

    @Override
    @Transactional(noRollbackFor = OtpDeliveryFailedException.class)
    public DeliveryOtpResponse requestDeliveryOtp(Long tripId, Long deliveryOrderId, DeliveryOtpRequest request, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        TripDeliveryOrder row = tripDeliveryOrderRepository.findByTripIdAndDeliveryOrderId(trip.getId(), deliveryOrderId)
                .orElseThrow(() -> new OutboundDeliveryException("DELIVERY_ORDER_NOT_IN_TRIP", HttpStatus.FORBIDDEN, "Delivery order not in trip"));
        if (row.getSplitPlan() != null) {
            SplitDeliveryPlan plan = row.getSplitPlan();
            if (plan.getLeadDriver() == null || !plan.getLeadDriver().getId().equals(actor.getId())) {
                throw new OutboundDeliveryException("SPLIT_LEAD_DRIVER_REQUIRED", HttpStatus.FORBIDDEN, "Only lead driver can request OTP");
            }
        }
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        requirePod(delivery);
        Dealer dealer = delivery.getDeliveryOrder().getDealer();
        if (dealer.getEmail() == null || dealer.getEmail().isBlank()) {
            throw rule("DEALER_EMAIL_MISSING", "Dealer email is required");
        }

        DeliveryOtpAttempt otp = otpRepository.findByDeliveryId(delivery.getId()).orElse(null);
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        if (otp != null && otp.getStatus() == com.wms.enums.order_fulfillment.DeliveryOtpStatus.LOCKED) {
            throw locked("OTP_RESET_REQUIRED", "OTP is locked");
        }
        if (otp != null && otp.getStatus() == com.wms.enums.order_fulfillment.DeliveryOtpStatus.ACTIVE && otp.getExpiresAt().isAfter(now)) {
            throw conflict("OTP_STILL_ACTIVE", "Current OTP is still active");
        }

        java.util.Map<String, Object> before = otp == null ? null : otpSnapshot(otp);
        String code = sixDigitOtp();
        if (otp == null) {
            otp = new DeliveryOtpAttempt();
            otp.setDelivery(delivery);
            otp.setCreatedAt(now);
        }
        otp.setOtpHash(sha256(code));
        otp.setRecipientEmail(dealer.getEmail());
        otp.setExpiresAt(now.plusMinutes(5));
        otp.setConsumedAt(null);
        otp.setStatus(com.wms.enums.order_fulfillment.DeliveryOtpStatus.PENDING);
        otp.setAttemptCount(0);
        
        DeliveryOtpAttempt saved = otpRepository.save(otp);

        try {
            sendOtpEmail(dealer.getEmail(), code);
            saved.setStatus(com.wms.enums.order_fulfillment.DeliveryOtpStatus.ACTIVE);
            saved.setIssuedAt(java.time.OffsetDateTime.now());
            saved = otpRepository.save(saved);
            auditLogService.log(actor, AuditAction.REQUEST_OTP, "DELIVERY_OTP_ATTEMPT", saved.getId(), "OTP-" + delivery.getDeliveryNumber(), trip.getWarehouse().getId(), before, otpSnapshot(saved));
            return toOtpResponse(saved);
        } catch (Exception e) {
            saved.setStatus(com.wms.enums.order_fulfillment.DeliveryOtpStatus.SEND_FAILED);
            otpRepository.save(saved);
            throw new OtpDeliveryFailedException("Failed to send OTP via email");
        }
    }

    @Override
    @Transactional
    public DeliveryAttemptResponse confirmDelivery(Long tripId, Long deliveryOrderId, ConfirmDeliveryRequest request, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        requirePod(delivery);

        DeliveryOtpAttempt otp = otpRepository.findByDeliveryId(delivery.getId())
                .orElseThrow(() -> rule("OTP_NOT_REQUESTED", "Delivery OTP was not requested"));
        verifyOtp(otp, request.getOtp());

        java.util.Map<String, Object> before = attemptSnapshot(delivery);
        decrementTransitInventory(delivery.getDeliveryOrder());
        autoInvoiceService.createForConfirmedDelivery(delivery.getDeliveryOrder(), actor);

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        otp.setStatus(com.wms.enums.order_fulfillment.DeliveryOtpStatus.VERIFIED);
        otp.setConsumedAt(now);
        otpRepository.save(otp);

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setOtpVerifiedAt(now);
        delivery.setDeliveredAt(now);
        delivery.setUpdatedAt(now);
        
        delivery.getDeliveryOrder().setStatus(DeliveryOrderStatus.COMPLETED);
        delivery.getDeliveryOrder().setUpdatedAt(now);
        deliveryOrderRepository.save(delivery.getDeliveryOrder());

        Delivery saved = deliveryRepository.save(delivery);
        auditLogService.log(actor, AuditAction.CONFIRM_DELIVERY, "DELIVERY", saved.getId(), saved.getDeliveryNumber(), trip.getWarehouse().getId(), before, attemptSnapshot(saved));
        
        return toAttemptResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAttemptResponse failDelivery(Long tripId, Long deliveryOrderId, FailDeliveryRequest request, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        java.util.Map<String, Object> before = attemptSnapshot(delivery);

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        delivery.setUpdatedAt(now);

        delivery.getDeliveryOrder().setStatus(DeliveryOrderStatus.RETURNED);
        delivery.getDeliveryOrder().setUpdatedAt(now);
        deliveryOrderRepository.save(delivery.getDeliveryOrder());

        Delivery saved = deliveryRepository.save(delivery);
        auditLogService.log(actor, AuditAction.FAIL_DELIVERY, "DELIVERY", saved.getId(), saved.getDeliveryNumber(), trip.getWarehouse().getId(), before, attemptSnapshot(saved));
        
        return toAttemptResponse(saved);
    }

    @Override
    @Transactional
    public TripDriverViewResponse completeTrip(Long tripId, TripCompleteRequest request, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            throw rule("TRIP_NOT_READY_TO_COMPLETE", "Trip must be IN_TRANSIT");
        }

        java.util.List<TripDeliveryOrder> rows = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(tripId);
        boolean notReady = rows.stream().map(TripDeliveryOrder::getDeliveryOrder)
                .anyMatch(order -> !TERMINAL_DO_STATUSES.contains(order.getStatus()));
        if (notReady) {
            throw new OutboundDeliveryException("TRIP_NOT_READY_TO_COMPLETE", HttpStatus.BAD_REQUEST, "All delivery orders must be COMPLETED or RETURNED");
        }

        java.util.Map<String, Object> before = tripSnapshot(trip);
        java.time.OffsetDateTime now = request.getReturnedAt() == null ? java.time.OffsetDateTime.now() : request.getReturnedAt();
        
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(now);
        trip.setUpdatedAt(now);
        
        java.util.Optional<SplitDeliveryLeg> splitLegOpt = splitDeliveryLegRepository.findByTripId(trip.getId());
        if (splitLegOpt.isPresent()) {
            SplitDeliveryLeg leg = splitLegOpt.get();
            leg.setStatus(SplitDeliveryPlanStatus.COMPLETED);
            splitDeliveryLegRepository.save(leg);
            
            SplitDeliveryPlan plan = leg.getSplitPlan();
            boolean allLegsCompleted = splitDeliveryLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId()).stream()
                    .allMatch(l -> l.getStatus() == SplitDeliveryPlanStatus.COMPLETED);
            
            if (allLegsCompleted) {
                plan.setStatus(SplitDeliveryPlanStatus.COMPLETED);
                splitDeliveryPlanRepository.save(plan);
                trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                trip.getDriver().setStatus(DriverStatus.AVAILABLE);
            }
        } else {
            trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            trip.getDriver().setStatus(DriverStatus.AVAILABLE);
        }

        Trip saved = tripRepository.save(trip);
        auditLogService.log(actor, AuditAction.COMPLETE_TRIP, "TRIP", saved.getId(), saved.getTripNumber(), saved.getWarehouse().getId(), before, tripSnapshot(saved));
        return toTripDriverView(saved);
    }

    @Override
    @Transactional
    public DeliveryOtpResponse resetDeliveryOtp(Long deliveryOrderId, ResetDeliveryOtpRequest request, User actor) {
        // Admin/nhân sự hỗ trợ reset OTP bị khóa để tài xế có thể xin mã mới.
        Delivery delivery = deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(
                        deliveryOrderId, CURRENT_ATTEMPT_STATUSES)
                .orElseThrow(() -> notFound("Current delivery attempt not found"));
        DeliveryOtpAttempt otp = otpRepository.findByDeliveryId(delivery.getId())
                .orElseThrow(() -> notFound("Delivery OTP row not found"));
        if (otp.getStatus() != DeliveryOtpStatus.LOCKED) {
            throw locked("OTP_RESET_REQUIRED", "Only locked OTP rows can be reset");
        }
        Map<String, Object> before = otpSnapshot(otp);
        otp.setStatus(DeliveryOtpStatus.EXPIRED);
        otp.setAttemptCount(0);
        otp.setConsumedAt(null);
        otp.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        DeliveryOtpAttempt saved = otpRepository.save(otp);
        auditLogService.log(actor, AuditAction.RESET_DELIVERY_OTP, "DELIVERY_OTP_ATTEMPT",
                saved.getId(), "OTP-" + delivery.getDeliveryNumber(),
                delivery.getDeliveryOrder().getWarehouse().getId(), before, otpSnapshot(saved));
        return toOtpResponse(saved);
    }

    private Trip assignedTrip(Long tripId, User actor) {
        // Validate quyền tài xế: tài khoản đăng nhập phải đúng là tài xế được gán vào chuyến.
        return tripRepository.findAssignedDriverTrip(tripId, actor.getId())
                .orElseThrow(() -> new OutboundDeliveryException("DRIVER_NOT_ASSIGNED_TO_TRIP",
                        HttpStatus.FORBIDDEN, "Driver is not assigned to this trip"));
    }

    private Delivery currentAttempt(Trip trip, Long deliveryOrderId) {
        // Lấy lần giao hiện tại của đơn trong chuyến. Nếu đơn không thuộc chuyến hoặc không còn đang giao thì chặn.
        tripDeliveryOrderRepository.findByTripIdAndDeliveryOrderId(trip.getId(), deliveryOrderId)
                .orElseThrow(() -> new OutboundDeliveryException("DELIVERY_ORDER_NOT_IN_TRIP",
                        HttpStatus.FORBIDDEN, "Delivery order does not belong to this trip"));
        return deliveryRepository.findCurrentAttempt(trip.getId(), deliveryOrderId,
                        trip.getDriver().getId(), CURRENT_ATTEMPT_STATUSES)
                .orElseThrow(() -> notFound("Current delivery attempt not found"));
    }



    private void verifyOtp(DeliveryOtpAttempt otp, String rawOtp) {
        // Validate OTP: phải đang hiệu lực, chưa hết hạn, sai quá 3 lần thì khóa để tránh dò mã.
        OffsetDateTime now = OffsetDateTime.now();
        if (otp.getStatus() == DeliveryOtpStatus.LOCKED) {
            throw locked("OTP_RESET_REQUIRED", "OTP is locked and requires admin reset");
        }
        if (otp.getStatus() != DeliveryOtpStatus.ACTIVE) {
            throw rule("OTP_NOT_REQUESTED", "No active OTP exists for this delivery");
        }
        if (otp.getExpiresAt().isBefore(now)) {
            otp.setStatus(DeliveryOtpStatus.EXPIRED);
            otpRepository.save(otp);
            throw rule("DELIVERY_OTP_EXPIRED", "Delivery OTP expired");
        }
        if (!Objects.equals(otp.getOtpHash(), sha256(rawOtp))) {
            int attempts = otp.getAttemptCount() == null ? 0 : otp.getAttemptCount();
            otp.setAttemptCount(attempts + 1);
            if (otp.getAttemptCount() >= 3) {
                otp.setStatus(DeliveryOtpStatus.LOCKED);
                otpRepository.save(otp);
                throw locked("OTP_MAX_ATTEMPTS_EXCEEDED", "OTP max attempts exceeded");
            }
            otpRepository.save(otp);
            throw new OutboundDeliveryException("DELIVERY_OTP_INVALID",
                    HttpStatus.BAD_REQUEST, "Delivery OTP is invalid");
        }
    }

    private void decrementTransitInventory(DeliveryOrder order) {
        // Khi giao thành công, hàng rời khỏi kho ảo đang vận chuyển và không còn nằm trong tồn kho hệ thống.
        List<DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderId(order.getId());
        for (DeliveryOrderItem item : items) {
            Inventory transit = inventoryRepository.findTransitRowForDeliveryConfirmation(
                            item.getProduct().getId(), item.getBatch().getId())
                    .orElseThrow(() -> rule("IN_TRANSIT_STOCK_NOT_FOUND", "In-transit stock not found"));
            BigDecimal after = value(transit.getTotalQty()).subtract(value(item.getIssuedQty()));
            if (after.compareTo(ZERO) < 0) {
                throw rule("IN_TRANSIT_STOCK_NOT_FOUND", "In-transit stock is insufficient");
            }
            transit.setTotalQty(after);
            transit.setUpdatedAt(OffsetDateTime.now());
            saveInventory(transit);
        }
    }

    private TripDriverViewResponse toTripDriverView(Trip trip) {
        return toTripDriverView(trip, transferSummaryByTrip(trip));
    }

    private TripDriverViewResponse toTripDriverView(Trip trip, InterWarehouseTransfer transfer) {
        // Gom dữ liệu chuyến theo góc nhìn tài xế: thông tin xe, tuyến, điểm giao và trạng thái từng lần giao.
        // Với chuyến DELIVERY, danh sách điểm dừng là các delivery order; với chuyến TRANSFER, danh sách này thường rỗng.
        List<TripDeliveryOrder> rows = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId());
        Map<Long, Delivery> attempts = rows.isEmpty()
                ? Map.of()
                : deliveryRepository.findByTripIdAndDeliveryOrderIdIn(
                                trip.getId(),
                                rows.stream().map(row -> row.getDeliveryOrder().getId()).toList())
                        .stream()
                        .collect(Collectors.toMap(d -> d.getDeliveryOrder().getId(), Function.identity(), (first, ignored) -> first));
        TripType tripType = trip.getTripType() == null ? TripType.DELIVERY : trip.getTripType();
        // Nhánh điều chuyển nội bộ: response vẫn dùng chung model màn tài xế, nhưng bổ sung transferId/kho nguồn/kho đích/số dòng TRF.
        return TripDriverViewResponse.builder()
                .tripId(trip.getId())
                .tripNumber(trip.getTripNumber())
                .status(trip.getStatus())
                .tripType(tripType)
                .tripTypeLabel(tripTypeLabel(tripType))
                .transferId(transfer == null ? null : transfer.getId())
                .driverId(trip.getDriver().getId())
                .driverName(trip.getDriver().getFullName())
                .vehicleId(trip.getVehicle().getId())
                .vehiclePlate(trip.getVehicle().getPlateNumber())
                .plannedDate(trip.getPlannedDate())
                .plannedStartAt(trip.getPlannedStartAt())
                .plannedEndAt(trip.getPlannedEndAt())
                .totalWeightKg(trip.getTotalWeightKg())
                .totalVolumeM3(trip.getTotalVolumeM3())
                .deliveryStopCount(rows.size())
                .sourceWarehouseCode(transfer == null ? null : transfer.getSourceWarehouse().getCode())
                .destinationWarehouseCode(transfer == null ? null : transfer.getDestinationWarehouse().getCode())
                .transferLineCount(transfer == null ? null : transfer.getItems().size())
                .deliveryOrders(rows.stream()
                        .map(row -> {
                            DeliveryOrder order = row.getDeliveryOrder();
                            Dealer dealer = order.getDealer();
                            SplitDeliveryPlan splitPlan = row.getSplitPlan();
                            SplitDeliveryPlanStatus legStatus = null;
                            if (splitPlan != null) {
                                legStatus = splitDeliveryLegRepository.findByTripId(trip.getId())
                                        .map(SplitDeliveryLeg::getStatus)
                                        .orElse(null);
                            }
                            return DriverDeliveryOrderResponse.builder()
                                    .doId(order.getId())
                                    .doNumber(order.getDoNumber())
                                    .dealerName(dealer == null ? null : dealer.getName())
                                    .dealerAddress(dealer == null ? null : dealer.getDefaultDeliveryAddress())
                                    .status(order.getStatus())
                                    .stopOrder(row.getStopOrder())
                                    .currentAttempt(toAttemptResponseOrNull(attempts.get(order.getId())))
                                    .splitPlanId(splitPlan == null ? null : splitPlan.getId())
                                    .splitPlanStatus(splitPlan == null ? null : splitPlan.getStatus())
                                    .isSplitLead(splitPlan != null && splitPlan.getLeadDriver() != null && splitPlan.getLeadDriver().getId().equals(trip.getDriver().getId()))
                                    .splitLegStatus(legStatus)
                                    .build();
                        })
                        .toList())
                .build();
    }

    private Map<Long, InterWarehouseTransfer> transferSummariesByTripId(List<Trip> trips) {
        // Với chuyến điều chuyển nội bộ, lấy thêm thông tin kho nguồn/kho đích để tài xế nhìn được đúng tuyến.
        List<Long> transferTripIds = trips.stream()
                .filter(trip -> trip.getTripType() == TripType.TRANSFER)
                .map(Trip::getId)
                .toList();
        if (transferTripIds.isEmpty()) {
            return Map.of();
        }
        return interWarehouseTransferRepository.findByTripIdInWithSummary(transferTripIds)
                .stream()
                .filter(transfer -> transfer.getTrip() != null)
                .collect(Collectors.toMap(
                        transfer -> transfer.getTrip().getId(),
                        Function.identity(),
                        (first, ignored) -> first));
    }

    private InterWarehouseTransfer transferSummaryByTrip(Trip trip) {
        // Chỉ chuyến điều chuyển nội bộ mới có bản ghi InterWarehouseTransfer gắn với trip.
        if (trip.getTripType() != TripType.TRANSFER) {
            return null;
        }
        return interWarehouseTransferRepository.findByTripIdWithSummary(trip.getId()).orElse(null);
    }

    private String tripTypeLabel(TripType tripType) {
        // Label cho màn tài xế: tách rõ chuyến giao đại lý và chuyến điều chuyển nội bộ.
        return tripType == TripType.TRANSFER ? "Dieu chuyen noi bo" : "Giao dai ly";
    }

    private DeliveryAttemptResponse toAttemptResponseOrNull(Delivery delivery) {
        return delivery == null ? null : toAttemptResponse(delivery);
    }

    private DeliveryAttemptResponse toAttemptResponse(Delivery delivery) {
        return DeliveryAttemptResponse.builder()
                .deliveryId(delivery.getId())
                .attemptNumber(delivery.getAttemptNumber())
                .status(delivery.getStatus())
                .podImageUrl(delivery.getPodImageUrl())
                .podSignatureUrl(delivery.getPodSignatureUrl())
                .podTimestamp(delivery.getPodTimestamp())
                .otpVerifiedAt(delivery.getOtpVerifiedAt())
                .failureReason(delivery.getFailureReason())
                .dispatchedAt(delivery.getDispatchedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .build();
    }

    private DeliveryOtpResponse toOtpResponse(DeliveryOtpAttempt otp) {
        return DeliveryOtpResponse.builder()
                .deliveryId(otp.getDelivery().getId())
                .recipientEmail(otp.getRecipientEmail())
                .status(otp.getStatus())
                .expiresAt(otp.getExpiresAt())
                .attemptCount(otp.getAttemptCount())
                .build();
    }

    private void validatePodFile(MultipartFile file) {
        // Validate ảnh POD: bắt buộc là ảnh và giới hạn 5MB để tránh upload file sai loại/quá lớn.
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_POD_BYTES
                || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new OutboundDeliveryException("POD_FILE_INVALID",
                    HttpStatus.BAD_REQUEST, "POD file must be an image up to 5MB");
        }
    }

    private String storePodFile(MultipartFile file, String prefix) {
        // Lưu ảnh POD xuống thư mục uploads/pod và trả đường dẫn để frontend hiển thị lại.
        try {
            Files.createDirectories(Path.of("uploads", "pod"));
            String ext = extension(file.getOriginalFilename());
            String filename = prefix + "-" + UUID.randomUUID() + ext;
            Path target = Path.of("uploads", "pod", filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/pod/" + filename;
        } catch (IOException ex) {
            throw new OutboundDeliveryException("POD_STORAGE_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not store POD evidence");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".bin";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private void requirePod(Delivery delivery) {
        // Trước khi xin OTP hoặc xác nhận giao thành công phải có đủ ảnh hàng và ảnh ký nhận/chứng từ.
        boolean hasObjectKeys = delivery.getGoodsImageObjectKey() != null && delivery.getSignedDocumentObjectKey() != null;
        boolean hasUrls = delivery.getPodImageUrl() != null && delivery.getPodSignatureUrl() != null;
        if (!hasObjectKeys && !hasUrls) {
            throw new OutboundDeliveryException("MISSING_POD",
                    HttpStatus.BAD_REQUEST, "Both POD images are required");
        }
    }

    private void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Delivery confirmation OTP");
        message.setText("Your delivery confirmation OTP is: " + otp + "\nThis code is valid for 5 minutes.");
        mailSender.send(message);
    }

    private String sixDigitOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private void saveInventory(Inventory inventory) {
        // Lưu tồn kho kèm xử lý xung đột version để tránh hai thao tác cùng ghi đè một dòng tồn.
        try {
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw conflict("INVENTORY_VERSION_CONFLICT", "Inventory version conflict");
        }
    }

    private void audit(User actor, AuditAction action, Delivery delivery,
                       Map<String, Object> before, Map<String, Object> after) {
        // Ghi lịch sử cho từng lần giao: tài xế nào thao tác, đơn nào, trước/sau ra sao.
        auditLogService.log(actor, action, "DELIVERY", delivery.getId(),
                delivery.getDeliveryNumber(), delivery.getDeliveryOrder().getWarehouse().getId(), before, after);
    }

    private java.util.Map<String, Object> attemptSnapshot(com.wms.entity.order_fulfillment.Delivery delivery) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("status", delivery.getStatus());
        map.put("podImageUrl", delivery.getPodImageUrl());
        map.put("podSignatureUrl", delivery.getPodSignatureUrl());
        return map;
    }

    private java.util.Map<String, Object> otpSnapshot(com.wms.entity.order_fulfillment.DeliveryOtpAttempt otp) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("status", otp.getStatus());
        map.put("attemptCount", otp.getAttemptCount());
        map.put("expiresAt", otp.getExpiresAt());
        map.put("issuedAt", otp.getIssuedAt());
        return map;
    }
    private Map<String, Object> tripSnapshot(Trip trip) {
        return Map.of(
                "tripId", trip.getId(),
                "status", trip.getStatus(),
                "vehicleStatus", trip.getVehicle().getStatus(),
                "driverStatus", trip.getDriver().getStatus());
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }

    private OutboundDeliveryException conflict(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.CONFLICT, message);
    }

    private OutboundDeliveryException rule(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private OutboundDeliveryException locked(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.LOCKED, message);
    }
}
