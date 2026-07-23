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

    def wait_for(self, condition, timeout=None):
        """Poll `condition(driver)` instead of a flat sleep, so a slow first
        request (cold Vite/Spring start) gets more time and a warm one
        doesn't pay for a fixed delay it didn't need."""
        try:
            return WebDriverWait(self.driver, timeout or self.timeout).until(condition)
        except Exception:
            return None

    def wait_network_idle(self, idle_ms=500, timeout=15):
        """Poll performance.getEntriesByType('resource') until no new
        network request has started for `idle_ms`. document.readyState
        only proves the SPA shell booted; this proves the page's own
        data fetch(es) (axios calls fired from useEffect) actually
        settled, not just that the request was sent."""
        script = "return performance.getEntriesByType('resource').length"
        end = time.time() + timeout
        last_count = None
        stable_since = time.time()
        while time.time() < end:
            try:
                count = self.driver.execute_script(script)
            except Exception:
                return
            if count != last_count:
                last_count = count
                stable_since = time.time()
            elif (time.time() - stable_since) * 1000 >= idle_ms:
                return
            time.sleep(0.1)

    def has_error_toast(self):
        """The app's Toast.jsx always renders role='alert' with a
        border-danger-* class for type='error' (see frontend
        components/common/Toast.jsx) -- the standard error-reporting
        channel per frontend/CLAUDE.md, so this is a reliable generic
        signal that an API call failed and surfaced to the user."""
        return len(self.find_elements(By.CSS_SELECTOR, "[role='alert'][class*='danger']")) > 0

    def sleep(self, seconds):
        time.sleep(seconds)
