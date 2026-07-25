# -*- coding: utf-8 -*-
"""
Round 3 business-flow scripts: one real create/mutate-and-verify action per
module, using whichever role actually has permission to perform it (checked
against AppRoutes.jsx route guards AND each page's own hasRole()-gated
buttons -- several buttons are gated more narrowly than the route itself,
e.g. CEO can view /admin/products but the "Add product" button is hidden
from CEO specifically).

Each flow_* function takes a Selenium `driver` already logged in as the
correct role and returns (passed: bool, detail: str). A prerequisite that
legitimately doesn't exist in current data (no unpaid invoice to pay, no
delivered DO to return) is reported as a Skipped-style False with a specific
reason, not silently faked as Passed.
"""

import time
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from .wms_pages import ModulePage


def _tag():
    return str(int(time.time()))


def _short_exc(e):
    """Selenium exceptions stringify to their full message PLUS the raw
    multi-line ChromeDriver native stacktrace (that's what dumped a wall of
    `chromedriver!GetHandleVerifier [...]` lines for OUT-004) -- keep just
    the exception type and the first real message line."""
    first_line = str(e).splitlines()[0].strip() if str(e).strip() else "(no message)"
    return f"Exception: {type(e).__name__}: {first_line}"


def flow_auth001(driver):
    """ADMIN creates a new user on /admin/users."""
    page = ModulePage(driver)
    page.navigate_to("/admin/users")
    tag = _tag()
    try:
        page.by_testid_clickable("open-create-user-modal").click()
        page.wait_for(lambda d: len(d.find_elements(
            By.CSS_SELECTOR, "[data-testid='user-form-code']")) > 0, timeout=10)

        page.type_by_testid("user-form-code", f"AUTOTEST-{tag}")
        page.type_by_testid("user-form-full-name", "Selenium AutoTest")
        page.type_by_testid("user-form-email", f"autotest.{tag}@example.com")
        page.type_by_testid("user-form-password", "AutoTest123")

        return page.submit_and_verify_by_testid("user-form-submit", "user-form-code")
    except Exception as e:
        return False, _short_exc(e)


def flow_mdm002(driver):
    """STOREKEEPER creates a product on /admin/products."""
    page = ModulePage(driver)
    page.navigate_to("/admin/products")
    tag = _tag()
    try:
        page.by_testid_clickable("open-create-product-modal").click()
        page.wait_for(lambda d: len(d.find_elements(
            By.CSS_SELECTOR, "[data-testid='product-form-sku']")) > 0, timeout=10)

        page.type_by_testid("product-form-sku", f"AUTOTEST-{tag}")
        page.type_by_testid("product-form-name", "Selenium AutoTest Product")
        # unit/unit_per_pack/weight/volume/reorder_point all have valid
        # defaults already (Cái/1/0/0/10), no need to touch them.

        return page.submit_and_verify_by_testid("product-form-submit", "product-form-sku")
    except Exception as e:
        return False, _short_exc(e)


def flow_rcv003(driver):
    """PLANNER creates a purchase receipt on /inbound/create."""
    page = ModulePage(driver)
    page.navigate_to("/inbound/create")
    tag = _tag()
    try:
        page.select_first_real_option_by_label("Nhà cung cấp")
        page.type_by_label("Người liên hệ", "Selenium AutoTest")
        page.type_by_label("Mã chứng từ nguồn", f"AUTOTEST-{tag}")

        # Search a broad, near-universal substring instead of a specific
        # SKU: this flow only needs *some* valid product on the receipt to
        # prove creation works end-to-end, and ReceiptForm.jsx searches a
        # locally-fetched (size=200, presumably newest-first) product list
        # client-side -- a single hardcoded old SKU is exactly the kind of
        # thing that can silently fall outside that window as the catalog
        # grows (including from this suite's own AUTOTEST-* product runs).
        found, reason = page.search_and_pick_first_result(
            "Tìm kiếm sản phẩm", "a",
            input_testid="receipt-product-search", result_testid="receipt-product-search-result",
        )
        if not found:
            # "a" failing too means it's not about which SKU -- ask the
            # backend directly what /products actually returns for this
            # session instead of theorizing further.
            status, body = page.fetch_authenticated("/api/v1/products?page=0&size=200")
            api_summary = f"GET /products -> status={status}, body[:300]={str(body)[:300]}"
            return False, f"{reason} | {api_summary}"
        # expected_qty defaults to 1, unit_cost defaults to 0.00 -- both
        # already pass ReceiptForm's client validation as-is.

        return page.submit_and_verify("Lập Lệnh Nhập Kho", "Người liên hệ")
    except Exception as e:
        return False, _short_exc(e)


