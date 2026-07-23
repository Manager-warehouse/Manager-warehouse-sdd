package com.wms.controller.price_management;


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
import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.request.PriceHistoryUpdateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.price_management.PriceHistoryService;
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
    private final CurrentUserService currentUserService;

    public PriceHistoryController(PriceHistoryService priceHistoryService,
                                  CurrentUserService currentUserService) {
        this.priceHistoryService = priceHistoryService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    @Operation(summary = "Tạo bản giá mới (PENDING)")
    public PriceHistoryResponse create(@Valid @RequestBody PriceHistoryCreateRequest request) {
        return priceHistoryService.create(request, currentUserService.getRequiredCurrentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    @Operation(summary = "Sửa bản giá PENDING")
    public PriceHistoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PriceHistoryUpdateRequest request) {
        return priceHistoryService.update(id, request, currentUserService.getRequiredCurrentUser());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    @Operation(summary = "Hủy bản giá PENDING (soft cancel)")
    public PriceHistoryResponse cancel(@PathVariable Long id) {
        return priceHistoryService.cancel(id, currentUserService.getRequiredCurrentUser());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ACCOUNTANT_MANAGER', 'ADMIN')")
    @Operation(summary = "Duyệt bản giá PENDING")
    public PriceHistoryResponse approve(@PathVariable Long id) {
        return priceHistoryService.approve(id, currentUserService.getRequiredCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    @Operation(summary = "Chi tiết bản giá; ACCOUNTANT_MANAGER nhận thêm previous_approved")
    public PriceHistoryResponse getById(@PathVariable Long id) {
        return priceHistoryService.getById(id, currentUserService.getRequiredCurrentUser());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    @Operation(summary = "Danh sách bản giá (filter: product_id, warehouse_id, status, effective_date range)")
    public List<PriceHistoryResponse> getAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PriceHistoryStatus status,
            @RequestParam(required = false) LocalDate effectiveDateFrom,
            @RequestParam(required = false) LocalDate effectiveDateTo) {
        return priceHistoryService.getAll(productId, warehouseId, status, effectiveDateFrom, effectiveDateTo);
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLANNER', 'ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    @Operation(summary = "Tra cứu giá APPROVED tại ngày cụ thể cho một kho (preview trước khi tạo DO)")
    public ResponseEntity<PriceHistoryResponse> lookup(
            @RequestParam Long productId,
            @RequestParam Long warehouseId,
            @RequestParam LocalDate date) {
        return priceHistoryService.lookupApproved(productId, warehouseId, date)
                .map(ph -> ResponseEntity.ok(priceHistoryService.getById(ph.getId(), null)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    @Operation(summary = "Import bảng giá từ file Excel (.xlsx)")
    public ResponseEntity<PriceImportResponse> importExcel(@RequestParam("file") MultipartFile file) {
        PriceImportResponse result = priceHistoryService.importFromExcel(file, currentUserService.getRequiredCurrentUser());
        int status = result.getFailedCount() == 0 ? 201 : 207;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    @Operation(summary = "Download file Excel mẫu")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("price_import");
            Row header = sheet.createRow(0);
            String[] cols = {"product_sku", "warehouse_code", "effective_date", "cost_price", "selling_price", "notes"};
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

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'ADMIN', 'CEO')")
    @Operation(summary = "Xuất danh sách bản giá ra file Excel (.xlsx)")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) PriceHistoryStatus status) {
        List<PriceHistoryResponse> entries = priceHistoryService.getAll(productId, warehouseId, status, null, null);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("bang-gia");
            Row header = sheet.createRow(0);
            String[] cols = {"product_sku", "warehouse_code", "effective_date", "cost_price", "selling_price", "notes"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int rowIdx = 1;
            for (PriceHistoryResponse e : entries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getProductSku());
                row.createCell(1).setCellValue(e.getWarehouseCode() != null ? e.getWarehouseCode() : "");
                row.createCell(2).setCellValue(e.getEffectiveDate() != null ? e.getEffectiveDate().toString() : "");
                row.createCell(3).setCellValue(e.getCostPrice() != null ? e.getCostPrice().doubleValue() : 0);
                row.createCell(4).setCellValue(e.getSellingPrice() != null ? e.getSellingPrice().doubleValue() : 0);
                row.createCell(5).setCellValue(e.getNotes() != null ? e.getNotes() : "");
            }
            wb.write(out);
            String filename = "bang-gia-" + java.time.LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Không thể xuất file Excel", e);
        }
    }
}
