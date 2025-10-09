from playwright.sync_api import sync_playwright, expect

def run_verification(playwright):
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page()

    try:
        # Set a higher timeout for the goto action
        page.goto("http://localhost:5173", timeout=90000)

        # 1. Join the game for the first time
        page.get_by_placeholder("Enter your name").fill("Human Player")
        page.get_by_role("button", name="Join Game").click()
        expect(page.get_by_role("heading", name="Lobby")).to_be_visible()
        expect(page.get_by_text("Human Player")).to_be_visible()

        # 2. Add AI Player and verify it's added
        add_ai_button = page.get_by_role("button", name="Add AI Player")
        expect(add_ai_button).to_be_visible()
        add_ai_button.click()
        expect(page.get_by_text("Computer")).to_be_visible()
        expect(add_ai_button).to_be_hidden()

        # 3. Click "Reset Lobby"
        reset_lobby_button = page.get_by_role("button", name="Reset Lobby")
        expect(reset_lobby_button).to_be_visible()
        page.screenshot(path="jules-scratch/verification/verification_lobby_before_reset.png")
        reset_lobby_button.click()

        # 4. Verify user is returned to the name input screen
        expect(page.get_by_placeholder("Enter your name")).to_be_visible()
        page.screenshot(path="jules-scratch/verification/verification_name_input_after_reset.png")

        # 5. Rejoin the game
        page.get_by_role("button", name="Join Game").click()
        expect(page.get_by_role("heading", name="Lobby")).to_be_visible()

        # 6. Add AI player again and start the game
        add_ai_button = page.get_by_role("button", name="Add AI Player")
        expect(add_ai_button).to_be_visible()
        add_ai_button.click()
        page.get_by_role("button", name="I'm Ready!").click()

        # 7. Force a collision to end the game
        expect(page.locator("div.views-container")).to_be_visible()
        page.keyboard.press("ArrowDown")
        expect(page.get_by_role("heading", name="Game Over")).to_be_visible(timeout=30000)

        # 8. Return to lobby and verify AI is ready
        page.get_by_role("button", name="Start Again").click()
        expect(page.get_by_text("Computer - Ready")).to_be_visible()
        page.screenshot(path="jules-scratch/verification/verification_post_game_ai_ready.png")

    except Exception as e:
        print(f"An error occurred: {e}")
        page.screenshot(path="jules-scratch/verification/error.png")
        raise e  # re-raise the exception to fail the step
    finally:
        browser.close()

with sync_playwright() as playwright:
    run_verification(playwright)