def flow_out004(driver):
    """PLANNER creates a delivery order on /outbound/delivery-orders."""
    page = ModulePage(driver)
    page.navigate_to("/outbound/delivery-orders")
    try:
        page.by_testid_clickable("open-create-do-modal").click()
        dealer_select_present = page.wait_for(lambda d: len(d.find_elements(
            By.CSS_SELECTOR, "[data-testid='do-dealer-select']")) > 0, timeout=8)
        if not dealer_select_present:
            modal_open = len(driver.find_elements(
                By.XPATH, "//h3[contains(normalize-space(.), 'Lập đơn xuất hàng')]")) > 0
            if not modal_open:
                # A click that lands during a transient re-render can
                # silently no-op (this is the exact OUT-004 click-not-
                # registering bug this suite hit before testids existed) --
                # only retry-click if the modal genuinely never opened, not
                # if it opened and just rendered its contents slowly (a
                # second click there would land on the backdrop, not the
                # now-hidden trigger).
                page.by_testid_clickable("open-create-do-modal").click()
            dealer_select_present = page.wait_for(lambda d: len(d.find_elements(
                By.CSS_SELECTOR, "[data-testid='do-dealer-select']")) > 0, timeout=15)
        if not dealer_select_present:
            return False, "Modal never opened: [data-testid='do-dealer-select'] absent after retry"

        page.select_first_real_option_by_testid("do-dealer-select")
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 7 * 86400))
        page.set_date_by_testid("do-delivery-date", future_date)

        # First item row is always present on open, so index 0 always exists.
        page.select_first_real_option_by_testid("do-item-product-0")

        page.wait_network_idle(idle_ms=500, timeout=10)
        if not page.by_testid("do-submit").is_enabled():
            return False, "Skipped: submit disabled, likely no APPROVED price entry for the picked product/warehouse"

        return page.submit_and_verify_by_testid("do-submit", "do-dealer-select")
    except Exception as e:
        return False, _short_exc(e)


def flow_trf005(driver):
    """WAREHOUSE_MANAGER creates a transfer request on /transfers/requests."""
    page = ModulePage(driver)
    page.navigate_to("/transfers/requests")
    try:
        page.by_testid_clickable("open-create-transfer-modal").click()
        page.wait_for(lambda d: len(d.find_elements(
            By.CSS_SELECTOR, "[data-testid='transfer-source-warehouse']")) > 0, timeout=10)

        page.select_first_real_option_by_testid("transfer-source-warehouse")
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 7 * 86400))
        page.set_date_by_testid("transfer-needed-date", future_date)
        page.type_by_testid("transfer-business-reason", "Selenium AutoTest transfer request")

        # First item row is always present on open, so index 0 always exists.
        page.select_first_real_option_by_testid("transfer-item-product-0")
        qty_field = page.by_testid("transfer-item-qty-0")
        qty_field.clear()
        qty_field.send_keys("1")

        return page.submit_and_verify_by_testid("transfer-submit", "transfer-source-warehouse")
    except Exception as e:
        return False, _short_exc(e)


def flow_stk006(driver):
    """STOREKEEPER creates a stocktake on /stocktake/new."""
    page = ModulePage(driver)
    page.navigate_to("/stocktake/new")
    try:
        page.wait_network_idle(idle_ms=500, timeout=10)
        if not page.by_testid("stocktake-submit").is_enabled():
            return False, "Skipped: submit disabled (no open accounting period, or no warehouse selected)"

        return page.submit_and_verify_by_testid("stocktake-submit", "stocktake-period-select")
    except Exception as e:
        return False, _short_exc(e)


def flow_prc007(driver):
    """ACCOUNTANT creates a price entry on /finance/price-list."""
    page = ModulePage(driver)
    page.navigate_to("/finance/price-list")
    try:
        page.by_testid_clickable("open-create-price-modal").click()
        # The modal fetches its own product list on mount (separate from
        # anything else this flow waits on, unlike flow_rcv003 which
        # incidentally waits on a shared Promise.all via the supplier
        # select) -- give it a moment to actually populate before typing
        # into the search box, or the search runs against an empty list.
        page.wait_network_idle(idle_ms=500, timeout=10)
        # Same reasoning as flow_rcv003 -- any valid product proves the
        # price-entry flow works, so search broadly instead of assuming a
        # specific SKU is within whatever page of products got fetched.
        found, reason = page.search_and_pick_first_result(
            "Nhập tên sản phẩm", "a",
            input_testid="price-product-search", result_testid="price-product-search-result",
        )
        if not found:
            return False, reason

        # A fixed "tomorrow" collides with itself on every rerun within
        # the same day: same first-matched product + same warehouse + same
        # effective_date is a duplicate price entry, and the backend
        # correctly 409s it -- that's the app doing its job, not a defect.
        # Spread the offset across ~300 days (deterministic per run via the
        # epoch-seconds tag) so same-day reruns don't collide with data
        # left behind by earlier runs.
        offset_days = 1 + (int(_tag()) % 300)
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + offset_days * 86400))
        page.set_date_by_testid("price-effective-date", future_date)
        page.type_by_testid("price-cost-price", "100000")
        page.type_by_testid("price-selling-price", "150000")

        return page.submit_and_verify_by_testid("price-submit", "price-effective-date")
    except Exception as e:
        return False, _short_exc(e)


