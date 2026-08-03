package com.wms.mapper.dealer_management;


import com.wms.dto.response.dealer_management.DealerResponse;
import com.wms.entity.dealer_management.Dealer;
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
