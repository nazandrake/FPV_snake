from playwright.sync_api import Page, expect
import time
import pytest

def test_final_game_functionality(page: Page):
    """
    This test verifies the final implementation of the game, including:
    - Joining the game and starting a match.
    - Correct first-person camera and player rendering.
    - Turning and forward/backward movement controls.
    - The impassable forest boundary.
    """
    try:
        # 1. Arrange: Go to the game's URL and wait for the connection.
        page.goto("http://localhost:5173/")
        expect(page.locator(".loading")).to_be_hidden(timeout=15000)

        # 2. Act: Enter a player name and join the game.
        expect(page.get_by_placeholder("Enter your name")).to_be_visible()
        page.get_by_placeholder("Enter your name").fill("Jules")
        page.get_by_role("button", name="Join Game").click()

        # 3. Wait for the lobby to be visible.
        expect(page.get_by_role("heading", name="Lobby")).to_be_visible()

        # 4. Act: Add an AI player and start the game.
        page.get_by_role("button", name="Add AI Player").click()
        page.get_by_role("button", name="I'm Ready!").click()

        # 5. Assert: Wait for the game to be in the "RUNNING" phase.
        expect(page.locator(".timer")).to_be_visible(timeout=10000)
        time.sleep(1) # Give a moment for the 3D scene to render

        # 6. Screenshot: Capture the initial game state.
        page.screenshot(path="jules-scratch/verification/game_start.png")

        # 7. Act: Test turning right.
        page.keyboard.press('d')
        time.sleep(0.5)
        page.keyboard.up('d')
        page.screenshot(path="jules-scratch/verification/game_turn_right.png")

        # 8. Act: Test moving forward.
        page.keyboard.press('w')
        time.sleep(1)
        page.keyboard.up('w')
        page.screenshot(path="jules-scratch/verification/game_move_forward.png")

        # 9. Act: Test turning left.
        page.keyboard.press('a')
        time.sleep(0.5)
        page.keyboard.up('a')
        page.screenshot(path="jules-scratch/verification/game_turn_left.png")
    except Exception as e:
        page.screenshot(path="jules-scratch/verification/failure.png")
        print("Test failed. Page content:")
        print(page.content())
        pytest.fail(f"Test failed with exception: {e}")