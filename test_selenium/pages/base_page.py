# -*- coding: utf-8 -*-
"""
Base Page Object Model for Selenium E2E Automation
"""

import time
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import TimeoutException


class BasePage:
    def __init__(self, driver, timeout=10):
        self.driver = driver
        self.timeout = timeout
        self.wait = WebDriverWait(driver, timeout)

    def open(self, url):
        self.driver.get(url)

    def find_element(self, by, value):
        try:
            return self.wait.until(EC.presence_of_element_located((by, value)))
        except TimeoutException:
            # Selenium's default TimeoutException message is empty --
            # without this, every timeout in every flow reads as the
            # useless "TimeoutException: Message:" with no clue which of
            # the dozens of find_element calls actually timed out.
            raise TimeoutException(f"No element found for {by}='{value}' within {self.timeout}s")

    def find_elements(self, by, value):
        return self.driver.find_elements(by, value)

    def _wait_clickable(self, by, value):
        try:
            return self.wait.until(EC.element_to_be_clickable((by, value)))
        except TimeoutException:
            raise TimeoutException(f"Element not clickable for {by}='{value}' within {self.timeout}s")

    def click(self, by, value):
        self._wait_clickable(by, value).click()

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

    def by_testid(self, testid):
        """Preferred over every label/position-based heuristic below --
        stable across copy changes, immune to the whole class of DOM-
        proximity/text-collision bugs this suite spent a long time
        chasing. Only exists on pages that have had data-testid added
        (AUTH-001, MDM-002, TRF-005, STK-006, PRC-007 so far); everything
        else still relies on the fallback strategies."""
        return self.find_element(By.CSS_SELECTOR, f"[data-testid='{testid}']")

    def by_testid_clickable(self, testid):
        return self._wait_clickable(By.CSS_SELECTOR, f"[data-testid='{testid}']")

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
        field = label_el.find_element(
            By.XPATH, "parent::div//*[self::input or self::select or self::textarea][1]"
        )
        # find_element only checks presence in the DOM, not that a just-
        # opened modal has finished animating in -- interacting with a
        # field mid-transition can silently land on nothing. Give it a
        # short beat to actually become visible.
        self.wait_for(lambda d: field.is_displayed(), timeout=5)
        return field

    def type_by_label(self, label_text, value):
        field = self._field_for_label(label_text)
        field.clear()
        field.send_keys(value)
        # Defensive verify-and-retry: a field that silently didn't receive
        # the keystrokes (e.g. focus stolen by an in-progress transition)
        # is worse than a slow test -- it submits a form with a blank
        # required field and blames "validation" for something invisible.
        if field.get_attribute("value") != value:
            field = self._field_for_label(label_text)
            field.clear()
            field.send_keys(value)

    def _field_by_testid(self, testid):
        field = self.by_testid(testid)
        self.wait_for(lambda d: field.is_displayed(), timeout=5)
        return field

    def type_by_testid(self, testid, value):
        field = self._field_by_testid(testid)
        field.clear()
        field.send_keys(value)
        if field.get_attribute("value") != value:
            field = self._field_by_testid(testid)
            field.clear()
            field.send_keys(value)

    def set_date_by_testid(self, testid, iso_date):
        self._set_react_value(self._field_by_testid(testid), iso_date)

    def _set_react_value(self, element, value):
        """Set a controlled input's value via JS in one atomic call instead
        of character-by-character send_keys. Two things send_keys can't
        survive: (1) a re-render mid-typing stealing focus/replacing the
        node (this is what left ReceiptForm's product search silently
        empty -- its onFocus triggers a state update), and (2) plain
        `element.value = x` alone gets silently swallowed by React's
        controlled-input value tracker, which only notices a change when
        it comes through the *native* (non-React-overridden) value setter
        -- the standard workaround for scripted React input."""
        self.driver.execute_script(
            "const [el, value] = arguments;"
            "const proto = Object.getPrototypeOf(el);"
            "const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;"
            "setter.call(el, value);"
            "el.dispatchEvent(new Event('input', {bubbles: true}));"
            "el.dispatchEvent(new Event('change', {bubbles: true}));",
            element, value,
        )

    def set_date_by_label(self, label_text, iso_date):
        """<input type="date"> is unreliable with send_keys under a React
        controlled component; set the value directly instead."""
        self._set_react_value(self._field_for_label(label_text), iso_date)

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

    def select_first_real_option_by_testid(self, testid, timeout=15):
        self.wait_for(lambda d: len(Select(self.by_testid(testid)).options) > 1, timeout=timeout)
        Select(self.by_testid(testid)).select_by_index(1)

    def click_by_text(self, text, tag="button"):
        # Was find_element (presence only) + .click() -- clicking a button
        # that's present but not yet clickable (mid-transition, or still
        # disabled a beat after render) can silently no-op: no exception,
        # but the onClick handler never fires and nothing opens. This is
        # what actually happened to OUT-004's "Lập đơn xuất mới" click.
        self._wait_clickable(By.XPATH, f"//{tag}[contains(normalize-space(.), '{text}')]").click()

    def is_button_disabled(self, text, tag="button"):
        el = self.find_element(By.XPATH, f"//{tag}[contains(normalize-space(.), '{text}')]")
        return not el.is_enabled()

    def search_and_pick_first_result(self, placeholder_substr, search_term, timeout=10, input_testid=None, result_testid=None):
        """For the custom product search-dropdown pattern (ReceiptForm,
        PriceEntryModal): type into the input, then click the first
        rendered result row. Returns (found, reason) so a caller can tell
        "search input missing" apart from "zero matches" apart from
        "dropdown never rendered". Pass `input_testid`/`result_testid`
        where the page has them (PRC-007) -- falls back to placeholder
        text + a generic `.cursor-pointer` class match otherwise."""
        result_selector = f"[data-testid='{result_testid}']" if result_testid else "div.cursor-pointer"

        def locate():
            if input_testid:
                el = self.by_testid(input_testid)
            else:
                el = self.find_element(By.CSS_SELECTOR, f"input[placeholder*='{placeholder_substr}']")
            self.wait_for(lambda d: el.is_displayed(), timeout=5)
            return el

        try:
            input_el = locate()
        except Exception:
            return False, f"Search input not found (placeholder containing '{placeholder_substr}')"

        # Manually reproduced this exact scenario in a real (non-headless)
        # browser: the JS native-setter trick worked fine there, so this
        # isn't a React-tracker problem. The real gap was that this method
        # -- unlike _field_for_label -- never waited for the field to be
        # visible before interacting, the same modal/render race that hit
        # AUTH-001. Real send_keys (proven reliable everywhere else once
        # that wait exists) instead of a JS-dispatched event is also just
        # less exposed to whatever headless-vs-headed event-timing
        # difference made the JS approach unreliable under ChromeDriver.
        input_el.clear()
        input_el.send_keys(search_term)
        if input_el.get_attribute("value") != search_term:
            input_el = locate()
            input_el.clear()
            input_el.send_keys(search_term)
        actual_value = input_el.get_attribute("value")
        if actual_value != search_term:
            return False, f"Search input still shows '{actual_value}' after two send_keys attempts (expected '{search_term}')"

        def dropdown_settled(d):
            has_results = len(d.find_elements(By.CSS_SELECTOR, result_selector)) > 0
            no_match_shown = len(d.find_elements(By.XPATH, "//div[contains(text(), 'Không tìm thấy')]")) > 0
            return has_results or no_match_shown

        self.wait_for(dropdown_settled, timeout=timeout)
        results = self.find_elements(By.CSS_SELECTOR, result_selector)
        if not results:
            return False, (
                f"No product matched search term '{search_term}' (input actually held '{actual_value}'; "
                "dropdown rendered but showed no results)"
            )
        results[0].click()
        return True, "ok"

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
            self._wait_clickable(By.XPATH, f"{scope_xpath}//button[contains(normalize-space(.), '{submit_button_text}')]").click()
        else:
            self.click_by_text(submit_button_text)

        def marker_gone(d):
            return len(d.find_elements(By.XPATH, f"//label[contains(normalize-space(.), '{marker_label_text}')]")) == 0

        self.wait_for(lambda d: marker_gone(d) or self.has_error_toast(), timeout=timeout)
        self.wait_network_idle(idle_ms=500, timeout=10)

        if self.has_error_toast():
            return False, "Error toast shown after submit"
        if not marker_gone(self.driver):
            inline_err = self.get_visible_error_text()
            detail = "Form/modal still open after submit"
            detail += f": {inline_err}" if inline_err else " (no error text found -- check screenshot)"
            return False, detail
        return True, "ok"

    def submit_and_verify_by_testid(self, submit_testid, marker_testid, timeout=15):
        """testid version of submit_and_verify. No scope_xpath needed --
        unlike button text, a trigger and its modal's submit always get
        distinct testids by construction, so there's no equivalent of the
        "Ghi nhận Phiếu thu (Quét OCR)" collision to work around."""
        self.by_testid_clickable(submit_testid).click()

        def marker_gone(d):
            return len(d.find_elements(By.CSS_SELECTOR, f"[data-testid='{marker_testid}']")) == 0

        self.wait_for(lambda d: marker_gone(d) or self.has_error_toast(), timeout=timeout)
        self.wait_network_idle(idle_ms=500, timeout=10)

        if self.has_error_toast():
            return False, "Error toast shown after submit"
        if not marker_gone(self.driver):
            inline_err = self.get_visible_error_text()
            detail = "Form/modal still open after submit"
            detail += f": {inline_err}" if inline_err else " (no error text found -- check screenshot)"
            return False, detail
        return True, "ok"

    def has_error_toast(self):
        """The app's Toast.jsx always renders role='alert' with a
        border-danger-* class for type='error' (see frontend
        components/common/Toast.jsx) -- the standard error-reporting
        channel per frontend/CLAUDE.md, so this is a reliable generic
        signal that an API call failed and surfaced to the user."""
        return len(self.find_elements(By.CSS_SELECTOR, "[role='alert'][class*='danger']")) > 0

    def get_visible_error_text(self):
        """Several forms (Login.jsx, UserFormModal.jsx) render their own
        local validation-error banner as a plain <div class="...danger...">
        instead of going through the toast system -- has_error_toast() is
        blind to these. Returns the first non-empty match's text, or None."""
        for el in self.find_elements(By.CSS_SELECTOR, "div[class*='danger']"):
            text = el.text.strip()
            if text:
                return text
        return None

    def get_console_errors(self):
        """Real browser console SEVERE-level entries (requires build_driver()
        to have set goog:loggingPrefs). Every prior diagnostic here has been
        DOM-based, which is structurally blind to a click whose onClick
        handler throws (or awaits a rejected promise) without React ever
        rendering a visible error -- the button still looks present and
        clickable to Selenium the whole time. This is the one signal that
        isn't."""
        try:
            entries = self.driver.get_log("browser")
        except Exception:
            return []
        return [e["message"] for e in entries if e.get("level") == "SEVERE"]

    def fetch_authenticated(self, path):
        """Call a relative API path directly from inside the authenticated
        page session (same wms_token the app's own axios client attaches),
        bypassing whatever client-side state/filtering/rendering logic
        might be silently dropping data before it ever reaches the UI.
        Returns (status, body_text) or (None, error) if the fetch itself
        couldn't be made."""
        script = """
        const path = arguments[0];
        const callback = arguments[1];
        const token = sessionStorage.getItem('wms_token');
        fetch(path, { headers: token ? { 'Authorization': 'Bearer ' + token } : {} })
            .then(async (res) => {
                const text = await res.text();
                callback([res.status, text]);
            })
            .catch((err) => callback([null, String(err)]));
        """
        try:
            status, body = self.driver.execute_async_script(script, path)
            return status, body
        except Exception as e:
            return None, f"execute_async_script failed: {e}"

    def sleep(self, seconds):
        time.sleep(seconds)
