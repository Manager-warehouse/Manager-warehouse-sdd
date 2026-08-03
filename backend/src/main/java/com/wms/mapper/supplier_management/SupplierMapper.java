package com.wms.mapper.supplier_management;


import com.wms.dto.response.supplier_management.SupplierResponse;
import com.wms.entity.supplier_management.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {
    public SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .companyName(supplier.getCompanyName())
                .taxCode(supplier.getTaxCode())
                .phone(supplier.getPhone())
                .contactPerson(supplier.getContactPerson())
                .address(supplier.getAddress())
                .isActive(supplier.getIsActive())
                .currentBalance(supplier.getCurrentBalance())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
