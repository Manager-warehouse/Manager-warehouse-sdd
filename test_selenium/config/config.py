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
# ADMIN_USER only unlocks /admin/users, /admin/config, /admin/audit-logs per
# AppRoutes.jsx RBAC. CEO_USER is used for the other 9 modules because CEO is
# allowed on nearly every protected route.
ADMIN_USER = {
    "username": os.environ.get("WMS_ADMIN_EMAIL", "admin@phucanh.vn"),
    "password": os.environ.get("WMS_ADMIN_PASSWORD", "password123"),
}
CEO_USER = {
    "username": os.environ.get("WMS_CEO_EMAIL", "ceo@phucanh.vn"),
    "password": os.environ.get("WMS_CEO_PASSWORD", "password123"),
}
