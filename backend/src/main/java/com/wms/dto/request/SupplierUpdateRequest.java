package com.wms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierUpdateRequest {

    @Size(max = 255)
    private String companyName;

    @Size(max = 20)
    private String taxCode;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String contactPerson;

    private String address;
}
