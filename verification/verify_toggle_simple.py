from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto("http://localhost:3000/#/config")
        # Just dump the page content to see what's loaded
        print(page.content())
        browser.close()

if __name__ == "__main__":
    run()
