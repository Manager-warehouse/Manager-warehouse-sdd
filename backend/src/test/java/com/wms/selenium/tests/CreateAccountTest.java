package com.wms.selenium.tests;

import com.wms.selenium.pages.CreateAccountPage;
import com.wms.selenium.pages.LoginPage;
import com.wms.selenium.utils.ExcelReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Selenium E2E test cho tính năng "Tạo tài khoản mới" tại /admin/users.
 *
 * Cách chạy:
 *   1. Chạy frontend:  cd frontend && npm run dev          (http://localhost:5173)
 *   2. Chạy backend:   cd backend  && mvn spring-boot:run  (http://localhost:8080)
 *   3. Chạy test:      mvn test -Dtest=CreateAccountTest -pl backend
 *
 * Tạo file Excel test data:
 *   mvn exec:java -Dexec.mainClass=com.wms.selenium.utils.GenerateExcelTestData -pl backend
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("selenium")
class CreateAccountTest {

    // ------------------------------------------------------------------ Config
    private static final String BASE_URL      = System.getProperty("selenium.baseUrl",   "http://localhost:5173");
    private static final String ADMIN_EMAIL   = System.getProperty("selenium.adminEmail", "admin@phucanh.vn");
    private static final String ADMIN_PASS    = System.getProperty("selenium.adminPass",  "Admin@123");
    private static final String EXCEL_PATH;

    static {
        Path p = Paths.get("src", "test", "resources", "testdata", "create_account_testdata.xlsx");
        EXCEL_PATH = p.toAbsolutePath().toString();
    }

    // ------------------------------------------------------------------ Shared state
    private static WebDriver driver;
    private static LoginPage loginPage;
    private static CreateAccountPage createAccountPage;

    @BeforeAll
    static void setupDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // Bỏ comment dòng dưới nếu muốn chạy không hiện cửa sổ (CI/CD):
        // options.addArguments("--headless=new");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,900");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        loginPage = new LoginPage(driver);
        createAccountPage = new CreateAccountPage(driver);
    }

    @BeforeAll
    static void loginAsAdmin() {
        loginPage.open(BASE_URL);
        loginPage.login(ADMIN_EMAIL, ADMIN_PASS);
        loginPage.waitForLoginSuccess();
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        // Chờ 5 giây để xem kết quả trước khi Chrome đóng
        Thread.sleep(5000);
        if (driver != null) {
            driver.quit();
        }
    }

    // ------------------------------------------------------------------ Data provider

    static Stream<?> provideTestData() throws IOException {
        return ExcelReader.readTestData(EXCEL_PATH, "CreateAccount");
    }

    // ------------------------------------------------------------------ Tests

    @ParameterizedTest(name = "[{index}] {0} — {1}")
    @MethodSource("provideTestData")
    @Order(1)
    void testCreateAccount(
            String testCaseId,
            String description,
            String employeeCode,
            String fullName,
            String email,
            String phone,
            String password,
            String role,
            String shift,
            String warehousesStr,
            String expectedResult,
            String expectedMessage
    ) {
        // Mở modal mỗi lần test (nếu modal đã đóng thì navigate lại)
        createAccountPage.navigateTo(BASE_URL);
        createAccountPage.openModal();

        int[] warehouses = CreateAccountPage.parseWarehouses(warehousesStr);

        createAccountPage.fillForm(
                employeeCode, fullName, email, phone,
                password, role, shift, warehouses
        );
        createAccountPage.submit();

        if ("SUCCESS".equalsIgnoreCase(expectedResult)) {
            Assertions.assertTrue(
                createAccountPage.isModalClosed() || createAccountPage.isSuccessToastVisible(),
                testCaseId + " [" + description + "]: Mong đợi tạo tài khoản THÀNH CÔNG nhưng modal vẫn còn mở hoặc không có toast success"
            );
        } else {
            Assertions.assertTrue(
                createAccountPage.hasError(expectedMessage),
                testCaseId + " [" + description + "]: Mong đợi lỗi chứa '" + expectedMessage
                        + "' nhưng không thấy thông báo đó"
            );
        }
    }
}
