package com.wms.service;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.DiscrepancyIncidentRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_transfer.impl.DiscrepancyIncidentServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscrepancyIncidentServiceTest {

    @Mock
    private DiscrepancyIncidentRepository incidentRepository;
    @Mock
    private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock
    private AuditLogService auditLogService;

    private DiscrepancyIncidentServiceImpl service;
    private User manager;
    private DiscrepancyIncident incident;

    @BeforeEach
    void setUp() {
        service = new DiscrepancyIncidentServiceImpl(
                incidentRepository,
                assignmentRepository,
                auditLogService
        );
        manager = user(10L, UserRole.WAREHOUSE_MANAGER, "Warehouse Manager");
        incident = incident(99L, 1L, 2L);
    }

    @Test
    void listIncidents_filtersWarehouseManagerScope() {
        DiscrepancyIncident outOfScope = incident(100L, 3L, 4L);
        when(incidentRepository.findByStatus(eq("OPEN"), any(Sort.class)))
                .thenReturn(List.of(incident, outOfScope));
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId()))
                .thenReturn(List.of(2L));

        List<DiscrepancyIncidentResponse> result = service.listIncidents("OPEN", manager);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(99L);
        assertThat(result.get(0).destinationWarehouseCode()).isEqualTo("WH-02");
    }

    @Test
    void resolveIncident_updatesOpenIncidentAndWritesAudit() {
        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId()))
                .thenReturn(List.of(2L));
        when(incidentRepository.save(any(DiscrepancyIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscrepancyIncidentResponse response = service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest(
                        "RESOLVED_CARRIER_FAULT",
                        "Đối chiếu ảnh bàn giao, thiếu do vận chuyển."
                ),
                manager
        );

        assertThat(response.status()).isEqualTo("RESOLVED_CARRIER_FAULT");
        assertThat(response.resolutionNote()).isEqualTo("Đối chiếu ảnh bàn giao, thiếu do vận chuyển.");
        assertThat(response.resolvedById()).isEqualTo(manager.getId());
        assertThat(response.resolvedAt()).isNotNull();

        ArgumentCaptor<DiscrepancyIncident> savedCaptor = ArgumentCaptor.forClass(DiscrepancyIncident.class);
        verify(incidentRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getStatus()).isEqualTo("RESOLVED_CARRIER_FAULT");

        verify(auditLogService).log(
                eq(manager),
                eq(AuditAction.STATUS_CHANGE),
                eq("DISCREPANCY_INCIDENT"),
                eq(99L),
                eq("SHORTAGE-99"),
                eq(2L),
                any(),
                any()
        );
    }

    @Test
    void resolveIncident_rejectsAlreadyResolvedIncident() {
        incident.setStatus("RESOLVED_ACCEPTED");
        when(incidentRepository.findWithDetailsById(99L)).thenReturn(Optional.of(incident));
        when(assignmentRepository.findWarehouseIdsByUserId(manager.getId()))
                .thenReturn(List.of(2L));

        assertThatThrownBy(() -> service.resolveIncident(
                99L,
                new DiscrepancyIncidentResolveRequest("RESOLVED_ACCEPTED", "Đã xử lý"),
                manager
        ))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DISCREPANCY_INCIDENT_NOT_OPEN");

        verify(incidentRepository, never()).save(any());
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

    private User user(Long id, UserRole role, String name) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(name);
        return user;
    }
}
