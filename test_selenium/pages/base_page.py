# -*- coding: utf-8 -*-
"""
Base Page Object Model for Selenium E2E Automation
"""

import time
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By


class BasePage:
    def __init__(self, driver, timeout=10):
        self.driver = driver
        self.timeout = timeout
        self.wait = WebDriverWait(driver, timeout)

    def open(self, url):
        self.driver.get(url)

    def find_element(self, by, value):
        return self.wait.until(EC.presence_of_element_located((by, value)))

    def find_elements(self, by, value):
        return self.driver.find_elements(by, value)

    def click(self, by, value):
        element = self.wait.until(EC.element_to_be_clickable((by, value)))
        element.click()

    def type(self, by, value, text):
        element = self.find_element(by, value)
        element.clear()
        element.send_keys(text)

    def get_text(self, by, value):
        element = self.find_element(by, value)
        return element.text

    def is_visible(self, by, value, timeout=5):
        try:
            WebDriverWait(self.driver, timeout).until(
                EC.visibility_of_element_located((by, value))
            )
            return True
        except Exception:
            return False

    def sleep(self, seconds):
        time.sleep(seconds)
