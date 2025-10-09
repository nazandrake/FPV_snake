# Multiplayer Snake Game with First-Person View

This project is a real-time, two-player multiplayer snake game with a unique first-person 3D perspective, built with a modern tech stack and designed for easy deployment with Docker.

## Features

-   **Classic Snake Gameplay:** Control your snake, eat food to grow, and avoid collisions with walls, yourself, and the other player.
-   **Real-Time Multiplayer:** Play against another person in real-time on a shared game board.
-   **Dual Views:**
    -   **Top-Down View:** A classic 2D perspective of the entire game board.
    -   **First-Person View:** A 3D view from the snake's perspective, rendering the walls and the other player's snake.
-   **Distinct Player Colors:** Each snake has a unique and easily identifiable color.
-   **Keyboard Controls:** Use the arrow keys to control your snake's direction.
-   **Sound Effects:** Audio feedback for key events like eating food, winning, and losing.
-   **Containerized Deployment:** The entire application is containerized with Docker for simple, one-command deployment.

## Technologies Used

-   **Backend:** Kotlin with Ktor for handling WebSocket connections and game logic.
-   **Frontend:** Vue.js with Vite for a reactive and fast user interface.
-   **3D Rendering:** Three.js for the first-person view.
-   **Build Tools:** Gradle for the backend, npm for the frontend.
-   **Containerization:** Docker for building and running the application.

## Getting Started

### Prerequisites

-   Docker must be installed on your system.

### Build and Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd <repository-directory>
    ```

2.  **Build and run the Docker container:**
    From the root of the project, run the following command:
    ```bash
    docker build -t multiplayer-snake . && docker run -p 8080:8080 multiplayer-snake
    ```

3.  **Play the game:**
    Open two browser tabs and navigate to `http://localhost:8080`. The game will start once two players have connected.
    -   Control your snake with the **arrow keys**.
    -   The first player to connect will be red, and the second will be blue.

Enjoy the game!