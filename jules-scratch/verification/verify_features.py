from playwright.sync_api import Page, expect

def test_game_features(page: Page):
    """
    This test verifies the new game features:
    - Unlimited players (by adding more than one AI player).
    - Colored player names in the lobby, scoreboard, and game-over screen.
    """
    # 1. Navigate to the app and join the game.
    page.goto("http://localhost:5173")
    page.get_by_placeholder("Enter your name").fill("Jules")
    page.get_by_role("button", name="Join Game").click()

    # 2. Add AI players and take a screenshot of the lobby.
    page.get_by_role("button", name="Add AI Player").click()
    page.get_by_role("button", name="Add AI Player").click()
    page.screenshot(path="jules-scratch/verification/lobby.png")

    # 3. Start the game and take a screenshot of the game view.
    page.get_by_role("button", name="I'm Ready!").click()
    expect(page.get_by_role("heading", name="Scores")).to_be_visible()
    page.screenshot(path="jules-scratch/verification/game_view.png")

    # 4. Wait for the game to end and take a screenshot of the game-over screen.
    expect(page.get_by_role("heading", name="Game Over")).to_be_visible(timeout=60000) # Wait up to a minute for the game to end
    page.screenshot(path="jules-scratch/verification/game_over.png")