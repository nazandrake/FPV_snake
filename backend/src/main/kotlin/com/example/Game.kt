package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Serializable
enum class Weather {
    SUNNY,
    RAIN,
    SNOW,
    FOG,
    THUNDERSTORM
}

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
    val winner: String? = null,
    val weather: Weather,
    val nextWeather: Weather? = null,
    val weatherTransitionProgress: Float = 0.0f
)

class Game {
    private val players = ConcurrentHashMap<String, Player>()
    private lateinit var food: Point
    private var obstacles = listOf<Point>()
    private val boardSize = 60
    var phase = GamePhase.LOBBY
        private set
    private var winner: String? = null
    private var weather = Weather.SUNNY
    private var nextWeather: Weather? = null
    private var weatherTransitionProgress = 0.0f
    private var weatherTransitionTicks = 0
    private var currentWeatherDuration = 0
    private val weatherTransitionDuration = 100 // ticks for transition
    private var lightningStrikes = mutableMapOf<Point, Int>() // Point -> remaining ticks

    init {
        food = generateFood()
        setNextWeatherTransition()
    }

    private fun setNextWeatherTransition() {
        currentWeatherDuration = Random.nextInt(200, 600)
        weatherTransitionTicks = currentWeatherDuration
    }

    fun getGameState(): GameState {
        val allObstacles = obstacles + lightningStrikes.keys
        return GameState(
            players = players,
            food = food,
            obstacles = allObstacles,
            boardSize = boardSize,
            phase = phase,
            winner = winner,
            weather = weather,
            nextWeather = nextWeather,
            weatherTransitionProgress = weatherTransitionProgress
        )
    }

    private fun generateRandomColor(): String {
        val r = Random.nextInt(256)
        val g = Random.nextInt(256)
        val b = Random.nextInt(256)
        return "#%02x%02x%02x".format(r, g, b)
    }

    private fun generateRandomStartPosition(direction: Direction = Direction.RIGHT): Point {
        while (true) {
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))

            val nextHead = when (direction) {
                Direction.UP -> Point(point.x, point.y - 1)
                Direction.DOWN -> Point(point.x, point.y + 1)
                Direction.LEFT -> Point(point.x - 1, point.y)
                Direction.RIGHT -> Point(point.x + 1, point.y)
            }

            val isSafeFromWalls = nextHead.x >= 0 && nextHead.x < boardSize && nextHead.y >= 0 && nextHead.y < boardSize
            val isSafeFromObstacles = obstacles.none { it == point }
            val isSafeFromPlayers = players.values.none { player -> player.snake.any { it == point } }

            if (isSafeFromWalls && isSafeFromObstacles && isSafeFromPlayers) {
                return point
            }
        }
    }

    fun addPlayer(id: String) {
        val playerIndex = players.size
        val color = generateRandomColor()
        val startPoint = generateRandomStartPosition(Direction.RIGHT)

        val snake = mutableListOf(startPoint)
        players[id] = Player(id = id, snake = snake, direction = Direction.RIGHT, color = color, name = "Player ${playerIndex + 1}")
    }

    fun addAiPlayer() {
        val aiId = "ai-player-${(1000..9999).random()}"
        val color = generateRandomColor()
        val startPoint = generateRandomStartPosition(Direction.RIGHT)

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
        if (phase == GamePhase.LOBBY && players.isNotEmpty() && players.values.all { it.ready }) {
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
        players.values.forEach { player ->
            player.score = 0
            player.ready = player.isAi // AI is always ready
            player.direction = Direction.RIGHT
            player.snake.clear()
        }

        food = generateFood()

        val usedPositions = mutableSetOf<Point>(food)
        players.values.forEach { player ->
            var startPoint: Point
            do {
                startPoint = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            } while (usedPositions.contains(startPoint))
            usedPositions.add(startPoint)
            player.snake.add(startPoint)
        }
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
        updateWeather()
        // AI Player Logic
        val aiPlayers = players.values.filter { it.isAi }
        if (aiPlayers.isNotEmpty()) {
            val currentGameState = getGameState()
            aiPlayers.forEach { aiPlayer ->
                val nextDirection = AIPlayer.getNextDirection(currentGameState, aiPlayer.id)
                changeDirection(aiPlayer.id, nextDirection)
            }
        }

        if (weather != Weather.SNOW || Random.nextDouble() < 0.7) { // 30% chance to skip movement in snow
            moveSnakes()
        }
        checkCollisions()
        checkFood()
    }

    private fun updateWeather() {
        if (nextWeather != null) {
            // We are in a transition
            weatherTransitionProgress += 1.0f / weatherTransitionDuration
            if (weatherTransitionProgress >= 1.0f) {
                weather = nextWeather!!
                nextWeather = null
                weatherTransitionProgress = 0.0f
                setNextWeatherTransition()
                if (weather != Weather.THUNDERSTORM) {
                    lightningStrikes.clear()
                }
            }
        } else {
            // Waiting for the next transition to start
            weatherTransitionTicks--
            if (weatherTransitionTicks <= 0) {
                nextWeather = Weather.values().filter { it != weather }.random()
                weatherTransitionProgress = 0.0f
            }
        }

        if (weather == Weather.THUNDERSTORM || (nextWeather == Weather.THUNDERSTORM && weatherTransitionProgress > 0)) {
            // Add new lightning strikes
            if (Random.nextDouble() < 0.1) { // 10% chance each tick
                generateRandomEmptyPoint()?.let { strikePoint ->
                    lightningStrikes[strikePoint] = Random.nextInt(50, 150) // 1-3 seconds duration
                }
            }

            // Update and remove old strikes
            val iterator = lightningStrikes.iterator()
            while (iterator.hasNext()) {
                val (point, ticks) = iterator.next()
                if (ticks - 1 <= 0) {
                    iterator.remove()
                } else {
                    lightningStrikes[point] = ticks - 1
                }
            }
        }
    }

    private fun generateRandomEmptyPoint(): Point? {
        repeat(100) { // Try up to 100 times to find an empty spot
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            if (players.values.none { player -> player.snake.any { it == point } } &&
                obstacles.none { it == point } &&
                !lightningStrikes.containsKey(point) &&
                point != food
            ) {
                return point
            }
        }
        return null // Return null if no empty point is found
    }

    private fun moveSnakes() {
        players.values.forEach { player ->
            var newDirection = player.direction
            if (weather == Weather.RAIN && Random.nextDouble() < 0.1) { // 10% chance to ignore direction change
                // Keep the old direction
            }

            val head = player.snake.first().let {
                when (newDirection) {
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

        // Remove the losers from the game state
        if (losers.isNotEmpty()) {
            losers.forEach { loserId ->
                players.remove(loserId)
            }
        }

        // Now, check if the game is over. The game ends if 1 or 0 players are left.
        if (players.size <= 1 && phase == GamePhase.RUNNING) {
            phase = GamePhase.GAME_OVER
            // If one player is left, they are the winner. If zero are left, it's a tie (winner is null).
            winner = players.keys.firstOrNull()
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