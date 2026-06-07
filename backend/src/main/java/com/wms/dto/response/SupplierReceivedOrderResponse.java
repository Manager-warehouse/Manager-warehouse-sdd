package com.wms.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SupplierReceivedOrderResponse {
    private Long id;
    private String documentNumber;
    private LocalDate documentDate;
    private String status;
    private String sourceType;
}
