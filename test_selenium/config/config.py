# -*- coding: utf-8 -*-
"""
Configuration settings for Selenium E2E Test Suite
"""

import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

# Application URLs
APP_URL = os.environ.get("WMS_APP_URL", "http://localhost:3000")
API_URL = os.environ.get("WMS_API_URL", "http://localhost:8080/api/v1")

# Excel Report Path
EXCEL_REPORT_PATH = BASE_DIR.parent / "docs" / "test" / "test_final.xlsx"
MD_REPORT_PATH = BASE_DIR.parent / "docs" / "test" / "result_test.md"

# Browser Settings
HEADLESS = os.environ.get("SELENIUM_HEADLESS", "true").lower() == "true"
EXPLICIT_WAIT = 10  # seconds
IMPLICIT_WAIT = 5   # seconds

# Test Users
ADMIN_USER = {
    "username": "admin",
    "password": "Password123!",
}
MANAGER_USER = {
    "username": "wh_manager_hp",
    "password": "Password123!",
}
STOREKEEPER_USER = {
    "username": "storekeeper_hp",
    "password": "Password123!",
}
