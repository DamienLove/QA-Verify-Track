from playwright.sync_api import sync_playwright, expect
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context()
        page = context.new_page()
        page.goto("http://localhost:3000")

        # Handle Auth - Create Account
        # Check if we are on login page
        try:
            expect(page.get_by_text("QA Verify & Track")).to_be_visible(timeout=5000)

            # Click Sign Up toggle
            page.get_by_role("button", name="Need an account? Sign up").click()

            email = f"testuser_{int(time.time())}@example.com"
            password = "password123"

            page.fill("input[type='email']", email)
            page.fill("input[type='password']", password)

            page.get_by_role("button", name="Create Account").click()
        except Exception as e:
            print(f"Login step skipped or failed: {e}")

        # Wait for Home Page
        expect(page.get_by_text("My Projects")).to_be_visible(timeout=15000)

        # Click Notes button
        page.get_by_label("Open Notes").click()

        # Wait for Notes modal
        notes_modal = page.locator("div[role='dialog']")
        expect(notes_modal).to_be_visible()

        # Check title
        expect(page.locator("#notes-modal-title")).to_have_text("Notes")

        # Check textarea focus
        textarea = page.locator("textarea")
        expect(textarea).to_be_focused()

        # Check aria-labelledby
        actual_label = textarea.get_attribute("aria-labelledby")
        print(f"Actual aria-labelledby: '{actual_label}'")
        assert actual_label == "notes-modal-title"

        # Type something
        textarea.fill("Hello World")

        # Take screenshot of open modal
        page.screenshot(path="verification/verification_notes_ux.png")

        # Press Escape
        page.keyboard.press("Escape")

        # Expect modal to close
        expect(notes_modal).not_to_be_visible()

        print("Verification successful!")

        browser.close()

if __name__ == "__main__":
    run()
