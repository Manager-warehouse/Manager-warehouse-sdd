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

import re
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

        # The actual root cause (found via screenshot, not guessing): "Mã
        # nhân viên" -- the first field typed right after the modal opens --
        # was left completely empty, with Chrome's own native "Please fill
        # out this field" tooltip blocking submit silently (invisible to
        # has_error_toast()/get_visible_error_text(), which only see the
        # app's own DOM). Modal.jsx animates in over 300ms; a native click's
        # coordinates can be computed mid-transition and miss. Every field
        # here is typed in that same animation window, so all four go
        # through the JS-driven setter instead of click+send_keys.
        ok, reason = page.type_by_testid_js("user-form-code", f"AUTOTEST-{tag}")
        if not ok:
            return False, reason
        ok, reason = page.type_by_testid_js("user-form-full-name", "Selenium AutoTest")
        if not ok:
            return False, reason
        ok, reason = page.type_by_testid_js("user-form-email", f"autotest.{tag}@example.com")
        if not ok:
            return False, reason
        ok, reason = page.type_by_testid_js("user-form-password", "AutoTest123")
        if not ok:
            return False, reason

        return page.submit_and_verify_by_testid("user-form-submit", "user-form-code", use_js=True)
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
        # RCV-003 showed a deterministic, environment-proof failure of
        # WebDriver's native click/send_keys on this page -- identical
        # across headless/headed, session order, native-Chrome-popup
        # suppression, and a full dev-server restart -- while the exact
        # same account/pages/sequence worked every time through a
        # different (CDP-based) automation path. That points at the
        # native WebDriver input-dispatch mechanism itself, not the app,
        # so every interaction on this flow goes through the JS-driven
        # variants (native-setter value + dispatched click) instead.
        ok, reason = page.type_by_label_js("Người liên hệ", "Selenium AutoTest")
        if not ok:
            return False, reason
        ok, reason = page.type_by_label_js("Mã chứng từ nguồn", f"AUTOTEST-{tag}")
        if not ok:
            return False, reason

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
            use_js=True,
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

        return page.submit_and_verify("Lập Lệnh Nhập Kho", "Người liên hệ", use_js=True)
    except Exception as e:
        return False, _short_exc(e)


