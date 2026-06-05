package com.wms.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SupplierResponse {
    private Long id;
    private String code;
    private String companyName;
    private String taxCode;
    private String phone;
    private String contactPerson;
    private String address;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
