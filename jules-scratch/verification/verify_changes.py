from playwright.sync_api import sync_playwright, expect

def run(playwright):
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context()
    page = context.new_page()

    try:
        # Navigate to the game
        page.goto("http://localhost:5173/")

        # Enter player name and join
        page.get_by_placeholder("Enter your name").fill("Jules")
        page.get_by_role("button", name="Join Game").click()

        # Wait for the lobby and click ready
        expect(page.get_by_role("button", name="Ready")).to_be_visible(timeout=10000)
        page.get_by_role("button", name="Ready").click()

        # Wait for the game canvases to be visible
        expect(page.locator(".top-down-container canvas")).to_be_visible(timeout=10000)
        expect(page.locator(".fpv canvas")).to_be_visible(timeout=10000)

        # Give it a moment to render the scene
        page.wait_for_timeout(1000)

        # Take a screenshot
        page.screenshot(path="jules-scratch/verification/verification.png")

    finally:
        browser.close()

with sync_playwright() as playwright:
    run(playwright)