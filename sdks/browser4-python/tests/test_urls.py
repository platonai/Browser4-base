"""
Test URL constants for integration tests.

Matches the TestUrls.kt file from Kotlin SDK tests to ensure
consistent test data across both SDKs.
"""

import os

# Mock server base URL
MOCK_SERVER_BASE = os.environ.get("MOCK_SERVER_BASE", "http://localhost:18080")

# Simple static page (for basic navigation tests)
SIMPLE_PAGE = f"{MOCK_SERVER_BASE}/ec/"

# Product list page
PRODUCT_LIST = f"{MOCK_SERVER_BASE}/ec/b?node=1292115012"

# Product detail page
PRODUCT_DETAIL = f"{MOCK_SERVER_BASE}/ec/dp/B0E000001"

# Generated assets base URL
GENERATED_BASE = f"{MOCK_SERVER_BASE}/generated"

# Assets base URL
ASSETS_BASE = f"{MOCK_SERVER_BASE}/assets"

# Assets-p base URL
ASSETS_P_BASE = f"{MOCK_SERVER_BASE}/assets-p"

# Simple DOM page for testing form interactions
SIMPLE_DOM = f"{ASSETS_BASE}/dom.html"

# Form test page for comprehensive form interaction tests
FORM_PAGE = f"{ASSETS_BASE}/test-pages/form-page.html"

# Error test page for testing error conditions and edge cases
ERROR_PAGE = f"{ASSETS_BASE}/test-pages/error-page.html"

# Keyboard test page for testing keyboard interactions
KEYBOARD_PAGE = f"{ASSETS_BASE}/test-pages/keyboard-test.html"

# Interactive page 1
INTERACTIVE_1 = f"{GENERATED_BASE}/interactive-1.html"

# Interactive page 2 (with hover card)
INTERACTIVE_2 = f"{GENERATED_BASE}/interactive-2.html"

# Multi-screen interactive page for scrolling tests
MULTI_SCREENS = f"{GENERATED_BASE}/interactive-screens.html"


def is_mock_server_running() -> bool:
    """
    Verify if mock server is running.
    
    Returns:
        True if the server is accessible, False otherwise.
    """
    import urllib.request
    try:
        with urllib.request.urlopen(MOCK_SERVER_BASE, timeout=5):
            return True
    except Exception:
        return False
