import re
from playwright.sync_api import Page, expect, sync_playwright

def test_toggle_accessibility(page: Page):
    # 1. Arrange: Go to the app (need to login first)
    page.goto("http://localhost:3000/")

    # Wait for login page
    page.wait_for_selector("text=QA Verify & Track")

    try:
        page.click("button:has-text('Sign In')", timeout=2000)
    except:
         # Try to login if sign in fails or form is needed
         page.fill("input[type=email]", "test@example.com")
         page.fill("input[type=password]", "password123")
         page.click("button:has-text('Sign In')")

    try:
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
    toggle = page.locator("#dark-mode-toggle")

    # 3. Assert: Check accessibility attributes
    # The toggle might be true (Dark Mode on) or false. We just check if it has the attribute.
    expect(toggle).to_have_attribute("role", "switch")

    is_checked = toggle.get_attribute("aria-checked")
    print(f"Current state: {is_checked}")

    # Click it to toggle
    toggle.click()

    # Verify it flipped
    expected_new_state = "false" if is_checked == "true" else "true"
    expect(toggle).to_have_attribute("aria-checked", expected_new_state)

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
