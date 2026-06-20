package com.wms.dto.response;

import com.wms.enums.CreditStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DealerResponse {
    private Long id;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String defaultDeliveryAddress;
    private String region;
    private Integer paymentTermDays;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private CreditStatus creditStatus;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
