package com.wms.selenium.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Run this once to generate the input Excel test data file.
 * java -cp ... com.wms.selenium.utils.GenerateTestData
 */
public class GenerateTestData {

    public static void main(String[] args) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CreateAccount");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Headers
        String[] headers = {
                "TestCaseID", "Description", "Code", "FullName", "Email",
                "Phone", "Password", "Role", "Shift", "Warehouses", "ExpectedResult"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Test data rows
        Object[][] data = {
                // Valid test cases
                {"TC_001", "Tao tai khoan hop le - Warehouse Staff",
                        "NV-TEST-001", "Nguyen Van A", "nguyenvana@phucanh.vn",
                        "0912345001", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Success"},

                {"TC_002", "Tao tai khoan hop le - Storekeeper",
                        "NV-TEST-002", "Tran Thi B", "tranthib@phucanh.vn",
                        "0912345002", "Test@1234", "STOREKEEPER", "Ca chiều", "2", "Success"},

                {"TC_003", "Tao tai khoan hop le - Warehouse Manager",
                        "NV-TEST-003", "Le Van C", "levanc@phucanh.vn",
                        "0912345003", "Test@1234", "WAREHOUSE_MANAGER", "Cả ngày", "1,2", "Success"},

                {"TC_004", "Tao tai khoan hop le - Planner",
                        "NV-TEST-004", "Pham Thi D", "phamthid@phucanh.vn",
                        "0912345004", "Test@1234", "PLANNER", "Ca sáng", "3", "Success"},

                {"TC_005", "Tao tai khoan hop le - Dispatcher",
                        "NV-TEST-005", "Hoang Van E", "hoangvane@phucanh.vn",
                        "0912345005", "Test@1234", "DISPATCHER", "Ca sáng", "1", "Success"},

                {"TC_006", "Tao tai khoan hop le - Driver",
                        "NV-TEST-006", "Vu Thi F", "vuthif@phucanh.vn",
                        "0912345006", "Test@1234", "DRIVER", "Ca sáng", "2", "Success"},

                {"TC_007", "Tao tai khoan hop le - Accountant",
                        "NV-TEST-007", "Ngo Van G", "ngovang@phucanh.vn",
                        "0912345007", "Test@1234", "ACCOUNTANT", "Ca sáng", "1", "Success"},

                {"TC_008", "Tao tai khoan hop le - Admin (khong can kho)",
                        "NV-TEST-008", "Do Thi H", "dothih@phucanh.vn",
                        "0912345008", "Test@1234", "ADMIN", "Ca sáng", "", "Success"},

                {"TC_009", "Tao tai khoan hop le - CEO (khong can kho)",
                        "NV-TEST-009", "Bui Van I", "buivani@phucanh.vn",
                        "0912345009", "Test@1234", "CEO", "Ca sáng", "", "Success"},

                // Invalid test cases - missing required fields
                {"TC_010", "Thieu Ma nhan vien",
                        "", "Nguyen Van K", "nguyenk@phucanh.vn",
                        "0912345010", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                {"TC_011", "Thieu Ho ten",
                        "NV-TEST-011", "", "test011@phucanh.vn",
                        "0912345011", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                {"TC_012", "Thieu Email",
                        "NV-TEST-012", "Nguyen Van L", "",
                        "0912345012", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                {"TC_013", "Thieu Mat khau",
                        "NV-TEST-013", "Nguyen Van M", "nguyenm@phucanh.vn",
                        "0912345013", "", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                // Invalid test cases - password validation
                {"TC_014", "Mat khau qua ngan (duoi 8 ky tu)",
                        "NV-TEST-014", "Nguyen Van N", "nguyenn@phucanh.vn",
                        "0912345014", "Ab1", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                {"TC_015", "Mat khau chi co chu, khong co so",
                        "NV-TEST-015", "Nguyen Van O", "nguyeno@phucanh.vn",
                        "0912345015", "abcdefgh", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                {"TC_016", "Mat khau chi co so, khong co chu",
                        "NV-TEST-016", "Nguyen Van P", "nguyenp@phucanh.vn",
                        "0912345016", "12345678", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                // Invalid test cases - email format
                {"TC_017", "Email sai dinh dang",
                        "NV-TEST-017", "Nguyen Van Q", "invalid-email",
                        "0912345017", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                // Invalid test cases - warehouse validation
                {"TC_018", "Nhan vien nghiep vu khong co kho",
                        "NV-TEST-018", "Nguyen Van R", "nguyenr@phucanh.vn",
                        "0912345018", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "", "Error"},

                // Duplicate email test
                {"TC_019", "Email da ton tai (admin@phucanh.vn)",
                        "NV-TEST-019", "Nguyen Van S", "admin@phucanh.vn",
                        "0912345019", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1", "Error"},

                // Edge cases
                {"TC_020", "Ho ten co dau tieng Viet",
                        "NV-TEST-020", "Nguyen Van Anh", "nguyenvananh@phucanh.vn",
                        "0912345020", "Test@1234", "WAREHOUSE_STAFF", "Ca sáng", "1,2,3", "Success"},
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data[i][j].toString());
                cell.setCellStyle(dataStyle);
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 15000));
        }

        String outputPath = "src/test/resources/testdata/CreateAccount_TestData.xlsx";
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("Test data generated: " + outputPath);
    }
}
