package com.wms.mapper;

import com.wms.dto.response.DealerResponse;
import com.wms.entity.Dealer;
import org.springframework.stereotype.Component;

@Component
public class DealerMapper {
    public DealerResponse toResponse(Dealer dealer) {
        return DealerResponse.builder()
                .id(dealer.getId())
                .code(dealer.getCode())
                .name(dealer.getName())
                .phone(dealer.getPhone())
                .email(dealer.getEmail())
                .defaultDeliveryAddress(dealer.getDefaultDeliveryAddress())
                .region(dealer.getRegion())
                .bankAccountNumber(dealer.getBankAccountNumber())
                .bankName(dealer.getBankName())
                .paymentTermDays(dealer.getPaymentTermDays())
                .creditLimit(dealer.getCreditLimit())
                .currentBalance(dealer.getCurrentBalance())
                .creditStatus(dealer.getCreditStatus())
                .isActive(dealer.getIsActive())
                .createdAt(dealer.getCreatedAt())
                .updatedAt(dealer.getUpdatedAt())
                .build();
    }
}
