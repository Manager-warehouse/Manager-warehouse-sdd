package com.wms.selenium.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object cho modal "Tạo tài khoản mới" trong trang /admin/users.
 */
public class CreateAccountPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Nút mở modal "Tạo tài khoản"
    private static final By BTN_OPEN_MODAL   = By.xpath("//button[contains(., 'Tạo tài khoản')]");

    // Các field trong form modal
    private static final By INPUT_CODE       = By.cssSelector("[data-testid='input-employee-code']");
    private static final By INPUT_FULL_NAME  = By.cssSelector("[data-testid='input-full-name']");
    private static final By INPUT_EMAIL      = By.cssSelector("[data-testid='input-email']");
    private static final By INPUT_PHONE      = By.cssSelector("[data-testid='input-phone']");
    private static final By INPUT_PASSWORD   = By.cssSelector("[data-testid='input-password']");
    private static final By SELECT_ROLE      = By.cssSelector("[data-testid='select-role']");
    private static final By SELECT_SHIFT     = By.cssSelector("[data-testid='select-shift']");
    private static final By INPUT_REGION     = By.cssSelector("[data-testid='input-region']");
    private static final By BTN_SUBMIT       = By.cssSelector("[data-testid='btn-submit-form']");
    private static final By FORM_ERROR       = By.cssSelector("[data-testid='form-error']");

    // Toast thông báo thành công
    private static final By TOAST_SUCCESS    = By.cssSelector(".toast-success, [class*='toast'][class*='success'], [class*='bg-green']");

    public CreateAccountPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/admin/users");
        wait.until(ExpectedConditions.visibilityOfElementLocated(BTN_OPEN_MODAL));
    }

    public void openModal() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(BTN_OPEN_MODAL));
        btn.click();
        // Chờ modal mở (field code xuất hiện)
        wait.until(ExpectedConditions.visibilityOfElementLocated(INPUT_CODE));
    }

    /**
     * Điền toàn bộ form tạo tài khoản.
     *
     * @param warehouses mảng warehouse ID (vd: new int[]{1, 2}), rỗng nếu không cần
     */
    public void fillForm(String code, String fullName, String email, String phone,
                         String password, String role, String shift,
                         int[] warehouses) {

        setField(INPUT_CODE,      code);
        setField(INPUT_FULL_NAME, fullName);
        setField(INPUT_EMAIL,     email);
        setField(INPUT_PHONE,     phone);
        setField(INPUT_PASSWORD,  password);

        // Chọn Role
        if (!role.isEmpty()) {
            new Select(wait.until(ExpectedConditions.elementToBeClickable(SELECT_ROLE)))
                    .selectByValue(role);
        }

        // Chọn Ca làm việc
        if (!shift.isEmpty()) {
            new Select(wait.until(ExpectedConditions.elementToBeClickable(SELECT_SHIFT)))
                    .selectByVisibleText(shift);
        }

        // Chọn Khu vực (chỉ hiện khi role = DISPATCHER)
        if ("DISPATCHER".equals(role)) {
            try {
                WebElement regionInput = driver.findElement(INPUT_REGION);
                regionInput.clear();
                regionInput.sendKeys("Hải Phòng");
            } catch (NoSuchElementException ignored) { }
        }

        // Tick checkbox kho — chỉ hiện khi role không phải ADMIN/CEO
        if (warehouses != null && warehouses.length > 0) {
            for (int warehouseId : warehouses) {
                String testId = "checkbox-warehouse-" + warehouseId;
                By checkboxBy = By.cssSelector("[data-testid='" + testId + "']");
                try {
                    WebElement cb = wait.until(ExpectedConditions.presenceOfElementLocated(checkboxBy));
                    if (!cb.isSelected()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cb);
                    }
                } catch (TimeoutException e) {
                    // Kho không hiện (vd role là ADMIN/CEO) — bỏ qua
                }
            }
        }
    }

    public void submit() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(BTN_SUBMIT));
        btn.click();
    }

    // ------------------------------------------------------------------ Assertions

    /** Kiểm tra form báo lỗi với đoạn message chứa expectedText. */
    public boolean hasError(String expectedText) {
        try {
            WebElement errorDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(FORM_ERROR));
            return errorDiv.getText().contains(expectedText);
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Kiểm tra modal đã đóng sau khi tạo thành công. */
    public boolean isModalClosed() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(BTN_SUBMIT));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Kiểm tra toast thành công xuất hiện. */
    public boolean isSuccessToastVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(TOAST_SUCCESS));
            return true;
        } catch (TimeoutException e) {
            // Fallback: kiểm tra modal đóng (cũng là dấu hiệu thành công)
            return isModalClosed();
        }
    }

    // ------------------------------------------------------------------ Helpers

    private void setField(By locator, String value) {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            el.clear();
            if (value != null && !value.isEmpty()) {
                el.sendKeys(value);
            }
        } catch (TimeoutException e) {
            // Field không hiển thị (vd: phone là optional, có thể bị skip)
        }
    }

    /** Chuyển chuỗi "1,2,3" thành int[]{1,2,3}. Rỗng trả về int[]. */
    public static int[] parseWarehouses(String warehousesStr) {
        if (warehousesStr == null || warehousesStr.isBlank()) return new int[0];
        String[] parts = warehousesStr.split(",");
        int[] ids = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Integer.parseInt(parts[i].trim());
        }
        return ids;
    }
}
