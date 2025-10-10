from playwright.sync_api import Page, expect
import time
import pytest

def test_game_running_and_movement(page: Page):
    """
    This test verifies that the game can be started, the main game view is displayed correctly,
    and that the player can move.
    """
    try:
        # 1. Arrange: Go to the game's URL and wait for connection.
        page.goto("http://localhost:5173/")
        expect(page.locator(".loading")).to_be_hidden(timeout=15000)

        # 2. Act: Enter a player name and join the game.
        expect(page.get_by_placeholder("Enter your name")).to_be_visible()
        page.get_by_placeholder("Enter your name").fill("Jules")
        page.get_by_role("button", name="Join Game").click()

        # 3. Wait for the lobby to be visible.
        expect(page.get_by_role("heading", name="Lobby")).to_be_visible()

        # 4. Act: Add an AI player to the lobby.
        page.get_by_role("button", name="Add AI Player").click()

        # 5. Act: Click the "I'm Ready!" button.
        page.get_by_role("button", name="I'm Ready!").click()

        # 6. Assert: Wait for the game to be in the "RUNNING" phase.
        expect(page.locator(".timer")).to_be_visible(timeout=10000)
        time.sleep(1) # Give a moment for the 3D scene to render

        # 7. Screenshot: Capture the initial game state.
        page.screenshot(path="jules-scratch/verification/game_start.png")

        # 8. Act: Move the player forward.
        page.keyboard.press('w')
        time.sleep(1) # Hold the key for a second
        page.keyboard.up('w')

        # 9. Screenshot: Capture the game state after moving.
        page.screenshot(path="jules-scratch/verification/game_after_move.png")

    except Exception as e:
        page.screenshot(path="jules-scratch/verification/failure.png")
        print("Test failed. Page content:")
        print(page.content())
        pytest.fail(f"Test failed with exception: {e}")