package com.wms.controller;

import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.User;
import com.wms.enums.PriceHistoryStatus;
import com.wms.service.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/price-history")
@Tag(name = "Price History", description = "Quản lý bảng giá & giá vốn (Spec 007)")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Tạo bản giá mới (PENDING)")
    public PriceHistoryResponse create(
            @Valid @RequestBody PriceHistoryCreateRequest request,
            @AuthenticationPrincipal User actor) {
        return priceHistoryService.create(request, actor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Sửa bản giá PENDING")
    public PriceHistoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PriceHistoryUpdateRequest request,
            @AuthenticationPrincipal User actor) {
        return priceHistoryService.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Hủy bản giá PENDING (soft cancel)")
    public PriceHistoryResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User actor) {
        return priceHistoryService.cancel(id, actor);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ACCOUNTANT_MANAGER')")
    @Operation(summary = "Duyệt bản giá PENDING")
    public PriceHistoryResponse approve(
            @PathVariable Long id,
            @AuthenticationPrincipal User actor) {
        return priceHistoryService.approve(id, actor);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Chi tiết bản giá; ACCOUNTANT_MANAGER nhận thêm previous_approved")
    public PriceHistoryResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User actor) {
        return priceHistoryService.getById(id, actor);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Danh sách bản giá (filter: product_id, status)")
    public List<PriceHistoryResponse> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) PriceHistoryStatus status) {
        return priceHistoryService.getAll(productId, status);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLANNER', 'ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Tra cứu giá APPROVED tại ngày cụ thể (preview trước khi tạo DO)")
    public ResponseEntity<PriceHistoryResponse> lookup(
            @RequestParam Long productId,
            @RequestParam LocalDate date) {
        return priceHistoryService.lookupApproved(productId, date)
                .map(ph -> ResponseEntity.ok(priceHistoryService.getById(ph.getId(), null)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Import bảng giá từ file Excel (.xlsx)")
    public ResponseEntity<PriceImportResponse> importExcel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User actor) {
        PriceImportResponse result = priceHistoryService.importFromExcel(file, actor);
        int status = result.getFailedCount() == 0 ? 201 : 207;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Operation(summary = "Download file Excel mẫu")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("price_import");
            Row header = sheet.createRow(0);
            String[] cols = {"product_sku", "effective_date", "end_date", "cost_price", "selling_price", "notes"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            wb.write(out);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=price_import_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        }
    }
}

@RestController
@RequestMapping("/api/v1/products")
class ProductPriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    ProductPriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/{id}/price-history")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER')")
    @Operation(summary = "Lịch sử tất cả bản giá của một sản phẩm")
    public ProductPriceHistoryResponse getByProduct(@PathVariable Long id) {
        return priceHistoryService.getByProduct(id);
    }
}
