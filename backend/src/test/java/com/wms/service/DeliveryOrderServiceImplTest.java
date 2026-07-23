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
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectReturnRequest;
import com.wms.dto.request.ReturnedGoodsApprovalRequest;
import com.wms.dto.request.ReturnedGoodsCountQcItemRequest;
import com.wms.dto.request.ReturnedGoodsCountQcRequest;
import com.wms.dto.request.ReturnedGoodsPutawayCompleteRequest;
import com.wms.dto.request.ReturnedGoodsPutawayPlanItemRequest;
import com.wms.dto.request.ReturnedGoodsPutawayPlanRequest;
import com.wms.dto.response.DeliveryOrderResponse;
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
import com.wms.enums.order_fulfillment.ReturnedGoodsQualityResult;
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
import com.wms.repository.QuarantineRecordRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                dealerRepository, warehouseRepository, productRepository, inventoryRepository,
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
    }

    @Test
    void createDeliveryOrder_allowsCreditLimitEquality() {
        stubSuccessfulCreate(new BigDecimal("100.00"));

        DeliveryOrderResponse response = service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getReservedQty()).isEqualByComparingTo("10.00");
        verify(deliveryOrderRepository).save(any(DeliveryOrder.class));
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
    void saveDeliveryOrderPickingPlan_autoBuildsFifoAllocationsWhenRequestIsEmpty() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.NEW);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(ZERO);
        reservation.setReservedQty(new BigDecimal("10.00"));
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of());
        when(inventoryRepository.findValidFifoCandidates(20L, 30L)).thenReturn(List.of(inventory));
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
        verify(deliveryOrderRepository).save(any(DeliveryOrder.class));
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
    void cancelDeliveryOrder_rejectsNonWarehouseManager() {
        assertThatThrownBy(() -> service.cancelDeliveryOrder(100L, cancelRequest(), planner))
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
        when(entityManager.getReference(WarehouseLocation.class, 32L)).thenReturn(zone2);

        DeliveryOrderResponse response = service.saveDeliveryOrderReplacementPlan(100L, replacementPlanRequest(), storekeeper);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        assertThat(replacementInventory.getReservedQty()).isEqualByComparingTo("2.00");
        assertThat(response.getItems().get(0).getAllocations()).hasSize(2);
        assertThat(response.getItems().get(0).getAllocations().get(1).isReplacement()).isTrue();
    }

    @Test
    void saveDeliveryOrderPickQcResult_movesInventoryAndStatus() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.WAITING_PICKING);
        DeliveryOrderItem item = item(order, product, new BigDecimal("10.00"));
        item.setPlannedQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("10.00"));
        inventory.setCostPrice(new BigDecimal("1.50"));
        DeliveryOrderItemAllocation allocation = allocation(900L, item, inventory, zone,
                new BigDecimal("10.00"), ZERO, false);
        WarehouseLocation stagingBin = bin(880L, warehouse, zone);
        WarehouseLocation quarantineZone = zone(91L, warehouse);
        quarantineZone.setIsQuarantine(true);
        WarehouseLocation quarantineBin = bin(990L, warehouse, quarantineZone);
        quarantineBin.setIsQuarantine(true);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(allocation));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L))).thenReturn(List.of());
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(entityManager.find(WarehouseLocation.class, 990L)).thenReturn(quarantineBin);
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 880L)).thenReturn(Optional.empty());
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 71L, 990L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quarantineRecordRepository.save(any(QuarantineRecord.class))).thenAnswer(invocation -> {
            QuarantineRecord record = invocation.getArgument(0);
            record.setId(700L);
            return record;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboundQcRecordRepository.save(any(OutboundQcRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(allocationRepository.save(any(DeliveryOrderItemAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryOrderResponse response = service.saveDeliveryOrderPickQcResult(100L, pickQcResultRequest(), warehouseStaff);

        assertThat(response.getStatus()).isEqualTo(DeliveryOrderStatus.QC_PENDING_APPROVAL);
        assertThat(inventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(inventory.getReservedQty()).isEqualByComparingTo("0.00");
        assertThat(item.getQcPassQty()).isEqualByComparingTo("8.00");
        assertThat(item.getQcFailQty()).isEqualByComparingTo("2.00");
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

        OutboundQcRecord oldRow = new OutboundQcRecord();
        oldRow.setAllocation(oldPassed);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(allocationRepository.findByDeliveryOrderItemDeliveryOrderId(100L)).thenReturn(List.of(oldPassed, replacement));
        when(outboundQcRecordRepository.findByDeliveryOrderIdAndIdempotencyKey(100L, "qc-100")).thenReturn(List.of());
        when(outboundQcRecordRepository.findByAllocationIdIn(List.of(900L, 901L))).thenReturn(List.of(oldRow));
        when(entityManager.find(WarehouseLocation.class, 880L)).thenReturn(stagingBin);
        when(inventoryRepository.findConcreteReservationRowForUpdate(20L, 30L, 72L, 880L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void submitReturnedGoodsCountQc_createsReturnedFlowFromShippedQcPassLines() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        OutboundQcRecord shipped = outboundQcRecord(item, new BigDecimal("8.00"));

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.existsByDeliveryOrderId(100L)).thenReturn(false);
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(100L))).thenReturn(List.of(shipped));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> {
            ReturnedDeliveryFlow flow = invocation.getArgument(0);
            flow.setId(300L);
            return flow;
        });

        ReturnedGoodsFlowResponse response = service.submitReturnedGoodsCountQc(
                100L, returnedCountQcRequest(new BigDecimal("8.00"), ReturnedGoodsQualityResult.PASSED),
                warehouseStaff);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getExpectedQty()).isEqualByComparingTo("8.00");
        assertThat(response.getItems().get(0).getCountedQty()).isEqualByComparingTo("8.00");
    }

    @Test
    void approveReturnedGoods_blocksQuantityMismatchUntilStaffRechecks() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                new BigDecimal("8.00"), new BigDecimal("7.00"), ReturnedGoodsQualityResult.PASSED, null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));

        assertThatThrownBy(() -> service.approveReturnedGoods(100L, new ReturnedGoodsApprovalRequest(), storekeeper))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("RETURN_QTY_MISMATCH");
    }

    @Test
    void approveReturnedGoods_marksFlowApprovedAfterCompleteCountQc() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), ReturnedGoodsQualityResult.PASSED, null);

        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(returnedDeliveryFlowRepository.findByDeliveryOrderId(100L)).thenReturn(Optional.of(flow));
        when(returnedDeliveryFlowRepository.save(any(ReturnedDeliveryFlow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnedGoodsFlowResponse response = service.approveReturnedGoods(100L, new ReturnedGoodsApprovalRequest(), storekeeper);

        assertThat(response.getFlowStatus()).isEqualTo(ReturnedDeliveryFlowStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.RETURNED);
        assertThat(flow.getApprovedByStorekeeper()).isEqualTo(storekeeper);
    }

    @Test
    void planReturnedGoodsPutaway_setsStorekeeperDestinationLocation() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.APPROVED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), ReturnedGoodsQualityResult.PASSED, null);

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
    void completeReturnedGoodsPutaway_movesInventoryBackAndMarksDeliveryFailed() {
        DeliveryOrder order = order(100L, DeliveryOrderStatus.RETURNED);
        DeliveryOrderItem item = item(order, product, new BigDecimal("8.00"));
        ReturnedDeliveryFlow flow = returnedFlow(order, item, ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED,
                new BigDecimal("8.00"), new BigDecimal("8.00"), ReturnedGoodsQualityResult.PASSED, bin);
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

    private void stubSuccessfulCreate(BigDecimal inventoryAvailable) {
        stubCreateUntilAvailability(inventoryAvailable, reservation.getReservedQty());
        when(deliveryOrderRepository.existsByDoNumber("DO-" + LocalDate.now().toString().replace("-", "") + "-0001"))
                .thenReturn(false);
        when(deliveryOrderRepository.save(any(DeliveryOrder.class))).thenAnswer(invocation -> {
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
        when(priceHistoryService.lookupApproved(30L, 20L, LocalDate.of(2026, 6, 18)))
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
        request.setDocumentDate(LocalDate.of(2026, 6, 18));
        request.setExpectedDeliveryDate(LocalDate.of(2026, 6, 20));
        request.setItems(List.of(item));
        return request;
    }

    private DeliveryOrderCancelRequest cancelRequest() {
        DeliveryOrderCancelRequest request = new DeliveryOrderCancelRequest();
        request.setCancelReason("Customer changed order");
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

    private ReturnedGoodsCountQcRequest returnedCountQcRequest(BigDecimal countedQty,
                                                               ReturnedGoodsQualityResult qualityResult) {
        ReturnedGoodsCountQcItemRequest item = new ReturnedGoodsCountQcItemRequest();
        item.setDoItemId(200L);
        item.setProductId(30L);
        item.setBatchId(71L);
        item.setCountedQty(countedQty);
        item.setQualityResult(qualityResult);
        item.setQualityReason(qualityResult == ReturnedGoodsQualityResult.FAILED ? "Damaged on return" : null);
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
        product.setIsActive(true);
        return product;
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
        return bin;
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

    private ReturnedDeliveryFlow returnedFlow(DeliveryOrder order,
                                              DeliveryOrderItem item,
                                              ReturnedDeliveryFlowStatus status,
                                              BigDecimal expectedQty,
                                              BigDecimal countedQty,
                                              ReturnedGoodsQualityResult qualityResult,
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
        flowItem.setCountedQty(countedQty);
        flowItem.setQualityResult(qualityResult);
        flowItem.setDestinationLocation(destinationLocation);
        flowItem.setPlannedQty(destinationLocation == null ? null : countedQty);
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
        allocation.setCreatedBy(storekeeper);
        allocation.setCreatedAt(OffsetDateTime.now());
        allocation.setUpdatedAt(OffsetDateTime.now());
        return allocation;
    }
}
