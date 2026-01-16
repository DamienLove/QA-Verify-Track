from playwright.sync_api import sync_playwright, expect
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Mock GitHub API responses

        # 1. Mock Stats (Search)
        page.route("**/search/issues*", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"total_count": 5}'
        ))

        # 2. Mock Issues list
        page.route("**/repos/test/test/issues?*", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='[]'
        ))

        # 3. Mock PR list (return one PR with conflicts)
        page.route("**/repos/test/test/pulls?*", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='''[{
                "id": 123,
                "number": 1,
                "title": "Test PR",
                "head": {"ref": "feature"},
                "base": {"ref": "main"},
                "user": {"login": "tester", "avatar_url": ""},
                "draft": false,
                "mergeable": false
            }]'''
        ))

        # 4. Mock PR Detail (mergeable: null)
        # We want to verify polling, so we return null a few times.
        # But for simplicity, let's just return null and see if the error message appears after timeout.
        # Or better, return null first, then null, then... wait, the app polls 5 times.
        # If I return null always, I expect the "Computing mergeability" error.
        page.route("**/repos/test/test/pulls/1", lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"mergeable": null}'
        ))

        # Also need to mock user/owner type check
        page.route("**/users/test", lambda route: route.fulfill(
             status=200,
             body='{"type": "User"}'
        ))

        page.goto("http://localhost:3000")

        # Login Flow
        # Check if we are on login page
        if page.locator("text=Sign In").is_visible():
            print("Logging in...")
            page.click("text=Need an account? Sign up")
            page.fill("input[type=email]", "test@example.com")
            page.fill("input[type=password]", "password123")
            page.click("text=Create Account")
            # Wait for navigation/login
            page.wait_for_timeout(2000)

        # Add Repository
        print("Adding repository...")
        # Check if we need to click 'Config' or if we are already there (fresh account has no repos)
        # Fresh account shows "No Projects Found".

        # If "No Projects Found" is visible, click "Go to Config" or use the header add button
        if page.locator("text=No Projects Found").is_visible():
             page.click("text=Go to Config")
        else:
             # Just go to config directly to be safe? Or click add button
             page.goto("http://localhost:3000/#/config")

        # Wait for "Add Repository" header
        expect(page.get_by_role("heading", name="Add Repository")).to_be_visible()

        page.fill("input[placeholder='org']", "test")
        page.fill("input[placeholder='repo']", "test")
        page.fill("input[value='New App']", "TestApp") # Name of app

        # Display Name
        # The first input is Display Name (based on code reading: formData.displayName input is first in "GitHub Details" section but after "Display Name" label)
        # Better use layout selectors or placeholders
        # There is no placeholder for Display Name, but it's the first input in "GitHub Details"
        # Let's use generic filling based on placeholders which are present for owner/repo

        # Owner and Repo are filled.
        # Display Name:
        page.locator("label:has-text('Display Name') + input").fill("Test Repo")

        # Use custom token toggle? Default is off (using global).
        # We need to set a token to make requests work (in real app), but we mocked network.
        # However, `App.tsx` checks `if (!activeToken)`.
        # So we MUST provide a token.

        # Toggle "Use repo-specific PAT"
        page.click("button:has(.bg-gray-300)") # Assuming it's the toggle

        # Fill Token
        page.fill("input[type=password]", "dummy_token")

        page.click("text=Save")

        # Expect to be back on list (Home)
        expect(page.get_by_text("Test Repo")).to_be_visible()

        # Go to Dashboard
        print("Navigating to dashboard...")
        page.click("a[aria-label='View repository dashboard']")

        # Switch to PRs tab
        print("Switching to PRs tab...")
        page.click("button:has-text('PRs')")

        # Expect to see the PR
        expect(page.get_by_text("Test PR")).to_be_visible()

        # In the mock, we returned mergeable: false (initially in list), so "Conflict" badge should be visible?
        # In `getPullRequests` service method: `hasConflicts: false` is default.
        # `App.tsx` uses `pr.hasConflicts` which comes from `getPullRequests`.
        # `githubService.getPullRequests` sets `hasConflicts: false` hardcoded.
        # So initially it shows "Merge" button, NOT "Resolve Conflicts".

        # Wait, if `hasConflicts` is false, it shows "Merge".
        # But `handleResolveConflicts` is called when?
        # Ah, the button logic:
        # {pr.hasConflicts ? ( Resolve... ) : ( Merge... )}

        # So initially I see "Merge".
        # If I click "Merge", it calls `handleMergeSequence`.

        # I want to test `handleResolveConflicts`.
        # This function is ONLY called if `pr.hasConflicts` is true.

        # How does `pr.hasConflicts` become true?
        # Only if I previously called something that set it?
        # Or if `getPullRequests` returned it?

        # `githubService.getPullRequests` implementation:
        # return response.map((pr: any) => ({ ... hasConflicts: false, ... }));

        # It seems `hasConflicts` is ALWAYS false initially.
        # So I can only click "Merge".

        # Let's check `App.tsx`:
        # const handleMergeSequence = async (pr: PullRequest) => { ... mergePR ... }

        # It seems the "Resolve Conflicts" button only appears if `pr.hasConflicts` is true.
        # But `hasConflicts` is initialized to `false`.
        # And I don't see any code in `handleSync` that updates `hasConflicts` based on detailed check.

        # Wait, maybe I missed something in `App.tsx` or `githubService.ts`.
        # `handleResolveConflicts` is the function I modified.
        # It is called when clicking "Resolve Conflicts".

        # If "Resolve Conflicts" button is never shown, I can't test it via UI click.

        # How does a user ever see "Resolve Conflicts"?
        # Maybe `handleMergeSequence` fails and sets it?
        # `handleMergeSequence` -> catch error -> setPrError. It doesn't seem to set `hasConflicts`.

        # Is there any way `hasConflicts` becomes true?
        # I'll grep for `hasConflicts`.
        pass

if __name__ == "__main__":
    run()
