# -*- coding: utf-8 -*-
"""
Configuration settings for Selenium E2E Test Suite
"""

import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

# Application URLs
# frontend/vite.config.js hardcodes the dev server to port 3001; 3000 is
# assumed to be a separate deployed/live instance, not `npm run dev`.
APP_URL = os.environ.get("WMS_APP_URL", "http://localhost:3001")
API_URL = os.environ.get("WMS_API_URL", "http://localhost:8080/api/v1")

# Excel Report Path
EXCEL_REPORT_PATH = BASE_DIR.parent / "docs" / "test" / "test_final.xlsx"
MD_REPORT_PATH = BASE_DIR.parent / "docs" / "test" / "result_test.md"

# Browser Settings
HEADLESS = os.environ.get("SELENIUM_HEADLESS", "true").lower() == "true"
EXPLICIT_WAIT = 10  # seconds
IMPLICIT_WAIT = 5   # seconds

# Test Users
# Login is by email (see frontend/src/pages/Auth/Login.jsx), not a "username".
# All 10 role accounts below are real HP-warehouse-scoped seed accounts,
# sharing the password "password123", supplied directly by the user.
# Each is overridable via its own env var pair if the account ever changes.
ADMIN_USER = {
    "username": os.environ.get("WMS_ADMIN_EMAIL", "admin@phucanh.vn"),
    "password": os.environ.get("WMS_ADMIN_PASSWORD", "password123"),
}
CEO_USER = {
    "username": os.environ.get("WMS_CEO_EMAIL", "ceo@phucanh.vn"),
    "password": os.environ.get("WMS_CEO_PASSWORD", "password123"),
}
STOREKEEPER_USER = {
    "username": os.environ.get("WMS_STOREKEEPER_EMAIL", "storekeeperHP@gmail.com"),
    "password": os.environ.get("WMS_STOREKEEPER_PASSWORD", "password123"),
}
WAREHOUSE_MANAGER_USER = {
    "username": os.environ.get("WMS_WAREHOUSE_MANAGER_EMAIL", "manager.hp@phucanh.vn"),
    "password": os.environ.get("WMS_WAREHOUSE_MANAGER_PASSWORD", "password123"),
}
WAREHOUSE_STAFF_USER = {
    "username": os.environ.get("WMS_WAREHOUSE_STAFF_EMAIL", "hpwhstaff@gmail.com"),
    "password": os.environ.get("WMS_WAREHOUSE_STAFF_PASSWORD", "password123"),
}
PLANNER_USER = {
    "username": os.environ.get("WMS_PLANNER_EMAIL", "planer@gmail.com"),
    "password": os.environ.get("WMS_PLANNER_PASSWORD", "password123"),
}
DISPATCHER_USER = {
    "username": os.environ.get("WMS_DISPATCHER_EMAIL", "dispatcher-HP@gmail.com"),
    "password": os.environ.get("WMS_DISPATCHER_PASSWORD", "password123"),
}
DRIVER_USER = {
    "username": os.environ.get("WMS_DRIVER_EMAIL", "driverHP@gmail.com"),
    "password": os.environ.get("WMS_DRIVER_PASSWORD", "password123"),
}
ACCOUNTANT_USER = {
    "username": os.environ.get("WMS_ACCOUNTANT_EMAIL", "accountantHP@phucanh.vn"),
    "password": os.environ.get("WMS_ACCOUNTANT_PASSWORD", "password123"),
}
ACCOUNTANT_MANAGER_USER = {
    "username": os.environ.get("WMS_ACCOUNTANT_MANAGER_EMAIL", "acc_managerHP@phucanh.vn"),
    "password": os.environ.get("WMS_ACCOUNTANT_MANAGER_PASSWORD", "password123"),
}
