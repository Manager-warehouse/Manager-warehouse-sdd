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


def flow_auth001(driver):
    """ADMIN creates a new user on /admin/users."""
    page = ModulePage(driver)
    page.navigate_to("/admin/users")
    tag = _tag()
    try:
        page.click_by_text("Tạo tài khoản")
        page.wait_for(lambda d: len(d.find_elements(
            By.XPATH, "//label[contains(normalize-space(.), 'Mã nhân viên')]")) > 0, timeout=10)

        page.type_by_label("Mã nhân viên", f"AUTOTEST-{tag}")
        page.type_by_label("Họ và tên nhân viên", "Selenium AutoTest")
        page.type_by_label("Địa chỉ Email", f"autotest.{tag}@example.com")
        page.type_by_label("Mật khẩu khởi tạo", "AutoTest123")

        return page.submit_and_verify("Lưu lại", "Mã nhân viên")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_mdm002(driver):
    """STOREKEEPER creates a product on /admin/products."""
    page = ModulePage(driver)
    page.navigate_to("/admin/products")
    tag = _tag()
    try:
        page.click_by_text("Thêm sản phẩm mới")
        page.wait_for(lambda d: len(d.find_elements(
            By.XPATH, "//label[contains(normalize-space(.), 'Mã SKU')]")) > 0, timeout=10)

        page.type_by_label("Mã SKU", f"AUTOTEST-{tag}")
        page.type_by_label("Tên sản phẩm", "Selenium AutoTest Product")
        # unit/unit_per_pack/weight/volume/reorder_point all have valid
        # defaults already (Cái/1/0/0/10), no need to touch them.

        return page.submit_and_verify("Tạo mới", "Mã SKU")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_rcv003(driver):
    """PLANNER creates a purchase receipt on /inbound/create."""
    page = ModulePage(driver)
    page.navigate_to("/inbound/create")
    tag = _tag()
    try:
        page.select_first_real_option_by_label("Nhà cung cấp")
        page.type_by_label("Người liên hệ", "Selenium AutoTest")
        page.type_by_label("Mã chứng từ nguồn", f"AUTOTEST-{tag}")

        found = page.search_and_pick_first_result("Tìm kiếm sản phẩm", "PROD-001")
        if not found:
            return False, "No product found via search (seed product PROD-001 missing?)"
        # expected_qty defaults to 1, unit_cost defaults to 0.00 -- both
        # already pass ReceiptForm's client validation as-is.

        return page.submit_and_verify("Lập Lệnh Nhập Kho", "Người liên hệ")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_out004(driver):
    """PLANNER creates a delivery order on /outbound/delivery-orders."""
    page = ModulePage(driver)
    page.navigate_to("/outbound/delivery-orders")
    try:
        page.click_by_text("Lập đơn xuất mới")
        page.wait_for(lambda d: len(d.find_elements(
            By.XPATH, "//label[contains(normalize-space(.), 'Đại lý nhận hàng')]")) > 0, timeout=10)

        page.select_first_real_option_by_label("Đại lý nhận hàng")
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 7 * 86400))
        page.set_date_by_label("Ngày giao dự kiến", future_date)

        # The item product select has no `label` prop (rendered bare
        # inside the items table), so it's the first <select> in the
        # only <table> on this modal -- pick the first real product.
        product_select = page.find_element(By.XPATH, "(//table//select)[1]")
        page.wait_for(lambda d: len(Select(product_select).options) > 1, timeout=15)
        Select(page.find_element(By.XPATH, "(//table//select)[1]")).select_by_index(1)

        page.wait_network_idle(idle_ms=500, timeout=10)
        if page.is_button_disabled("Tạo đơn xuất"):
            return False, "Skipped: submit disabled, likely no APPROVED price entry for the picked product/warehouse"

        return page.submit_and_verify("Tạo đơn xuất", "Đại lý nhận hàng")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_trf005(driver):
    """WAREHOUSE_MANAGER creates a transfer request on /transfers/requests."""
    page = ModulePage(driver)
    page.navigate_to("/transfers/requests")
    try:
        page.click_by_text("Tạo yêu cầu")
        page.wait_for(lambda d: len(d.find_elements(
            By.XPATH, "//label[contains(normalize-space(.), 'Kho nguồn')]")) > 0, timeout=10)

        page.select_first_real_option_by_label("Kho nguồn")
        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 7 * 86400))
        page.set_date_by_label("Ngày cần hàng", future_date)
        page.type_by_label("Lý do nghiệp vụ", "Selenium AutoTest transfer request")

        # The item product select has no dedicated <label> (rendered bare
        # inside the item row), so it's the first <select> in the item row
        # container -- pick the first real product.
        product_select = page.find_element(By.XPATH, "(//div[contains(@class,'p-3.5')]//select)[1]")
        page.wait_for(lambda d: len(Select(product_select).options) > 1, timeout=15)
        Select(page.find_element(By.XPATH, "(//div[contains(@class,'p-3.5')]//select)[1]")).select_by_index(1)

        # Quantity input sits next to the product select in the same row,
        # has no label, and no stable class -- it's the first number input.
        qty_input = page.find_element(By.XPATH, "(//div[contains(@class,'p-3.5')]//input[@type='number'])[1]")
        qty_input.clear()
        qty_input.send_keys("1")

        return page.submit_and_verify("Tạo bản nháp", "Kho nguồn")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_stk006(driver):
    """STOREKEEPER creates a stocktake on /stocktake/new."""
    page = ModulePage(driver)
    page.navigate_to("/stocktake/new")
    try:
        page.wait_network_idle(idle_ms=500, timeout=10)
        if page.is_button_disabled("Tạo phiếu kiểm kê"):
            return False, "Skipped: submit disabled (no open accounting period, or no warehouse selected)"

        return page.submit_and_verify("Tạo phiếu kiểm kê", "Kỳ kế toán")
    except Exception as e:
        return False, f"Exception: {e}"


