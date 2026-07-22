package com.wms.service;


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
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.ConfirmDeliveryRequest;
import com.wms.dto.request.DeliveryOtpRequest;
import com.wms.dto.request.FailDeliveryRequest;
import com.wms.dto.request.ResetDeliveryOtpRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOtpAttempt;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferItem;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.order_fulfillment.Trip;
import com.wms.entity.order_fulfillment.TripDeliveryOrder;
import com.wms.entity.access_control.User;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderType;
import com.wms.enums.order_fulfillment.DeliveryOtpStatus;
import com.wms.enums.order_fulfillment.DeliveryStatus;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.order_fulfillment.TripType;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.enums.warehouse_location.WarehouseType;
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
import com.wms.service.order_fulfillment.impl.DriverDeliveryServiceImpl;
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
import org.springframework.mail.SimpleMailMessage;
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
    @Mock private InterWarehouseTransferRepository interWarehouseTransferRepository;
    @Mock private AutoInvoiceService autoInvoiceService;
    @Mock private BillingNotificationRepository billingNotificationRepository;
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
                interWarehouseTransferRepository,
                autoInvoiceService, billingNotificationRepository, auditLogService, mailSender);
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
    void listMyTrips_mapsDeliveryAndTransferSummariesForAssignedDriverOnly() {
        Warehouse destination = Warehouse.builder().id(21L).code("HN").name("Ha Noi")
                .type(WarehouseType.PHYSICAL).isActive(true).build();
        Trip transferTrip = Trip.builder().id(51L).tripNumber("TTR-1").warehouse(warehouse)
                .driver(trip.getDriver()).vehicle(trip.getVehicle()).dispatcher(actor).plannedDate(LocalDate.now())
                .tripType(TripType.TRANSFER).status(TripStatus.PLANNED).build();
        InterWarehouseTransfer transfer = InterWarehouseTransfer.builder()
                .id(500L)
                .transferNumber("TRF-1")
                .sourceWarehouse(warehouse)
                .destinationWarehouse(destination)
                .trip(transferTrip)
                .items(List.of(
                        InterWarehouseTransferItem.builder().id(501L).plannedQty(BigDecimal.ONE).build(),
                        InterWarehouseTransferItem.builder().id(502L).plannedQty(BigDecimal.TEN).build()))
                .build();
        TripDeliveryOrder row = TripDeliveryOrder.builder().trip(trip).deliveryOrder(order).stopOrder(1).build();

        when(tripRepository.findAssignedDriverTrips(actor.getId())).thenReturn(List.of(trip, transferTrip));
        when(interWarehouseTransferRepository.findByTripIdInWithSummary(List.of(51L))).thenReturn(List.of(transfer));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(50L)).thenReturn(List.of(row));
        when(deliveryRepository.findByTripIdAndDeliveryOrderIdIn(50L, List.of(70L))).thenReturn(List.of(delivery));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(51L)).thenReturn(List.of());

        List<com.wms.dto.response.TripDriverViewResponse> responses = service.listMyTrips(actor);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTripType()).isEqualTo(TripType.DELIVERY);
        assertThat(responses.get(0).getTripTypeLabel()).isEqualTo("Giao dai ly");
        assertThat(responses.get(0).getDeliveryStopCount()).isEqualTo(1);
        assertThat(responses.get(1).getTripType()).isEqualTo(TripType.TRANSFER);
        assertThat(responses.get(1).getTripTypeLabel()).isEqualTo("Dieu chuyen noi bo");
        assertThat(responses.get(1).getTransferId()).isEqualTo(500L);
        assertThat(responses.get(1).getSourceWarehouseCode()).isEqualTo("HP");
        assertThat(responses.get(1).getDestinationWarehouseCode()).isEqualTo("HN");
        assertThat(responses.get(1).getTransferLineCount()).isEqualTo(2);
        assertThat(responses.get(1).getDeliveryOrders()).isEmpty();
        verify(tripRepository).findAssignedDriverTrips(actor.getId());
        verify(tripRepository, never()).findAll();
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
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        service.confirmDelivery(50L, 70L, request, actor);

        assertThat(transit.getTotalQty()).isEqualByComparingTo("3");
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.COMPLETED);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.VERIFIED);
        verify(autoInvoiceService).createForConfirmedDelivery(order, actor);
        verify(inventoryRepository).save(transit);
    }

    @Test
    void confirmDelivery_allowsAutoInvoiceIdempotentReplayOnRetry() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456");
        DeliveryOrderItem item = item(BigDecimal.valueOf(2), BigDecimal.valueOf(50));
        Inventory transit = Inventory.builder().id(90L).totalQty(BigDecimal.valueOf(5))
                .reservedQty(BigDecimal.ZERO).costPrice(BigDecimal.TEN).build();
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(autoInvoiceService.createForConfirmedDelivery(order, actor)).thenReturn(autoInvoiceResult(true));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        service.confirmDelivery(50L, 70L, request, actor);

        verify(autoInvoiceService).createForConfirmedDelivery(order, actor);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.COMPLETED);
    }

    @Test
    void confirmDelivery_scopesAutoInvoiceToConfirmedDeliveryOrderOnly() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOrder sibling = DeliveryOrder.builder().id(71L).doNumber("DO-2").dealer(order.getDealer())
                .warehouse(warehouse).status(DeliveryOrderStatus.IN_TRANSIT).build();
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456");
        DeliveryOrderItem item = item(BigDecimal.ONE, BigDecimal.valueOf(10));
        Inventory transit = Inventory.builder().id(90L).totalQty(BigDecimal.valueOf(5))
                .reservedQty(BigDecimal.ZERO).costPrice(BigDecimal.TEN).build();
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        service.confirmDelivery(50L, 70L, request, actor);

        verify(autoInvoiceService).createForConfirmedDelivery(order, actor);
        verify(autoInvoiceService, never()).createForConfirmedDelivery(eq(sibling), any());
    }

    @Test
    void confirmDelivery_rollsBackStatusChangesWhenAutoInvoiceFails() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456");
        DeliveryOrderItem item = item(BigDecimal.ONE, BigDecimal.valueOf(10));
        Inventory transit = Inventory.builder().id(90L).totalQty(BigDecimal.valueOf(5))
                .reservedQty(BigDecimal.ZERO).costPrice(BigDecimal.TEN).build();
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(autoInvoiceService.createForConfirmedDelivery(order, actor))
                .thenThrow(new IllegalStateException("invoice persistence failed"));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        assertThatThrownBy(() -> service.confirmDelivery(50L, 70L, request, actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invoice persistence failed");

        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.IN_TRANSIT);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
        assertThat(otp.getStatus()).isEqualTo(DeliveryOtpStatus.ACTIVE);
        verify(deliveryOrderRepository, never()).save(any());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void confirmDelivery_doesNotCreatePaymentOrSendNotifications() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOtpAttempt otp = otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456");
        DeliveryOrderItem item = item(BigDecimal.ONE, BigDecimal.valueOf(10));
        Inventory transit = Inventory.builder().id(90L).totalQty(BigDecimal.valueOf(5))
                .reservedQty(BigDecimal.ZERO).costPrice(BigDecimal.TEN).build();
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L)).thenReturn(Optional.of(otp));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.of(transit));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        service.confirmDelivery(50L, 70L, request, actor);

        verify(autoInvoiceService, times(1)).createForConfirmedDelivery(order, actor);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void confirmDelivery_rejectsMissingTransitStockBeforeAutoInvoice() {
        delivery.setPodImageUrl("/uploads/pod/goods.jpg");
        delivery.setPodSignatureUrl("/uploads/pod/sign.jpg");
        DeliveryOrderItem item = item(BigDecimal.valueOf(2), BigDecimal.valueOf(50));
        stubCurrentAttempt();
        when(otpRepository.findByDeliveryId(80L))
                .thenReturn(Optional.of(otp(DeliveryOtpStatus.ACTIVE, OffsetDateTime.now().plusMinutes(2), 0, "123456")));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(70L)).thenReturn(List.of(item));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(100L, 200L)).thenReturn(Optional.empty());

        ConfirmDeliveryRequest request = new ConfirmDeliveryRequest();
        request.setOtp("123456");
        assertThatThrownBy(() -> service.confirmDelivery(50L, 70L, request, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("In-transit stock");
        verify(autoInvoiceService, never()).createForConfirmedDelivery(any(), any());
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

    private AutoInvoiceResult autoInvoiceResult(boolean idempotent) {
        return new AutoInvoiceResult(900L, "INV-1", order.getId(), order.getDealer().getId(),
                BigDecimal.valueOf(100), LocalDate.now(), LocalDate.now().plusDays(30),
                com.wms.enums.billing_payment.InvoiceStatus.UNPAID, idempotent, List.of());
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
