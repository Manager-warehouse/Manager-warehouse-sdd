package com.wms.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object cho trang đăng nhập (/login).
 */
public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private static final By EMAIL_INPUT    = By.cssSelector("[data-testid='input-login-email']");
    private static final By PASSWORD_INPUT = By.cssSelector("[data-testid='input-login-password']");
    private static final By SUBMIT_BUTTON  = By.cssSelector("[data-testid='btn-login-submit']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_INPUT));
    }

    public void login(String email, String password) {
        WebElement emailField = wait.until(ExpectedConditions.elementToBeClickable(EMAIL_INPUT));
        emailField.clear();
        emailField.sendKeys(email);

        WebElement passwordField = driver.findElement(PASSWORD_INPUT);
        passwordField.clear();
        passwordField.sendKeys(password);

        driver.findElement(SUBMIT_BUTTON).click();
    }

    /** Chờ redirect về dashboard sau khi login thành công. */
    public void waitForLoginSuccess() {
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }
}
