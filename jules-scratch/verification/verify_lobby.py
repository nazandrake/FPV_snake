from playwright.sync_api import Page, expect
import time
import pytest

def test_lobby_rendering(page: Page):
    """
    This test verifies that the lobby screen is rendered correctly after connecting to the server.
    """
    try:
        # 1. Arrange: Go to the game's URL and wait for the connection.
        page.goto("http://localhost:5173/")
        expect(page.locator(".loading")).to_be_hidden(timeout=15000)

        # 2. Act: Enter a player name and join the game.
        expect(page.get_by_placeholder("Enter your name")).to_be_visible()
        page.get_by_placeholder("Enter your name").fill("Jules")
        page.get_by_role("button", name="Join Game").click()

        # 3. Assert: Wait for the lobby to be visible.
        expect(page.get_by_role("heading", name="Lobby")).to_be_visible()

        # 4. Screenshot: Capture the lobby screen.
        page.screenshot(path="jules-scratch/verification/lobby_screen.png")

    except Exception as e:
        page.screenshot(path="jules-scratch/verification/failure.png")
        print("Test failed. Page content:")
        print(page.content())
        pytest.fail(f"Test failed with exception: {e}")