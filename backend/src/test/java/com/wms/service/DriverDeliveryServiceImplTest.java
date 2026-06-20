package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.entity.Batch;
import com.wms.entity.Dealer;
import com.wms.entity.Delivery;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.DeliveryOtpAttempt;
import com.wms.entity.Driver;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.Trip;
import com.wms.entity.TripDeliveryOrder;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import com.wms.enums.DeliveryOtpStatus;
import com.wms.enums.DeliveryStatus;
import com.wms.enums.DriverStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.UserRole;
import com.wms.enums.VehicleStatus;
import com.wms.enums.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryOtpAttemptRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.TripDeliveryOrderRepository;
import com.wms.repository.TripRepository;
import com.wms.service.impl.DriverDeliveryServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DriverDeliveryServiceImplTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripDeliveryOrderRepository tripDeliveryOrderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryOtpAttemptRepository otpRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private JavaMailSender mailSender;

    private DriverDeliveryServiceImpl service;
    private User actor;
    private Warehouse warehouse;
    private Trip trip;
    private DeliveryOrder order;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        service = new DriverDeliveryServiceImpl(tripRepository, tripDeliveryOrderRepository, deliveryRepository,
                otpRepository, deliveryOrderRepository, deliveryOrderItemRepository, inventoryRepository,
                invoiceRepository, auditLogService, mailSender);
        actor = User.builder().id(10L).fullName("Driver").role(UserRole.DRIVER).build();
        warehouse = Warehouse.builder().id(20L).code("HP").name("Hai Phong")
                .type(WarehouseType.PHYSICAL).isActive(true).build();
        Driver driver = Driver.builder().id(30L).user(actor).warehouse(warehouse)
                .status(DriverStatus.ON_TRIP).isActive(true).build();
        Vehicle vehicle = Vehicle.builder().id(40L).warehouse(warehouse)
                .status(VehicleStatus.ON_TRIP).isActive(true).build();
        trip = Trip.builder().id(50L).tripNumber("TRIP-1").warehouse(warehouse)
                .driver(driver).vehicle(vehicle).dispatcher(actor).plannedDate(LocalDate.now())
                .tripType(TripType.DELIVERY).status(TripStatus.IN_TRANSIT).build();
        Dealer dealer = Dealer.builder().id(60L).name("Dealer").email("dealer@example.com")
                .currentBalance(BigDecimal.ZERO).paymentTermDays(30).creditLimit(BigDecimal.TEN).build();
        order = DeliveryOrder.builder().id(70L).doNumber("DO-1").dealer(dealer).warehouse(warehouse)
                .type(DeliveryOrderType.SALE).status(DeliveryOrderStatus.IN_TRANSIT)
                .createdBy(actor).documentDate(LocalDate.now()).build();
        delivery = Delivery.builder().id(80L).deliveryNumber("DLV-1").trip(trip)
                .deliveryOrder(order).driver(driver).vehicle(vehicle).attemptNumber(1)
                .status(DeliveryStatus.IN_TRANSIT).createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()).build();
    }

    @Test
    void getAssignedTrip_rejectsDriverOutsideAssignedTrip() {
        when(tripRepository.findAssignedDriverTrip(50L, actor.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAssignedTrip(50L, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("Driver is not assigned");
    }

    @Test
    void uploadPodEvidence_rejectsMissingOversizedOrNonImageFiles() {
        stubCurrentAttempt();
        MockMultipartFile textFile = new MockMultipartFile(
                "goodsImage", "pod.txt", "text/plain", "not image".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile imageFile = image("signDocumentImage");

        assertThatThrownBy(() -> service.uploadPodEvidence(50L, 70L, textFile, imageFile, null, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("POD file");

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void requestDeliveryOtp_rejectsMissingPod() {
        stubCurrentAttempt();

        assertThatThrownBy(() -> service.requestDeliveryOtp(50L, 70L, new DeliveryOtpRequest(), actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("Both POD images");
    }

    @Test
    void requestDeliveryOtp_rejectsMissingDealerEmail() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        order.getDealer().setEmail(" ");
        stubCurrentAttempt();

        assertThatThrownBy(() -> service.requestDeliveryOtp(50L, 70L, new DeliveryOtpRequest(), actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("Dealer email");
    }

    @Test
    void requestDeliveryOtp_updatesSameRowAfterExpiry() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.EXPIRED, OffsetDateTime.now().minusMinutes(1), 2, "000000");
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(otpRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.requestDeliveryOtp(50L, 70L, new DeliveryOtpRequest(), actor);

        ArgumentCaptor<DeliveryOtpAttempt> captor = ArgumentCaptor.forClass(DeliveryOtpAttempt.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(otp);
        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.ACTIVE);
        assertThat(otp.getAttemptCount()).isZero();
        assertThat(otp.getConsumedAt()).isNull();
    }

    @Test
    void requestDeliveryOtp_blocksResendWhileActive() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L))
                .thenReturn(Optional.of(otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456")));

        assertThatThrownBy(() -> service.requestDeliveryOtp(50L, 70L, new DeliveryOtpRequest(), actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("still active");
    }

    @Test
    void confirmDelivery_locksOtpAfterThirdWrongSubmission() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 2, "123456");
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("000000");
        assertThatThrownBy(() -> service.confirmDelivery(50L, 70L, request, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("max attempts");

        assertThat(otp.getAttemptCount()).isEqualTo(3);
        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.LOCKED);
        verify(otpRepository).save(otp);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void confirmDelivery_rejectsExpiredOtp() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().minusMinutes(1), 0, "123456");
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        assertThatThrownBy(() -> service.confirmDelivery(50L, 70L, request, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("expired");

        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.EXPIRED);
    }

    @Test
    void confirmDelivery_updatesAttemptInventoryInvoiceAndOrder() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456");
        DeliveryOrderItem item = item(BigDecimal.valueOf(2), BigDecimal.valueOf(50));
        Inventory transit = Inventory.builder().id(90L).totalQty(BigDecimal.valueOf(5))
                .reservedQty(BigDecimal.ZERO).costPrice(BigDecimal.TEN).build();
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(invoiceRepository.existsByDeliveryOrderId(70L)).thenReturn(false);
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        service.confirmDelivery(50L, 70L, request, actor);

        assertThat(transit.getTotalQty()).isEqualByComparingTo("3");
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.COMPLETED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.VERIFIED);
        assertThat(order.getDealer().getCurrentBalance()).isEqualByComparingTo("100");
        verify(invoiceRepository).save(any());
        verify(inventoryRepository).save(transit);
    }

    @Test
    void confirmDelivery_rejectsExistingInvoiceOrMissingTransitStock() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L))
                .thenReturn(Optional.of(otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456")));
        when(invoiceRepository.existsByDeliveryOrderId(70L)).thenReturn(true);

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        assertThatThrownBy(() -> service.confirmDelivery(50L, 70L, request, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("Invoice already exists");
    }

    @Test
    void failDelivery_movesAttemptAndOrderWithoutInventoryMutation() {
        stubCurrentAttempt();
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        FailDeliveryRequest request = new FailDeliveryRequest();
        request.setFailureReason("Dealer refused goods");

        service.failDelivery(50L, 70L, request, actor);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.RETURNED);
        assertThat(delivery.getFailureReason()).isEqualTo("Dealer refused goods");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void completeTrip_rejectsBeforeAllDeliveryOrdersTerminal() {
        TripDeliveryOrder row = TripDeliveryOrder.builder().trip(trip).deliveryOrder(order).stopOrder(1).build();
        when(tripRepository.findAssignedDriverTrip(50L, actor.getId())).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(50L)).thenReturn(List.of(row));

        assertThatThrownBy(() -> service.completeTrip(50L, new TripCompleteRequest(), actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("COMPLETED or RETURNED");
    }

    @Test
    void completeTrip_releasesVehicleAndDriver() {
        order.setStatus(DeliveryOrderStatus.COMPLETED);
        TripDeliveryOrder row = TripDeliveryOrder.builder().trip(trip).deliveryOrder(order).stopOrder(1).build();
        when(tripRepository.findAssignedDriverTrip(50L, actor.getId())).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(50L)).thenReturn(List.of(row));
        when(tripRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.completeTrip(50L, new TripCompleteRequest(), actor);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getVehicle().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(trip.getDriver().getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void resetDeliveryOtp_rejectsMissingCurrentAttemptOrOtpRow() {
        when(deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(eq(70L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetDeliveryOtp(70L, resetRequest(), actor))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Current delivery attempt");
    }

    @Test
    void resetDeliveryOtp_resetsLockedRowForSameRowReuse() {
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.LOCKED, OffsetDateTime.now().plusMinutes(2), 3, "123456");
        when(deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(eq(70L), any()))
                .thenReturn(Optional.of(delivery));
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(otpRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resetDeliveryOtp(70L, resetRequest(), actor);

        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.EXPIRED);
        assertThat(otp.getAttemptCount()).isZero();
        assertThat(otp.getConsumedAt()).isNull();
    }

    private void stubCurrentAttempt() {
        when(tripRepository.findAssignedDriverTrip(50L, actor.getId())).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdAndDeliveryOrderId(50L, 70L))
                .thenReturn(Optional.of(TripDeliveryOrder.builder().trip(trip).deliveryOrder(order).stopOrder(1).build()));
        when(deliveryRepository.findCurrentAttempt(eq(50L), eq(70L), eq(30L), any()))
                .thenReturn(Optional.of(delivery));
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg",
                "fake image".getBytes(StandardCharsets.UTF_8));
    }

    private DeliveryOtpAttempt otp(DeliveryOtpStatus status, OffsetDateTime expiresAt, int attempts, String rawOtp) {
        DeliveryOtpAttempt otp = new DeliveryOtpAttempt();
        otp.setId(300L);
        otp.setDelivery(delivery);
        otp.setRecipientEmail("dealer@example.com");
        otp.setOtpHash(sha256(rawOtp));
        otp.setExpiresAt(expiresAt);
        otp.setStatus(status);
        otp.setAttemptCount(attempts);
        otp.setCreatedAt(OffsetDateTime.now());
        return otp;
    }

    private DeliveryOrderItem item(BigDecimal qty, BigDecimal unitPrice) {
        Product product = Product.builder().id(100L).sku("SKU").name("Pan").unit("pcs").build();
        Batch batch = Batch.builder().id(200L).batchNumber("B1").product(product).warehouse(warehouse).build();
        return DeliveryOrderItem.builder()
                .id(400L)
                .deliveryOrder(order)
                .product(product)
                .batch(batch)
                .issuedQty(qty)
                .unitPrice(unitPrice)
                .build();
    }

    private ResetDeliveryOtpRequest resetRequest() {
        ResetDeliveryOtpRequest request = new ResetDeliveryOtpRequest();
        request.setResetReason("Dealer requested a fresh OTP");
        return request;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
