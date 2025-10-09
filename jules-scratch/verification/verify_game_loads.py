from playwright.sync_api import sync_playwright, expect

def run_verification():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        try:
            # Go to the game page
            page.goto("http://localhost:8080")

            # Wait for the "Waiting for another player..." message to appear
            # This confirms the frontend is loaded and WebSocket is connected.
            waiting_message = page.locator("text=Waiting for another player...")
            expect(waiting_message).to_be_visible(timeout=15000) # Increased timeout for container startup

            # Take a screenshot to verify the initial state
            page.screenshot(path="jules-scratch/verification/verification.png")
            print("Screenshot taken successfully.")

        except Exception as e:
            print(f"An error occurred: {e}")
            # Take a screenshot even on failure for debugging
            page.screenshot(path="jules-scratch/verification/error.png")
        finally:
            browser.close()

if __name__ == "__main__":
    run_verification()