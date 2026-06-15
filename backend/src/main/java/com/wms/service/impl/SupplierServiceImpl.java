package com.wms.service.impl;

import com.wms.dto.request.SupplierCreateRequest;
import com.wms.dto.request.SupplierUpdateRequest;
import com.wms.dto.response.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.SupplierReceivedOrderResponse;
import com.wms.dto.response.SupplierResponse;
import com.wms.entity.Receipt;
import com.wms.entity.Supplier;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.SupplierMapper;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.SupplierRepository;
import com.wms.service.SupplierService;
import com.wms.util.PartnerAuditUtil;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ReceiptRepository receiptRepository;
    private final SupplierMapper supplierMapper;
    private final PartnerAuditUtil auditUtil;

    public SupplierServiceImpl(SupplierRepository supplierRepository,
                               ReceiptRepository receiptRepository,
                               SupplierMapper supplierMapper,
                               PartnerAuditUtil auditUtil) {
        this.supplierRepository = supplierRepository;
        this.receiptRepository = receiptRepository;
        this.supplierMapper = supplierMapper;
        this.auditUtil = auditUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        return supplierMapper.toResponse(findSupplier(id));
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierCreateRequest request, User actor) {
        if (supplierRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Supplier code already exists: " + request.getCode());
        }

        OffsetDateTime now = OffsetDateTime.now();
        Supplier supplier = new Supplier();
        supplier.setCode(request.getCode());
        supplier.setCompanyName(request.getCompanyName());
        supplier.setTaxCode(request.getTaxCode());
        supplier.setPhone(request.getPhone());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setAddress(request.getAddress());
        supplier.setIsActive(true);
        supplier.setCreatedBy(actor);
        supplier.setUpdatedBy(actor);
        supplier.setCreatedAt(now);
        supplier.setUpdatedAt(now);

        Supplier saved = supplierRepository.save(supplier);
        auditUtil.logChange(actor, AuditAction.CREATE, "SUPPLIER", saved.getId(), saved.getCode(),
                Map.of(), snapshot(saved));
        return supplierMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierUpdateRequest request, User actor) {
        Supplier supplier = findSupplier(id);
        Map<String, Object> before = snapshot(supplier);
        if (request.getCompanyName() != null) {
            supplier.setCompanyName(request.getCompanyName());
        }
        if (request.getTaxCode() != null) {
            supplier.setTaxCode(request.getTaxCode());
        }
        if (request.getPhone() != null) {
            supplier.setPhone(request.getPhone());
        }
        if (request.getContactPerson() != null) {
            supplier.setContactPerson(request.getContactPerson());
        }
        if (request.getAddress() != null) {
            supplier.setAddress(request.getAddress());
        }
        supplier.setUpdatedBy(actor);
        supplier.setUpdatedAt(OffsetDateTime.now());

        Supplier saved = supplierRepository.save(supplier);
        auditUtil.logChange(actor, AuditAction.UPDATE, "SUPPLIER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return supplierMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateSupplier(Long id, User actor) {
        Supplier supplier = findSupplier(id);
        Map<String, Object> before = snapshot(supplier);
        supplier.setIsActive(false);
        supplier.setUpdatedBy(actor);
        supplier.setUpdatedAt(OffsetDateTime.now());
        Supplier saved = supplierRepository.save(supplier);
        auditUtil.logChange(actor, AuditAction.SOFT_DELETE, "SUPPLIER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
    }

    @Override
    @Transactional
    public SupplierResponse reactivateSupplier(Long id, User actor) {
        Supplier supplier = findSupplier(id);
        Map<String, Object> before = snapshot(supplier);
        supplier.setIsActive(true);
        supplier.setUpdatedBy(actor);
        supplier.setUpdatedAt(OffsetDateTime.now());
        Supplier saved = supplierRepository.save(supplier);
        auditUtil.logChange(actor, AuditAction.UPDATE, "SUPPLIER", saved.getId(), saved.getCode(),
                before, snapshot(saved));
        return supplierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierReceivedOrderResponse> getReceivedOrders(Long supplierId) {
        findSupplier(supplierId);
        return receiptRepository.findBySupplierIdOrderByDocumentDateDescCreatedAtDesc(supplierId)
                .stream()
                .map(this::toReceivedOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierReceivedOrderDetailResponse getReceivedOrderDetail(Long supplierId, Long orderId) {
        findSupplier(supplierId);
        Receipt receipt = receiptRepository.findByIdAndSupplierId(orderId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Received order not found with id: " + orderId));
        return SupplierReceivedOrderDetailResponse.builder()
                .id(receipt.getId())
                .documentNumber(receipt.getReceiptNumber())
                .documentDate(receipt.getDocumentDate())
                .status(receipt.getStatus().name())
                .sourceType(receipt.getType().name())
                .sourceOrderCode(receipt.getSourceOrderCode())
                .sourceChannel(receipt.getSourceChannel() != null ? receipt.getSourceChannel().name() : null)
                .contactPerson(receipt.getContactPerson())
                .notes(receipt.getNotes())
                .build();
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private SupplierReceivedOrderResponse toReceivedOrderResponse(Receipt receipt) {
        return SupplierReceivedOrderResponse.builder()
                .id(receipt.getId())
                .documentNumber(receipt.getReceiptNumber())
                .documentDate(receipt.getDocumentDate())
                .status(receipt.getStatus().name())
                .sourceType(receipt.getType().name())
                .build();
    }

    private Map<String, Object> snapshot(Supplier supplier) {
        return PartnerAuditUtil.values(
                "code", supplier.getCode(),
                "companyName", supplier.getCompanyName(),
                "taxCode", supplier.getTaxCode(),
                "phone", supplier.getPhone(),
                "contactPerson", supplier.getContactPerson(),
                "address", supplier.getAddress(),
                "isActive", supplier.getIsActive());
    }
}
