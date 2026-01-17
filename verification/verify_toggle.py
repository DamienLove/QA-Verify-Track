import re
from playwright.sync_api import Page, expect, sync_playwright

def test_toggle_accessibility(page: Page):
    # 1. Arrange: Go to the app (need to login first)
    # Since login is required, we need to bypass or handle it.
    # We will try to create an account or login.
    page.goto("http://localhost:3000/")

    # Wait for login page
    page.wait_for_selector("text=QA Verify & Track")

    # Login (using a dummy account if needed, or creating one)
    # Assuming we can just create one easily.
    # Fill email and password
    page.fill("input[type=email]", "test@example.com")
    page.fill("input[type=password]", "password123")

    # Click "Create Account" or "Sign In"
    # The button text depends on state, let's assume it defaults to Sign In, but we might need to toggle to Create Account
    # First, let's just try to click the button.
    # If it fails, we handle it.

    # Actually, let's switch to "Create Account" to be safe?
    # Or just try to sign in. The app uses Firebase Auth, which might fail without network or config.
    # However, the user provided memory says: "Automated verification scripts for protected routes must implement a login or account creation flow to bypass Firebase Authentication guards."

    # Let's try to sign in.
    page.click("button:has-text('Sign In')")

    # Wait for navigation to Home Page or Dashboard
    # If sign in fails (e.g. user doesn't exist), we might need to handle that.
    # But let's assume for this test environment we might need to mock auth or use a known user?
    # Wait, I don't have a known user.
    # I should try to click "Need an account? Sign up" first.

    try:
        # Check if we are still on login page after a moment
        page.wait_for_timeout(2000)
        if page.is_visible("text=QA Verify & Track"):
             page.click("text=Need an account? Sign up")
             page.fill("input[type=email]", "test" + str(re.sub(r'\D', '', str(page.evaluate("Date.now()")))) + "@example.com")
             page.fill("input[type=password]", "password123")
             page.click("button:has-text('Create Account')")
    except:
        pass

    # Wait for home page
    page.wait_for_selector("text=My Projects", timeout=10000)

    # Navigate to Config page
    page.click("a[href='#/config']")

    # Wait for Config page
    page.wait_for_selector("text=Configuration")

    # 2. Act: Find the toggle buttons
    # We are looking for the Dark Mode toggle
    toggle = page.locator("#dark-mode-toggle")

    # 3. Assert: Check accessibility attributes
    expect(toggle).to_have_attribute("role", "switch")
    expect(toggle).to_have_attribute("aria-checked", "false") # Assuming default is light mode

    # Click it to toggle
    toggle.click()
    expect(toggle).to_have_attribute("aria-checked", "true")

    # 4. Screenshot
    page.screenshot(path="verification/verification.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_toggle_accessibility(page)
        except Exception as e:
            print(f"Test failed: {e}")
            page.screenshot(path="verification/error.png")
            raise e
        finally:
            browser.close()
