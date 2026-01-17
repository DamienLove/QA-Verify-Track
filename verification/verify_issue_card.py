
import time
from playwright.sync_api import sync_playwright, expect

def verify_dashboard(page):
    # Navigate to localhost:3000 (from vite.config.ts)
    page.goto("http://localhost:3000")

    # Wait for potential redirect or load
    time.sleep(2)

    # Check if we are on login page
    if page.get_by_text("QA Verify & Track").is_visible():
        print("On Login Page")

        # Try to login/signup
        page.fill("input[type='email']", "bolt@example.com")
        page.fill("input[type='password']", "password123")

        # Try Sign In first
        page.click("button:has-text('Sign In')")
        time.sleep(2)

        # If still on page, try Sign Up
        if page.get_by_text("QA Verify & Track").is_visible():
             print("Sign In failed/not found, switching to Sign Up")
             # Toggle to sign up
             if page.get_by_text("Need an account?").is_visible():
                 page.click("button:has-text('Need an account?')")
                 time.sleep(1)

             page.fill("input[type='email']", "bolt@example.com")
             page.fill("input[type='password']", "password123")
             page.click("button:has-text('Create Account')")
             time.sleep(3)

    # Now we should be on HomePage.
    # Check if we see "My Projects" or similar.
    page.screenshot(path="verification/dashboard_home.png")

    # If no projects, we can't easily see the Dashboard/IssueCard.
    # But we can verify the app loads and renders without crashing.

    print("Screenshot taken at verification/dashboard_home.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_dashboard(page)
        except Exception as e:
            print(f"Error: {e}")
            page.screenshot(path="verification/error.png")
        finally:
            browser.close()
