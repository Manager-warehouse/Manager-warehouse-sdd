package com.wms.service;

import com.wms.dto.request.supplier_management.SupplierCreateRequest;
import com.wms.dto.request.supplier_management.SupplierUpdateRequest;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.supplier_management.SupplierReceivedOrderResponse;
import com.wms.dto.response.supplier_management.SupplierResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.supplier_management.SupplierMapper;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.supplier_management.impl.SupplierServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private PartnerAuditUtil auditUtil;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private Supplier supplier;
    private User actor;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setFullName("Test Admin");

        supplier = new Supplier();
        supplier.setId(10L);
        supplier.setCode("SUP-001");
        supplier.setCompanyName("Supplier Alpha");
        supplier.setIsActive(true);
    }

    @Test
    void getAllSuppliers_shouldReturnMappedList() {
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));
        SupplierResponse resp = SupplierResponse.builder().id(10L).code("SUP-001").build();
        when(supplierMapper.toResponse(supplier)).thenReturn(resp);

        List<SupplierResponse> result = supplierService.getAllSuppliers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("SUP-001");
    }

    @Test
    void getSupplierById_whenFound_shouldReturnResponse() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        SupplierResponse resp = SupplierResponse.builder().id(10L).build();
        when(supplierMapper.toResponse(supplier)).thenReturn(resp);

        SupplierResponse result = supplierService.getSupplierById(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getSupplierById_whenNotFound_shouldThrowException() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier not found");
    }

    @Test
    void createSupplier_whenCodeExists_shouldThrowDuplicateException() {
        SupplierCreateRequest req = new SupplierCreateRequest();
        req.setCode("SUP-001");
        when(supplierRepository.existsByCode("SUP-001")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(req, actor))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createSupplier_whenValid_shouldSaveAndAudit() {
        SupplierCreateRequest req = new SupplierCreateRequest();
        req.setCode("SUP-NEW");
        req.setCompanyName("New Supplier");

        when(supplierRepository.existsByCode("SUP-NEW")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });

        SupplierResponse resp = SupplierResponse.builder().id(11L).code("SUP-NEW").build();
        when(supplierMapper.toResponse(any(Supplier.class))).thenReturn(resp);

        SupplierResponse result = supplierService.createSupplier(req, actor);

        assertThat(result.getId()).isEqualTo(11L);
        verify(supplierRepository).save(any(Supplier.class));
        verify(auditUtil).logChange(eq(actor), any(), eq("SUPPLIER"), eq(11L), eq("SUP-NEW"), any(), any());
    }

    @Test
    void updateSupplier_whenValid_shouldUpdateAndAudit() {
        SupplierUpdateRequest req = new SupplierUpdateRequest();
        req.setCompanyName("Updated Name");

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        SupplierResponse resp = SupplierResponse.builder().id(10L).companyName("Updated Name").build();
        when(supplierMapper.toResponse(supplier)).thenReturn(resp);

        SupplierResponse result = supplierService.updateSupplier(10L, req, actor);

        assertThat(result.getCompanyName()).isEqualTo("Updated Name");
        verify(auditUtil).logChange(eq(actor), any(), eq("SUPPLIER"), eq(10L), eq("SUP-001"), any(), any());
    }

    @Test
    void deactivateSupplier_shouldSetIsActiveFalse() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        supplierService.deactivateSupplier(10L, actor);

        assertThat(supplier.getIsActive()).isFalse();
        verify(auditUtil).logChange(eq(actor), any(), eq("SUPPLIER"), eq(10L), eq("SUP-001"), any(), any());
    }

    @Test
    void reactivateSupplier_shouldSetIsActiveTrue() {
        supplier.setIsActive(false);
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        supplierService.reactivateSupplier(10L, actor);

        assertThat(supplier.getIsActive()).isTrue();
        verify(auditUtil).logChange(eq(actor), any(), eq("SUPPLIER"), eq(10L), eq("SUP-001"), any(), any());
    }

    @Test
    void getReceivedOrders_shouldReturnOrderList() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));

        Receipt receipt = new Receipt();
        receipt.setId(100L);
        receipt.setReceiptNumber("REC-001");
        receipt.setDocumentDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setType(ReceiptType.PURCHASE);

        when(receiptRepository.findBySupplierIdOrderByDocumentDateDescCreatedAtDesc(10L))
                .thenReturn(List.of(receipt));

        List<SupplierReceivedOrderResponse> result = supplierService.getReceivedOrders(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocumentNumber()).isEqualTo("REC-001");
    }

    @Test
    void getReceivedOrderDetail_whenFound_shouldReturnDetail() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));

        Receipt receipt = new Receipt();
        receipt.setId(100L);
        receipt.setReceiptNumber("REC-001");
        receipt.setDocumentDate(LocalDate.now());
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setType(ReceiptType.PURCHASE);

        when(receiptRepository.findByIdAndSupplierId(100L, 10L)).thenReturn(Optional.of(receipt));

        SupplierReceivedOrderDetailResponse result = supplierService.getReceivedOrderDetail(10L, 100L);

        assertThat(result.getDocumentNumber()).isEqualTo("REC-001");
    }
}
