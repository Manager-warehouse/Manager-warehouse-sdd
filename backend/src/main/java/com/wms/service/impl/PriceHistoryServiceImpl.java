package com.wms.service.impl;

import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceHistoryResponse.PreviousApprovedRef;
import com.wms.dto.response.PriceHistoryResponse.UserRef;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.PriceImportResponse.CreatedRow;
import com.wms.dto.response.PriceImportResponse.FailedRow;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.Notification;
import com.wms.entity.PriceHistory;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.PriceHistoryStatus;
import com.wms.enums.UserRole;
import com.wms.exception.PriceHistoryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.NotificationRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.PriceHistoryService;
import com.wms.util.PartnerAuditUtil;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PriceHistoryServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT_DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int EXCEL_MAX_ROWS = 1000;

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PartnerAuditUtil auditUtil;

    public PriceHistoryServiceImpl(PriceHistoryRepository priceHistoryRepository,
                                   ProductRepository productRepository,
                                   WarehouseRepository warehouseRepository,
                                   UserRepository userRepository,
                                   NotificationRepository notificationRepository,
                                   PartnerAuditUtil auditUtil) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.auditUtil = auditUtil;
    }

    @Override
    @Transactional
    public PriceHistoryResponse create(PriceHistoryCreateRequest req, User actor) {
        validateDateRange(req.getEffectiveDate(), req.getEndDate());
        Product product = requireProduct(req.getProductId());
        Warehouse warehouse = requireWarehouse(req.getWarehouseId());
        checkNoApprovedOverlap(product.getId(), warehouse.getId(), req.getEffectiveDate(), req.getEndDate(), null);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PriceHistory ph = PriceHistory.builder()
                .product(product)
                .warehouse(warehouse)
                .effectiveDate(req.getEffectiveDate())
                .endDate(req.getEndDate())
                .costPrice(req.getCostPrice())
                .sellingPrice(req.getSellingPrice())
                .notes(req.getNotes())
                .status(PriceHistoryStatus.PENDING)
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PriceHistory saved = priceHistoryRepository.save(ph);
        notifyAccountantManagers(saved, "Bản giá mới chờ duyệt: " + product.getName());
        auditUtil.logChange(actor, AuditAction.PRICE_CREATE, "PRICE_HISTORY", saved.getId(),
                String.valueOf(saved.getId()), Map.of(), snapshot(saved));
        return toResponse(saved, null);
    }

    @Override
    @Transactional
    public PriceHistoryResponse update(Long id, PriceHistoryUpdateRequest req, User actor) {
        PriceHistory ph = require(id);
        guardEditable(ph, actor);
        validateDateRange(req.getEffectiveDate(), req.getEndDate());
        checkNoApprovedOverlap(ph.getProduct().getId(), ph.getWarehouse().getId(), req.getEffectiveDate(), req.getEndDate(), id);

        Map<String, Object> before = snapshot(ph);
        ph.setEffectiveDate(req.getEffectiveDate());
        ph.setEndDate(req.getEndDate());
        ph.setCostPrice(req.getCostPrice());
        ph.setSellingPrice(req.getSellingPrice());
        ph.setNotes(req.getNotes());
        ph.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        PriceHistory saved = priceHistoryRepository.save(ph);
        auditUtil.logChange(actor, AuditAction.PRICE_UPDATE, "PRICE_HISTORY", saved.getId(),
                String.valueOf(saved.getId()), before, snapshot(saved));
        return toResponse(saved, null);
    }

    @Override
    @Transactional
    public PriceHistoryResponse cancel(Long id, User actor) {
        PriceHistory ph = require(id);
        guardEditable(ph, actor);

        Map<String, Object> before = snapshot(ph);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ph.setStatus(PriceHistoryStatus.CANCELLED);
        ph.setCancelledBy(actor);
        ph.setCancelledAt(now);
        ph.setUpdatedAt(now);

        PriceHistory saved = priceHistoryRepository.save(ph);
        auditUtil.logChange(actor, AuditAction.PRICE_CANCEL, "PRICE_HISTORY", saved.getId(),
                String.valueOf(saved.getId()), before, snapshot(saved));
        return toResponse(saved, null);
    }

    @Override
    @Transactional
    public PriceHistoryResponse approve(Long id, User actor) {
        PriceHistory ph = require(id);
        if (ph.getStatus() == PriceHistoryStatus.APPROVED) throw PriceHistoryException.alreadyApproved();
        if (ph.getStatus() == PriceHistoryStatus.CANCELLED) throw PriceHistoryException.alreadyCancelled();

        // Maker-Checker: creator must not self-approve (defensive; RBAC already blocks this via roles)
        if (ph.getCreatedBy().getId().equals(actor.getId())) {
            throw new PriceHistoryException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "FORBIDDEN_APPROVE_OWN",
                    "Người tạo bản giá không được tự duyệt.");
        }

        // Re-check overlap at approval time (race-condition guard)
        checkNoApprovedOverlap(ph.getProduct().getId(), ph.getWarehouse().getId(), ph.getEffectiveDate(), ph.getEndDate(), id);

        Map<String, Object> before = snapshot(ph);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        ph.setApprovedBy(actor);
        ph.setApprovedAt(now);
        ph.setUpdatedAt(now);

        PriceHistory saved = priceHistoryRepository.save(ph);
        auditUtil.logChange(actor, AuditAction.PRICE_APPROVE, "PRICE_HISTORY", saved.getId(),
                String.valueOf(saved.getId()), before, snapshot(saved));
        return toResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceHistoryResponse getById(Long id, User actor) {
        PriceHistory ph = require(id);
        PreviousApprovedRef prev = buildPreviousApproved(ph);
        return toResponse(ph, prev);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getAll(Long productId, Long warehouseId, PriceHistoryStatus status) {
        List<PriceHistory> list;
        if (productId != null && warehouseId != null && status != null) {
            list = priceHistoryRepository.findByProductIdAndWarehouseIdAndStatusOrderByCreatedAtAsc(productId, warehouseId, status);
        } else if (productId != null && warehouseId != null) {
            list = priceHistoryRepository.findByProductIdAndWarehouseIdOrderByCreatedAtDesc(productId, warehouseId);
        } else if (productId != null && status != null) {
            list = priceHistoryRepository.findByProductIdAndStatusOrderByCreatedAtAsc(productId, status);
        } else if (warehouseId != null && status != null) {
            list = priceHistoryRepository.findByWarehouseIdAndStatusOrderByCreatedAtAsc(warehouseId, status);
        } else if (productId != null) {
            list = priceHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId);
        } else if (warehouseId != null) {
            list = priceHistoryRepository.findByWarehouseIdOrderByCreatedAtAsc(warehouseId);
        } else if (status != null) {
            list = priceHistoryRepository.findByStatusOrderByCreatedAtAsc(status);
        } else {
            list = priceHistoryRepository.findAllByOrderByCreatedAtAsc();
        }
        // Populate previous_approved for PENDING entries so the approval list can show delta comparisons.
        return list.stream().map(p -> {
            PreviousApprovedRef prev = (p.getStatus() == PriceHistoryStatus.PENDING)
                    ? buildPreviousApproved(p) : null;
            return toResponse(p, prev);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPriceHistoryResponse getByProduct(Long productId) {
        Product product = requireProduct(productId);
        List<PriceHistoryResponse> entries = priceHistoryRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream().map(p -> toResponse(p, null)).toList();
        return ProductPriceHistoryResponse.builder()
                .productId(productId)
                .productSku(product.getSku())
                .entries(entries)
                .build();
    }

    @Override
    public Optional<PriceHistory> lookupApproved(Long productId, Long warehouseId, LocalDate date) {
        return priceHistoryRepository.findApprovedAtDate(productId, warehouseId, date);
    }

    @Override
    @Transactional
    public PriceImportResponse importFromExcel(MultipartFile file, User actor) {
        validateExcelFile(file);

        List<CreatedRow> created = new ArrayList<>();
        List<FailedRow> failed = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            validateExcelHeaders(sheet);

            int dataRowCount = sheet.getLastRowNum(); // row 0 = header
            if (dataRowCount > EXCEL_MAX_ROWS) {
                throw new PriceHistoryException(
                        HttpStatus.BAD_REQUEST,
                        "EXCEL_TOO_MANY_ROWS", "File vượt quá 1.000 dòng dữ liệu.");
            }

            for (int i = 1; i <= dataRowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                processExcelRow(row, i + 1, actor, created, failed);
            }
        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new PriceHistoryException(
                    HttpStatus.BAD_REQUEST,
                    "EXCEL_FORMAT_INVALID", "Không đọc được file Excel.");
        }

        if (!created.isEmpty()) {
            notifyAccountantManagersImport(created.size());
            auditUtil.logChange(actor, AuditAction.PRICE_IMPORT, "PRICE_HISTORY", 0L,
                    "IMPORT", Map.of(), Map.of("created", created.size(), "failed", failed.size()));
        }

        return PriceImportResponse.builder()
                .totalRows(created.size() + failed.size())
                .createdCount(created.size())
                .failedCount(failed.size())
                .created(created)
                .failed(failed)
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void validateDateRange(LocalDate effective, LocalDate end) {
        if (effective != null && end != null && effective.isAfter(end)) {
            throw PriceHistoryException.invalidDateRange();
        }
    }

    private void checkNoApprovedOverlap(Long productId, Long warehouseId, LocalDate effective, LocalDate end, Long excludeId) {
        List<PriceHistory> overlaps = priceHistoryRepository
                .findApprovedOverlapping(productId, warehouseId, effective, end, excludeId);
        if (!overlaps.isEmpty()) throw PriceHistoryException.overlappingDate();
    }

    private void guardEditable(PriceHistory ph, User actor) {
        if (ph.getStatus() == PriceHistoryStatus.APPROVED) throw PriceHistoryException.alreadyApproved();
        if (ph.getStatus() == PriceHistoryStatus.CANCELLED) throw PriceHistoryException.alreadyCancelled();
        if (!ph.getCreatedBy().getId().equals(actor.getId())) {
            throw new AccessDeniedException(
                    "Chỉ người tạo bản giá mới được sửa hoặc hủy.");
        }
    }

    private PriceHistory require(Long id) {
        return priceHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bản giá không tồn tại: " + id));
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại: " + productId));
    }

    private Warehouse requireWarehouse(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho không tồn tại: " + warehouseId));
    }

    private void notifyAccountantManagers(PriceHistory ph, String message) {
        List<User> managers = userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (User mgr : managers) {
            notificationRepository.save(Notification.builder()
                    .recipient(mgr)
                    .type("PRICE_PENDING_APPROVAL")
                    .referenceType("price_history")
                    .referenceId(ph.getId())
                    .message(message)
                    .isRead(false)
                    .createdAt(now)
                    .build());
        }
    }

    private void notifyAccountantManagersImport(int count) {
        List<User> managers = userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String msg = count + " bản giá mới được import, chờ duyệt.";
        for (User mgr : managers) {
            notificationRepository.save(Notification.builder()
                    .recipient(mgr)
                    .type("PRICE_PENDING_APPROVAL")
                    .referenceType("price_history_import")
                    .message(msg)
                    .isRead(false)
                    .createdAt(now)
                    .build());
        }
    }

    private PreviousApprovedRef buildPreviousApproved(PriceHistory ph) {
        List<PriceHistory> latest = priceHistoryRepository.findLatestApproved(ph.getProduct().getId(), ph.getWarehouse().getId());
        if (latest.isEmpty()) return null;

        PriceHistory prev = latest.get(0);
        BigDecimal costDelta = ph.getCostPrice().subtract(prev.getCostPrice());
        BigDecimal sellDelta = ph.getSellingPrice().subtract(prev.getSellingPrice());
        BigDecimal costPct = prev.getCostPrice().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : costDelta.divide(prev.getCostPrice(), 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellPct = prev.getSellingPrice().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : sellDelta.divide(prev.getSellingPrice(), 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        return PreviousApprovedRef.builder()
                .id(prev.getId())
                .effectiveDate(prev.getEffectiveDate())
                .endDate(prev.getEndDate())
                .costPrice(prev.getCostPrice())
                .sellingPrice(prev.getSellingPrice())
                .costPriceDelta(costDelta)
                .costPriceDeltaPct(costPct)
                .sellingPriceDelta(sellDelta)
                .sellingPriceDeltaPct(sellPct)
                .build();
    }

    private void validateExcelFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new PriceHistoryException(
                    HttpStatus.BAD_REQUEST,
                    "EXCEL_FORMAT_INVALID", "File phải là định dạng .xlsx");
        }
    }

    private void validateExcelHeaders(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) throw new PriceHistoryException(
                HttpStatus.BAD_REQUEST,
                "EXCEL_FORMAT_INVALID", "File Excel thiếu dòng header.");

        String[] required = {"product_sku", "warehouse_code", "effective_date", "end_date", "cost_price", "selling_price"};
        for (int col = 0; col < required.length; col++) {
            Cell cell = header.getCell(col);
            String val = cell == null ? "" : cell.getStringCellValue().trim().toLowerCase();
            if (!val.equals(required[col])) {
                throw new PriceHistoryException(
                        HttpStatus.BAD_REQUEST,
                        "EXCEL_FORMAT_INVALID",
                        "Cột " + (char) ('A' + col) + " phải là '" + required[col] + "'");
            }
        }
    }

    protected void processExcelRow(Row row, int displayRow, User actor,
                                   List<CreatedRow> created, List<FailedRow> failed) {
        String sku = cellString(row, 0);
        String warehouseCode = cellString(row, 1);
        String effectiveDateStr = cellString(row, 2);
        String endDateStr = cellString(row, 3);
        String costStr = cellString(row, 4);
        String sellStr = cellString(row, 5);
        String notes = cellString(row, 6);

        // Presence check
        if (sku.isEmpty() || warehouseCode.isEmpty() || effectiveDateStr.isEmpty()
                || endDateStr.isEmpty() || costStr.isEmpty() || sellStr.isEmpty()) {
            failed.add(failRow(displayRow, sku, "MISSING_REQUIRED_FIELD", "Thiếu trường bắt buộc"));
            return;
        }

        // Product lookup
        Optional<Product> productOpt = productRepository.findBySkuAndIsActiveTrue(sku);
        if (productOpt.isEmpty()) {
            failed.add(failRow(displayRow, sku, "PRODUCT_NOT_FOUND", "SKU không tồn tại hoặc đã bị vô hiệu hóa"));
            return;
        }

        // Warehouse lookup
        Optional<Warehouse> warehouseOpt = warehouseRepository.findByCode(warehouseCode);
        if (warehouseOpt.isEmpty()) {
            failed.add(failRow(displayRow, sku, "WAREHOUSE_NOT_FOUND", "Mã kho '" + warehouseCode + "' không tồn tại"));
            return;
        }

        // Date parse
        LocalDate effective, end;
        try {
            effective = parseExcelDate(row.getCell(2), effectiveDateStr);
            end = parseExcelDate(row.getCell(3), endDateStr);
        } catch (DateTimeParseException e) {
            failed.add(failRow(displayRow, sku, "INVALID_DATE_FORMAT", "Định dạng ngày không hợp lệ"));
            return;
        }

        if (effective.isAfter(end)) {
            failed.add(failRow(displayRow, sku, "INVALID_DATE_RANGE", "effective_date > end_date"));
            return;
        }

        // Price parse
        BigDecimal cost, sell;
        try {
            cost = new BigDecimal(costStr);
            sell = new BigDecimal(sellStr);
        } catch (NumberFormatException e) {
            failed.add(failRow(displayRow, sku, "INVALID_PRICE", "Giá không phải số hợp lệ"));
            return;
        }

        if (cost.compareTo(BigDecimal.ZERO) <= 0 || sell.compareTo(BigDecimal.ZERO) <= 0) {
            failed.add(failRow(displayRow, sku, "INVALID_PRICE", "Giá phải lớn hơn 0"));
            return;
        }

        Product product = productOpt.get();
        Warehouse warehouse = warehouseOpt.get();
        List<PriceHistory> overlaps = priceHistoryRepository
                .findApprovedOverlapping(product.getId(), warehouse.getId(), effective, end, null);
        if (!overlaps.isEmpty()) {
            failed.add(failRow(displayRow, sku, "OVERLAPPING_EFFECTIVE_DATE",
                    "Trùng với bản giá APPROVED " + overlaps.get(0).getEffectiveDate()
                            + "–" + overlaps.get(0).getEndDate()));
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PriceHistory ph = PriceHistory.builder()
                .product(product)
                .warehouse(warehouse)
                .effectiveDate(effective)
                .endDate(end)
                .costPrice(cost)
                .sellingPrice(sell)
                .notes(notes.isEmpty() ? null : notes)
                .status(PriceHistoryStatus.PENDING)
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PriceHistory saved = priceHistoryRepository.save(ph);
        created.add(CreatedRow.builder()
                .row(displayRow)
                .productSku(sku)
                .priceHistoryId(saved.getId())
                .build());
    }

    private LocalDate parseExcelDate(Cell cell, String raw) {
        // Excel stores dates as numeric serials; try that first.
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String trimmed = raw.trim();
        // Accept dd/MM/yyyy (Vietnamese convention) and yyyy-MM-dd (ISO, common in Excel text cells).
        try {
            return LocalDate.parse(trimmed, DATE_FMT_DMY);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(trimmed); // falls back to ISO formatter; re-throws if still invalid
        }
    }

    private String cellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FMT_DMY)
                    : BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            case BLANK -> "";
            default -> "";
        };
    }

    private FailedRow failRow(int row, String sku, String code, String msg) {
        return FailedRow.builder().row(row).productSku(sku).errorCode(code).message(msg).build();
    }

    private Map<String, Object> snapshot(PriceHistory ph) {
        return Map.of(
                "status", ph.getStatus(),
                "effectiveDate", ph.getEffectiveDate(),
                "endDate", ph.getEndDate(),
                "costPrice", ph.getCostPrice(),
                "sellingPrice", ph.getSellingPrice()
        );
    }

    private PriceHistoryResponse toResponse(PriceHistory ph, PreviousApprovedRef prev) {
        return PriceHistoryResponse.builder()
                .id(ph.getId())
                .productId(ph.getProduct().getId())
                .productSku(ph.getProduct().getSku())
                .productName(ph.getProduct().getName())
                .warehouseId(ph.getWarehouse().getId())
                .warehouseName(ph.getWarehouse().getName())
                .effectiveDate(ph.getEffectiveDate())
                .endDate(ph.getEndDate())
                .costPrice(ph.getCostPrice())
                .sellingPrice(ph.getSellingPrice())
                .status(ph.getStatus().name())
                .notes(ph.getNotes())
                .createdBy(userRef(ph.getCreatedBy()))
                .createdAt(ph.getCreatedAt())
                .approvedBy(ph.getApprovedBy() != null ? userRef(ph.getApprovedBy()) : null)
                .approvedAt(ph.getApprovedAt())
                .cancelledBy(ph.getCancelledBy() != null ? userRef(ph.getCancelledBy()) : null)
                .cancelledAt(ph.getCancelledAt())
                .previousApproved(prev)
                .build();
    }

    private UserRef userRef(User u) {
        if (u == null) return null;
        return UserRef.builder().id(u.getId()).fullName(u.getFullName()).build();
    }
}
