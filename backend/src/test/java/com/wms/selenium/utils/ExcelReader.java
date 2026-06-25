package com.wms.selenium.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.provider.Arguments;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Đọc dữ liệu test case từ file Excel (.xlsx).
 *
 * Cột Excel (header ở row 0, data từ row 1):
 * 0  TestCaseID      — TC001, TC002, ...
 * 1  Description     — Mô tả ngắn
 * 2  EmployeeCode    — Mã nhân viên (NV-xxx)
 * 3  FullName        — Họ và tên
 * 4  Email           — Địa chỉ email
 * 5  Phone           — Số điện thoại (có thể rỗng)
 * 6  Password        — Mật khẩu
 * 7  Role            — ADMIN | CEO | WAREHOUSE_MANAGER | STOREKEEPER | WAREHOUSE_STAFF | ACCOUNTANT | PLANNER | DISPATCHER | DRIVER
 * 8  Shift           — Ca sáng | Ca chiều | Cả ngày
 * 9  Warehouses      — ID kho cách nhau bằng dấu phẩy (vd: "1,2") hoặc rỗng
 * 10 ExpectedResult  — SUCCESS | FAIL
 * 11 ExpectedMessage — Đoạn text cần xuất hiện trong thông báo lỗi (nếu FAIL)
 */
public class ExcelReader {

    private static final int COL_COUNT = 12;

    public static Stream<Arguments> readTestData(String filePath, String sheetName) throws IOException {
        List<Arguments> args = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + filePath);
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {   // skip header row 0
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String[] cells = new String[COL_COUNT];
                for (int j = 0; j < COL_COUNT; j++) {
                    Cell cell = row.getCell(j);
                    cells[j] = cell == null ? "" : getCellValue(cell);
                }

                args.add(Arguments.of(
                        cells[0],   // testCaseId
                        cells[1],   // description
                        cells[2],   // employeeCode
                        cells[3],   // fullName
                        cells[4],   // email
                        cells[5],   // phone
                        cells[6],   // password
                        cells[7],   // role
                        cells[8],   // shift
                        cells[9],   // warehouses (comma-separated)
                        cells[10],  // expectedResult
                        cells[11]   // expectedMessage
                ));
            }
        }

        return args.stream();
    }

    private static String getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // tránh "1.0" thay vì "1" cho số nguyên
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA  -> {
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                        .getCreationHelper().createFormulaEvaluator();
                yield getCellValue(evaluator.evaluateInCell(cell));
            }
            default -> cell.getStringCellValue().trim();
        };
    }
}
