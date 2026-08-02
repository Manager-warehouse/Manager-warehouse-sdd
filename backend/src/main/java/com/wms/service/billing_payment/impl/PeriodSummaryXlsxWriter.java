package com.wms.service.billing_payment.impl;

import com.wms.dto.response.InvoiceResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.dto.response.PeriodSummaryResponse;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.dto.response.SupplierPaymentResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Builds the .xlsx export for a period summary - same inline Apache POI pattern as
// PriceHistoryController.exportExcel (build in memory, write to ByteArrayOutputStream),
// just split into its own class since this report has six sheets instead of one.
final class PeriodSummaryXlsxWriter {

    private PeriodSummaryXlsxWriter() {
    }

    static byte[] write(PeriodSummaryResponse summary) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeOverviewSheet(wb, summary);
            writeInvoiceSheet(wb, summary.getInvoices());
            writePaymentSheet(wb, summary.getPayments());
            writeSupplierInvoiceSheet(wb, summary.getSupplierInvoices());
            writeSupplierPaymentSheet(wb, summary.getSupplierPayments());
            writePriceChangeSheet(wb, summary.getPriceChanges());
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Khong the xuat file Excel", e);
        }
    }

    private static void writeOverviewSheet(Workbook wb, PeriodSummaryResponse s) {
        Sheet sheet = wb.createSheet("Tong quan");
        String[][] rows = {
                {"Ky ke toan", s.getPeriodName()},
                {"Tu ngay", toText(s.getStartDate())},
                {"Den ngay", toText(s.getEndDate())},
                {"Trang thai", String.valueOf(s.getStatus())},
                {"So Hoa don Ban", String.valueOf(s.getInvoiceCount())},
                {"Tong Hoa don Ban", String.valueOf(s.getInvoiceTotal())},
                {"So Phieu thu", String.valueOf(s.getPaymentCount())},
                {"Tong Phieu thu", String.valueOf(s.getPaymentTotal())},
                {"So Hoa don Mua", String.valueOf(s.getSupplierInvoiceCount())},
                {"Tong Hoa don Mua", String.valueOf(s.getSupplierInvoiceTotal())},
                {"So Phieu chi", String.valueOf(s.getSupplierPaymentCount())},
                {"Tong Phieu chi", String.valueOf(s.getSupplierPaymentTotal())},
                {"Gia von (COGS)", String.valueOf(s.getCogs())},
                {"Loi nhuan gop", String.valueOf(s.getGrossMargin())},
                {"So thay doi gia", String.valueOf(s.getPriceChangeCount())}
        };
        int rowIdx = 0;
        for (String[] r : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r[0]);
            row.createCell(1).setCellValue(nullToEmpty(r[1]));
        }
    }

    private static void writeInvoiceSheet(Workbook wb, List<InvoiceResponse> invoices) {
        Sheet sheet = wb.createSheet("Hoa don Ban");
        writeHeader(sheet, "so_hoa_don", "so_do", "dai_ly", "tong_tien", "da_thu",
                "ngay_phat_hanh", "han_thanh_toan", "trang_thai");
        int rowIdx = 1;
        for (InvoiceResponse e : invoices) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(e.getInvoiceNumber()));
            row.createCell(1).setCellValue(nullToEmpty(e.getDoNumber()));
            row.createCell(2).setCellValue(nullToEmpty(e.getDealerName()));
            row.createCell(3).setCellValue(toDouble(e.getTotalAmount()));
            row.createCell(4).setCellValue(toDouble(e.getPaidAmount()));
            row.createCell(5).setCellValue(toText(e.getIssueDate()));
            row.createCell(6).setCellValue(toText(e.getDueDate()));
            row.createCell(7).setCellValue(e.getStatus() != null ? e.getStatus().name() : "");
        }
    }

    private static void writePaymentSheet(Workbook wb, List<PaymentReceiptResponse> payments) {
        Sheet sheet = wb.createSheet("Phieu thu");
        writeHeader(sheet, "so_phieu_thu", "dai_ly", "hoa_don", "so_tien", "ngay_thu", "hinh_thuc");
        int rowIdx = 1;
        for (PaymentReceiptResponse e : payments) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(e.getPaymentNumber()));
            row.createCell(1).setCellValue(nullToEmpty(e.getDealerName()));
            row.createCell(2).setCellValue(nullToEmpty(e.getInvoiceNumber()));
            row.createCell(3).setCellValue(toDouble(e.getAmount()));
            row.createCell(4).setCellValue(toText(e.getPaymentDate()));
            row.createCell(5).setCellValue(e.getPaymentMethod() != null ? e.getPaymentMethod().name() : "");
        }
    }

    private static void writeSupplierInvoiceSheet(Workbook wb, List<SupplierInvoiceResponse> invoices) {
        Sheet sheet = wb.createSheet("Hoa don Mua");
        writeHeader(sheet, "so_hoa_don", "nha_cung_cap", "tong_tien", "da_thanh_toan",
                "ngay_phat_hanh", "han_thanh_toan", "trang_thai");
        int rowIdx = 1;
        for (SupplierInvoiceResponse e : invoices) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(e.getInvoiceNumber()));
            row.createCell(1).setCellValue(nullToEmpty(e.getSupplierName()));
            row.createCell(2).setCellValue(toDouble(e.getTotalAmount()));
            row.createCell(3).setCellValue(toDouble(e.getPaidAmount()));
            row.createCell(4).setCellValue(toText(e.getIssueDate()));
            row.createCell(5).setCellValue(toText(e.getDueDate()));
            row.createCell(6).setCellValue(e.getStatus() != null ? e.getStatus().name() : "");
        }
    }

    private static void writeSupplierPaymentSheet(Workbook wb, List<SupplierPaymentResponse> payments) {
        Sheet sheet = wb.createSheet("Phieu chi");
        writeHeader(sheet, "so_phieu_chi", "nha_cung_cap", "hoa_don", "so_tien", "ngay_chi", "hinh_thuc");
        int rowIdx = 1;
        for (SupplierPaymentResponse e : payments) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(e.getPaymentNumber()));
            row.createCell(1).setCellValue(nullToEmpty(e.getSupplierName()));
            row.createCell(2).setCellValue(nullToEmpty(e.getInvoiceNumber()));
            row.createCell(3).setCellValue(toDouble(e.getAmount()));
            row.createCell(4).setCellValue(toText(e.getPaymentDate()));
            row.createCell(5).setCellValue(e.getPaymentMethod() != null ? e.getPaymentMethod().name() : "");
        }
    }

    private static void writePriceChangeSheet(Workbook wb, List<PriceHistoryResponse> priceChanges) {
        Sheet sheet = wb.createSheet("Bang gia");
        writeHeader(sheet, "san_pham", "kho", "ngay_hieu_luc", "gia_von", "gia_ban", "nguoi_duyet");
        int rowIdx = 1;
        for (PriceHistoryResponse e : priceChanges) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(e.getProductSku()));
            row.createCell(1).setCellValue(nullToEmpty(e.getWarehouseCode()));
            row.createCell(2).setCellValue(toText(e.getEffectiveDate()));
            row.createCell(3).setCellValue(toDouble(e.getCostPrice()));
            row.createCell(4).setCellValue(toDouble(e.getSellingPrice()));
            row.createCell(5).setCellValue(e.getApprovedBy() != null ? nullToEmpty(e.getApprovedBy().getFullName()) : "");
        }
    }

    private static void writeHeader(Sheet sheet, String... columns) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String toText(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }
}
