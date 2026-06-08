package com.wms.mapper;

import com.wms.dto.response.SupplierResponse;
import com.wms.entity.Supplier;
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
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
