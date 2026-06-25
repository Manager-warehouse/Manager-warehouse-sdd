package com.wms.selenium.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Tiện ích tạo file Excel chứa test data cho tính năng "Tạo tài khoản".
 *
 * Chạy main() một lần để sinh ra file:
 *   backend/src/test/resources/testdata/create_account_testdata.xlsx
 *
 * Sau đó chỉnh sửa file Excel trực tiếp nếu muốn thêm/sửa test case.
 */
public class GenerateExcelTestData {

    private static final String OUTPUT_PATH =
            "src/test/resources/testdata/create_account_testdata.xlsx";

    // Mỗi dòng = một test case: [ID, Desc, Code, FullName, Email, Phone, Password, Role, Shift, Warehouses, ExpectedResult, ExpectedMessage]
    private static final Object[][] TEST_CASES = {
        {"TestCaseID", "Description", "EmployeeCode", "FullName", "Email", "Phone", "Password", "Role", "Shift", "Warehouses", "ExpectedResult", "ExpectedMessage"},

        // === HAPPY PATH ===
        {"TC001", "ADMIN hợp lệ (không cần kho)", "NV-TEST-101", "Nguyễn Test Admin", "test.admin101@phucanh.vn", "0900000001", "TestPass1", "ADMIN", "Ca sáng", "", "SUCCESS", ""},
        {"TC002", "CEO hợp lệ (không cần kho)", "NV-TEST-102", "Trần Test CEO", "test.ceo102@phucanh.vn", "0900000002", "TestPass2", "CEO", "Cả ngày", "", "SUCCESS", ""},
        {"TC003", "STOREKEEPER với 1 kho", "NV-TEST-103", "Lê Test Thủ Kho", "test.keeper103@phucanh.vn", "0900000003", "TestPass3", "STOREKEEPER", "Ca sáng", "1", "SUCCESS", ""},
        {"TC004", "WAREHOUSE_MANAGER với 2 kho", "NV-TEST-104", "Phạm Test Manager", "test.manager104@phucanh.vn", "0900000004", "TestPass4", "WAREHOUSE_MANAGER", "Ca chiều", "1,2", "SUCCESS", ""},
        {"TC005", "WAREHOUSE_STAFF với kho HCM", "NV-TEST-105", "Hoàng Test Staff", "test.staff105@phucanh.vn", "", "TestPass5", "WAREHOUSE_STAFF", "Ca sáng", "3", "SUCCESS", ""},

        // === VALIDATION LỖI CLIENT ===
        {"TC006", "Thiếu mã nhân viên", "", "Nguyễn Thiếu Mã", "test.nocode@phucanh.vn", "", "TestPass6", "ADMIN", "Ca sáng", "", "FAIL", "Vui lòng điền mã nhân viên"},
        {"TC007", "Thiếu họ tên", "NV-TEST-107", "", "test.noname@phucanh.vn", "", "TestPass7", "ADMIN", "Ca sáng", "", "FAIL", "Vui lòng điền mã nhân viên"},
        {"TC008", "Thiếu email", "NV-TEST-108", "Nguyễn Thiếu Email", "", "", "TestPass8", "ADMIN", "Ca sáng", "", "FAIL", "Vui lòng điền mã nhân viên"},
        {"TC009", "Thiếu mật khẩu", "NV-TEST-109", "Nguyễn Thiếu Pass", "test.nopass@phucanh.vn", "", "", "ADMIN", "Ca sáng", "", "FAIL", "Mật khẩu bắt buộc"},
        {"TC010", "Mật khẩu ngắn hơn 8 ký tự", "NV-TEST-110", "Nguyễn Pass Ngắn", "test.shortpass@phucanh.vn", "", "ab1", "ADMIN", "Ca sáng", "", "FAIL", "Mật khẩu phải dài từ 8 ký tự"},
        {"TC011", "Mật khẩu chỉ có chữ (không có số)", "NV-TEST-111", "Nguyễn Pass Chữ", "test.letteronly@phucanh.vn", "", "abcdefgh", "ADMIN", "Ca sáng", "", "FAIL", "Mật khẩu mới phải chứa cả chữ và số"},
        {"TC012", "Mật khẩu chỉ có số (không có chữ)", "NV-TEST-112", "Nguyễn Pass Số", "test.digitonly@phucanh.vn", "", "12345678", "ADMIN", "Ca sáng", "", "FAIL", "Mật khẩu mới phải chứa cả chữ và số"},
        {"TC013", "Nhân viên nghiệp vụ không có kho", "NV-TEST-113", "Nguyễn Không Kho", "test.nowarehouse@phucanh.vn", "", "TestPass13", "STOREKEEPER", "Ca sáng", "", "FAIL", "Nhân viên nghiệp vụ phải được phân công"},

        // === LỖI SERVER ===
        // TC014: Email trùng — chỉ chạy được khi TC001 đã tạo email đó rồi
        {"TC014", "Email đã tồn tại trên hệ thống", "NV-TEST-114", "Nguyễn Trùng Email", "test.admin101@phucanh.vn", "", "TestPass14", "ADMIN", "Ca sáng", "", "FAIL", "Địa chỉ email này đã tồn tại"},
    };

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get("src/test/resources/testdata"));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CreateAccount");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int rowIdx = 0; rowIdx < TEST_CASES.length; rowIdx++) {
                Row row = sheet.createRow(rowIdx);
                Object[] testCase = TEST_CASES[rowIdx];
                for (int colIdx = 0; colIdx < testCase.length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    cell.setCellValue(String.valueOf(testCase[colIdx]));
                    if (rowIdx == 0) cell.setCellStyle(headerStyle);
                }
            }

            // Auto-size các cột
            for (int i = 0; i < TEST_CASES[0].length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(OUTPUT_PATH)) {
                workbook.write(fos);
            }
        }

        System.out.println("✅ File tạo thành công: " + OUTPUT_PATH);
        System.out.println("   Tổng test cases: " + (TEST_CASES.length - 1));
    }
}
