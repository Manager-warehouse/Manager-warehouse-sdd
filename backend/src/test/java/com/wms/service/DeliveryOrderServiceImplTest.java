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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.wms.dto.request.DeliveryOrderAllocationRequest;
import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.request.DeliveryOrderPickQcResultRequest;
import com.wms.dto.request.DeliveryOrderPickQcRowRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderQualityApprovalRequest;
import com.wms.dto.request.DeliveryOrderReplacementAllocationRequest;
import com.wms.dto.request.DeliveryOrderReplacementPlanRequest;
import com.wms.dto.request.DeliveryOrderReturnToBinRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectReturnRequest;
import com.wms.dto.request.ReturnedGoodsApprovalRequest;
import com.wms.dto.request.ReturnedGoodsCountQcItemRequest;
import com.wms.dto.request.ReturnedGoodsCountQcRequest;
import com.wms.dto.request.ReturnedGoodsPutawayCompleteRequest;
import com.wms.dto.request.ReturnedGoodsPutawayPlanItemRequest;
import com.wms.dto.request.ReturnedGoodsPutawayPlanRequest;
import com.wms.dto.request.ReturnedGoodsReceiveRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.dto.response.PickingCandidateResponse;
import com.wms.dto.response.ReturnedGoodsFlowResponse;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
import com.wms.entity.order_fulfillment.DeliveryOrderItemReplacement;
import com.wms.entity.order_fulfillment.OutboundQcRecord;
import com.wms.entity.order_fulfillment.ReturnedDeliveryFlow;
import com.wms.entity.order_fulfillment.ReturnedDeliveryFlowItem;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.QuarantineRecord;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.entity.stock_control.WarehouseProductReservation;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DeliveryOrderItemAllocationRepository;
import com.wms.repository.DeliveryOrderItemReplacementRepository;
import com.wms.repository.DeliveryOrderWarehouseApprovalRepository;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderType;
import com.wms.enums.order_fulfillment.ReturnedDeliveryFlowStatus;
import com.wms.enums.order_fulfillment.ReturnedGoodsQcDecision;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.OutboundDeliveryException;
import com.wms.mapper.DeliveryOrderMapper;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderItemReturnToBinRecordRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.OutboundQcRecordRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.ReturnedDeliveryFlowRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.stock_receiving.QuarantineRecordRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseProductReservationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.price_management.PriceHistoryService;
import com.wms.service.order_fulfillment.impl.DeliveryOrderServiceImpl;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceImplTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private DeliveryOrderItemAllocationRepository allocationRepository;
    @Mock private DeliveryOrderItemReturnToBinRecordRepository returnToBinRecordRepository;
    @Mock private DeliveryOrderItemReplacementRepository replacementRepository;
    @Mock private DeliveryOrderWarehouseApprovalRepository deliveryOrderWarehouseApprovalRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private OutboundQcRecordRepository outboundQcRecordRepository;
    @Mock private ReturnedDeliveryFlowRepository returnedDeliveryFlowRepository;
    @Mock private QuarantineRecordRepository quarantineRecordRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private WarehouseProductReservationRepository reservationRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private PartnerEligibilityService partnerEligibilityService;
    @Mock private PriceHistoryService priceHistoryService;
    @Mock private PartnerAuditUtil auditUtil;
    @Mock private EntityManager entityManager;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private SystemConfigService systemConfigService;

    private DeliveryOrderServiceImpl service;
    private User planner;
    private User manager;
    private Dealer dealer;
    private Warehouse warehouse;
    private Product product;
    private PriceHistory price;
    private WarehouseProductReservation reservation;
    private User storekeeper;
    private User warehouseStaff;
    private Batch batch;
    private WarehouseLocation zone;
    private WarehouseLocation bin;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        service = new DeliveryOrderServiceImpl(deliveryOrderRepository, deliveryOrderItemRepository,
                allocationRepository, returnToBinRecordRepository, replacementRepository,
                deliveryOrderWarehouseApprovalRepository,
                dealerRepository, warehouseRepository, productRepository, vehicleRepository, inventoryRepository,
                invoiceRepository, outboundQcRecordRepository, returnedDeliveryFlowRepository,
                quarantineRecordRepository, adjustmentRepository,
                priceHistoryRepository, reservationRepository, assignmentRepository,
                partnerEligibilityService, new DeliveryOrderMapper(), auditUtil, entityManager,
                priceHistoryService, accountingPeriodService, systemConfigService);
        lenient().when(accountingPeriodService.resolveOpenPeriod(any()))
                .thenReturn(AccountingPeriod.builder().id(1L).periodName("2026-06").build());
        planner = user(1L, UserRole.PLANNER);
        manager = user(2L, UserRole.WAREHOUSE_MANAGER);
        dealer = dealer(10L, new BigDecimal("480.00"), new BigDecimal("500.00"), CreditStatus.ACTIVE);
        warehouse = warehouse(20L, "HP");
        product = product(30L);
        price = price(product, new BigDecimal("2.00"));
        reservation = reservation(warehouse, product, new BigDecimal("5.00"));
        storekeeper = user(3L, UserRole.STOREKEEPER);
        warehouseStaff = user(4L, UserRole.WAREHOUSE_STAFF);
        zone = zone(31L, warehouse);
        bin = bin(801L, warehouse, zone);
        batch = batch(71L, product, warehouse);
        inventory = inventory(501L, warehouse, product, batch, bin, new BigDecimal("15.00"), ZERO);
        lenient().when(vehicleRepository.findByWarehouseIdAndIsActiveTrue(20L))
                .thenReturn(List.of(vehicle(91L, warehouse, VehicleStatus.AVAILABLE, new BigDecimal("100000.00"))));
    }

    @Test
    void createDeliveryOrder_rejectsWeightAboveCombinedActiveFleetCapacityWithoutMutation() {
        stubCreateUntilCredit();
        product.setWeightKg(new BigDecimal("10.000"));
        when(vehicleRepository.findByWarehouseIdAndIsActiveTrue(20L)).thenReturn(List.of(
                vehicle(91L, warehouse, VehicleStatus.AVAILABLE, new BigDecimal("40.00")),
                vehicle(92L, warehouse, VehicleStatus.ON_TRIP, new BigDecimal("30.00")),
                vehicle(93L, warehouse, VehicleStatus.MAINTENANCE, new BigDecimal("29.99"))));

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .satisfies(ex -> {
                    OutboundDeliveryException outbound = (OutboundDeliveryException) ex;
                    assertThat(outbound.getCode()).isEqualTo("DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY");
                    assertThat(outbound.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(outbound.getMessage()).isEqualTo(
                            "Tải trọng quá lớn để giao trong 1 lần, vui lòng chia nhỏ đơn thành nhiều phiếu xuất kho để có thể giao hàng.");
                });
        verify(deliveryOrderRepository, never()).saveAndFlush(any());
        verify(reservationRepository, never()).save(any());
        verify(auditUtil, never()).logChange(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createDeliveryOrder_allowsWeightEqualToCombinedFleetCapacityAcrossAllStatuses() {
        stubSuccessfulCreate(new BigDecimal("100.00"));
        product.setWeightKg(new BigDecimal("10.000"));
        when(vehicleRepository.findByWarehouseIdAndIsActiveTrue(20L)).thenReturn(List.of(
                vehicle(91L, warehouse, VehicleStatus.AVAILABLE, new BigDecimal("40.00")),
                vehicle(92L, warehouse, VehicleStatus.ON_TRIP, new BigDecimal("30.00")),
                vehicle(93L, warehouse, VehicleStatus.MAINTENANCE, new BigDecimal("30.00"))));

        DeliveryOrderResponse response = service.createDeliveryOrder(
                validRequest(new BigDecimal("10.00")), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        verify(vehicleRepository).findByWarehouseIdAndIsActiveTrue(20L);
    }

    @Test
    void createDeliveryOrder_rejectsProductWithoutPositiveWeight() {
        stubCreateUntilCredit();
        product.setWeightKg(null);

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .satisfies(ex -> {
                    OutboundDeliveryException outbound = (OutboundDeliveryException) ex;
                    assertThat(outbound.getCode()).isEqualTo("PRODUCT_WEIGHT_MISSING");
                    assertThat(outbound.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
        verify(deliveryOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void plannerUpdateDeliveryOrder_rejectsWeightAboveFleetCapacityWithoutChangingReservations() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem oldItem = item(order, product, new BigDecimal("5.00"));
        product.setWeightKg(new BigDecimal("10.000"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(oldItem));
        when(productRepository.findByIdAndIsActiveTrue(30L)).thenReturn(Optional.of(product));
        when(priceHistoryService.lookupApproved(eq(30L), eq(20L), any(LocalDate.class)))
                .thenReturn(Optional.of(price));
        when(vehicleRepository.findByWarehouseIdAndIsActiveTrue(20L)).thenReturn(List.of(
                vehicle(91L, warehouse, VehicleStatus.ON_TRIP, new BigDecimal("79.99"))));

        assertThatThrownBy(() -> service.updateDeliveryOrder(100L, updateRequest(), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY");
        verify(deliveryOrderItemRepository, never()).deleteAll(any());
        verify(reservationRepository, never()).save(any());
        verify(deliveryOrderRepository, never()).save(any());
        verify(auditUtil, never()).logChange(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createDeliveryOrder_allowsCreditLimitEquality() {
        stubSuccessfulCreate(new BigDecimal("100.00"));

        DeliveryOrderResponse response = service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getReservedQty()).isEqualByComparingTo("10.00");
        verify(deliveryOrderRepository).saveAndFlush(any(DeliveryOrder.class));
    }

    @Test
    void getAllDeliveryOrders_filtersToAssignedWarehouseForStorekeeper() {
        DeliveryOrder hpOrder = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrder hcmOrder = order(101L, DeliveryOrderStatus.NEW);
        Warehouse hcmWarehouse = warehouse(30L, "HCM");
        hcmOrder.setWarehouse(hcmWarehouse);
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderRepository.findDetailedByWarehouseIdIn(List.of(20L)))
                .thenReturn(List.of(hpOrder));

        List<DeliveryOrderResponse> responses = service.getAllDeliveryOrders(storekeeper);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getWarehouseId()).isEqualTo(20L);
    }

    @Test
    void getDeliveryOrderById_rejectsStorekeeperOutsideWarehouseScope() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> service.getDeliveryOrderById(100L, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void getDeliveryOrderById_includesAllocationQcSummary() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);
        WarehouseLocation stagingBin = bin(880L, warehouse, zone);
        stagingBin.setIsStaging(true);
        OutboundQcRecord qcRecord = new OutboundQcRecord();
        qcRecord.setDeliveryOrder(order);
        qcRecord.setDeliveryOrderItem(item);
        qcRecord.setAllocation(allocation);
        qcRecord.setQcPassQty(new BigDecimal("8.00"));
        qcRecord.setQcFailQty(new BigDecimal("2.00"));
        qcRecord.setStagingLocation(stagingBin);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of(qcRecord));

        DeliveryOrderResponse response = service.getDeliveryOrderById(100L, storekeeper);

        assertThat(response.getItems().get(0).getAllocations().get(0).getQcPassQty()).isEqualByComparingTo("8.00");
        assertThat(response.getItems().get(0).getAllocations().get(0).getQcFailQty()).isEqualByComparingTo("2.00");
        assertThat(response.getItems().get(0).getAllocations().get(0).getStagingLocationId()).isEqualTo(880L);
        assertThat(response.getItems().get(0).getAllocations().get(0).isQcCompleted()).isTrue();
    }

    @Test
    void saveDeliveryOrderPickingPlan_autoBuildsFifoAllocationsWhenRequestIsEmpty() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        reservation.setReservedQty(new BigDecimal("10.00"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(WarehouseProductReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation saved = invocation.getArgument(0);
            saved.setId(900L);
            return saved;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(List.of());
        request.setReturnToBinRecords(List.of());

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAllocations()).hasSize(1);
        assertThat(response.getItems().get(0).getAllocations().get(0).getLocationId()).isEqualTo(801L);
        assertThat(response.getItems().get(0).getAllocations().get(0).getZoneId()).isEqualTo(31L);
    }

    @Test
    void createDeliveryOrder_incrementsWarehouseProductReservation() {
        stubSuccessfulCreate(new BigDecimal("100.00"));

        service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner);

        assertThat(reservation.getReservedQty()).isEqualByComparingTo("15.00");
        verify(reservationRepository).save(reservation);
    }

    @Test
    void createDeliveryOrder_allowsStockAvailabilityEquality() {
        stubSuccessfulCreate(new BigDecimal("15.00"));

        DeliveryOrderResponse response = service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        verify(deliveryOrderRepository).saveAndFlush(any(DeliveryOrder.class));
    }

    @Test
    void createDeliveryOrder_rejectsCreditHoldDealer() {
        dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);
        stubCreateUntilCredit();

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("CREDIT_HOLD");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsOverdueInvoice() {
        dealer.setPaymentTermDays(15);
        stubCreateUntilCredit();
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), eq(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID)), any(LocalDate.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("CREDIT_HOLD");
        ArgumentCaptor<LocalDate> thresholdCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(invoiceRepository).existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), eq(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID)), thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(15));
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsPlannerOutsideWarehouseScope() {
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsInTransitWarehouse() {
        warehouse.setType(WarehouseType.IN_TRANSIT);
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_TYPE_INVALID");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsPastExpectedDeliveryDate() {
        DeliveryOrderCreateRequest request = validRequest(new BigDecimal("10.00"));
        request.setDocumentDate(LocalDate.now().minusDays(2));
        request.setExpectedDeliveryDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.createDeliveryOrder(request, planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVALID_DELIVERY_DATE");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsNonSaleType() {
        DeliveryOrderCreateRequest request = validRequest(new BigDecimal("10.00"));
        request.setType(DeliveryOrderType.ADJUSTMENT);

        assertThatThrownBy(() -> service.createDeliveryOrder(request, planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_TYPE_INVALID");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_subtractsAggregateReservationFromAvailability() {
        stubCreateUntilAvailability(new BigDecimal("12.00"), new BigDecimal("5.00"));

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .satisfies(ex -> {
                    OutboundDeliveryException outbound = (OutboundDeliveryException) ex;
                    assertThat(outbound.getCode()).isEqualTo("INSUFFICIENT_STOCK");
                    assertThat(outbound.getDetails()).containsKey("availableByProduct");
                    assertThat(outbound.getDetails()).doesNotContainKey("suggestedWarehouses");
                });
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsWhenValidInventoryAvailabilityIsInsufficient() {
        stubCreateUntilAvailability(new BigDecimal("9.00"), BigDecimal.ZERO);

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .satisfies(ex -> {
                    OutboundDeliveryException outbound = (OutboundDeliveryException) ex;
                    assertThat(outbound.getCode()).isEqualTo("INSUFFICIENT_STOCK");
                    assertThat(outbound.getDetails()).containsKey("availableByProduct");
                    assertThat(outbound.getDetails()).doesNotContainKey("suggestedWarehouses");
                });
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_translatesDuplicateDoNumberConflict() {
        stubCreateUntilAvailability(new BigDecimal("15.00"), BigDecimal.ZERO);
        when(deliveryOrderRepository.existsByDoNumber("DO-" + LocalDate.now().toString().replace("-", "") + "-0001"))
                .thenReturn(false);
        when(deliveryOrderRepository.saveAndFlush(any(DeliveryOrder.class)))
                .thenThrow(new DataIntegrityViolationException("delivery_orders_do_number_key"));

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("1.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .satisfies(ex -> {
                    OutboundDeliveryException outbound = (OutboundDeliveryException) ex;
                    assertThat(outbound.getCode()).isEqualTo("DELIVERY_ORDER_NUMBER_CONFLICT");
                    assertThat(outbound.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void plannerUpdateDeliveryOrder_replacesItemsAndAppliesReservationDeltaWhenNew() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem oldItem = item(order, product, new BigDecimal("5.00"));
        reservation.setReservedQty(new BigDecimal("5.00"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(oldItem));
        when(productRepository.findByIdAndIsActiveTrue(30L)).thenReturn(Optional.of(product));
        when(priceHistoryService.lookupApproved(eq(30L), eq(20L), any(LocalDate.class)))
                .thenReturn(Optional.of(price));
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), eq(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID)), any(LocalDate.class)))
                .thenReturn(false);
        when(inventoryRepository.sumValidAvailableQty(20L, 30L)).thenReturn(new BigDecimal("20.00"));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductId(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(WarehouseProductReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.save(any(DeliveryOrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.updateDeliveryOrder(100L, updateRequest(), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getRequestedQty()).isEqualByComparingTo("8.00");
        assertThat(reservation.getReservedQty()).isEqualByComparingTo("8.00");
        assertThat(order.getNotes()).isEqualTo("Updated before picking plan");
        verify(deliveryOrderItemRepository).deleteAll(List.of(oldItem));
        verify(auditUtil).logChange(eq(planner), eq(AuditAction.DELIVERY_ORDER_UPDATE), eq("DELIVERY_ORDER"),
                eq(100L), eq("DO-1"), any(), any());
    }

    @Test
    void cancelDeliveryOrder_releasesPlannerReservation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        reservation.setReservedQty(new BigDecimal("10.00"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.save(any(DeliveryOrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.cancelDeliveryOrder(100L, cancelRequest(), manager);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.CANCELLED);
        assertThat(item.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(reservation.getReservedQty()).isEqualByComparingTo("0.00");
    }

    @Test
    void plannerCancelDeliveryOrder_releasesReservationWhenNew() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        reservation.setReservedQty(new BigDecimal("10.00"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.save(any(DeliveryOrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.cancelDeliveryOrder(100L, cancelRequest(), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.CANCELLED);
        assertThat(item.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(reservation.getReservedQty()).isEqualByComparingTo("0.00");
    }

    @Test
    void cancelDeliveryOrder_rejectsWarehouseApproved() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAREHOUSE_APPROVED);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        assertThatThrownBy(() -> service.cancelDeliveryOrder(100L, cancelRequest(), manager))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_CANCEL_FORBIDDEN");
    }

    @Test
    void plannerCancelDeliveryOrder_rejectsAfterPickingPlanStarts() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelDeliveryOrder(100L, cancelRequest(), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_CANCEL_FORBIDDEN");
    }

    @Test
    void plannerCancelDeliveryOrder_rejectsConcreteAllocationWithoutInventoryMutation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));

        assertThatThrownBy(() -> service.cancelDeliveryOrder(100L, cancelRequest(), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_CANCEL_FORBIDDEN");
        verify(inventoryRepository, never()).save(any());
        verify(allocationRepository, never()).saveAll(any());
    }

    @Test
    void cancelDeliveryOrder_rejectsUnauthorizedRole() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelDeliveryOrder(100L, cancelRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void updateDeliveryOrder_rejectsAfterPickingPlanStarts() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));

        assertThatThrownBy(() -> service.updateDeliveryOrder(100L, updateRequest(), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_UPDATE_FORBIDDEN");
    }

    @Test
    void plannerUpdateDeliveryOrder_rejectsConcreteAllocationWithoutInventoryMutation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));

        assertThatThrownBy(() -> service.updateDeliveryOrder(100L, updateRequest(), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_UPDATE_FORBIDDEN");
        verify(inventoryRepository, never()).save(any());
        verify(allocationRepository, never()).saveAll(any());
    }

    @Test
    void updateDeliveryOrder_rejectsUnauthorizedRole() {
        assertThatThrownBy(() -> service.updateDeliveryOrder(100L, updateRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void saveDeliveryOrderPickingPlan_transfersReservationAndMovesStatus() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);
        reservation.setReservedQty(new BigDecimal("10.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(WarehouseProductReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation allocation = invocation.getArgument(0);
            allocation.setId(900L);
            return allocation;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, pickingPlanRequest(), storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(reservation.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("10.00");
        assertThat(item.getPlannedQty()).isEqualByComparingTo("10.00");
        verify(allocationRepository).save(any(DeliveryOrderItemAllocation.class));
    }

    @Test
    void saveDeliveryOrderPickingPlan_rejectsIncompleteItemTotals() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));

        DeliveryOrderPickingPlanRequest request = pickingPlanRequest();
        request.getAllocations().get(0).setPlannedQty(new BigDecimal("8.00"));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICKING_PLAN_QTY_MISMATCH");
    }

    @Test
    void saveDeliveryOrderPickingPlan_rejectsStorekeeperOutsideWarehouseScope() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, pickingPlanRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void saveDeliveryOrderPickingPlan_acceptsNullZoneIdForMultiLevelStructure() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);
        reservation.setReservedQty(new BigDecimal("10.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(WarehouseProductReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation allocation = invocation.getArgument(0);
            allocation.setId(900L);
            return allocation;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, null, new BigDecimal("10.00"))))); // zoneId is null

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(reservation.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("10.00");
        assertThat(item.getPlannedQty()).isEqualByComparingTo("10.00");
        verify(allocationRepository).save(any(DeliveryOrderItemAllocation.class));
    }

    @Test
    void saveDeliveryOrderPickingPlan_allowsSelectingNewerBatchWhileOlderStockExists() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);
        reservation.setReservedQty(new BigDecimal("10.00"));
        Batch newerBatch = batch(72L, product, warehouse);
        WarehouseLocation newerBin = bin(802L, warehouse, zone);
        Inventory newerInventory = inventory(502L, warehouse, product, newerBatch, newerBin,
                new BigDecimal("20.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(newerInventory));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(WarehouseProductReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation allocation = invocation.getArgument(0);
            allocation.setId(901L);
            return allocation;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(List.of(
                allocationRequest(200L, 502L, 72L, 802L, 31L, new BigDecimal("10.00"))));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(newerInventory.getReservedQty()).isEqualByComparingTo("10.00");
        verify(inventoryRepository, never()).findFifoRowsForPlanning(20L, 30L);
    }

    @Test
    void saveDeliveryOrderPickingPlan_rejectsStagingZoneAsSource() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        zone.setIsStaging(true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("10.00")))));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_ROW_INVALID");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void getPickingCandidates_allowsQcPendingApprovalForReplacementPlanning() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        WarehouseLocation replacementZone = zone(32L, warehouse);
        WarehouseLocation replacementBin = bin(802L, warehouse, replacementZone);
        Batch replacementBatch = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, replacementBatch, replacementBin,
                new BigDecimal("8.00"), new BigDecimal("1.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L)).thenReturn(List.of(replacementInventory));

        Map<Long, List<PickingCandidateResponse>> response = service.getPickingCandidates(100L, storekeeper);

        assertThat(response).containsKey(200L);
        assertThat(response.get(200L)).hasSize(1);
        assertThat(response.get(200L).get(0).getInventoryId()).isEqualTo(502L);
        assertThat(response.get(200L).get(0).getAvailableQty()).isEqualByComparingTo("7.00");
    }

    @Test
    void getPickingCandidates_ordersOlderReceivedBatchBeforeNewerBatch() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        Batch olderBatch = batch(71L, product, warehouse);
        olderBatch.setReceivedDate(LocalDate.of(2026, 5, 1));
        Batch newerBatch = batch(72L, product, warehouse);
        newerBatch.setReceivedDate(LocalDate.of(2026, 6, 1));
        WarehouseLocation newerBin = bin(802L, warehouse, zone);
        Inventory olderInventory = inventory(501L, warehouse, product, olderBatch, bin,
                new BigDecimal("10.00"), ZERO);
        Inventory newerInventory = inventory(502L, warehouse, product, newerBatch, newerBin,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L))
                .thenReturn(List.of(newerInventory, olderInventory));

        Map<Long, List<PickingCandidateResponse>> response = service.getPickingCandidates(100L, storekeeper);

        assertThat(response.get(200L))
                .extracting(PickingCandidateResponse::getInventoryId)
                .containsExactly(501L, 502L);
    }

    @Test
    void getPickingCandidates_doesNotAddCurrentPickedReservationForReplacementPlanning() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        Inventory pickedSource = inventory(501L, warehouse, product, batch, bin, ZERO, ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L)).thenReturn(List.of(pickedSource));

        Map<Long, List<PickingCandidateResponse>> response = service.getPickingCandidates(100L, storekeeper);

        assertThat(response.get(200L)).isEmpty();
    }

    @Test
    void getPickingCandidates_filtersRowsConsumedByPlannerReservationForReplacementPlanning() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory firstFifoRow = inventory(501L, warehouse, product, batch, bin, new BigDecimal("3.00"), ZERO);
        Inventory secondFifoRow = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("8.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L))
                .thenReturn(List.of(firstFifoRow, secondFifoRow));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductId(20L, 30L))
                .thenReturn(Optional.of(reservation(warehouse, product, new BigDecimal("5.00"))));

        Map<Long, List<PickingCandidateResponse>> response = service.getPickingCandidates(100L, storekeeper);

        assertThat(response.get(200L))
                .extracting(PickingCandidateResponse::getInventoryId)
                .containsExactly(502L);
        assertThat(response.get(200L).get(0).getAvailableQty()).isEqualByComparingTo("6.00");
    }

    @Test
    void getPickingCandidates_addsCurrentUnpickedReservationWhenRevisingPickingPlan() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        inventory.setTotalQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(inventoryRepository.findValidFifoCandidates(20L, 30L, 100L)).thenReturn(List.of(inventory));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));

        Map<Long, List<PickingCandidateResponse>> response = service.getPickingCandidates(100L, storekeeper);

        assertThat(response.get(200L).get(0).getAvailableQty()).isEqualByComparingTo("10.00");
    }

    @Test
    void saveDeliveryOrderPickingPlan_revisesConcreteReservationsByDelta() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory inventory2 = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("12.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));
        when(inventoryRepository.findByIdInWithLock(List.of(501L, 502L))).thenReturn(List.of(inventory, inventory2));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation allocation = invocation.getArgument(0);
            if (allocation.getId() == null) {
                allocation.setId(901L);
            }
            return allocation;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("6.00")),
                allocationRequest(200L, 502L, 72L, 802L, 32L, new BigDecimal("4.00")))));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("6.00");
        assertThat(inventory2.getReservedQty()).isEqualByComparingTo("4.00");
        assertThat(item.getPlannedQty()).isEqualByComparingTo("10.00");
        assertThat(response.getItems().get(0).getAllocations()).hasSize(2);
    }

    @Test
    void saveDeliveryOrderPickingPlan_requiresReturnRecordForChangedPickedAllocation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("3.00"), false);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory inventory2 = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("8.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));
        when(inventoryRepository.findByIdInWithLock(List.of(501L, 502L))).thenReturn(List.of(inventory, inventory2));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("2.00")),
                allocationRequest(200L, 502L, 72L, 802L, 32L, new BigDecimal("8.00")))));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICKED_GOODS_RETURN_REQUIRED");
    }

    @Test
    void saveDeliveryOrderPickingPlan_requiresReturnRecordWhenQcRecordExists() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("3.00"), false);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory inventory2 = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("8.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));
        when(inventoryRepository.findByIdInWithLock(List.of(501L, 502L))).thenReturn(List.of(inventory, inventory2));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("2.00")),
                allocationRequest(200L, 502L, 72L, 802L, 32L, new BigDecimal("8.00")))));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICKED_GOODS_RETURN_REQUIRED");
    }

    @Test
    void saveDeliveryOrderPickingPlan_allowsUnchangedPickedAllocationWithoutReturnRecord() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("3.00"), false);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));
        when(inventoryRepository.findByIdInWithLock(List.of(501L))).thenReturn(List.of(inventory));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("10.00")))));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickingPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        verify(returnToBinRecordRepository, never()).save(any());
    }

    @Test
    void saveDeliveryOrderPickingPlan_rejectsWrongReturnSourceLocation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        item.setPickedQty(new BigDecimal("4.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation existingAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("4.00"), false);

        WarehouseLocation sourceBin = bin(880L, warehouse, zone);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory inventory2 = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("8.00"), ZERO);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(existingAllocation));
        when(inventoryRepository.findByIdInWithLock(List.of(501L, 502L))).thenReturn(List.of(inventory, inventory2));
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(sourceBin);
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 880L))
                .thenReturn(Optional.empty());

        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("2.00")),
                allocationRequest(200L, 502L, 72L, 802L, 32L, new BigDecimal("8.00")))));
        request.setReturnToBinRecords(List.of(returnToBinRequest(900L, new BigDecimal("2.00"), 880L)));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickingPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_ROW_INVALID");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_requiresQcPendingApprovalStatus() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DELIVERY_ORDER_STATUS_INVALID");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_rejectsQuantityBeyondUnresolvedQcFail() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(new BigDecimal("1.00"));

        DeliveryOrderReplacementPlanRequest request = replacementPlanRequest();
        request.getReplacements().get(0).setQuantity(new BigDecimal("2.00"));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("QC_REPLACEMENT_REQUIRED");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_rejectsQuantityConsumedByPlannerReservation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("7.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("3.00"), false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory firstFifoRow = inventory(503L, warehouse, product, batch, bin, new BigDecimal("3.00"), ZERO);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("8.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("7.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(inventoryRepository.findFifoRowsForPlanning(20L, 30L))
                .thenReturn(List.of(firstFifoRow, replacementInventory));
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductId(20L, 30L))
                .thenReturn(Optional.of(reservation(warehouse, product, new BigDecimal("5.00"))));

        DeliveryOrderReplacementPlanRequest request = replacementPlanRequest();
        request.getReplacements().get(0).setQuantity(new BigDecimal("7.00"));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_ROW_INVALID");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_rejectsStagingZoneAsSource() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);

        WarehouseLocation stagingZone = zone(32L, warehouse);
        stagingZone.setIsStaging(true);
        WarehouseLocation stagingBin = bin(802L, warehouse, stagingZone);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, stagingBin,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_ROW_INVALID");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void saveDeliveryOrderReplacementPlan_translatesOptimisticLockConflict() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(ZERO);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Inventory.class, 502L));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_VERSION_CONFLICT");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_translatesInventoryRowConflict() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(ZERO);
        when(inventoryRepository.save(any(Inventory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate inventory row"));

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INVENTORY_ROW_CONFLICT");
    }

    @Test
    void saveDeliveryOrderReplacementPlan_reservesReplacementInventoryAndMovesStatus() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        item.setPickedQty(new BigDecimal("8.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(ZERO);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(replacementRepository.save(any(DeliveryOrderItemReplacement.class))).thenAnswer(invocation -> {
            DeliveryOrderItemReplacement replacement = invocation.getArgument(0);
            replacement.setId(700L);
            return replacement;
        });
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> {
            DeliveryOrderItemAllocation allocation = invocation.getArgument(0);
            if (allocation.getId() == null) {
                allocation.setId(901L);
            }
            return allocation;
        });
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.getReference(Inventory.class, 501L)).thenReturn(inventory);
        when(entityManager.getReference(Batch.class, 71L)).thenReturn(batch);
        when(entityManager.getReference(WarehouseLocation.class, 801L)).thenReturn(bin);
        when(entityManager.getReference(Batch.class, 72L)).thenReturn(batch2);
        when(entityManager.getReference(WarehouseLocation.class, 802L)).thenReturn(bin2);

        DeliveryOrderResponse response = service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(replacementInventory.getReservedQty()).isEqualByComparingTo("2.00");
        assertThat(response.getItems().get(0).getAllocations()).hasSize(2);
        assertThat(response.getItems().get(0).getAllocations().get(1).isReplacement()).isTrue();
    }

    @Test
    void saveDeliveryOrderReplacementPlan_allowsReplacementBinWithoutZone() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);
        WarehouseLocation binWithoutZone = bin(802L, warehouse, null);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, binWithoutZone,
                new BigDecimal("10.00"), ZERO);
        DeliveryOrderReplacementPlanRequest request = replacementPlanRequest();
        request.getReplacements().get(0).setReplacementZoneId(null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(ZERO);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(replacementRepository.save(any(DeliveryOrderItemReplacement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.getReference(Inventory.class, 501L)).thenReturn(inventory);
        when(entityManager.getReference(Batch.class, 71L)).thenReturn(batch);
        when(entityManager.getReference(WarehouseLocation.class, 801L)).thenReturn(bin);
        when(entityManager.getReference(Batch.class, 72L)).thenReturn(batch2);
        when(entityManager.getReference(WarehouseLocation.class, 802L)).thenReturn(binWithoutZone);

        DeliveryOrderResponse response = service.saveDeliveryOrderReplacementPlan(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(replacementInventory.getReservedQty()).isEqualByComparingTo("2.00");
        assertThat(response.getItems().get(0).getAllocations().get(1).getZoneId()).isEqualTo(802L);
    }

    @Test
    void saveDeliveryOrderReplacementPlan_rejectsInvalidFailedSource() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation failedAllocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("8.00"), false);

        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(failedAllocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L)))
                .thenReturn(List.of(failedQcRecord(failedAllocation, new BigDecimal("2.00"))));
        when(inventoryRepository.findByIdInWithLock(List.of(502L))).thenReturn(List.of(replacementInventory));
        when(replacementRepository.sumReplacementQtyByDeliveryOrderItemId(200L)).thenReturn(ZERO);

        DeliveryOrderReplacementPlanRequest request = replacementPlanRequest();
        request.getReplacements().get(0).setFailedInventoryId(999L);

        assertThatThrownBy(() -> service.saveDeliveryOrderReplacementPlan(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("QC_FAILED_ALLOCATION_INVALID");
    }

    @Test
    void saveDeliveryOrderPickQcResult_recordsResultWithoutMovingInventory() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        inventory.setCostPrice(new BigDecimal("1.50"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        WarehouseLocation stagingZone = zone(92L, warehouse);
        stagingZone.setIsStaging(true);
        WarehouseLocation stagingBin = bin(880L, warehouse, stagingZone);
        WarehouseLocation quarantineZone = zone(91L, warehouse);
        quarantineZone.setIsQuarantine(true);
        WarehouseLocation quarantineBin = bin(990L, warehouse, quarantineZone);
        product.setVolumeM3(new BigDecimal("0.50"));
        inventory.getLocation().setCurrentWeightKg(new BigDecimal("15.00"));
        inventory.getLocation().setCurrentVolumeM3(new BigDecimal("7.50"));
        stagingBin.setCurrentWeightKg(ZERO);
        stagingBin.setCurrentVolumeM3(ZERO);
        quarantineBin.setCurrentWeightKg(ZERO);
        quarantineBin.setCurrentVolumeM3(ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of());
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(entityManager.find(WarehouseLocation.class, 990L)).thenReturn(quarantineBin);
        when(outboundQcRecordRepository.save(any(OutboundQcRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.QC_PENDING_APPROVAL);
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("15.00");
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("10.00");
        assertThat(item.getQcPassQty()).isEqualByComparingTo("8.00");
        assertThat(item.getQcFailQty()).isEqualByComparingTo("2.00");
        assertThat(inventory.getLocation().getCurrentWeightKg()).isEqualByComparingTo("15.00");
        assertThat(stagingBin.getCurrentWeightKg()).isZero();
        assertThat(quarantineBin.getCurrentWeightKg()).isZero();
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(quarantineRecordRepository, never()).save(any(QuarantineRecord.class));
        verify(adjustmentRepository, never()).save(any(Adjustment.class));
        ArgumentCaptor<OutboundQcRecord> qcCaptor = ArgumentCaptor.forClass(OutboundQcRecord.class);
        verify(outboundQcRecordRepository).save(qcCaptor.capture());
        assertThat(qcCaptor.getValue().getInventoryMovedAt()).isNull();
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsCumulativePassAboveRequestedQuantity() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("9.50"));
        item.setPlannedQty(BigDecimal.ONE);
        inventory.setReservedQty(BigDecimal.ONE);
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                BigDecimal.ONE, ZERO, true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of());

        DeliveryOrderPickQcResultRequest request = pickQcResultRequest();
        DeliveryOrderPickQcRowRequest row = request.getResults().get(0);
        row.setPickedQty(BigDecimal.ONE);
        row.setQcPassQty(BigDecimal.ONE);
        row.setQcFailQty(ZERO);

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, request, warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICK_QC_RESULT_INVALID");
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsInactiveQuarantineZone() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        WarehouseLocation stagingZone = zone(92L, warehouse);
        stagingZone.setIsStaging(true);
        WarehouseLocation stagingBin = bin(880L, warehouse, stagingZone);
        WarehouseLocation quarantineZone = zone(91L, warehouse);
        quarantineZone.setIsQuarantine(true);
        quarantineZone.setIsActive(false);
        WarehouseLocation quarantineBin = bin(990L, warehouse, quarantineZone);

        stubPickQcFlow(order, item, allocation, stagingBin, quarantineBin);

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("QUARANTINE_LOCATION_INACTIVE");
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsInvalidPickedPassFailBalance() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of());

        DeliveryOrderPickQcResultRequest request = pickQcResultRequest();
        request.getResults().get(0).setQcPassQty(new BigDecimal("7.00"));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, request, warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICK_QC_RESULT_INVALID");
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsPartialSubmission() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation1 = allocation(900L, item, inventory, zone,
                new BigDecimal("6.00"), ZERO, false);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory inventory2 = inventory(502L, warehouse, product, batch2, bin2, new BigDecimal("4.00"), new BigDecimal("4.00"));
        DeliveryOrderItemAllocation allocation2 = allocation(901L, item, inventory2, zone2,
                new BigDecimal("4.00"), ZERO, false);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation1, allocation2));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L, 901L))).thenReturn(List.of());

        DeliveryOrderPickQcResultRequest request = pickQcResultRequest();
        request.getResults().get(0).setPickedQty(new BigDecimal("6.00"));
        request.getResults().get(0).setQcPassQty(new BigDecimal("6.00"));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, request, warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICK_QC_RESULT_INVALID");
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsWarehouseStaffOutsideWarehouseScope() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
    }

    @Test
    void saveDeliveryOrderPickQcResult_replaysSameIdempotencyKeyAfterSuccess() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("8.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);
        OutboundQcRecord replay = new OutboundQcRecord();
        replay.setId(1L);
        replay.setDeliveryOrder(order);
        replay.setAllocation(allocation);
        replay.setRequestHash("34720e7b54a5d56ce02674741efa44fbb0c032aee2f4447ff9bb8fc31c0a04ef");

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100"))
                .thenReturn(List.of(replay));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.QC_PENDING_APPROVAL);
    }

    @Test
    void saveDeliveryOrderPickQcResult_blocksDuplicateAllocationWithoutReplay() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        OutboundQcRecord existing = new OutboundQcRecord();
        existing.setAllocation(allocation);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("QC_RESULT_ALREADY_RECORDED");
    }

    @Test
    void saveDeliveryOrderPickQcResult_rejectsReusedIdempotencyKeyWithDifferentPayload() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        OutboundQcRecord replay = new OutboundQcRecord();
        replay.setRequestHash("different");

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100"))
                .thenReturn(List.of(replay));

        assertThatThrownBy(() -> service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
    }

    @Test
    void saveDeliveryOrderPickQcResult_allowsReplacementCycleWithOnlyNewAllocations() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setRequestedQty(new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("8.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        item.setPlannedQty(new BigDecimal("2.00"));

        DeliveryOrderItemAllocation oldPassed = allocation(900L, item, inventory, zone,
                new BigDecimal("8.00"), new BigDecimal("8.00"), false);
        WarehouseLocation zone2 = zone(32L, warehouse);
        WarehouseLocation bin2 = bin(802L, warehouse, zone2);
        Batch batch2 = batch(72L, product, warehouse);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch2, bin2,
                new BigDecimal("2.00"), new BigDecimal("2.00"));
        DeliveryOrderItemAllocation replacement = allocation(901L, item, replacementInventory, zone2,
                new BigDecimal("2.00"), ZERO, true);
        WarehouseLocation stagingBin = bin(880L, warehouse, zone2);
        stagingBin.setIsStaging(true);

        OutboundQcRecord oldRow = new OutboundQcRecord();
        oldRow.setAllocation(oldPassed);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(oldPassed, replacement));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L, 901L))).thenReturn(List.of(oldRow));
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(outboundQcRecordRepository.save(any(OutboundQcRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderPickQcResultRequest request = new DeliveryOrderPickQcResultRequest();
        request.setIdempotencyKey("qc-100");
        DeliveryOrderPickQcRowRequest row = new DeliveryOrderPickQcRowRequest();
        row.setDoItemId(200L);
        row.setAllocationId(901L);
        row.setBatchId(72L);
        row.setLocationId(802L);
        row.setZoneId(32L);
        row.setPickedQty(new BigDecimal("2.00"));
        row.setQcPassQty(new BigDecimal("2.00"));
        row.setQcFailQty(BigDecimal.ZERO);
        row.setStagingLocationId(880L);
        request.setResults(List.of(row));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickQcResult(100L, request, warehouseStaff);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.QC_PENDING_APPROVAL);
        assertThat(item.getQcPassQty()).isEqualByComparingTo("10.00");
    }

    @Test
    void approveDeliveryOrderQuality_blocksWhenRequestedQtyNotFullyPassed() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("8.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.approveDeliveryOrderQuality(
                100L, new DeliveryOrderQualityApprovalRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("QC_REPLACEMENT_REQUIRED");
    }

    @Test
    void approveDeliveryOrderQuality_movesApprovedPassAndFailInventory() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPickedQty(new BigDecimal("12.00"));
        item.setQcPassQty(new BigDecimal("10.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        inventory.setTotalQty(new BigDecimal("15.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation original = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);

        WarehouseLocation sourceZone2 = zone(32L, warehouse);
        WarehouseLocation sourceBin2 = bin(802L, warehouse, sourceZone2);
        Inventory replacementInventory = inventory(502L, warehouse, product, batch, sourceBin2,
                new BigDecimal("2.00"), new BigDecimal("2.00"));
        DeliveryOrderItemAllocation replacement = allocation(901L, item, replacementInventory, sourceZone2,
                new BigDecimal("2.00"), new BigDecimal("2.00"), true);

        WarehouseLocation stagingZone = zone(92L, warehouse);
        stagingZone.setIsStaging(true);
        WarehouseLocation stagingBin = bin(880L, warehouse, stagingZone);
        stagingBin.setCurrentWeightKg(ZERO);
        WarehouseLocation quarantineZone = zone(91L, warehouse);
        quarantineZone.setIsQuarantine(true);
        WarehouseLocation quarantineBin = bin(990L, warehouse, quarantineZone);
        quarantineBin.setCurrentWeightKg(ZERO);
        product.setWeightKg(BigDecimal.ONE);
        Inventory stagingInventory = inventory(700L, warehouse, product, batch, stagingBin, ZERO, ZERO);
        Inventory quarantineInventory = inventory(701L, warehouse, product, batch, quarantineBin, ZERO, ZERO);

        OutboundQcRecord originalQc = failedQcRecord(original, new BigDecimal("2.00"));
        originalQc.setId(600L);
        originalQc.setPickedQty(new BigDecimal("10.00"));
        originalQc.setQcPassQty(new BigDecimal("8.00"));
        originalQc.setStagingLocation(stagingBin);
        originalQc.setQuarantineLocation(quarantineBin);
        originalQc.setQcFailReason("Mop meo");
        originalQc.setIsActive(true);
        OutboundQcRecord replacementQc = failedQcRecord(replacement, ZERO);
        replacementQc.setId(601L);
        replacementQc.setStagingLocation(stagingBin);
        replacementQc.setIsActive(true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L))
                .thenReturn(List.of(original, replacement));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIsActiveTrue(100L))
                .thenReturn(List.of(originalQc, replacementQc));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L, 901L)))
                .thenReturn(List.of(originalQc, replacementQc));
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(entityManager.find(WarehouseLocation.class, 990L)).thenReturn(quarantineBin);
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 880L))
                .thenReturn(Optional.of(stagingInventory));
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 990L))
                .thenReturn(Optional.of(quarantineInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quarantineRecordRepository.save(any(QuarantineRecord.class))).thenAnswer(invocation -> {
            QuarantineRecord record = invocation.getArgument(0);
            record.setId(710L);
            return record;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboundQcRecordRepository.save(any(OutboundQcRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.approveDeliveryOrderQuality(
                100L, new DeliveryOrderQualityApprovalRequest(), storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.QC_COMPLETED);
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(inventory.getReservedQty()).isZero();
        assertThat(replacementInventory.getTotalQty()).isZero();
        assertThat(replacementInventory.getReservedQty()).isZero();
        assertThat(stagingInventory.getTotalQty()).isEqualByComparingTo("10.00");
        assertThat(stagingInventory.getReservedQty()).isEqualByComparingTo("10.00");
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("2.00");
        assertThat(originalQc.getInventoryMovedAt()).isNotNull();
        assertThat(replacementQc.getInventoryMovedAt()).isNotNull();
        assertThat(originalQc.getQuarantineRecord()).isNotNull();
        ArgumentCaptor<Adjustment> adjustmentCaptor = ArgumentCaptor.forClass(Adjustment.class);
        verify(adjustmentRepository).save(adjustmentCaptor.capture());
        assertThat(adjustmentCaptor.getValue().getApprovedBy()).isEqualTo(storekeeper);
        assertThat(adjustmentCaptor.getValue().getQuantityAdjustment()).isEqualByComparingTo("-2.00");
    }

    @Test
    void rejectDeliveryOrderQuality_keepsPendingInventoryAtSourceForStaffRecount() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPickedQty(new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("8.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        inventory.setTotalQty(new BigDecimal("15.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);

        WarehouseLocation stagingBin = bin(880L, warehouse, zone);
        stagingBin.setCurrentWeightKg(ZERO);
        WarehouseLocation quarantineBin = bin(990L, warehouse, zone);
        quarantineBin.setCurrentWeightKg(ZERO);
        inventory.getLocation().setCurrentWeightKg(new BigDecimal("15.00"));
        product.setWeightKg(BigDecimal.ONE);
        Inventory stagingInventory = inventory(700L, warehouse, product, batch, stagingBin,
                ZERO, ZERO);
        Inventory quarantineInventory = inventory(701L, warehouse, product, batch, quarantineBin,
                ZERO, ZERO);

        OutboundQcRecord qcRecord = new OutboundQcRecord();
        qcRecord.setId(600L);
        qcRecord.setDeliveryOrder(order);
        qcRecord.setDeliveryOrderItem(item);
        qcRecord.setAllocation(allocation);
        qcRecord.setBatch(batch);
        qcRecord.setPickedQty(new BigDecimal("10.00"));
        qcRecord.setQcPassQty(new BigDecimal("8.00"));
        qcRecord.setQcFailQty(new BigDecimal("2.00"));
        qcRecord.setQcFailReason("Mop meo");
        qcRecord.setStagingLocation(stagingBin);
        qcRecord.setQuarantineLocation(quarantineBin);
        qcRecord.setIsActive(true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIsActiveTrue(100L))
                .thenReturn(List.of(qcRecord));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of());
        when(outboundQcRecordRepository.findHistoryByAllocationIdIn(List.of(900L))).thenReturn(List.of(qcRecord));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(outboundQcRecordRepository.save(any(OutboundQcRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderQualityApprovalRequest request = new DeliveryOrderQualityApprovalRequest();
        request.setDecision(OutboundQualityDecision.REJECT);
        request.setRejectionReason("So luong thuc te khong khop");

        DeliveryOrderResponse response = service.approveDeliveryOrderQuality(100L, request, storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(order.getRejectionReason()).isEqualTo("So luong thuc te khong khop");
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("15.00");
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("10.00");
        assertThat(stagingInventory.getTotalQty()).isZero();
        assertThat(stagingInventory.getReservedQty()).isZero();
        assertThat(quarantineInventory.getTotalQty()).isZero();
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(quarantineRecordRepository, never()).save(any(QuarantineRecord.class));
        verify(adjustmentRepository, never()).save(any(Adjustment.class));
        assertThat(allocation.getPickedQty()).isZero();
        assertThat(item.getPickedQty()).isZero();
        assertThat(item.getQcPassQty()).isZero();
        assertThat(item.getQcFailQty()).isZero();
        assertThat(item.getPickedBy()).isNull();
        assertThat(order.getQcBy()).isNull();
        assertThat(qcRecord.getIsActive()).isFalse();
        assertThat(qcRecord.getRejectedBy()).isEqualTo(storekeeper);
        assertThat(qcRecord.getRejectionReason()).isEqualTo("So luong thuc te khong khop");
        assertThat(response.getItems().get(0).getAllocations().get(0).isQcCompleted()).isFalse();
        assertThat(response.getItems().get(0).getAllocations().get(0).getQcPassQty()).isEqualByComparingTo("8.00");
        assertThat(response.getItems().get(0).getAllocations().get(0).getQcFailQty()).isEqualByComparingTo("2.00");
        assertThat(response.getItems().get(0).getAllocations().get(0).getQcFailReason()).isEqualTo("Mop meo");
    }

    @Test
    void rejectDeliveryOrderQuality_requiresReason() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_PENDING_APPROVAL);
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));

        DeliveryOrderQualityApprovalRequest request = new DeliveryOrderQualityApprovalRequest();
        request.setDecision(OutboundQualityDecision.REJECT);

        assertThatThrownBy(() -> service.approveDeliveryOrderQuality(100L, request, storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("OUTBOUND_QC_REJECTION_REASON_REQUIRED");
    }

    @Test
    void requestPickingPlanAdjustment_recordsStaffRequestForImbalancedPlan() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        DeliveryOrderItemAllocation original = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        DeliveryOrderItemAllocation replacement = allocation(901L, item, inventory, zone,
                new BigDecimal("2.00"), ZERO, true);
        com.wms.dto.request.DeliveryOrderPickingPlanAdjustmentRequest request =
                new com.wms.dto.request.DeliveryOrderPickingPlanAdjustmentRequest();
        request.setReason("Yeu cau 10, dang phan bo 12");

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L))
                .thenReturn(List.of(original, replacement));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.requestPickingPlanAdjustment(100L, request, warehouseStaff);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(order.getRejectionReason()).isEqualTo("Yeu cau 10, dang phan bo 12");
        verify(auditUtil).logChange(eq(warehouseStaff),
                eq(AuditAction.PICKING_PLAN_ADJUSTMENT_REQUEST), eq("DELIVERY_ORDER"),
                eq(100L), any(), any(), any());
    }

    @Test
    void approveDeliveryOrderWarehouseRelease_movesStatusToApproved() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_COMPLETED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(deliveryOrderWarehouseApprovalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.approveDeliveryOrderWarehouseRelease(
                100L, new DeliveryOrderWarehouseApprovalRequest(), manager);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAREHOUSE_APPROVED);
    }

    @Test
    void rejectDeliveryOrderWarehouseRelease_returnsStagedPassAndKeepsFailedQty() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_COMPLETED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setQcPassQty(new BigDecimal("8.00"));
        item.setQcFailQty(new BigDecimal("2.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);
        WarehouseLocation stagingBin = bin(880L, warehouse, zone);
        stagingBin.setIsStaging(true);
        Inventory stagingInventory = inventory(700L, warehouse, product, batch, stagingBin,
                new BigDecimal("8.00"), new BigDecimal("8.00"));
        OutboundQcRecord qcRecord = new OutboundQcRecord();
        qcRecord.setAllocation(allocation);
        qcRecord.setQcPassQty(new BigDecimal("8.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of(qcRecord));
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 880L))
                .thenReturn(Optional.of(stagingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderWarehouseApprovalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.rejectDeliveryOrderWarehouseRelease(
                100L, warehouseRejectRequest(), manager);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.REJECTED);
        assertThat(stagingInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(stagingInventory.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("23.00");
    }

    @Test
    void rejectDeliveryOrderWarehouseRelease_rejectsIncompleteReturnCoverage() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.QC_COMPLETED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), new BigDecimal("10.00"), false);
        OutboundQcRecord qcRecord = new OutboundQcRecord();
        qcRecord.setAllocation(allocation);
        qcRecord.setQcPassQty(new BigDecimal("8.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of(qcRecord));

        DeliveryOrderWarehouseRejectRequest request = warehouseRejectRequest();
        request.getReturnToBinRecords().get(0).setReturnedQty(new BigDecimal("7.00"));

        assertThatThrownBy(() -> service.rejectDeliveryOrderWarehouseRelease(100L, request, manager))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("PICKED_GOODS_RETURN_REQUIRED");
    }

    @Test
    void confirmReturnedGoodsReceived_opensCountQcPendingFlowFromShippedQcPassLines() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        OutboundQcRecord shipped = outboundQcRecord(item, new BigDecimal("8.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.existsByDeliveryOrderId(100L)).thenReturn(false);
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(100L))).thenReturn(List.of(shipped));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> {
            ReturnedDeliveryFlow flow = invocation.getArgument(0);
            flow.setId(300L);
            return flow;
        });

        ReturnedGoodsFlowResponse response = service.confirmReturnedGoodsReceived(
                100L, new ReturnedGoodsReceiveRequest(), storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.COUNT_QC_PENDING);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getExpectedQty()).isEqualByComparingTo("8.00");
        assertThat(response.getItems().get(0).getActualQty()).isNull();
    }

    @Test
    void submitReturnedGoodsCountQc_blocksBeforeStorekeeperArrivalConfirmation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitReturnedGoodsCountQc(
                100L, returnedCountQcRequest(new BigDecimal("8.00"), new BigDecimal("8.00"), ZERO, null),
                warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_FLOW_NOT_FOUND");
    }

    @Test
    void submitReturnedGoodsCountQc_validatesActualPassFailSplitAndFailureReason() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_PENDING,
                new BigDecimal("8.00"), null, null, null, null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));

        assertThatThrownBy(() -> service.submitReturnedGoodsCountQc(
                100L, returnedCountQcRequest(new BigDecimal("8.00"), new BigDecimal("7.00"), new BigDecimal("1.00"), null),
                warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_QUALITY_REASON_REQUIRED");
    }

    @Test
    void submitReturnedGoodsCountQc_requiresShortageReasonAndRejectsOverReceipt() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_PENDING,
                new BigDecimal("8.00"), null, null, null, null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));

        assertThatThrownBy(() -> service.submitReturnedGoodsCountQc(
                100L, returnedCountQcRequest(new BigDecimal("7.00"), new BigDecimal("7.00"), ZERO, null),
                warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_SHORTAGE_REASON_REQUIRED");

        assertThatThrownBy(() -> service.submitReturnedGoodsCountQc(
                100L, returnedCountQcRequest(new BigDecimal("9.00"), new BigDecimal("9.00"), ZERO, null),
                warehouseStaff))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_QTY_EXCEEDS_EXPECTED");
    }

    @Test
    void submitReturnedGoodsCountQc_derivesShortageFromActualReceivedQuantity() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_PENDING,
                new BigDecimal("8.00"), null, null, null, null);
        ReturnedGoodsCountQcRequest request = returnedCountQcRequest(
                new BigDecimal("7.00"), new BigDecimal("7.00"), ZERO, null);
        request.getItems().get(0).setShortageReason("One carton was missing from the vehicle");

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.submitReturnedGoodsCountQc(100L, request, warehouseStaff);

        assertThat(response.getItems().get(0).getActualQty()).isEqualByComparingTo("7.00");
        assertThat(response.getItems().get(0).getShortageQty()).isEqualByComparingTo("1.00");
        assertThat(response.getItems().get(0).getShortageReason())
                .isEqualTo("One carton was missing from the vehicle");
    }

    @Test
    void approveReturnedGoods_rejectsWithReasonAndBlocksPutawayPlanning() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), new BigDecimal("7.00"), new BigDecimal("1.00"), null);
        ReturnedGoodsApprovalRequest rejectRequest = new ReturnedGoodsApprovalRequest();
        rejectRequest.setDecision(ReturnedGoodsQcDecision.REJECT);
        rejectRequest.setRejectionReason("Count and QC photos do not match");

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.approveReturnedGoods(100L, rejectRequest, storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.QC_REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Count and QC photos do not match");
        assertThatThrownBy(() -> service.planReturnedGoodsPutaway(
                100L, returnedPutawayPlanRequest(new BigDecimal("8.00")), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_FLOW_STATUS_INVALID");
    }

    @Test
    void approveReturnedGoods_marksFlowAcceptedAfterCompleteCountQc() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), new BigDecimal("8.00"), ZERO, null);
        ReturnedGoodsApprovalRequest acceptRequest = new ReturnedGoodsApprovalRequest();
        acceptRequest.setDecision(ReturnedGoodsQcDecision.ACCEPT);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.approveReturnedGoods(100L, acceptRequest, storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.QC_APPROVED);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.RETURNED);
        assertThat(flow.getApprovedByStorekeeper()).isEqualTo(storekeeper);
    }

    @Test
    void planReturnedGoodsPutaway_setsStorekeeperDestinationLocation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.QC_APPROVED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), new BigDecimal("8.00"), ZERO, null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(entityManager.find(WarehouseLocation.class, 801L)).thenReturn(bin);
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.planReturnedGoodsPutaway(
                100L, returnedPutawayPlanRequest(new BigDecimal("8.00")), storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED);
        assertThat(response.getItems().get(0).getDestinationLocationId()).isEqualTo(801L);
        assertThat(flow.getPutawayPlannedByStorekeeper()).isEqualTo(storekeeper);
        assertThat(flow.getItems().get(0).getPlannedQty()).isEqualByComparingTo("8.00");
    }

    @Test
    void planReturnedGoodsPutaway_splitsPassToBinAndFailedToQuarantine() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("5.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.QC_APPROVED,
                new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("3.00"),
                new BigDecimal("2.00"), null);
        WarehouseLocation quarantineBin = bin(880L, warehouse, zone);
        quarantineBin.setIsQuarantine(true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(entityManager.find(WarehouseLocation.class, 801L)).thenReturn(bin);
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(quarantineBin);
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.planReturnedGoodsPutaway(
                100L, returnedPutawayPlanRequest(new BigDecimal("3.00"), 880L, new BigDecimal("2.00")),
                storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED);
        assertThat(response.getItems().get(0).getDestinationLocationId()).isEqualTo(801L);
        assertThat(response.getItems().get(0).getFailedDestinationLocationId()).isEqualTo(880L);
        assertThat(flow.getItems().get(0).getPlannedQty()).isEqualByComparingTo("3.00");
        assertThat(flow.getItems().get(0).getFailedPlannedQty()).isEqualByComparingTo("2.00");
    }

    @Test
    void completeReturnedGoodsPutaway_movesInventoryBackAndMarksDeliveryFailed() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), new BigDecimal("8.00"), ZERO, bin);
        Inventory transitInventory = inventory(900L, warehouse(99L, "INTRANSIT"), product, batch, bin,
                new BigDecimal("8.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(30L, 71L))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 801L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.completeReturnedGoodsPutaway(
                100L, new ReturnedGoodsPutawayCompleteRequest(), warehouseStaff);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.PUTAWAY_COMPLETED);
        assertThat(response.getDeliveryOrderStatus()).isEqualTo(DeliveryOrderStatus.DELIVERY_FAILED);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.DELIVERY_FAILED);
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("23.00");
        assertThat(flow.getItems().get(0).getPutawayCompletedQty()).isEqualByComparingTo("8.00");
    }

    @Test
    void completeReturnedGoodsPutaway_reconcilesApprovedShortageFromTransit() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED,
                new BigDecimal("10.00"), new BigDecimal("8.00"), new BigDecimal("8.00"), ZERO, bin);
        flow.setApprovedByStorekeeper(storekeeper);
        flow.getItems().get(0).setShortageQty(new BigDecimal("2.00"));
        flow.getItems().get(0).setShortageReason("Two units missing on vehicle return");
        Inventory transitInventory = inventory(900L, warehouse(99L, "INTRANSIT"), product, batch, bin,
                new BigDecimal("10.00"), ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(30L, 71L))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 801L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.completeReturnedGoodsPutaway(100L, new ReturnedGoodsPutawayCompleteRequest(), warehouseStaff);

        ArgumentCaptor<Adjustment> adjustmentCaptor = ArgumentCaptor.forClass(Adjustment.class);
        verify(adjustmentRepository).save(adjustmentCaptor.capture());
        Adjustment adjustment = adjustmentCaptor.getValue();
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("23.00");
        assertThat(adjustment.getType()).isEqualTo(AdjustmentType.RETURN_SHORTAGE);
        assertThat(adjustment.getQuantityAdjustment()).isEqualByComparingTo("-2.00");
        assertThat(adjustment.getReason()).isEqualTo("Two units missing on vehicle return");
        assertThat(adjustment.getApprovedBy()).isEqualTo(storekeeper);
    }

    @Test
    void completeReturnedGoodsPutaway_movesFailedQuantityToQuarantineOnly() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("5.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED,
                new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("3.00"),
                new BigDecimal("2.00"), bin);
        WarehouseLocation quarantineBin = bin(880L, warehouse, zone);
        quarantineBin.setIsQuarantine(true);
        flow.getItems().get(0).setFailedDestinationLocation(quarantineBin);
        flow.getItems().get(0).setPlannedQty(new BigDecimal("3.00"));
        flow.getItems().get(0).setFailedPlannedQty(new BigDecimal("2.00"));
        Inventory transitInventory = inventory(900L, warehouse(99L, "INTRANSIT"), product, batch, bin,
                new BigDecimal("5.00"), ZERO);
        Inventory quarantineInventory = inventory(901L, warehouse, product, batch, quarantineBin, ZERO, ZERO);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(inventoryRepository.findTransitRowForDeliveryConfirmation(30L, 71L))
                .thenReturn(Optional.of(transitInventory));
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 801L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 880L))
                .thenReturn(Optional.of(quarantineInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.completeReturnedGoodsPutaway(
                100L, new ReturnedGoodsPutawayCompleteRequest(), warehouseStaff);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.PUTAWAY_COMPLETED);
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("18.00");
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("2.00");
        assertThat(flow.getItems().get(0).getPutawayCompletedQty()).isEqualByComparingTo("3.00");
        assertThat(flow.getItems().get(0).getFailedPutawayCompletedQty()).isEqualByComparingTo("2.00");
    }

    private void stubSuccessfulCreate(BigDecimal inventoryAvailable) {
        stubCreateUntilAvailability(inventoryAvailable, reservation.getReservedQty());
        when(deliveryOrderRepository.existsByDoNumber("DO-" + LocalDate.now().toString().replace("-", "") + "-0001"))
                .thenReturn(false);
        when(deliveryOrderRepository.saveAndFlush(any(DeliveryOrder.class))).thenAnswer(invocation -> {
            DeliveryOrder order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(deliveryOrderItemRepository.save(any(DeliveryOrderItem.class))).thenAnswer(invocation -> {
            DeliveryOrderItem item = invocation.getArgument(0);
            item.setId(200L);
            return item;
        });
    }

    private void stubCreateUntilAvailability(BigDecimal inventoryAvailable, BigDecimal plannerReserved) {
        reservation.setReservedQty(plannerReserved);
        stubCreateUntilCredit();
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), eq(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID)), any(LocalDate.class)))
                .thenReturn(false);
        when(inventoryRepository.sumValidAvailableQty(20L, 30L)).thenReturn(inventoryAvailable);
        when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductId(20L, 30L))
                .thenReturn(Optional.of(reservation));
        lenient().when(reservationRepository.findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(20L, 30L))
                .thenReturn(Optional.of(reservation));
    }

    private void stubCreateUntilCredit() {
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(productRepository.findByIdAndIsActiveTrue(30L)).thenReturn(Optional.of(product));
        when(priceHistoryService.lookupApproved(eq(30L), eq(20L), any(LocalDate.class)))
                .thenReturn(Optional.of(price));
    }

    private DeliveryOrderCreateRequest validRequest(BigDecimal qty) {
        DeliveryOrderItemCreateRequest item = new DeliveryOrderItemCreateRequest();
        item.setProductId(30L);
        item.setRequestedQty(qty);
        DeliveryOrderCreateRequest request = new DeliveryOrderCreateRequest();
        request.setDealerId(10L);
        request.setWarehouseId(20L);
        request.setType(DeliveryOrderType.SALE);
        request.setDocumentDate(LocalDate.now());
        request.setExpectedDeliveryDate(LocalDate.now().plusDays(2));
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryOrderCancelRequest cancelRequest() {
        DeliveryOrderCancelRequest request = new DeliveryOrderCancelRequest();
        request.setCancelReason("Customer changed order");
        return request;
    }

    private DeliveryOrderUpdateRequest updateRequest() {
        DeliveryOrderUpdateRequest request = new DeliveryOrderUpdateRequest();
        DeliveryOrderItemCreateRequest item = new DeliveryOrderItemCreateRequest();
        item.setProductId(30L);
        item.setRequestedQty(new BigDecimal("8.00"));
        request.setDealerId(10L);
        request.setWarehouseId(20L);
        request.setType(DeliveryOrderType.SALE);
        request.setDocumentDate(LocalDate.now());
        request.setExpectedDeliveryDate(LocalDate.now().plusDays(2));
        request.setNotes("Updated before picking plan");
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryOrderPickingPlanRequest pickingPlanRequest() {
        DeliveryOrderPickingPlanRequest request = new DeliveryOrderPickingPlanRequest();
        request.setAllocations(new ArrayList<>(List.of(
                allocationRequest(200L, 501L, 71L, 801L, 31L, new BigDecimal("10.00")))));
        return request;
    }

    private DeliveryOrderReplacementPlanRequest replacementPlanRequest() {
        DeliveryOrderReplacementAllocationRequest replacement = new DeliveryOrderReplacementAllocationRequest();
        replacement.setDoItemId(200L);
        replacement.setFailedInventoryId(501L);
        replacement.setFailedBatchId(71L);
        replacement.setFailedLocationId(801L);
        replacement.setReplacementInventoryId(502L);
        replacement.setReplacementBatchId(72L);
        replacement.setReplacementLocationId(802L);
        replacement.setReplacementZoneId(32L);
        replacement.setQuantity(new BigDecimal("2.00"));
        replacement.setReason("QC fail scratched cookware");
        DeliveryOrderReplacementPlanRequest request = new DeliveryOrderReplacementPlanRequest();
        request.setReplacements(List.of(replacement));
        return request;
    }

    private DeliveryOrderPickQcResultRequest pickQcResultRequest() {
        DeliveryOrderPickQcRowRequest row = new DeliveryOrderPickQcRowRequest();
        row.setDoItemId(200L);
        row.setAllocationId(900L);
        row.setBatchId(71L);
        row.setLocationId(801L);
        row.setZoneId(31L);
        row.setPickedQty(new BigDecimal("10.00"));
        row.setQcPassQty(new BigDecimal("8.00"));
        row.setQcFailQty(new BigDecimal("2.00"));
        row.setQcFailReason("Surface scratch");
        row.setStagingLocationId(880L);
        row.setQuarantineLocationId(990L);
        DeliveryOrderPickQcResultRequest request = new DeliveryOrderPickQcResultRequest();
        request.setIdempotencyKey("qc-100");
        request.setResults(List.of(row));
        return request;
    }

    private DeliveryOrderWarehouseRejectRequest warehouseRejectRequest() {
        DeliveryOrderWarehouseRejectReturnRequest row = new DeliveryOrderWarehouseRejectReturnRequest();
        row.setDoItemId(200L);
        row.setAllocationId(900L);
        row.setBatchId(71L);
        row.setReturnedQty(new BigDecimal("8.00"));
        row.setSourceLocationId(880L);
        row.setOriginalLocationId(801L);
        row.setOriginalZoneId(31L);
        row.setReason("Return staged goods after reject");
        DeliveryOrderWarehouseRejectRequest request = new DeliveryOrderWarehouseRejectRequest();
        request.setReason("Seal issue found before loading");
        request.setReturnToBinRecords(new ArrayList<>(List.of(row)));
        return request;
    }

    private ReturnedGoodsCountQcRequest returnedCountQcRequest(BigDecimal actualQty,
                                                               BigDecimal passQty,
                                                               BigDecimal failQty,
                                                               String failureReason) {
        ReturnedGoodsCountQcItemRequest item = new ReturnedGoodsCountQcItemRequest();
        item.setDoItemId(200L);
        item.setProductId(30L);
        item.setBatchId(71L);
        item.setActualQty(actualQty);
        item.setQualityPassQty(passQty);
        item.setQualityFailQty(failQty);
        item.setQualityFailureReason(failureReason);
        ReturnedGoodsCountQcRequest request = new ReturnedGoodsCountQcRequest();
        request.setItems(List.of(item));
        return request;
    }

    private ReturnedGoodsPutawayPlanRequest returnedPutawayPlanRequest(BigDecimal plannedQty) {
        ReturnedGoodsPutawayPlanItemRequest item = new ReturnedGoodsPutawayPlanItemRequest();
        item.setDoItemId(200L);
        item.setBatchId(71L);
        item.setDestinationLocationId(801L);
        item.setPlannedQty(plannedQty);
        ReturnedGoodsPutawayPlanRequest request = new ReturnedGoodsPutawayPlanRequest();
        request.setItems(List.of(item));
        request.setNotes("Plan returned goods putaway");
        return request;
    }

    private ReturnedGoodsPutawayPlanRequest returnedPutawayPlanRequest(BigDecimal plannedQty,
                                                                       Long failedDestinationLocationId,
                                                                       BigDecimal failedPlannedQty) {
        ReturnedGoodsPutawayPlanRequest request = returnedPutawayPlanRequest(plannedQty);
        request.getItems().get(0).setFailedDestinationLocationId(failedDestinationLocationId);
        request.getItems().get(0).setFailedPlannedQty(failedPlannedQty);
        return request;
    }

    private DeliveryOrderAllocationRequest allocationRequest(Long doItemId,
                                                             Long inventoryId,
                                                             Long batchId,
                                                             Long locationId,
                                                             Long zoneId,
                                                             BigDecimal plannedQty) {
        DeliveryOrderAllocationRequest allocation = new DeliveryOrderAllocationRequest();
        allocation.setDoItemId(doItemId);
        allocation.setInventoryId(inventoryId);
        allocation.setBatchId(batchId);
        allocation.setLocationId(locationId);
        allocation.setZoneId(zoneId);
        allocation.setPlannedQty(plannedQty);
        return allocation;
    }

    private DeliveryOrderReturnToBinRequest returnToBinRequest(Long allocationId,
                                                               BigDecimal returnedQty,
                                                               Long sourceLocationId) {
        DeliveryOrderReturnToBinRequest request = new DeliveryOrderReturnToBinRequest();
        request.setAllocationId(allocationId);
        request.setReturnedQty(returnedQty);
        request.setSourceLocationId(sourceLocationId);
        request.setReason("Return picked goods");
        return request;
    }

    private DeliveryOrder order(Long id, DeliveryOrderStatus status) {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(id);
        order.setDoNumber("DO-1");
        order.setDealer(dealer);
        order.setWarehouse(warehouse);
        order.setType(DeliveryOrderType.SALE);
        order.setStatus(status);
        order.setDocumentDate(LocalDate.of(2026, 6, 18));
        order.setCreatedBy(planner);
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        return order;
    }

    private DeliveryOrderItem item(DeliveryOrder order, Product product, BigDecimal reservedQty) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setId(200L);
        item.setDeliveryOrder(order);
        item.setProduct(product);
        item.setRequestedQty(reservedQty);
        item.setReservedQty(reservedQty);
        item.setPlannedQty(ZERO);
        item.setPickedQty(ZERO);
        item.setQcPassQty(ZERO);
        item.setQcFailQty(ZERO);
        item.setIssuedQty(BigDecimal.ZERO);
        return item;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(role.name());
        return user;
    }

    private Dealer dealer(Long id, BigDecimal balance, BigDecimal limit, CreditStatus status) {
        Dealer dealer = new Dealer();
        dealer.setId(id);
        dealer.setCreditStatus(status);
        dealer.setCurrentBalance(balance);
        dealer.setCreditLimit(limit);
        dealer.setPaymentTermDays(30);
        dealer.setIsActive(true);
        return dealer;
    }

    private Warehouse warehouse(Long id, String code) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setCode(code);
        warehouse.setIsActive(true);
        return warehouse;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setWeightKg(BigDecimal.ONE);
        product.setIsActive(true);
        return product;
    }

    private Vehicle vehicle(Long id, Warehouse warehouse, VehicleStatus status, BigDecimal maxWeightKg) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setWarehouse(warehouse);
        vehicle.setStatus(status);
        vehicle.setMaxWeightKg(maxWeightKg);
        vehicle.setIsActive(true);
        return vehicle;
    }

    private PriceHistory price(Product product, BigDecimal sellingPrice) {
        PriceHistory price = new PriceHistory();
        price.setProduct(product);
        price.setSellingPrice(sellingPrice);
        return price;
    }

    private WarehouseProductReservation reservation(Warehouse warehouse, Product product, BigDecimal qty) {
        WarehouseProductReservation reservation = new WarehouseProductReservation();
        reservation.setWarehouse(warehouse);
        reservation.setProduct(product);
        reservation.setReservedQty(qty);
        reservation.setCreatedAt(OffsetDateTime.now());
        reservation.setUpdatedAt(OffsetDateTime.now());
        return reservation;
    }

    private WarehouseLocation zone(Long id, Warehouse warehouse) {
        WarehouseLocation zone = new WarehouseLocation();
        zone.setId(id);
        zone.setWarehouse(warehouse);
        zone.setType(LocationType.ZONE);
        zone.setIsActive(true);
        zone.setIsQuarantine(false);
        zone.setIsStaging(false);
        return zone;
    }

    private WarehouseLocation bin(Long id, Warehouse warehouse, WarehouseLocation parent) {
        WarehouseLocation bin = new WarehouseLocation();
        bin.setId(id);
        bin.setWarehouse(warehouse);
        bin.setType(LocationType.BIN);
        bin.setParent(parent);
        bin.setIsActive(true);
        bin.setIsQuarantine(false);
        bin.setIsStaging(false);
        bin.setIsLocked(false);
        return bin;
    }

    private void stubPickQcFlow(DeliveryOrder order, DeliveryOrderItem item,
            DeliveryOrderItemAllocation allocation, WarehouseLocation stagingBin,
            WarehouseLocation quarantineBin) {
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(order.getId())).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(warehouseStaff.getId()))
                .thenReturn(List.of(order.getWarehouse().getId()));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(order.getId())).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(order.getId()))
                .thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(order.getId(), "qc-100"))
                .thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(allocation.getId()))).thenReturn(List.of());
        when(entityManager.find(WarehouseLocation.class, stagingBin.getId())).thenReturn(stagingBin);
        when(entityManager.find(WarehouseLocation.class, quarantineBin.getId())).thenReturn(quarantineBin);
    }

    private Batch batch(Long id, Product product, Warehouse warehouse) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProduct(product);
        batch.setWarehouse(warehouse);
        batch.setReceivedDate(LocalDate.of(2026, 6, 1));
        return batch;
    }

    private OutboundQcRecord outboundQcRecord(DeliveryOrderItem item, BigDecimal passQty) {
        OutboundQcRecord record = new OutboundQcRecord();
        record.setDeliveryOrder(item.getDeliveryOrder());
        record.setDeliveryOrderItem(item);
        record.setBatch(batch);
        record.setQcPassQty(passQty);
        return record;
    }

    private OutboundQcRecord failedQcRecord(DeliveryOrderItemAllocation allocation, BigDecimal failQty) {
        OutboundQcRecord record = new OutboundQcRecord();
        record.setDeliveryOrder(allocation.getDeliveryOrderItem().getDeliveryOrder());
        record.setDeliveryOrderItem(allocation.getDeliveryOrderItem());
        record.setAllocation(allocation);
        record.setBatch(allocation.getBatch());
        record.setLocation(allocation.getLocation());
        record.setZone(allocation.getZone());
        BigDecimal passQty = allocation.getPickedQty() == null ? ZERO : allocation.getPickedQty();
        record.setPickedQty(passQty.add(failQty));
        record.setQcPassQty(passQty);
        record.setQcFailQty(failQty);
        return record;
    }

    private ReturnedDeliveryFlow returnedFlow(DeliveryOrder order,
                                              DeliveryOrderItem item,
                                              ReturnedDeliveryFlowStatus status,
                                              BigDecimal expectedQty,
                                              BigDecimal actualQty,
                                              BigDecimal passQty,
                                              BigDecimal failQty,
                                              WarehouseLocation destinationLocation) {
        ReturnedDeliveryFlow flow = new ReturnedDeliveryFlow();
        flow.setId(300L);
        flow.setDeliveryOrder(order);
        flow.setStatus(status);
        flow.setCreatedAt(OffsetDateTime.now());
        flow.setUpdatedAt(OffsetDateTime.now());
        ReturnedDeliveryFlowItem flowItem = new ReturnedDeliveryFlowItem();
        flowItem.setId(301L);
        flowItem.setFlow(flow);
        flowItem.setDeliveryOrderItem(item);
        flowItem.setProduct(product);
        flowItem.setBatch(batch);
        flowItem.setExpectedQty(expectedQty);
        flowItem.setActualQty(actualQty);
        flowItem.setQualityPassQty(passQty);
        flowItem.setQualityFailQty(failQty);
        flowItem.setDestinationLocation(destinationLocation);
        flowItem.setPlannedQty(destinationLocation == null ? null : actualQty);
        flow.getItems().add(flowItem);
        return flow;
    }

    private Inventory inventory(Long id,
                                Warehouse warehouse,
                                Product product,
                                Batch batch,
                                WarehouseLocation location,
                                BigDecimal totalQty,
                                BigDecimal reservedQty) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setLocation(location);
        inventory.setTotalQty(totalQty);
        inventory.setReservedQty(reservedQty);
        inventory.setCostPrice(new BigDecimal("1.50"));
        inventory.setUpdatedAt(OffsetDateTime.now());
        return inventory;
    }

    private DeliveryOrderItemAllocation allocation(Long id,
                                                   DeliveryOrderItem item,
                                                   Inventory inventory,
                                                   WarehouseLocation zone,
                                                   BigDecimal plannedQty,
                                                   BigDecimal pickedQty,
                                                   boolean replacement) {
        DeliveryOrderItemAllocation allocation = new DeliveryOrderItemAllocation();
        allocation.setId(id);
        allocation.setDeliveryOrderItem(item);
        allocation.setInventory(inventory);
        allocation.setBatch(inventory.getBatch());
        allocation.setLocation(inventory.getLocation());
        allocation.setZone(zone);
        allocation.setPlannedQty(plannedQty);
        allocation.setPickedQty(pickedQty);
        allocation.setReplacement(replacement);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setCreatedBy(storekeeper);
        allocation.setCreatedAt(OffsetDateTime.now());
        allocation.setUpdatedAt(OffsetDateTime.now());
        return allocation;
    }
}
