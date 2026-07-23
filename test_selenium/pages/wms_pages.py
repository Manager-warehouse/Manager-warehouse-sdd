# -*- coding: utf-8 -*-
"""
Page Object Models for 10 WMS Modules (Selenium Round 2)
"""

from selenium.webdriver.common.by import By
from .base_page import BasePage
from config.config import APP_URL


class LoginPage(BasePage):
    USERNAME_INPUT = (By.CSS_SELECTOR, "input[name='username'], input[type='text']")
    PASSWORD_INPUT = (By.CSS_SELECTOR, "input[name='password'], input[type='password']")
    LOGIN_BUTTON = (By.CSS_SELECTOR, "button[type='submit'], button")

    def login(self, username, password):
        self.open(f"{APP_URL}/login")
        self.sleep(1)
        if self.is_visible(*self.USERNAME_INPUT):
            self.type(*self.USERNAME_INPUT, username)
            self.type(*self.PASSWORD_INPUT, password)
            self.click(*self.LOGIN_BUTTON)
            self.sleep(1.5)


class ModulePage(BasePage):
    def navigate_to(self, path):
        self.open(f"{APP_URL}{path}")
        self.sleep(1.5)

    def check_page_loaded(self, path):
        self.navigate_to(path)
        return self.driver.current_url.startswith(APP_URL)
