# -*- coding: utf-8 -*-
"""
Page Object Models for 10 WMS Modules (Selenium Round 2)
"""

from selenium.webdriver.common.by import By
from .base_page import BasePage
from config.config import APP_URL


class LoginPage(BasePage):
    # frontend/src/pages/Auth/Login.jsx uses type="email" (not name="username")
    # and its shared Input component never sets a name attribute.
    EMAIL_INPUT = (By.CSS_SELECTOR, "input[type='email']")
    PASSWORD_INPUT = (By.CSS_SELECTOR, "input[type='password']")
    LOGIN_BUTTON = (By.CSS_SELECTOR, "button[type='submit']")

    def login(self, username, password):
        self.open(f"{APP_URL}/login")
        # 20s covers a cold Vite/Spring first request; a warm one resolves
        # near-instantly since wait_for/is_visible return as soon as met.
        if self.is_visible(*self.EMAIL_INPUT, timeout=20):
            self.type(*self.EMAIL_INPUT, username)
            self.type(*self.PASSWORD_INPUT, password)
            self.click(*self.LOGIN_BUTTON)
            self.wait_for(lambda d: "/login" not in d.current_url, timeout=20)


class ModulePage(BasePage):
    def navigate_to(self, path):
        self.open(f"{APP_URL}{path}")
        self.wait_for(lambda d: d.execute_script("return document.readyState") == "complete", timeout=20)
        self.wait_network_idle(idle_ms=500, timeout=15)

    def check_page_loaded(self, path):
        """Navigate to path, verify RBAC didn't bounce us to /login or
        /forbidden, and that the page's data fetch didn't surface a
        visible error toast. Returns (passed, current_url, reason)."""
        self.navigate_to(path)
        current_url = self.driver.current_url

        if "/login" in current_url or "/forbidden" in current_url:
            return False, current_url, f"RBAC bounced to {current_url}"
        if path not in current_url:
            return False, current_url, f"Unexpected URL {current_url}"
        if self.has_error_toast():
            return False, current_url, "Page loaded but an error toast was shown (API call likely failed)"
        return True, current_url, "ok"