def flow_out004(driver):
    """PLANNER creates a delivery order on /outbound/delivery-orders."""
    page = ModulePage(driver)
    page.navigate_to("/outbound/delivery-orders")
    try:
        # OUT-004 showed a deterministic, environment-proof failure of
        # WebDriver's native click (document.activeElement stayed at BODY
        # even after two native clicks on a button Selenium itself
        # confirmed was present/displayed/enabled) -- identical across
        # headless/headed, session order, native-Chrome-popup suppression,
        # and a full dev-server restart, while manual reproduction of the
        # exact same account/page/sequence always worked. That points at
        # the native WebDriver click dispatch itself, not the app, so this
        # (and every other interaction below) goes through the JS-driven
        # click/value-set variants instead.
        page.click_via_js_testid("open-create-do-modal")
        dealer_select_present = page.wait_for(lambda d: len(d.find_elements(
            By.CSS_SELECTOR, "[data-testid='do-dealer-select']")) > 0, timeout=10)
        if not dealer_select_present:
            page.click_via_js_testid("open-create-do-modal")
            dealer_select_present = page.wait_for(lambda d: len(d.find_elements(
                By.CSS_SELECTOR, "[data-testid='do-dealer-select']")) > 0, timeout=15)
        if not dealer_select_present:
            active = driver.execute_script(
                "const el = document.activeElement;"
                "return el ? (el.tagName + (el.getAttribute('data-testid') ? '[data-testid=' + el.getAttribute('data-testid') + ']' : '')) : 'null';"
            )
            return False, f"Modal never opened even via JS click; document.activeElement: {active}"

        page.select_first_real_option_by_testid("do-dealer-select")
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 7 * 86400))
        page.set_date_by_testid("do-delivery-date", future_date)

        # Blindly picking the first product in the catalog is why this used
        # to stall on "no APPROVED price entry" -- PRC-007 only ever creates
        # PENDING entries (need Kế toán trưởng approval), so there's no
        # guarantee the first product has a usable price at all. Ask the
        # backend which products already have an APPROVED price and pick
        # one of those instead. The LIST endpoint (GET /price-history?
        # status=APPROVED) turned out to be 403 for PLANNER -- only the
        # per-product LOOKUP endpoint is actually in PLANNER's scope (it's
        # the same one DeliveryOrders.jsx itself calls via
        # lookupItemPrice() once a product is picked), so check candidate
        # products one at a time against that instead of listing them all.
        warehouse_id = driver.execute_script(
            "const w = sessionStorage.getItem('wms_active_warehouse');"
            "return w ? JSON.parse(w).id : null;"
        )
        today = time.strftime("%Y-%m-%d")
        picked_product_id = None
        if warehouse_id:
            product_ids = [
                opt.get_attribute("value")
                for opt in Select(page.by_testid("do-item-product-0")).options
                if opt.get_attribute("value")
            ]
            for pid in product_ids[:20]:
                status, _ = page.fetch_authenticated(f"/api/v1/price-history/lookup?productId={pid}&warehouseId={warehouse_id}&date={today}")
                if status == 200:
                    picked_product_id = pid
                    break

        if picked_product_id and page.select_option_by_value_testid("do-item-product-0", picked_product_id):
            pass
        else:
            # No product with an APPROVED price exists at all -- fall back
            # to the old behavior so the failure reason stays accurate
            # (still not this flow's fault to fix; PLANNER can't approve
            # prices) instead of silently picking something that won't work.
            page.select_first_real_option_by_testid("do-item-product-0")

        page.wait_network_idle(idle_ms=500, timeout=10)
        if not page.by_testid("do-submit").is_enabled():
            return False, "Skipped: submit disabled -- no product with an APPROVED price entry exists for this warehouse yet (needs Kế toán trưởng approval first, outside PLANNER's role)"

        return page.submit_and_verify_by_testid("do-submit", "do-dealer-select", use_js=True)
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
        # Rotate WHICH matching result gets picked too (not just the date
        # below) -- always taking index 0 means every run competes for the
        # same product+date combo, and a narrow safe date window alone
        # can still saturate after enough reruns (confirmed: hit
        # OVERLAPPING_EFFECTIVE_DATE even with the 1-5 day spread).
        # Product x date gives a much larger collision-free space than
        # widening the date range would (which risks landing outside the
        # open accounting period again).
        found, reason = page.search_and_pick_first_result(
            "Nhập tên sản phẩm", "a",
            input_testid="price-product-search", result_testid="price-product-search-result",
            pick_index=int(_tag()) % 7,
        )
        if not found:
            return False, reason

        # A fixed "tomorrow" collides with itself on every rerun within
        # the same day: same first-matched product + same warehouse + same
        # effective_date is a duplicate price entry, and the backend
        # correctly 409s it -- that's the app doing its job, not a defect.
        # A wide spread (previously up to 300 days) overshot into a period
        # that isn't open yet -- a real run hit PERIOD_CLOSED for 2026-12,
        # meaning only a window near "today" is actually usable. Keep the
        # spread small so it stays inside the open period.
        page.type_by_testid("price-cost-price", "100000")
        page.type_by_testid("price-selling-price", "150000")

        # A random pick of product x date can still collide often enough
        # to be worth actively reacting to (confirmed: kept hitting
        # OVERLAPPING_EFFECTIVE_DATE across several reruns even after
        # widening the pick space) -- rather than gamble again on a wider
        # probabilistic range, retry with a fresh date on that specific
        # error, which is a guaranteed fix within a few attempts instead
        # of a hopeful one.
        for attempt in range(6):
            offset_days = 1 + attempt + (int(_tag()) % 5)
            future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + offset_days * 86400))
            page.set_date_by_testid("price-effective-date", future_date)

            passed, detail = page.submit_and_verify_by_testid("price-submit", "price-effective-date")
            if passed or "OVERLAPPING_EFFECTIVE_DATE" not in detail:
                return passed, detail
        return passed, detail
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

        # The default dealer (index 1) frequently has no unpaid invoice --
        # that's a fact about this one dealer, not about whether any dealer
        # anywhere has one. handleDealerChangeInPayment() recomputes
        # available invoices from data already fetched client-side (no
        # extra network round-trip per dealer), so iterate dealers instead
        # of giving up on the first one, the same fix that worked for
        # OUT-004's product picker.
        dealer_select = page._field_for_label("Đại lý nộp tiền")
        dealer_options = Select(dealer_select).options
        found_dealer_with_invoice = False
        for idx in range(1, min(len(dealer_options), 21)):
            Select(page._field_for_label("Đại lý nộp tiền")).select_by_index(idx)
            invoice_select = page._field_for_label("Hóa đơn cấn trừ")
            if len(Select(invoice_select).options) > 1:
                found_dealer_with_invoice = True
                break
        if not found_dealer_with_invoice:
            checked = min(len(dealer_options) - 1, 20)
            return False, f"Skipped: no unpaid invoice found for any of the first {checked} dealers checked"
        invoice_select = page._field_for_label("Hóa đơn cấn trừ")
        Select(invoice_select).select_by_index(1)

        # The displayed "Dư nợ: X" comes from this page's own cached
        # invoice list (fetched once on load), which can already be stale
        # relative to the backend's authoritative remaining -- confirmed:
        # submitting half of that displayed figure still hit
        # OVERPAYMENT_EXCEEDS_INVOICE. The error message itself states the
        # backend's real remaining balance ("...exceeds invoice remaining
        # balance of X"), so use THAT as ground truth on retry instead of
        # trusting the frontend figure at all.
        selected_text = Select(invoice_select).first_selected_option.text
        match = re.search(r"Dư nợ:\s*([\d.,]+)", selected_text)
        remaining = int(match.group(1).replace(",", "").replace(".", "")) if match else 0

        form_scope = "//form[.//label[contains(normalize-space(.), 'Đại lý nộp tiền')]]"
        for attempt in range(4):
            safe_amount = max(1, remaining // 2)
            page.type_by_label_js("Số tiền thu nợ", str(safe_amount))
            page.wait_network_idle(idle_ms=300, timeout=5)
            # The header trigger button reads "Ghi nhận Phiếu thu (Quét
            # OCR)", a superset of the modal's actual submit text, and
            # stays in the DOM behind the modal -- scope the click to the
            # form so it can't match the wrong (header) button.
            passed, detail = page.submit_and_verify("Ghi nhận Phiếu thu", "Đại lý nộp tiền", scope_xpath=form_scope)
            if passed:
                return passed, detail
            overpay_match = re.search(r"exceeds invoice remaining balance of ([\d.]+)", detail)
            if not overpay_match:
                return passed, detail
            remaining = int(float(overpay_match.group(1)))
        return passed, detail
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
        # fetchData() on the CREATE tab awaits FOUR sequential API calls in
        # series (getReturns -> getDealers -> getBinLocations ->
        # getDeliveryOrders), and getDeliveryOrders -- the one that
        # actually populates this dropdown -- is last. A generic
        # wait_network_idle(500ms) can easily read "settled" in the gap
        # between two of those awaited calls, before the last one has even
        # fired, and conclude "no DO available" while it's still empty.
        # Confirmed by manual testing: DOs do exist. Poll the dropdown
        # itself for real options instead of trusting a generic network-
        # idle heuristic, the same pattern already used for every other
        # async-populated <select> in this suite.
        page.wait_network_idle(idle_ms=500, timeout=10)
        page.wait_for(lambda d: len(Select(page._field_for_label("Chọn đơn xuất hàng gốc")).options) > 1, timeout=15)
        do_options = Select(page._field_for_label("Chọn đơn xuất hàng gốc")).options
        if len(do_options) <= 1:
            return False, "Skipped: no DELIVERED/COMPLETED delivery order available to build a return against"

        # Always picking the same DO eventually collides with its own
        # prior test runs (RETURN_EXCEEDS_ORIGINAL_SALE, once enough
        # cumulative returns land against it) -- retrying across DOs
        # alone still hit product ID 11 twice in a row, meaning it's
        # likely a common/low-ID product showing up as row 1 across many
        # DOs, with its allowance broadly exhausted from repeated testing.
        # Retry across ITEM ROWS within each DO too, not just across DOs --
        # a different row is a different product, which may still have
        # return capacity even if row 1's product is maxed out everywhere.
        num_options = len(do_options) - 1
        start_idx = 1 + (int(_tag()) % num_options)
        passed, detail = False, "no DO tried"
        for tried in range(min(num_options, 10)):
            idx = 1 + ((start_idx - 1 + tried) % num_options)
            Select(page._field_for_label("Chọn đơn xuất hàng gốc")).select_by_index(idx)
            page.wait_network_idle(idle_ms=500, timeout=10)

            qty_inputs = page.find_elements(By.XPATH, "//table//input[@type='number']")
            for row_idx in range(min(len(qty_inputs), 10)):
                # Re-fetch fresh each attempt and reset every row: only the
                # target row gets "1" (the rest "0", excluded by
                # itemsToSubmit's expectedQty > 0 filter), so exactly one
                # product is attempted per submit.
                qty_inputs = page.find_elements(By.XPATH, "//table//input[@type='number']")
                for i, inp in enumerate(qty_inputs):
                    inp.clear()
                    inp.send_keys("1" if i == row_idx else "0")

                passed, detail = page.submit_and_verify("Lập phiếu trả hàng", "Chọn đơn xuất hàng gốc")
                if passed or "RETURN_EXCEEDS_ORIGINAL_SALE" not in detail:
                    return passed, detail
        return passed, detail
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
