package com.wms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestItemRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.TransferRequestStatus;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.transfer.InterWarehouseTransferService;
import com.wms.service.transfer.impl.TransferRequestServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferRequestServiceImplTest {

    @Mock private TransferRequestRepository requestRepository;
    @Mock private TransferRequestItemRepository requestItemRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private InterWarehouseTransferService transferService;
    @Mock private PartnerAuditUtil auditUtil;

    @InjectMocks private TransferRequestServiceImpl service;

    private User manager;
    private User ceo;
    private User planner;
    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;
    private Product product;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);
        manager.setFullName("Manager A");

        ceo = new User();
        ceo.setId(2L);
        ceo.setRole(UserRole.CEO);
        ceo.setFullName("CEO B");

        planner = new User();
        planner.setId(3L);
        planner.setRole(UserRole.PLANNER);
        planner.setFullName("Planner C");

        sourceWarehouse = new Warehouse();
        sourceWarehouse.setId(10L);
        sourceWarehouse.setCode("HP-01");
        sourceWarehouse.setName("Hai Phong Warehouse");

        destinationWarehouse = new Warehouse();
        destinationWarehouse.setId(20L);
        destinationWarehouse.setCode("HN-01");
        destinationWarehouse.setName("Ha Noi Warehouse");

        product = new Product();
        product.setId(100L);
        product.setSku("SKU-PROD-1");
        product.setName("Product 1");

        request = new TransferRequest();
        request.setId(500L);
        request.setRequestNumber("TRQ-20260628-0001");
        request.setSourceWarehouse(sourceWarehouse);
        request.setDestinationWarehouse(destinationWarehouse);
        request.setStatus(TransferRequestStatus.DRAFT);
        request.setCreatedBy(manager);
        request.setCreatedAt(OffsetDateTime.now());

        List<TransferRequestItem> items = new ArrayList<>();
        TransferRequestItem item = new TransferRequestItem();
        item.setId(501L);
        item.setTransferRequest(request);
        item.setProduct(product);
        item.setRequestedQty(new BigDecimal("10.00"));
        items.add(item);
        request.setItems(items);
    }

    @Test
    void createTransferRequest_success() {
        TransferRequestItemRequest itemReq = new TransferRequestItemRequest(product.getId(), new BigDecimal("10.00"));

        TransferRequestCreateRequest createReq = new TransferRequestCreateRequest(
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                "Pls approve",
                List.of(itemReq)
        );

        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        when(warehouseRepository.findById(sourceWarehouse.getId())).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepository.findById(destinationWarehouse.getId())).thenReturn(Optional.of(destinationWarehouse));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(requestItemRepository.save(any(TransferRequestItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(request);

        TransferRequestResponse response = service.createRequest(createReq, manager);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(TransferRequestStatus.DRAFT);
        assertThat(response.sourceWarehouseId()).isEqualTo(sourceWarehouse.getId());
        verify(requestRepository, times(1)).save(any(TransferRequest.class));
    }

    @Test
    void createTransferRequest_failsIfNoDestinationScope() {
        TransferRequestCreateRequest createReq = new TransferRequestCreateRequest(
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                "Outside scope",
                List.of()
        );

        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(sourceWarehouse.getId())); // Not assigned to destination

        assertThatThrownBy(() -> service.createRequest(createReq, manager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");
    }

    @Test
    void submitRequest_success() {
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId())).thenReturn(List.of(destinationWarehouse.getId()));
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(request);

        TransferRequestResponse response = service.submitRequest(request.getId(), manager);

        assertThat(response.status()).isEqualTo(TransferRequestStatus.SUBMITTED);
        verify(requestRepository, times(1)).save(request);
    }

    @Test
    void approveRequest_successByCeo() {
        request.setStatus(TransferRequestStatus.SUBMITTED);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(request);

        TransferRequestResponse response = service.approveRequest(request.getId(), ceo);

        assertThat(response.status()).isEqualTo(TransferRequestStatus.APPROVED);
        assertThat(request.getApprovedBy()).isEqualTo(ceo);
        assertThat(request.getApprovedAt()).isNotNull();
    }

    @Test
    void rejectRequest_successByCeo() {
        request.setStatus(TransferRequestStatus.SUBMITTED);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(request);

        TransferRequestRejectRequest rejectReq = new TransferRequestRejectRequest("Not needed now");

        TransferRequestResponse response = service.rejectRequest(request.getId(), rejectReq, ceo);

        assertThat(response.status()).isEqualTo(TransferRequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("Not needed now");
    }

    @Test
    void convertToTransfer_successByPlanner() {
        request.setStatus(TransferRequestStatus.APPROVED);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        
        InterWarehouseTransferResponse transferResponse = new InterWarehouseTransferResponse(
                88L, "TRF-20260628-8888", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, false, false, null,
                null, null, null, null, false, null, null, List.of()
        );
        when(transferService.createTransfer(any(InterWarehouseTransferCreateRequest.class), eq(planner)))
                .thenReturn(transferResponse);
        
        when(requestRepository.save(any(TransferRequest.class))).thenReturn(request);

        TransferRequestResponse response = service.convertToTransfer(request.getId(), planner);

        assertThat(response.status()).isEqualTo(TransferRequestStatus.CONVERTED);
        verify(transferService, times(1)).createTransfer(any(InterWarehouseTransferCreateRequest.class), eq(planner));
    }

    @Test
    void stockLookup_success() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.sumValidAvailableQty(sourceWarehouse.getId(), product.getId()))
                .thenReturn(new BigDecimal("50.00"));
        when(warehouseRepository.findAll()).thenReturn(List.of(sourceWarehouse, destinationWarehouse));

        List<WarehouseStockLookupResponse> stock = service.stockLookup(product.getId(), manager);

        assertThat(stock).hasSize(2);
        WarehouseStockLookupResponse hpStock = stock.stream()
                .filter(s -> s.warehouseId().equals(sourceWarehouse.getId()))
                .findFirst().orElseThrow();
        assertThat(hpStock.availableQty()).isEqualByComparingTo("50.00");
    }
}
