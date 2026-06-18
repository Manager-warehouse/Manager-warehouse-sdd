package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.PriceHistory;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseProductReservation;
import com.wms.enums.CreditStatus;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.PriceHistoryStatus;
import com.wms.enums.UserRole;
import com.wms.exception.OutboundDeliveryException;
import com.wms.mapper.DeliveryOrderMapper;
import com.wms.repository.DealerRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseProductReservationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.DeliveryOrderServiceImpl;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryOrderServiceImplTest {

    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private WarehouseProductReservationRepository reservationRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private PartnerEligibilityService partnerEligibilityService;
    @Mock private PartnerAuditUtil auditUtil;
    @Mock private EntityManager entityManager;

    private DeliveryOrderServiceImpl service;
    private User planner;
    private User manager;
    private Dealer dealer;
    private Warehouse warehouse;
    private Product product;
    private PriceHistory price;
    private WarehouseProductReservation reservation;

    @BeforeEach
    void setUp() {
        service = new DeliveryOrderServiceImpl(deliveryOrderRepository, deliveryOrderItemRepository,
                dealerRepository, warehouseRepository, productRepository, inventoryRepository,
                invoiceRepository, priceHistoryRepository, reservationRepository, assignmentRepository,
                partnerEligibilityService, new DeliveryOrderMapper(), auditUtil, entityManager);
        planner = user(1L, UserRole.PLANNER);
        manager = user(2L, UserRole.WAREHOUSE_MANAGER);
        dealer = dealer(10L, new BigDecimal("480.00"), new BigDecimal("500.00"), CreditStatus.ACTIVE);
        warehouse = warehouse(20L, "HP");
        product = product(30L);
        price = price(product, new BigDecimal("2.00"));
        reservation = reservation(warehouse, product, new BigDecimal("5.00"));
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
    void createDeliveryOrder_incrementsWarehouseProductReservation() {
        stubSuccessfulCreate(new BigDecimal("100.00"));

        service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner);

        assertThat(reservation.getReservedQty()).isEqualByComparingTo("15.00");
        verify(reservationRepository).save(reservation);
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
        stubCreateUntilCredit();
        when(invoiceRepository.existsByDealerIdAndStatusInAndDueDateBefore(
                eq(10L), eq(List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID)), any(LocalDate.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("CREDIT_HOLD");
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
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INSUFFICIENT_STOCK");
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void createDeliveryOrder_rejectsWhenValidInventoryAvailabilityIsInsufficient() {
        stubCreateUntilAvailability(new BigDecimal("9.00"), BigDecimal.ZERO);
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createDeliveryOrder(validRequest(new BigDecimal("10.00")), planner))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("INSUFFICIENT_STOCK");
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
        when(reservationRepository.findByWarehouseIdAndProductId(20L, 30L)).thenReturn(Optional.of(reservation));
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
        when(reservationRepository.findByWarehouseIdAndProductId(20L, 30L)).thenReturn(Optional.of(reservation));
    }

    private void stubCreateUntilCredit() {
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(productRepository.findByIdAndIsActiveTrue(30L)).thenReturn(Optional.of(product));
        when(priceHistoryRepository.findEffectivePrices(30L, PriceHistoryStatus.APPROVED, LocalDate.of(2026, 6, 18)))
                .thenReturn(List.of(price));
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
}
