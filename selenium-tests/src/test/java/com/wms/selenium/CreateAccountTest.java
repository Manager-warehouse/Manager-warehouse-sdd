package com.wms.selenium;

import com.wms.selenium.utils.ExcelUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CreateAccountTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private List<Map<String, String>> testData;
    private List<Map<String, String>> results;

    private static final String BASE_URL = "https://manager-warehouse.online";
    private static final String INPUT_FILE = "src/test/resources/testdata/CreateAccount_TestData.xlsx";
    private static final String OUTPUT_FILE = "test-output/CreateAccount_TestResult.xlsx";
    private static final String SHEET_NAME = "CreateAccount";

    private static final String ADMIN_EMAIL = "admin@phucanh.vn";
    private static final String ADMIN_PASSWORD = "password123";

    private static final Map<String, String> WAREHOUSE_MAP = Map.of(
            "1", "Hải Phòng",
            "2", "Hà Nội",
            "3", "Hồ Chí Minh"
    );

    @BeforeClass
    public void setUp() throws IOException {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setPageLoadTimeout(Duration.ofSeconds(60));

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        testData = ExcelUtils.readTestData(INPUT_FILE, SHEET_NAME);
        results = new ArrayList<>();

        loginAsAdmin();
    }

    @Test
    public void testCreateAccounts() {
        for (int i = 0; i < testData.size(); i++) {
            Map<String, String> data = testData.get(i);
            String testCaseId = data.getOrDefault("TestCaseID", "TC_" + (i + 1));
            String description = data.getOrDefault("Description", "");

            System.out.println("=== Running " + testCaseId + ": " + description + " ===");

            Map<String, String> result = new LinkedHashMap<>();
            result.put("TestCaseID", testCaseId);
            result.put("Description", description);
            result.put("Code", data.getOrDefault("Code", ""));
            result.put("FullName", data.getOrDefault("FullName", ""));
            result.put("Email", data.getOrDefault("Email", ""));
            result.put("Phone", data.getOrDefault("Phone", ""));
            result.put("Password", data.getOrDefault("Password", ""));
            result.put("Role", data.getOrDefault("Role", ""));
            result.put("Shift", data.getOrDefault("Shift", ""));
            result.put("Warehouses", data.getOrDefault("Warehouses", ""));
            result.put("ExpectedResult", data.getOrDefault("ExpectedResult", ""));

            try {
                String actualResult = executeTestCase(data);
                String expectedResult = data.getOrDefault("ExpectedResult", "");

                boolean passed = matchExpected(actualResult, expectedResult);
                result.put("ActualResult", actualResult);
                result.put("Result", passed ? "PASS" : "FAIL");
                result.put("Timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                System.out.println("  Result: " + (passed ? "PASS" : "FAIL") + " | " + actualResult);
            } catch (Exception e) {
                result.put("ActualResult", "Exception: " + e.getMessage());
                result.put("Result", "FAIL");
                result.put("Timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("  Exception: " + e.getMessage());
            }

            results.add(result);
        }
    }

    @AfterClass
    public void tearDown() throws IOException {
        java.io.File outputDir = new java.io.File("test-output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String outputFile = OUTPUT_FILE;
        try {
            ExcelUtils.writeResults(outputFile, SHEET_NAME, results);
        } catch (IOException e) {
            // If file is locked, write to a timestamped file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            outputFile = "test-output/CreateAccount_TestResult_" + timestamp + ".xlsx";
            ExcelUtils.writeResults(outputFile, SHEET_NAME, results);
        }
        System.out.println("\nTest results exported to: " + outputFile);

        if (driver != null) {
            driver.quit();
        }
    }

    private void loginAsAdmin() {
        driver.get(BASE_URL + "/login");
        sleep(5000);

        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='email']")));
        emailInput.clear();
        emailInput.sendKeys(ADMIN_EMAIL);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[type='password']")));
        passwordInput.clear();
        passwordInput.sendKeys(ADMIN_PASSWORD);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@type='submit']")));
        loginButton.click();

        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
        sleep(3000);
    }

    private String executeTestCase(Map<String, String> data) {
        navigateToUserManagement();
        sleep(2000);

        openCreateAccountForm();
        sleep(1500);

        // Make email unique by appending timestamp for valid test cases
        Map<String, String> testDataCopy = new HashMap<>(data);
        String expectedResult = data.getOrDefault("ExpectedResult", "");
        String email = data.getOrDefault("Email", "");
        if (expectedResult.equalsIgnoreCase("Success") && !email.isEmpty()) {
            String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
            String uniqueEmail = email.replace("@", timestamp + "@");
            testDataCopy.put("Email", uniqueEmail);
        }

        // Make code unique too
        String code = data.getOrDefault("Code", "");
        if (expectedResult.equalsIgnoreCase("Success") && !code.isEmpty()) {
            String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
            testDataCopy.put("Code", code + "-" + timestamp);
        }

        fillCreateAccountForm(testDataCopy);
        sleep(500);

        clickSubmitButton();
        sleep(3000);

        return captureResult();
    }

    private void navigateToUserManagement() {
        driver.get(BASE_URL + "/admin/users");
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[contains(text(),'Tạo tài khoản')]")));
    }

    private void openCreateAccountForm() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Tạo tài khoản')]")));
        btn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'Tạo tài khoản mới')]")));
    }

    private void fillCreateAccountForm(Map<String, String> data) {
        // Employee Code - placeholder "Ví dụ: NV-005"
        fillInputByPlaceholder("NV-005", data.getOrDefault("Code", ""));

        // Full Name - placeholder "Ví dụ: Nguyễn Văn A"
        fillInputByPlaceholder("Nguyễn Văn A", data.getOrDefault("FullName", ""));

        // Email - placeholder "nhanvien@phucanh.vn"
        fillInputByPlaceholder("nhanvien@phucanh.vn", data.getOrDefault("Email", ""));

        // Phone - placeholder "Ví dụ: 0912345678"
        String phone = data.getOrDefault("Phone", "");
        if (!phone.isEmpty()) {
            fillInputByPlaceholder("0912345678", phone);
        }

        // Password - placeholder "Mật khẩu tối thiểu 8 ký tự"
        fillInputByPlaceholder("8 ký tự", data.getOrDefault("Password", ""));

        // Role dropdown - select by value (ADMIN, CEO, WAREHOUSE_STAFF, etc.)
        String role = data.getOrDefault("Role", "");
        if (!role.isEmpty()) {
            selectRoleDropdown(role);
        }

        // Shift dropdown
        String shift = data.getOrDefault("Shift", "");
        if (!shift.isEmpty()) {
            selectShiftDropdown(shift);
        }

        // Warehouses checkboxes
        String warehouses = data.getOrDefault("Warehouses", "");
        if (!warehouses.isEmpty()) {
            selectWarehouses(warehouses);
        }
    }

    private void fillInputByPlaceholder(String placeholderPart, String value) {
        if (value == null || value.isEmpty()) return;
        try {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//input[contains(@placeholder,'" + placeholderPart + "')]")));
            input.clear();
            input.sendKeys(value);
        } catch (TimeoutException e) {
            System.out.println("Warning: Could not find input with placeholder containing '" + placeholderPart + "'");
        }
    }

    private void selectRoleDropdown(String roleValue) {
        try {
            List<WebElement> selects = driver.findElements(By.tagName("select"));
            for (WebElement sel : selects) {
                List<WebElement> opts = sel.findElements(By.tagName("option"));
                for (WebElement opt : opts) {
                    String val = opt.getAttribute("value");
                    if (val != null && val.equals(roleValue)) {
                        new Select(sel).selectByValue(roleValue);
                        sleep(300);
                        return;
                    }
                }
            }
            System.out.println("Warning: Role value '" + roleValue + "' not found in any dropdown");
        } catch (Exception e) {
            System.out.println("Warning: Could not select role '" + roleValue + "': " + e.getMessage());
        }
    }

    private void selectShiftDropdown(String shiftText) {
        try {
            List<WebElement> selects = driver.findElements(By.tagName("select"));
            for (WebElement sel : selects) {
                List<WebElement> opts = sel.findElements(By.tagName("option"));
                for (WebElement opt : opts) {
                    if (opt.getText().contains(shiftText)) {
                        new Select(sel).selectByVisibleText(opt.getText());
                        sleep(300);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not select shift '" + shiftText + "': " + e.getMessage());
        }
    }

    private void selectWarehouses(String warehousesStr) {
        String[] warehouseIds = warehousesStr.split(",");
        for (String id : warehouseIds) {
            String trimmedId = id.trim();
            try {
                // First try by checkbox value attribute
                WebElement checkbox = driver.findElement(
                        By.xpath("//input[@type='checkbox' and @value='" + trimmedId + "']"));
                if (!checkbox.isSelected()) {
                    checkbox.click();
                    sleep(200);
                }
            } catch (Exception e) {
                // Fallback: find by warehouse name text
                String warehouseName = WAREHOUSE_MAP.getOrDefault(trimmedId, "");
                if (!warehouseName.isEmpty()) {
                    try {
                        WebElement label = driver.findElement(
                                By.xpath("//*[contains(text(),'" + warehouseName + "')]/ancestor::label | " +
                                        "//*[contains(text(),'" + warehouseName + "')]"));
                        label.click();
                        sleep(200);
                    } catch (Exception ex) {
                        System.out.println("Warning: Could not select warehouse '" + trimmedId + "': " + ex.getMessage());
                    }
                }
            }
        }
    }

    private void clickSubmitButton() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Lưu lại')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", btn);
            sleep(300);
            btn.click();
        } catch (TimeoutException e) {
            // Fallback
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Lưu') or contains(text(),'Save')]")));
            btn.click();
        }
    }

    private String captureResult() {
        // Check for toast/notification first (most reliable indicator)
        try {
            WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//*[contains(@class,'toast') or contains(@class,'Toast') or " +
                                    "contains(@class,'Toastify') or contains(@class,'notification') or " +
                                    "contains(@class,'alert') or contains(@class,'snackbar')]")));
            String text = toast.getText().trim();
            if (!text.isEmpty()) {
                if (text.toLowerCase().contains("thành công") || text.toLowerCase().contains("success")
                        || text.toLowerCase().contains("tạo")) {
                    return "Success: " + text;
                }
                return "Error: " + text;
            }
        } catch (TimeoutException ignored) {
        }

        // Check if form/modal closed (means success - form disappears after create)
        try {
            boolean formGone = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.xpath("//*[contains(text(),'Tạo tài khoản mới')]")));
            if (formGone) {
                return "Success: Form closed - Account created";
            }
        } catch (TimeoutException ignored) {
        }

        // Check for validation error messages (red text near form fields)
        try {
            List<WebElement> errorMsgs = driver.findElements(
                    By.xpath("//*[contains(@class,'error') or contains(@class,'danger') or " +
                            "contains(@class,'invalid') or contains(@class,'text-red')]"));
            for (WebElement errorMsg : errorMsgs) {
                String text = errorMsg.getText().trim();
                if (!text.isEmpty() && text.length() > 2) {
                    return "Error: " + text;
                }
            }
        } catch (Exception ignored) {
        }

        // Check if form is still visible (means validation error or something blocked submit)
        try {
            driver.findElement(By.xpath("//*[contains(text(),'Tạo tài khoản mới')]"));
            return "Error: Form still open - validation failed";
        } catch (org.openqa.selenium.NoSuchElementException ignored) {
        }

        return "Unknown: Could not determine result";
    }

    private boolean matchExpected(String actual, String expected) {
        if (expected == null || expected.isEmpty()) return true;
        String actualLower = actual.toLowerCase();
        String expectedLower = expected.toLowerCase();

        if (expectedLower.contains("success") || expectedLower.contains("pass")) {
            return actualLower.contains("success");
        }
        if (expectedLower.contains("error") || expectedLower.contains("fail")) {
            return actualLower.contains("error");
        }
        return actualLower.contains(expectedLower);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
