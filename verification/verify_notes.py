from playwright.sync_api import sync_playwright, expect

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto("http://localhost:3000")

        # Wait for textarea
        textarea = page.locator("textarea")
        expect(textarea).to_be_visible()

        # Create a string longer than 10000
        long_text = "a" * 10005

        # Fill the textarea
        # Note: fill() usually checks maxLength, but we are enforcing it via React state logic
        # which might be slightly different. But let's try fill().
        # Actually, since it's a controlled component that truncates on change, fill() simulates typing.
        textarea.fill(long_text)

        # Check the value
        value = textarea.input_value()
        print(f"Value length: {len(value)}")

        assert len(value) == 10000, f"Expected 10000 chars, got {len(value)}"

        # Check the counter text
        counter = page.get_by_text("10000/10000")
        expect(counter).to_be_visible()

        page.screenshot(path="verification/verification.png")
        print("Verification successful!")
        browser.close()

if __name__ == "__main__":
    run()