def flow_prc007(driver):
    """ACCOUNTANT creates a price entry on /finance/price-list."""
    page = ModulePage(driver)
    page.navigate_to("/finance/price-list")
    try:
        page.click_by_text("Thêm bản giá")
        found = page.search_and_pick_first_result("Nhập tên sản phẩm", "PROD-001")
        if not found:
            return False, "No product found via search (seed product PROD-001 missing?)"

        future_date = time.strftime("%Y-%m-%d", time.localtime(time.time() + 1 * 86400))
        page.set_date_by_label("Hiệu lực từ ngày", future_date)
        page.type_by_label("Giá vốn", "100000")
        page.type_by_label("Giá bán", "150000")

        return page.submit_and_verify("Tạo bản giá", "Hiệu lực từ ngày")
    except Exception as e:
        return False, f"Exception: {e}"


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
        return False, f"Exception: {e}"


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
        return False, f"Exception: {e}"


def flow_rpt010(driver):
    """CEO: verify the dashboard's KPI cards render real computed values,
    not just that the route/RBAC resolved -- this is a data-presence check
    since the report is read-only (no create action exists)."""
    page = ModulePage(driver)
    page.navigate_to("/reports/ceo-dashboard")
    try:
        page.wait_network_idle(idle_ms=500, timeout=15)
        if page.has_error_toast():
            return False, "Error toast shown while loading dashboard"

        body_text = page.driver.find_element(By.TAG_NAME, "body").text
        if "Lỗi Truy Cập Báo Cáo" in body_text:
            return False, "Dashboard rendered its own error state (report API call failed)"
        if "VNĐ" not in body_text and "Top 5" not in body_text:
            return False, "Dashboard loaded but KPI cards did not render expected content"

        return True, "ok"
    except Exception as e:
        return False, f"Exception: {e}"
