package com.wms.service;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.entity.warehouse_transfer.DiscrepancyHoldEntry;
import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferAllocation;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferItem;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.DiscrepancyIncidentRepository;
import com.wms.repository.DiscrepancyHoldEntryRepository;
import com.wms.repository.InterWarehouseTransferAllocationRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.AdjustmentRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_transfer.impl.InterWarehouseTransferHelper;
import com.wms.service.warehouse_transfer.impl.DiscrepancyIncidentServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscrepancyIncidentServiceTest {

    @Mock
    private DiscrepancyIncidentRepository incidentRepository;
    @Mock
    private DiscrepancyHoldEntryRepository holdEntryRepository;
    @Mock
    private InterWarehouseTransferAllocationRepository allocationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WarehouseLocationRepository locationRepository;
    @Mock
    private AdjustmentRepository adjustmentRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PartnerAuditUtil auditUtil;
    @Mock
    private InterWarehouseTransferHelper transferHelper;

    private DiscrepancyIncidentServiceImpl service;
    private User manager;
    private DiscrepancyIncident incident;

    @BeforeEach
    void setUp() {
        service = new DiscrepancyIncidentServiceImpl(
                incidentRepository,
                holdEntryRepository,
                allocationRepository,
                inventoryRepository,
                locationRepository,
                adjustmentRepository,
                auditLogService,
                auditUtil,
                transferHelper
        );
        manager = user(10L, UserRole.WAREHOUSE_MANAGER, "Warehouse Manager");
        incident = incident(99L, 1L, 2L);
    }

    @Test
    void listIncidents_rejectsWarehouseManagerBecauseOnlyCeoCanSeeDiscrepancies() {
        assertThatThrownBy(() -> service.listIncidents("OPEN", manager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DISCREPANCY_INCIDENT_ACCESS_DENIED");
    }

    @Test
    void listIncidents_allowsCeoToSeeAllIncidents() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        when(incidentRepository.findByStatus(eq("OPEN"), any(Sort.class)))
                .thenReturn(List.of(incident));

        List<DiscrepancyIncidentResponse> result = service.listIncidents("OPEN", ceo);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(99L);
        assertThat(result.get(0).destinationWarehouseCode()).isEqualTo("WH-02");
    }

    @Test
    void resolveIncident_updatesOpenIncidentAndWritesAudit() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        incident.setIncidentType("OVER_RECEIPT");
        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(DiscrepancyIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscrepancyIncidentResponse response = service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest(
                        "RESOLVED_DESTINATION_COUNT_ERROR",
                        "Kho đích đếm nhầm phần thừa."
                ),
                ceo
        );

        assertThat(response.status()).isEqualTo("RESOLVED_DESTINATION_COUNT_ERROR");
        assertThat(response.resolutionNote()).isEqualTo("Kho đích đếm nhầm phần thừa.");
        assertThat(response.resolvedById()).isEqualTo(ceo.getId());
        assertThat(response.resolvedAt()).isNotNull();

        ArgumentCaptor<DiscrepancyIncident> savedCaptor = ArgumentCaptor.forClass(DiscrepancyIncident.class);
        verify(incidentRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo("RESOLVED_DESTINATION_COUNT_ERROR");

        verify(auditLogService).log(
                eq(ceo),
                eq(AuditAction.STATUS_CHANGE),
                eq("DISCREPANCY_INCIDENT"),
                eq(99L),
                eq("OVER_RECEIPT-99"),
                eq(2L),
                any(),
                any()
        );
    }

    @Test
    void resolveIncident_rejectsAlreadyResolvedIncident() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        incident.setStatus("RESOLVED_ACCEPTED");
        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest("RESOLVED_SOURCE_FAULT", "Đã xử lý"),
                ceo
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DISCREPANCY_INCIDENT_NOT_OPEN");

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolveOverReceiptAsSourceFault_deductsSourceAndReleasesHoldToDestination() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        incident.setIncidentType("OVER_RECEIPT");
        incident.setQuantity(BigDecimal.valueOf(100));
        Batch batch = new Batch();
        batch.setId(77L);
        WarehouseLocation sourceLocation = location(11L);
        WarehouseLocation destinationLocation = location(22L);
        Inventory sourceInventory = inventory(501L, incident.getTransfer().getSourceWarehouse(),
                incident.getProduct(), batch, sourceLocation, BigDecimal.valueOf(4900));
        Inventory destinationInventory = inventory(502L, incident.getTransfer().getDestinationWarehouse(),
                incident.getProduct(), batch, destinationLocation, BigDecimal.ZERO);
        DiscrepancyHoldEntry hold = DiscrepancyHoldEntry.builder()
                .id(1L)
                .incident(incident)
                .warehouse(incident.getTransfer().getDestinationWarehouse())
                .product(incident.getProduct())
                .batch(batch)
                .holdLocation(destinationLocation)
                .holdQty(BigDecimal.valueOf(100))
                .build();

        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(holdEntryRepository.findByIncidentId(99L)).thenReturn(List.of(hold));
        when(inventoryRepository.findReservableForUpdate(1L, 7L)).thenReturn(List.of(sourceInventory));
        when(inventoryRepository.findByStockKeyForUpdate(2L, 7L, 77L, 22L))
                .thenReturn(Optional.of(destinationInventory));
        when(incidentRepository.save(any(DiscrepancyIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest("RESOLVED_SOURCE_FAULT", "Kho nguồn giao thừa 100."),
                ceo
        );

        assertThat(sourceInventory.getTotalQty()).isEqualByComparingTo("4800");
        verify(transferHelper).upsertInventory(
                incident.getTransfer().getDestinationWarehouse(),
                incident.getProduct(),
                batch,
                destinationLocation,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO
        );
        verify(adjustmentRepository, times(2)).save(any(Adjustment.class));
    }

    @Test
    void resolveShortageAsSourceFault_returnsMissingQtyToSourceWarehouse() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        incident.setIncidentType("SHORTAGE");
        incident.setQuantity(BigDecimal.valueOf(7));
        Batch batch = new Batch();
        batch.setId(77L);
        WarehouseLocation sourceLocation = location(11L);
        Inventory sourceInventory = inventory(501L, incident.getTransfer().getSourceWarehouse(),
                incident.getProduct(), batch, sourceLocation, BigDecimal.valueOf(5893));
        InterWarehouseTransferItem item = InterWarehouseTransferItem.builder()
                .id(5L)
                .transfer(incident.getTransfer())
                .product(incident.getProduct())
                .build();
        InterWarehouseTransferAllocation allocation = InterWarehouseTransferAllocation.builder()
                .id(6L)
                .transferItem(item)
                .inventory(sourceInventory)
                .allocatedQty(BigDecimal.valueOf(97))
                .build();

        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(transferHelper.items(incident.getTransfer())).thenReturn(List.of(item));
        when(allocationRepository.findByTransferItemId(5L)).thenReturn(List.of(allocation));
        when(incidentRepository.save(any(DiscrepancyIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest("RESOLVED_SOURCE_FAULT", "Kho nguồn chưa giao đủ 7."),
                ceo
        );

        assertThat(sourceInventory.getTotalQty()).isEqualByComparingTo("5900");
        verify(inventoryRepository).save(sourceInventory);
        verify(locationRepository).save(sourceLocation);
        verify(adjustmentRepository).save(any(Adjustment.class));
        verify(transferHelper, never()).upsertInventory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void resolveShortageAsDestinationCountError_addsMissingQtyToDestination() {
        User ceo = user(20L, UserRole.CEO, "CEO");
        incident.setIncidentType("SHORTAGE");
        incident.setQuantity(BigDecimal.valueOf(4));
        Batch batch = new Batch();
        batch.setId(77L);
        WarehouseLocation sourceLocation = location(11L);
        WarehouseLocation destinationLocation = location(22L);
        Inventory sourceInventory = inventory(501L, incident.getTransfer().getSourceWarehouse(),
                incident.getProduct(), batch, sourceLocation, BigDecimal.valueOf(4900));
        Inventory destinationInventory = inventory(502L, incident.getTransfer().getDestinationWarehouse(),
                incident.getProduct(), batch, destinationLocation, BigDecimal.valueOf(96));
        InterWarehouseTransferItem item = InterWarehouseTransferItem.builder()
                .id(5L)
                .transfer(incident.getTransfer())
                .product(incident.getProduct())
                .destinationLocation(destinationLocation)
                .build();
        InterWarehouseTransferAllocation allocation = InterWarehouseTransferAllocation.builder()
                .id(6L)
                .transferItem(item)
                .inventory(sourceInventory)
                .allocatedQty(BigDecimal.valueOf(100))
                .build();

        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(transferHelper.items(incident.getTransfer())).thenReturn(List.of(item));
        when(allocationRepository.findByTransferItemId(5L)).thenReturn(List.of(allocation));
        when(inventoryRepository.findByStockKeyForUpdate(2L, 7L, 77L, 22L))
                .thenReturn(Optional.of(destinationInventory));
        when(incidentRepository.save(any(DiscrepancyIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest("RESOLVED_DESTINATION_COUNT_ERROR", "Kho đích đếm thiếu 4."),
                ceo
        );

        verify(transferHelper).upsertInventory(
                incident.getTransfer().getDestinationWarehouse(),
                incident.getProduct(),
                batch,
                destinationLocation,
                BigDecimal.valueOf(4),
                BigDecimal.ZERO
        );
        verify(adjustmentRepository).save(any(Adjustment.class));
    }

    private DiscrepancyIncident incident(Long id, Long sourceId, Long destinationId) {
        Warehouse source = warehouse(sourceId);
        Warehouse destination = warehouse(destinationId);
        Product product = new Product();
        product.setId(7L);
        product.setSku("SKU-PA-007");
        product.setName("Chảo chống dính");

        InterWarehouseTransfer transfer = new InterWarehouseTransfer();
        transfer.setId(12L);
        transfer.setTransferNumber("TRF-20260728-0012");
        transfer.setSourceWarehouse(source);
        transfer.setDestinationWarehouse(destination);

        return DiscrepancyIncident.builder()
                .id(id)
                .transfer(transfer)
                .product(product)
                .incidentType("SHORTAGE")
                .quantity(BigDecimal.valueOf(10))
                .status("OPEN")
                .resolutionNote("Thiếu khi nhận hàng")
                .createdAt(OffsetDateTime.now().minusHours(2))
                .updatedAt(OffsetDateTime.now().minusHours(2))
                .build();
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setCode(String.format("WH-%02d", id));
        return warehouse;
    }

    private WarehouseLocation location(Long id) {
        WarehouseLocation location = new WarehouseLocation();
        location.setId(id);
        location.setCurrentVolumeM3(BigDecimal.ZERO);
        location.setCurrentWeightKg(BigDecimal.ZERO);
        return location;
    }

    private Inventory inventory(Long id, Warehouse warehouse, Product product, Batch batch,
                                WarehouseLocation location, BigDecimal qty) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setLocation(location);
        inventory.setTotalQty(qty);
        inventory.setReservedQty(BigDecimal.ZERO);
        inventory.setCostPrice(BigDecimal.ZERO);
        return inventory;
    }

    private User user(Long id, UserRole role, String name) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(name);
        return user;
    }
}
