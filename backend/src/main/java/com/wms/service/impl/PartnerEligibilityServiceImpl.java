package com.wms.service.impl;

import com.wms.entity.Dealer;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.DealerRepository;
import com.wms.service.PartnerEligibilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerEligibilityServiceImpl implements PartnerEligibilityService {

    private final DealerRepository dealerRepository;

    public PartnerEligibilityServiceImpl(DealerRepository dealerRepository) {
        this.dealerRepository = dealerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureDealerActive(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + dealerId));
        if (!Boolean.TRUE.equals(dealer.getIsActive())) {
            throw new BusinessRuleViolationException("Dealer is inactive");
        }
    }
}
