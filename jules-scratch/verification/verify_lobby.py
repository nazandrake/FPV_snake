from playwright.sync_api import sync_playwright, Page, expect

def verify_lobby(page: Page):
    """
    This script verifies the lobby functionality of the snake game,
    and logs WebSocket traffic for debugging.
    """
    # Listen for all WebSocket events
    page.on("websocket", lambda ws: print(f"WebSocket created: {ws.url}"))
    page.on("websocket", lambda ws: ws.on("framereceived", lambda payload: print(f"Received: {payload}")))
    page.on("websocket", lambda ws: ws.on("framesent", lambda payload: print(f"Sent: {payload}")))
    page.on("websocket", lambda ws: ws.on("close", lambda: print("WebSocket closed")))

    # 1. Navigate to the app.
    page.goto("http://localhost:5173/")

    # 2. Wait for the name input to be visible.
    expect(page.get_by_placeholder("Enter your name")).to_be_visible(timeout=10000)

    # 3. Enter a player name and join the game.
    page.get_by_placeholder("Enter your name").fill("Jules")
    page.get_by_role("button", name="Join Game").click()

    # 4. Verify we are in the lobby and the player is not ready.
    expect(page.get_by_text("Lobby")).to_be_visible(timeout=10000)
    expect(page.get_by_text("Jules - Not Ready")).to_be_visible(timeout=10000)

    # 5. Click the ready button.
    page.get_by_role("button", name="I'm Ready!").click()

    # 6. Verify the player is now ready.
    expect(page.get_by_text("Jules - Ready")).to_be_visible(timeout=10000)
    expect(page.get_by_role("button", name="Waiting for others...")).to_be_visible()

    # 7. Take the final screenshot.
    page.screenshot(path="jules-scratch/verification/verification.png")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        verify_lobby(page)
        browser.close()

if __name__ == "__main__":
    main()