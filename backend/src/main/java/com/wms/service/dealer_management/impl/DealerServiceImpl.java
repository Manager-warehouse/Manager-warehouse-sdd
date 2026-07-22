package com.wms.service.dealer_management.impl;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.request.dealer_management.DealerCreateRequest;
import com.wms.dto.request.dealer_management.DealerCreditLimitUpdateRequest;
import com.wms.dto.request.dealer_management.DealerCreditStatusUpdateRequest;
import com.wms.dto.request.dealer_management.DealerPaymentTermUpdateRequest;
import com.wms.dto.request.dealer_management.DealerUpdateRequest;
import com.wms.dto.response.dealer_management.DealerResponse;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.user_configuration.SystemConfig;
import com.wms.entity.access_control.User;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.user_configuration.SystemConfigKey;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.dealer_management.DealerMapper;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.service.dealer_management.DealerService;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealerServiceImpl implements DealerService {

    private static final int NET_30 = 30;
    private static final int NET_60 = 60;

    private final DealerRepository dealerRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final DealerMapper dealerMapper;
    private final PartnerAuditUtil auditUtil;

    public DealerServiceImpl(DealerRepository dealerRepository,
                             SystemConfigRepository systemConfigRepository,
                             DealerMapper dealerMapper,
                             PartnerAuditUtil auditUtil) {
        this.dealerRepository = dealerRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.dealerMapper = dealerMapper;
        this.auditUtil = auditUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealerResponse> getAllDealers() {
        return dealerRepository.findAll().stream()
                .map(dealerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DealerResponse getDealerById(Long id) {
        return dealerMapper.toResponse(findDealer(id));
    }

    @Override
    @Transactional
    public DealerResponse createDealer(DealerCreateRequest request, User actor) {
        if (dealerRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Dealer code already exists: " + request.getCode());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Dealer dealer = new Dealer();
        dealer.setCode(request.getCode());
        dealer.setName(request.getName());
        dealer.setPhone(request.getPhone());
        dealer.setEmail(request.getEmail());
        dealer.setDefaultDeliveryAddress(request.getDefaultDeliveryAddress());
        dealer.setRegion(request.getRegion());
        dealer.setPaymentTermDays(resolveIntConfig(SystemConfigKey.DEFAULT_PAYMENT_TERM_DAYS, NET_30));
        dealer.setCreditLimit(resolveDecimalConfig(SystemConfigKey.DEFAULT_CREDIT_LIMIT, BigDecimal.ZERO));
        dealer.setCurrentBalance(BigDecimal.ZERO);
        dealer.setCreditStatus(CreditStatus.ACTIVE);
        dealer.setIsActive(true);
        dealer.setCreatedBy(actor);
        dealer.setUpdatedBy(actor);
        dealer.setCreatedAt(now);
        dealer.setUpdatedAt(now);

        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.CREATE, "DEALER", saved.getId(), saved.getCode(),
                Map.of(), snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updateDealer(Long id, DealerUpdateRequest request, User actor) {
        Dealer dealer = findDealer(id);
        Map<String, Object> before = snapshot(dealer);
        if (request.getName() != null) {
            dealer.setName(request.getName());
        }
        if (request.getPhone() != null) {
            dealer.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            dealer.setEmail(request.getEmail());
        }
        if (request.getDefaultDeliveryAddress() != null) {
            dealer.setDefaultDeliveryAddress(request.getDefaultDeliveryAddress());
        }
        if (request.getRegion() != null) {
            dealer.setRegion(request.getRegion());
        }
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.UPDATE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateDealer(Long id, User actor) {
        Dealer dealer = findDealer(id);
        Map<String, Object> before = snapshot(dealer);
        dealer.setIsActive(false);
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.SOFT_DELETE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
    }

    @Override
    @Transactional
    public DealerResponse reactivateDealer(Long id, User actor) {
        Dealer dealer = findDealer(id);
        Map<String, Object> before = snapshot(dealer);
        dealer.setIsActive(true);
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.UPDATE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updateCreditLimit(Long id, DealerCreditLimitUpdateRequest request, User actor) {
        Dealer dealer = findDealer(id);
        BigDecimal newLimit = request.getCreditLimit();
        if (newLimit.compareTo(BigDecimal.ZERO) <= 0 ||
                newLimit.compareTo(dealer.getCurrentBalance()) <= 0) {
            throw new BusinessRuleViolationException("Credit limit must be positive and greater than current balance");
        }
        Map<String, Object> before = snapshot(dealer);
        dealer.setCreditLimit(newLimit);
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.UPDATE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updatePaymentTerm(Long id, DealerPaymentTermUpdateRequest request, User actor) {
        Integer paymentTermDays = request.getPaymentTermDays();
        if (paymentTermDays == null || (paymentTermDays != NET_30 && paymentTermDays != NET_60)) {
            throw new BusinessRuleViolationException("Payment term must be 30 or 60");
        }
        Dealer dealer = findDealer(id);
        Map<String, Object> before = snapshot(dealer);
        dealer.setPaymentTermDays(paymentTermDays);
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.UPDATE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DealerResponse updateCreditStatus(Long id, DealerCreditStatusUpdateRequest request, User actor) {
        Dealer dealer = findDealer(id);
        if (request.getCreditStatus() == CreditStatus.ACTIVE) {
            requireUnlockEligible(dealer);
        }
        Map<String, Object> before = snapshot(dealer);
        dealer.setCreditStatus(request.getCreditStatus());
        dealer.setUpdatedBy(actor);
        dealer.setUpdatedAt(OffsetDateTime.now());
        Dealer saved = dealerRepository.save(dealer);
        auditUtil.logChange(actor, AuditAction.STATUS_CHANGE, "DEALER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return dealerMapper.toResponse(saved);
    }

    // Manual unlock to ACTIVE must satisfy the same balance condition as the automatic unlock
    // (CRD-03, business.md): current balance below the credit-limit buffer. Locking to
    // CREDIT_HOLD has no precondition — a manager can always tighten credit.
    private void requireUnlockEligible(Dealer dealer) {
        BigDecimal currentBalance = dealer.getCurrentBalance() != null ? dealer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal bufferPct = resolveDecimalConfig(SystemConfigKey.CREDIT_UNLOCK_BUFFER_PCT, new BigDecimal("0.8"));
        BigDecimal unlockThreshold = creditLimit.multiply(bufferPct);
        if (currentBalance.compareTo(unlockThreshold) >= 0) {
            throw new BusinessRuleViolationException("Cannot unlock dealer: current balance " + currentBalance
                    + " is not below the unlock threshold " + unlockThreshold);
        }
    }

    private Dealer findDealer(Long id) {
        return dealerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + id));
    }

    private int resolveIntConfig(SystemConfigKey key, int fallback) {
        return systemConfigRepository.findByConfigKey(key.name())
                .map(SystemConfig::getConfigValue)
                .map(Integer::parseInt)
                .orElse(fallback);
    }

    private BigDecimal resolveDecimalConfig(SystemConfigKey key, BigDecimal fallback) {
        return systemConfigRepository.findByConfigKey(key.name())
                .map(SystemConfig::getConfigValue)
                .map(BigDecimal::new)
                .orElse(fallback);
    }

    private Map<String, Object> snapshot(Dealer dealer) {
        return PartnerAuditUtil.values(
                "code", dealer.getCode(),
                "name", dealer.getName(),
                "phone", dealer.getPhone(),
                "email", dealer.getEmail(),
                "defaultDeliveryAddress", dealer.getDefaultDeliveryAddress(),
                "region", dealer.getRegion(),
                "paymentTermDays", dealer.getPaymentTermDays(),
                "creditLimit", dealer.getCreditLimit(),
                "currentBalance", dealer.getCurrentBalance(),
                "creditStatus", dealer.getCreditStatus(),
                "isActive", dealer.getIsActive());
    }
}
