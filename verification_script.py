import os
from playwright.sync_api import sync_playwright

def verify_app(page):
    # Navigate to the app
    page.goto("http://localhost:3000")

    # Wait for the login screen or home screen
    page.wait_for_load_state("networkidle")

    # Screenshot the login page
    os.makedirs("/home/jules/verification", exist_ok=True)
    page.screenshot(path="/home/jules/verification/login_screen.png")
    print("Screenshot taken: login_screen.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_app(page)
        except Exception as e:
            print(f"Error: {e}")
        finally:
            browser.close()
