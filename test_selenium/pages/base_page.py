# -*- coding: utf-8 -*-
"""
Base Page Object Model for Selenium E2E Automation
"""

import time
from selenium.webdriver.support.ui import WebDriverWait, Select
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

    def _field_for_label(self, label_text):
        """Locate the input/select/textarea that goes with a label, without
        relying on htmlFor -- the shared Input component sets it, but
        several pages (ReceiptForm.jsx, StocktakeForm.jsx, the payment
        modal in DealerDebtInvoice.jsx, PriceListManagement.jsx) use raw
        <label>+<input> pairs with no id/htmlFor at all. In every layout
        seen across this codebase, the label and its field share the same
        immediate parent <div> regardless of which pattern is used, so
        that's the one assumption this relies on."""
        label_el = self.find_element(By.XPATH, f"//label[contains(normalize-space(.), '{label_text}')]")
        return label_el.find_element(
            By.XPATH, "parent::div//*[self::input or self::select or self::textarea][1]"
        )

    def type_by_label(self, label_text, value):
        field = self._field_for_label(label_text)
        field.clear()
        field.send_keys(value)

    def set_date_by_label(self, label_text, iso_date):
        """<input type="date"> is unreliable with send_keys under a React
        controlled component; set .value directly and dispatch the events
        React's synthetic listener actually reacts to."""
        field = self._field_for_label(label_text)
        self.driver.execute_script(
            "arguments[0].value = arguments[1];"
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            field, iso_date,
        )

    def select_by_label(self, label_text, value=None, visible_text=None):
        sel = Select(self._field_for_label(label_text))
        if value is not None:
            sel.select_by_value(str(value))
        else:
            sel.select_by_visible_text(visible_text)

    def select_first_real_option_by_label(self, label_text, timeout=15):
        """For selects populated async from an API (dealers/warehouses/
        products) where real option values are DB ids we can't predict --
        wait for options beyond the placeholder, then pick the first."""
        self.wait_for(lambda d: len(Select(self._field_for_label(label_text)).options) > 1, timeout=timeout)
        Select(self._field_for_label(label_text)).select_by_index(1)

    def click_by_text(self, text, tag="button"):
        el = self.find_element(By.XPATH, f"//{tag}[contains(normalize-space(.), '{text}')]")
        el.click()

    def is_button_disabled(self, text, tag="button"):
        el = self.find_element(By.XPATH, f"//{tag}[contains(normalize-space(.), '{text}')]")
        return not el.is_enabled()

    def search_and_pick_first_result(self, placeholder_substr, search_term, timeout=10):
        """For the custom product search-dropdown pattern (ReceiptForm,
        PriceEntryModal): type into the input by its placeholder, then
        click the first rendered result row."""
        input_el = self.find_element(By.CSS_SELECTOR, f"input[placeholder*='{placeholder_substr}']")
        input_el.clear()
        input_el.send_keys(search_term)
        result = self.wait_for(lambda d: d.find_element(By.CSS_SELECTOR, "div.cursor-pointer"), timeout=timeout)
        if not result:
            return False
        result.click()
        return True

    def submit_and_verify(self, submit_button_text, marker_label_text, timeout=15, scope_xpath=None):
        """Click submit, then verify by waiting for EITHER the form/modal to
        close (marker label disappears -- real success signal, since these
        forms only unmount on a successful API response) OR an error toast.
        Returns (passed, reason).

        `scope_xpath` scopes the button lookup to a containing element --
        needed where a trigger button's text is a superset of the actual
        submit button's text (e.g. header "Ghi nhận Phiếu thu (Quét OCR)"
        vs modal submit "Ghi nhận Phiếu thu"), since the trigger stays in
        the DOM behind the modal and would otherwise match first."""
        if scope_xpath:
            self.find_element(By.XPATH, f"{scope_xpath}//button[contains(normalize-space(.), '{submit_button_text}')]").click()
        else:
            self.click_by_text(submit_button_text)

        def marker_gone(d):
            return len(d.find_elements(By.XPATH, f"//label[contains(normalize-space(.), '{marker_label_text}')]")) == 0

        self.wait_for(lambda d: marker_gone(d) or self.has_error_toast(), timeout=timeout)
        self.wait_network_idle(idle_ms=500, timeout=10)

        if self.has_error_toast():
            return False, "Error toast shown after submit"
        if not marker_gone(self.driver):
            return False, "Form/modal still open after submit (client-side validation likely blocked it)"
        return True, "ok"

    def has_error_toast(self):
        """The app's Toast.jsx always renders role='alert' with a
        border-danger-* class for type='error' (see frontend
        components/common/Toast.jsx) -- the standard error-reporting
        channel per frontend/CLAUDE.md, so this is a reliable generic
        signal that an API call failed and surfaced to the user."""
        return len(self.find_elements(By.CSS_SELECTOR, "[role='alert'][class*='danger']")) > 0

    def sleep(self, seconds):
        time.sleep(seconds)
