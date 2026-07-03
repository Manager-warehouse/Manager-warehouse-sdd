package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @Size(max = 20)
    private String taxCode;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String contactPerson;

    @Size(max = 1000)
    private String address;
}
