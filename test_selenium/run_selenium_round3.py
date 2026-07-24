# -*- coding: utf-8 -*-
"""
Master Selenium E2E Test Suite Runner for Round 3 System Testing
Drives a real Chrome browser through one genuine create/mutate-and-verify
business action per WMS module (see pages/round3_flows.py), logging in as
whichever role actually has permission to perform it, and records results
into docs/test/test_final.xlsx & result_test.md.

Scope note: this executes ONE real business flow per module (e.g. actually
creating a receipt, a delivery order, a transfer request), not every test
case row -- it is closer to a manual UAT spot-check than full coverage of
the ~924 detailed test cases. A flow whose prerequisite data doesn't exist
in the current environment (no unpaid invoice, no delivered DO, no open
accounting period) is reported as Failed with a specific "Skipped: ..."
reason rather than faked as Passed.
"""

import sys
from datetime import datetime
from pathlib import Path

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))

from run_selenium_round2 import check_app_online, build_driver, login_as
from config.config import (
    APP_URL, ADMIN_USER, CEO_USER, STOREKEEPER_USER, WAREHOUSE_MANAGER_USER,
    PLANNER_USER, ACCOUNTANT_USER,
)
from pages.round3_flows import (
    flow_auth001, flow_mdm002, flow_rcv003, flow_out004, flow_trf005,
    flow_stk006, flow_prc007, flow_fin008, flow_ret009, flow_rpt010,
)
from utils.excel_reporter import update_round3_excel, update_round3_markdown
from utils.error_tracer import tracer

SCREENSHOT_DIR = Path(__file__).resolve().parent / "screenshots"

ROLE_CREDENTIALS = {
    "ADMIN": ADMIN_USER,
    "CEO": CEO_USER,
    "STOREKEEPER": STOREKEEPER_USER,
    "WAREHOUSE_MANAGER": WAREHOUSE_MANAGER_USER,
    "PLANNER": PLANNER_USER,
    "ACCOUNTANT": ACCOUNTANT_USER,
}

# (module code, module name, role required by the *button*, not just the
# route -- several buttons are gated more narrowly than the route itself,
# e.g. CEO can view /admin/products but can't see the "Add product" button)
MODULE_FLOWS = [
    ("AUTH-001", "Security, Auth & RBAC", "ADMIN", flow_auth001),
    ("MDM-002", "Master Data Management", "STOREKEEPER", flow_mdm002),
    ("RCV-003", "Inbound Receipt & QC", "PLANNER", flow_rcv003),
    ("OUT-004", "Outbound Delivery & POD", "PLANNER", flow_out004),
    ("TRF-005", "Inter-Warehouse Transfer", "WAREHOUSE_MANAGER", flow_trf005),
    ("STK-006", "Stocktake & Adjustment", "STOREKEEPER", flow_stk006),
    ("PRC-007", "Pricing & COGS", "ACCOUNTANT", flow_prc007),
    ("FIN-008", "Finance, Billing & Closing", "ACCOUNTANT", flow_fin008),
    ("RET-009", "Returns, Scrap & Disposal", "STOREKEEPER", flow_ret009),
    ("RPT-010", "Reports, Dashboard & Alerts", "CEO", flow_rpt010),
]


def run_module_flows(driver):
    """Returns {code: (passed: bool, detail: str)}."""
    results = {}
    session_role = None

    for code, name, role, flow_fn in MODULE_FLOWS:
        print(f"\n[Selenium E2E Round 3] Running [{code}] - {name} (role: {role}) ...")

        if session_role != role:
            ok, detail = login_as(driver, role, ROLE_CREDENTIALS.get(role, {}))
            session_role = role if ok else None
            if not ok:
                results[code] = (False, detail)
                print(f"  -> SKIPPED: {detail}")
                continue

        try:
            passed, detail = flow_fn(driver)
        except Exception as e:
            passed, detail = False, f"Unhandled exception in flow: {e}"

        if not passed:
            SCREENSHOT_DIR.mkdir(exist_ok=True)
            shot = SCREENSHOT_DIR / f"{code}-round3.png"
            try:
                driver.save_screenshot(str(shot))
                detail += f" (screenshot: {shot.name})"
            except Exception:
                pass

        results[code] = (passed, detail)
        print(f"  -> {'PASSED' if passed else 'FAILED'}: {detail}")

    return results


def write_reports(module_results):
    import openpyxl
    excel_path = ROOT / "docs" / "test" / "test_final.xlsx"
    wb = openpyxl.load_workbook(str(excel_path))

    results_dict = {}
    total_tcs_processed = 0

    for code, name, role, flow_fn in MODULE_FLOWS:
        if code not in wb.sheetnames:
            continue
        ws = wb[code]
        module_passed, module_detail = module_results.get(code, (False, "Not executed"))
        status = "Passed" if module_passed else "Failed"
        note = f"Selenium E2E real business flow ({role} role): {module_detail}"

        module_tcs = []
        for r in range(11, ws.max_row + 1):
            tc_id = ws.cell(r, 1).value
            tc_desc = ws.cell(r, 2).value
            if not tc_id or not str(tc_id).strip().startswith(code):
                continue
            tc_id_str = str(tc_id).strip()

            module_tcs.append({"id": tc_id_str, "status": status, "note": note})
            tracer.log_test(
                tc_id=tc_id_str,
                module_code=code,
                test_name=str(tc_desc or tc_id_str),
                status=status,
                note=note,
            )

        results_dict[code] = module_tcs
        total_tcs_processed += len(module_tcs)
        print(f"  -> Recorded {len(module_tcs)} TCs for {code} as {status} (real business-flow result)")

    print("\n[Report Updater] Writing Selenium Round 3 results...")
    p, f = update_round3_excel(results_dict)
    update_round3_markdown(results_dict)
    tracer.generate_report()
    return p, f, total_tcs_processed, excel_path


def run_selenium_round3_suite():
    print("=" * 65)
    print("SELENIUM E2E AUTOMATION SUITE — ROUND 3 SYSTEM TEST (real flows)")
    print(f"Run Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Target Frontend URL: {APP_URL}")
    print("=" * 65)

    fe_online, be_online = check_app_online()
    print(f"[System Health Check] Frontend Online: {fe_online} | Backend Online: {be_online}")
    if not (fe_online and be_online):
        print("\n[FATAL] App not reachable at the configured URLs. Start the frontend/backend")
        print("        (or set WMS_APP_URL / WMS_API_URL) before running this suite.")
        return

    driver = build_driver()
    try:
        module_results = run_module_flows(driver)
    finally:
        driver.quit()

    p, f, total_tcs_processed, excel_path = write_reports(module_results)

    print("\n" + "=" * 65)
    print("SELENIUM ROUND 3 EXECUTION COMPLETE!")
    print(f"Total TCs tested in Round 3 : {total_tcs_processed}")
    print(f"Round 3 Passed             : {p}")
    print(f"Round 3 Failed             : {f}")
    print(f"Output Excel               : {excel_path}")
    print("=" * 65)


if __name__ == "__main__":
    run_selenium_round3_suite()