def flow_fin008(driver):
    """ACCOUNTANT records a payment receipt against an unpaid invoice on
    /finance/invoices (standalone 'Ghi nhận Phiếu thu' entry point)."""
    page = ModulePage(driver)
    page.navigate_to("/finance/invoices")
    try:
        page.click_by_text("Ghi nhận Phiếu thu")
        page.wait_for(lambda d: len(d.find_elements(
            By.XPATH, "//label[contains(normalize-space(.), 'Đại lý nộp tiền')]")) > 0, timeout=10)
        page.wait_network_idle(idle_ms=500, timeout=10)

        invoice_select = page._field_for_label("Hóa đơn cấn trừ")
        options = Select(invoice_select).options
        if len(options) <= 1:
            return False, "Skipped: no unpaid invoice available for the default dealer to test payment recording against"
        Select(invoice_select).select_by_index(1)

        page.wait_network_idle(idle_ms=300, timeout=5)
        # The header trigger button reads "Ghi nhận Phiếu thu (Quét OCR)",
        # a superset of the modal's actual submit text, and stays in the
        # DOM behind the modal -- scope the click to the form so it can't
        # match the wrong (header) button.
        form_scope = "//form[.//label[contains(normalize-space(.), 'Đại lý nộp tiền')]]"
        return page.submit_and_verify("Ghi nhận Phiếu thu", "Đại lý nộp tiền", scope_xpath=form_scope)
    except Exception as e:
        return False, _short_exc(e)


def flow_ret009(driver):
    """STOREKEEPER creates a return receipt on /inbound/returns against a
    delivered DO."""
    page = ModulePage(driver)
    page.navigate_to("/inbound/returns")
    try:
        if page.is_button_disabled("Lập phiếu trả hàng mới"):
            return False, "Skipped: create button disabled (canManageReturnOperations gate)"
        page.click_by_text("Lập phiếu trả hàng mới")
        page.wait_network_idle(idle_ms=500, timeout=10)

        do_select = page._field_for_label("Chọn đơn xuất hàng gốc")
        if len(Select(do_select).options) <= 1:
            return False, "Skipped: no DELIVERED/COMPLETED delivery order available to build a return against"
        Select(do_select).select_by_index(1)
        page.wait_network_idle(idle_ms=500, timeout=10)

        qty_input = page.find_element(By.XPATH, "(//table//input[@type='number'])[1]")
        qty_input.clear()
        qty_input.send_keys("1")

        return page.submit_and_verify("Lập phiếu trả hàng", "Chọn đơn xuất hàng gốc")
    except Exception as e:
        return False, _short_exc(e)


def flow_rpt010(driver):
    """CEO: verify the dashboard's KPI cards render real computed values,
    not just that the route/RBAC resolved -- this is a data-presence check
    since the report is read-only (no create action exists)."""
    page = ModulePage(driver)
    page.navigate_to("/reports/ceo-dashboard")
    try:
        # wait_network_idle only tracks whether *new* requests started
        # recently -- a single slow aggregation query (P&L, top debtors,
        # KPIs computed across the whole system) that's already in flight
        # doesn't trip it, so it can return well before the dashboard
        # actually has data. Wait for the loading spinner itself to clear.
        page.wait_for(lambda d: "Đang tải dữ liệu báo cáo quản trị" not in
                      d.find_element(By.TAG_NAME, "body").text, timeout=30)

        if page.has_error_toast():
            return False, "Error toast shown while loading dashboard"

        body_text = page.driver.find_element(By.TAG_NAME, "body").text
        if "Đang tải dữ liệu báo cáo quản trị" in body_text:
            return False, "Dashboard still showing its loading spinner after 30s (aggregation query never returned)"
        if "Lỗi Truy Cập Báo Cáo" in body_text:
            return False, "Dashboard rendered its own error state (report API call failed)"
        if "VNĐ" not in body_text and "Top 5" not in body_text:
            return False, "Dashboard loaded but KPI cards did not render expected content"

        return True, "ok"
    except Exception as e:
        return False, _short_exc(e)
