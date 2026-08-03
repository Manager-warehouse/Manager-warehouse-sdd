package com.wms.service;

import com.wms.dto.request.supplier_management.SupplierCreateRequest;
import com.wms.dto.response.supplier_management.SupplierResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.supplier_management.Supplier;
import com.wms.mapper.supplier_management.SupplierMapper;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.supplier_management.impl.SupplierServiceImpl;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private PartnerAuditUtil auditUtil;

    private SupplierServiceImpl supplierService;
    private User actor;

    @BeforeEach
    void setUp() {
        // Real mapper on purpose: this test also guards that SupplierResponse actually
        // carries currentBalance through, not just that the entity field gets set.
        supplierService = new SupplierServiceImpl(
                supplierRepository, receiptRepository, new SupplierMapper(), auditUtil);
        actor = new User();
        actor.setId(1L);
    }

    @Test
    @DisplayName("Tạo NCC mới khởi tạo dư nợ hiện tại bằng 0, không để null")
    void createSupplier_initializesCurrentBalanceToZero() {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setCode("SUP-NEW");
        request.setCompanyName("Nha Cung Cap Moi");

        when(supplierRepository.existsByCode("SUP-NEW")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> {
            Supplier s = i.getArgument(0);
            s.setId(99L);
            return s;
        });

        SupplierResponse response = supplierService.createSupplier(request, actor);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        org.mockito.Mockito.verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
