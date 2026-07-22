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
import com.wms.entity.billing_payment.BillingNotification;
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
import com.wms.repository.BillingNotificationRepository;
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
import org.springframework.web.multipart.MultipartFile;

@Service
public class DriverDeliveryServiceImpl implements DriverDeliveryService {

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
    private final BillingNotificationRepository billingNotificationRepository;
    private final AuditLogService auditLogService;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public DriverDeliveryServiceImpl(TripRepository tripRepository,
                                     TripDeliveryOrderRepository tripDeliveryOrderRepository,
                                     DeliveryRepository deliveryRepository,
                                     DeliveryOtpAttemptRepository otpRepository,
                                     DeliveryOrderRepository deliveryOrderRepository,
                                     DeliveryOrderItemRepository deliveryOrderItemRepository,
                                     InventoryRepository inventoryRepository,
                                     InterWarehouseTransferRepository interWarehouseTransferRepository,
                                     AutoInvoiceService autoInvoiceService,
                                     BillingNotificationRepository billingNotificationRepository,
                                     AuditLogService auditLogService,
                                     JavaMailSender mailSender) {
        this.tripRepository = tripRepository;
        this.tripDeliveryOrderRepository = tripDeliveryOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.otpRepository = otpRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.interWarehouseTransferRepository = interWarehouseTransferRepository;
        this.autoInvoiceService = autoInvoiceService;
        this.billingNotificationRepository = billingNotificationRepository;
        this.auditLogService = auditLogService;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional(readOnly = true)
    public TripDriverViewResponse getAssignedTrip(Long tripId, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        InterWarehouseTransfer transfer = transferSummaryByTrip(trip);
        return toTripDriverView(trip, transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDriverViewResponse> listMyTrips(User actor) {
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
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        validatePodFile(goodsImage);
        validatePodFile(signDocumentImage);
        Map<String, Object> before = attemptSnapshot(delivery);
        delivery.setPodImageUrl(storePodFile(goodsImage, "goods"));
        delivery.setPodSignatureUrl(storePodFile(signDocumentImage, "signature"));
        delivery.setPodTimestamp(OffsetDateTime.now());
        delivery.setUpdatedAt(OffsetDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);
        audit(actor, AuditAction.UPLOAD_POD, saved, before, attemptSnapshot(saved));
        return toAttemptResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryOtpResponse requestDeliveryOtp(Long tripId, Long deliveryOrderId,
                                                  DeliveryOtpRequest request,
                                                  User actor) {
        Trip trip = assignedTrip(tripId, actor);
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        requirePod(delivery);
        Dealer dealer = delivery.getDeliveryOrder().getDealer();
        if (dealer.getEmail() == null || dealer.getEmail().isBlank()) {
            throw rule("DEALER_EMAIL_MISSING", "Dealer email is required before requesting delivery OTP");
        }
        DeliveryOtpAttempt otp = otpRepository.findByDeliveryId(delivery.getId()).orElse(null);
        OffsetDateTime now = OffsetDateTime.now();
        if (otp != null && otp.getStatus() == DeliveryOtpStatus.LOCKED) {
            throw locked("OTP_RESET_REQUIRED", "OTP is locked and requires admin reset");
        }
        if (otp != null && otp.getStatus() == DeliveryOtpStatus.ACTIVE && otp.getExpiresAt().isAfter(now)) {
            throw conflict("OTP_STILL_ACTIVE", "Current OTP is still active");
        }
        Map<String, Object> before = otp == null ? null : otpSnapshot(otp);
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
        otp.setStatus(DeliveryOtpStatus.ACTIVE);
        otp.setAttemptCount(0);
        DeliveryOtpAttempt saved = otpRepository.save(otp);
        sendOtpEmail(dealer.getEmail(), code);
        auditLogService.log(actor, AuditAction.REQUEST_OTP, "DELIVERY_OTP_ATTEMPT",
                saved.getId(), "OTP-" + delivery.getDeliveryNumber(), trip.getWarehouse().getId(),
                before, otpSnapshot(saved));
        return toOtpResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAttemptResponse confirmDelivery(Long tripId, Long deliveryOrderId,
                                                   ConfirmDeliveryRequest request,
                                                   User actor) {
        Trip trip = assignedTrip(tripId, actor);
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        requirePod(delivery);
        DeliveryOtpAttempt otp = otpRepository.findByDeliveryId(delivery.getId())
                .orElseThrow(() -> rule("OTP_NOT_REQUESTED", "Delivery OTP was not requested"));
        verifyOtp(otp, request.getOtp());
        Map<String, Object> before = attemptSnapshot(delivery);
        decrementTransitInventory(delivery.getDeliveryOrder());
        createBillingNotification(delivery.getDeliveryOrder());
        autoInvoiceService.createForConfirmedDelivery(delivery.getDeliveryOrder(), actor);
        OffsetDateTime now = OffsetDateTime.now();
        otp.setStatus(DeliveryOtpStatus.VERIFIED);
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
        audit(actor, AuditAction.CONFIRM_DELIVERY, saved, before, attemptSnapshot(saved));
        return toAttemptResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAttemptResponse failDelivery(Long tripId, Long deliveryOrderId,
                                                FailDeliveryRequest request,
                                                User actor) {
        Trip trip = assignedTrip(tripId, actor);
        Delivery delivery = currentAttempt(trip, deliveryOrderId);
        Map<String, Object> before = attemptSnapshot(delivery);
        OffsetDateTime now = OffsetDateTime.now();
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        delivery.setUpdatedAt(now);
        delivery.getDeliveryOrder().setStatus(DeliveryOrderStatus.RETURNED);
        delivery.getDeliveryOrder().setUpdatedAt(now);
        deliveryOrderRepository.save(delivery.getDeliveryOrder());
        Delivery saved = deliveryRepository.save(delivery);
        audit(actor, AuditAction.FAIL_DELIVERY, saved, before, attemptSnapshot(saved));
        return toAttemptResponse(saved);
    }

    @Override
    @Transactional
    public TripDriverViewResponse completeTrip(Long tripId, TripCompleteRequest request, User actor) {
        Trip trip = assignedTrip(tripId, actor);
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            throw rule("TRIP_NOT_READY_TO_COMPLETE", "Trip must be IN_TRANSIT before completion");
        }
        List<TripDeliveryOrder> rows = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(tripId);
        boolean notReady = rows.stream().map(TripDeliveryOrder::getDeliveryOrder)
                .anyMatch(order -> !TERMINAL_DO_STATUSES.contains(order.getStatus()));
        if (notReady) {
            throw rule("TRIP_NOT_READY_TO_COMPLETE", "All delivery orders must be COMPLETED or RETURNED");
        }
        Map<String, Object> before = tripSnapshot(trip);
        OffsetDateTime now = request.getReturnedAt() == null ? OffsetDateTime.now() : request.getReturnedAt();
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(now);
        trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        trip.getDriver().setStatus(DriverStatus.AVAILABLE);
        trip.setUpdatedAt(now);
        Trip saved = tripRepository.save(trip);
        auditLogService.log(actor, AuditAction.COMPLETE_TRIP, "TRIP", saved.getId(), saved.getTripNumber(),
                saved.getWarehouse().getId(), before, tripSnapshot(saved));
        return toTripDriverView(saved);
    }

    @Override
    @Transactional
    public DeliveryOtpResponse resetDeliveryOtp(Long deliveryOrderId, ResetDeliveryOtpRequest request, User actor) {
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
        return tripRepository.findAssignedDriverTrip(tripId, actor.getId())
                .orElseThrow(() -> new OutboundDeliveryException("DRIVER_NOT_ASSIGNED_TO_TRIP",
                        HttpStatus.FORBIDDEN, "Driver is not assigned to this trip"));
    }

    private Delivery currentAttempt(Trip trip, Long deliveryOrderId) {
        tripDeliveryOrderRepository.findByTripIdAndDeliveryOrderId(trip.getId(), deliveryOrderId)
                .orElseThrow(() -> new OutboundDeliveryException("DELIVERY_ORDER_NOT_IN_TRIP",
                        HttpStatus.FORBIDDEN, "Delivery order does not belong to this trip"));
        return deliveryRepository.findCurrentAttempt(trip.getId(), deliveryOrderId,
                        trip.getDriver().getId(), CURRENT_ATTEMPT_STATUSES)
                .orElseThrow(() -> notFound("Current delivery attempt not found"));
    }

    private void verifyOtp(DeliveryOtpAttempt otp, String rawOtp) {
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

    // Creates the accountant reconciliation worklist entry (Spec 008, billing_notifications)
    // before invoice creation runs, so AutoInvoiceServiceImpl can find and archive it in the
    // same transaction. totalAmountEstimate is a rough estimate independent of the formal
    // invoice total — it must not block delivery confirmation if line pricing is incomplete.
    private void createBillingNotification(DeliveryOrder order) {
        List<DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderId(order.getId());
        BigDecimal estimate = items.stream()
                .map(this::estimateLineAmount)
                .reduce(ZERO, BigDecimal::add);
        Dealer dealer = order.getDealer();
        BillingNotification notification = BillingNotification.builder()
                .deliveryOrder(order)
                .doNumber(order.getDoNumber())
                .dealer(dealer)
                .dealerName(dealer.getName())
                .warehouse(order.getWarehouse())
                .deliveredAt(OffsetDateTime.now())
                .totalAmountEstimate(estimate)
                .build();
        billingNotificationRepository.save(notification);
    }

    private BigDecimal estimateLineAmount(DeliveryOrderItem item) {
        BigDecimal quantity = value(item.getIssuedQty()).compareTo(ZERO) > 0
                ? value(item.getIssuedQty())
                : value(item.getRequestedQty());
        BigDecimal unitPrice = value(item.getUnitPrice());
        return quantity.multiply(unitPrice);
    }

    private TripDriverViewResponse toTripDriverView(Trip trip) {
        return toTripDriverView(trip, transferSummaryByTrip(trip));
    }

    private TripDriverViewResponse toTripDriverView(Trip trip, InterWarehouseTransfer transfer) {
        List<TripDeliveryOrder> rows = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId());
        Map<Long, Delivery> attempts = rows.isEmpty()
                ? Map.of()
                : deliveryRepository.findByTripIdAndDeliveryOrderIdIn(
                                trip.getId(),
                                rows.stream().map(row -> row.getDeliveryOrder().getId()).toList())
                        .stream()
                        .collect(Collectors.toMap(d -> d.getDeliveryOrder().getId(), Function.identity(), (first, ignored) -> first));
        TripType tripType = trip.getTripType() == null ? TripType.DELIVERY : trip.getTripType();
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
                        .map(row -> DriverDeliveryOrderResponse.builder()
                                .doId(row.getDeliveryOrder().getId())
                                .doNumber(row.getDeliveryOrder().getDoNumber())
                                .status(row.getDeliveryOrder().getStatus())
                                .stopOrder(row.getStopOrder())
                                .currentAttempt(toAttemptResponseOrNull(attempts.get(row.getDeliveryOrder().getId())))
                                .build())
                        .toList())
                .build();
    }

    private Map<Long, InterWarehouseTransfer> transferSummariesByTripId(List<Trip> trips) {
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
        if (trip.getTripType() != TripType.TRANSFER) {
            return null;
        }
        return interWarehouseTransferRepository.findByTripIdWithSummary(trip.getId()).orElse(null);
    }

    private String tripTypeLabel(TripType tripType) {
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
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_POD_BYTES
                || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new OutboundDeliveryException("POD_FILE_INVALID",
                    HttpStatus.BAD_REQUEST, "POD file must be an image up to 5MB");
        }
    }

    private String storePodFile(MultipartFile file, String prefix) {
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
        if (delivery.getPodImageUrl() == null || delivery.getPodSignatureUrl() == null) {
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
        try {
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw conflict("INVENTORY_VERSION_CONFLICT", "Inventory version conflict");
        }
    }

    private void audit(User actor, AuditAction action, Delivery delivery,
                       Map<String, Object> before, Map<String, Object> after) {
        auditLogService.log(actor, action, "DELIVERY", delivery.getId(),
                delivery.getDeliveryNumber(), delivery.getDeliveryOrder().getWarehouse().getId(), before, after);
    }

    private Map<String, Object> attemptSnapshot(Delivery delivery) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("deliveryId", delivery.getId());
        values.put("deliveryOrderId", delivery.getDeliveryOrder().getId());
        values.put("status", delivery.getStatus());
        values.put("podImageUrl", delivery.getPodImageUrl());
        values.put("podSignatureUrl", delivery.getPodSignatureUrl());
        values.put("failureReason", delivery.getFailureReason());
        return values;
    }

    private Map<String, Object> otpSnapshot(DeliveryOtpAttempt otp) {
        return Map.of(
                "id", otp.getId(),
                "deliveryId", otp.getDelivery().getId(),
                "status", otp.getStatus(),
                "attemptCount", otp.getAttemptCount(),
                "expiresAt", otp.getExpiresAt());
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
