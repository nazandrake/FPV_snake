package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Serializable
enum class GamePhase {
    LOBBY,
    RUNNING,
    GAME_OVER
}

@Serializable
data class GameState(
    val players: Map<String, Player>,
    val food: Point,
    val obstacles: List<Point>,
    val boardSize: Int,
    val phase: GamePhase,
    val winner: String? = null
)

class Game {
    private val players = ConcurrentHashMap<String, Player>()
    private lateinit var food: Point
    private var obstacles = listOf<Point>()
    private val boardSize = 60
    var phase = GamePhase.LOBBY
        private set
    private var winner: String? = null

    init {
        food = generateFood()
    }

    fun getGameState(): GameState {
        return GameState(players, food, obstacles, boardSize, phase, winner)
    }

    fun addPlayer(id: String) {
        if (players.size >= 2) return // Do not allow more than 2 players
        // Allow multiple players
        val colors = listOf("#ff0000", "#0000ff", "#00ff00", "#ffff00")
        val startPositions = listOf(Point(5, 10), Point(55, 10), Point(5, 50), Point(55, 50))
        val playerIndex = players.size

        val color = colors.getOrElse(playerIndex) { "#ffffff" } // Default to white
        val startPoint = startPositions.getOrElse(playerIndex) { Point(10, 10) }

        val snake = mutableListOf(startPoint)
        players[id] = Player(id = id, snake = snake, direction = Direction.RIGHT, color = color, name = "Player ${playerIndex + 1}")
    }

    fun addAiPlayer() {
        if (players.size >= 2) return // Do not allow more than 2 players
        // Allow multiple players
        val colors = listOf("#ff0000", "#0000ff", "#00ff00", "#ffff00")
        val startPositions = listOf(Point(5, 10), Point(55, 10), Point(5, 50), Point(55, 50))
        val playerIndex = players.size

        val aiId = "ai-player-${(1000..9999).random()}"
        val color = colors.getOrElse(playerIndex) { "#ffffff" }
        val startPoint = startPositions.getOrElse(playerIndex) { Point(15, 15) }

        val snake = mutableListOf(startPoint)
        players[aiId] = Player(
            id = aiId,
            snake = snake,
            direction = Direction.RIGHT,
            color = color,
            name = "Computer",
            isAi = true,
            ready = true
        )
    }

    fun removePlayer(id: String) {
        players.remove(id)
        if (phase == GamePhase.RUNNING && players.size < 2) {
            phase = GamePhase.GAME_OVER
            winner = players.keys.firstOrNull()
        }
    }

    fun setPlayerName(id: String, name: String) {
        // If player doesn't exist, add them. This handles re-joining after a hard reset.
        if (!players.containsKey(id)) {
            addPlayer(id)
        }
        players[id]?.name = name
    }

    fun setPlayerReady(id: String, isReady: Boolean) {
        players[id]?.ready = isReady
        if (phase == GamePhase.LOBBY && players.size > 1 && players.values.all { it.ready }) {
            startGame()
        }
    }

    private fun startGame() {
        phase = GamePhase.RUNNING
        generateObstacles()
    }

    fun resetGame() {
        phase = GamePhase.LOBBY
        winner = null
        obstacles = listOf()
        // Reset players
        players.values.forEachIndexed { index, player ->
            player.score = 0
            player.ready = player.isAi // AI is always ready
            player.direction = Direction.RIGHT
            val startX = if (index == 0) 5 else 15
            player.snake.clear()
            player.snake.add(Point(startX, 10))
        }
        food = generateFood()
    }

    fun hardResetGame() {
        phase = GamePhase.LOBBY
        winner = null
        players.clear()
        obstacles = listOf()
        food = generateFood()
    }

    fun changeDirection(id: String, newDirection: Direction) {
        val player = players[id] ?: return
        // Prevent snake from reversing
        if (
            (newDirection == Direction.UP && player.direction != Direction.DOWN) ||
            (newDirection == Direction.DOWN && player.direction != Direction.UP) ||
            (newDirection == Direction.LEFT && player.direction != Direction.RIGHT) ||
            (newDirection == Direction.RIGHT && player.direction != Direction.LEFT)
        ) {
            player.direction = newDirection
        }
    }

    fun update() {
        if (phase != GamePhase.RUNNING) return

        // AI Player Logic
        val aiPlayers = players.values.filter { it.isAi }
        if (aiPlayers.isNotEmpty()) {
            val currentGameState = getGameState()
            aiPlayers.forEach { aiPlayer ->
                val nextDirection = AIPlayer.getNextDirection(currentGameState, aiPlayer.id)
                changeDirection(aiPlayer.id, nextDirection)
            }
        }

        moveSnakes()
        checkCollisions()
        checkFood()
    }

    private fun moveSnakes() {
        players.values.forEach { player ->
            val head = player.snake.first().let {
                when (player.direction) {
                    Direction.UP -> Point(it.x, it.y - 1)
                    Direction.DOWN -> Point(it.x, it.y + 1)
                    Direction.LEFT -> Point(it.x - 1, it.y)
                    Direction.RIGHT -> Point(it.x + 1, it.y)
                }
            }
            player.snake.add(0, head)
            // if snake does not eat food, tail is removed
            if (head != food) {
                player.snake.removeLast()
            }
        }
    }

    private fun checkCollisions() {
        val losers = mutableSetOf<String>()

        players.values.forEach { player ->
            val head = player.snake.first()

            // Wall collision
            if (head.x < 0 || head.x >= boardSize || head.y < 0 || head.y >= boardSize) {
                losers.add(player.id)
            }

            // Self collision
            if (player.snake.drop(1).any { it == head }) {
                losers.add(player.id)
            }

            // Other player collision
            players.values.filter { it.id != player.id }.forEach { otherPlayer ->
                if (otherPlayer.snake.any { it == head }) {
                    losers.add(player.id)
                }
            }

            // Obstacle collision
            if (obstacles.any { it == head }) {
                losers.add(player.id)
            }
        }

        if (losers.isNotEmpty()) {
            phase = GamePhase.GAME_OVER
            if (losers.size == players.size) {
                // It's a tie
                winner = null
            } else {
                winner = players.keys.find { !losers.contains(it) }
            }
        }
    }


    private fun checkFood() {
        players.values.forEach { player ->
            if (player.snake.first() == food) {
                player.score++
                food = generateFood()
            }
        }
    }

    private fun generateFood(): Point {
        while (true) {
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            if (players.values.none { player -> player.snake.any { it == point } } && obstacles.none { it == point }) {
                return point
            }
        }
    }

    private fun generateObstacles() {
        val newObstacles = mutableListOf<Point>()
        val allPlayerSnakes = players.values.flatMap { it.snake }
        for (i in 0..10) {
            while (true) {
                val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
                if (
                    players.values.none { player -> player.snake.any { it == point } } &&
                    point != food &&
                    !newObstacles.contains(point)
                ) {
                    newObstacles.add(point)
                    break
                }
            }
        }
        obstacles = newObstacles
    }
}