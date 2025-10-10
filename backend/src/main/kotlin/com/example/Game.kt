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
enum class BuffType {
    SPEED,
    TIMER
}

@Serializable
data class Buff(
    val position: Point,
    val type: BuffType
)

@Serializable
data class GameState(
    val players: Map<String, Player>,
    val food: Point,
    val obstacles: List<Point>,
    val buffs: List<Buff>,
    val boardSize: Int,
    val phase: GamePhase,
    val winner: String? = null
)

class Game {
    private val players = ConcurrentHashMap<String, Player>()
    private lateinit var food: Point
    private var obstacles = listOf<Point>()
    private var buffs = mutableListOf<Buff>()
    private val boardSize = 60
    var phase = GamePhase.LOBBY
        private set
    private var winner: String? = null

    init {
        food = generateFood()
    }

    fun getGameState(): GameState {
        return GameState(players, food, obstacles, buffs, boardSize, phase, winner)
    }

    private fun generateRandomColor(): String {
        val r = Random.nextInt(256)
        val g = Random.nextInt(256)
        val b = Random.nextInt(256)
        return "#%02x%02x%02x".format(r, g, b)
    }

    private fun generateRandomStartPosition(): Point {
        while (true) {
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            if (players.values.none { it.position == point } && obstacles.none { it == point }) {
                return point
            }
        }
    }

    fun addPlayer(id: String) {
        val playerIndex = players.size
        val color = generateRandomColor()
        val startPoint = generateRandomStartPosition()

        players[id] = Player(
            id = id,
            position = startPoint,
            direction = Direction.RIGHT,
            color = color,
            name = "Player ${playerIndex + 1}"
        )
    }

    fun addAiPlayer() {
        val aiId = "ai-player-${(1000..9999).random()}"
        val color = generateRandomColor()
        val startPoint = generateRandomStartPosition()

        players[aiId] = Player(
            id = aiId,
            position = startPoint,
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
        generateBuffs()
    }

    fun resetGame() {
        phase = GamePhase.LOBBY
        winner = null
        obstacles = listOf()
        buffs.clear()
        // Reset players
        players.values.forEach { player ->
            player.score = 0
            player.ready = player.isAi // AI is always ready
            player.direction = Direction.RIGHT
            player.survivalTimer = 120
            player.timerBuffTimeGained = 0
        }

        food = generateFood()

        val usedPositions = mutableSetOf<Point>(food)
        players.values.forEach { player ->
            var startPoint: Point
            do {
                startPoint = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            } while (usedPositions.contains(startPoint))
            usedPositions.add(startPoint)
            player.position = startPoint
        }
    }

    fun hardResetGame() {
        phase = GamePhase.LOBBY
        winner = null
        players.clear()
        obstacles = listOf()
        buffs.clear()
        food = generateFood()
    }

    fun changeDirection(id: String, newDirection: Direction) {
        val player = players[id] ?: return
        player.direction = newDirection
    }

    fun startMoving(id: String, direction: Direction) {
        players[id]?.let {
            it.isMoving = true
            it.movingDirection = direction
            it.direction = direction // Update main direction for camera
        }
    }

    fun stopMoving(id: String) {
        players[id]?.isMoving = false
    }

    private fun movePlayer(player: Player) {
        val moveDirection = player.movingDirection ?: return
        var newPosition = player.position
        when (moveDirection) {
            Direction.UP -> newPosition = Point(player.position.x, player.position.y - 1)
            Direction.DOWN -> newPosition = Point(player.position.x, player.position.y + 1)
            Direction.LEFT -> newPosition = Point(player.position.x - 1, player.position.y)
            Direction.RIGHT -> newPosition = Point(player.position.x + 1, player.position.y)
        }

        // Wall collision
        if (newPosition.x < 0 || newPosition.x >= boardSize || newPosition.y < 0 || newPosition.y >= boardSize) {
            return // Stop movement
        }

        // Obstacle collision
        if (obstacles.any { it == newPosition }) {
            return // Stop movement
        }

        // Other player collision
        if (players.values.filter { it.id != player.id }.any { it.position == newPosition }) {
            return // Stop movement
        }

        player.position = newPosition
    }

    fun update() {
        if (phase != GamePhase.RUNNING) return

        // Process movement for all players
        val currentGameState = getGameState()
        players.values.forEach { player ->
            if (player.isAi) {
                // AI decides its move on every tick
                val move = AIPlayer.getMove(currentGameState, player.id)
                if (move != null) {
                    player.movingDirection = move
                    movePlayer(player)
                }
            } else {
                // Human player moves if the key is held down
                if (player.isMoving) {
                    movePlayer(player)
                }
            }
        }

        // Update survival timers and check for eliminations
        val eliminatedPlayers = mutableListOf<String>()
        players.values.forEach { player ->
            player.survivalTimer--
            if (player.survivalTimer <= 0) {
                eliminatedPlayers.add(player.id)
            }
        }

        eliminatedPlayers.forEach { removePlayer(it) }


        // Check for game over condition
        if (players.size <= 1 && phase == GamePhase.RUNNING) {
            phase = GamePhase.GAME_OVER
            winner = players.keys.firstOrNull()
        }

        checkConsumables()
        updateBuffs()
    }

    private fun checkCollisions() {
        // Collision logic is now handled in movePlayer
    }

    private fun updateBuffs() {
        val currentTime = System.currentTimeMillis()
        players.values.forEach { player ->
            if (player.hasSpeedBuff && currentTime >= player.speedBuffEndTime) {
                player.hasSpeedBuff = false
            }
        }
    }


    private fun checkConsumables() {
        players.values.forEach { player ->
            // Check for food
            if (player.position == food) {
                player.score++
                player.survivalTimer = 120 // Reset timer
                food = generateFood()
            }

            // Check for buffs
            val consumedBuff = buffs.find { it.position == player.position }
            if (consumedBuff != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - player.lastBuffConsumptionTime >= 30000) { // 30-second cooldown
                    applyBuff(player, consumedBuff)
                    player.lastBuffConsumptionTime = currentTime
                    buffs.remove(consumedBuff)
                }
            }
        }
    }

    private fun applyBuff(player: Player, buff: Buff) {
        when (buff.type) {
            BuffType.SPEED -> {
                player.hasSpeedBuff = true
                player.speedBuffEndTime = System.currentTimeMillis() + 15000 // 15 seconds
            }
            BuffType.TIMER -> {
                val timeToAdd = 5
                if (player.timerBuffTimeGained + timeToAdd <= 60) {
                    player.survivalTimer += timeToAdd
                    player.timerBuffTimeGained += timeToAdd
                } else {
                    val remainingTime = 60 - player.timerBuffTimeGained
                    player.survivalTimer += remainingTime
                    player.timerBuffTimeGained += remainingTime
                }
            }
        }
    }

    private fun generateFood(): Point {
        while (true) {
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            if (players.values.none { it.position == point } && obstacles.none { it == point } && buffs.none { it.position == point }) {
                return point
            }
        }
    }

    private fun generateBuffs() {
        // Generate one of each buff type for now
        spawnBuff(BuffType.SPEED)
        spawnBuff(BuffType.TIMER)
    }

    private fun spawnBuff(type: BuffType) {
        while (true) {
            val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
            if (players.values.none { it.position == point } && obstacles.none { it == point } && food != point && buffs.none { it.position == point }) {
                buffs.add(Buff(point, type))
                break
            }
        }
    }

    private fun generateObstacles() {
        val newObstacles = mutableListOf<Point>()
        for (i in 0..10) {
            while (true) {
                val point = Point(Random.nextInt(boardSize), Random.nextInt(boardSize))
                if (
                    players.values.none { it.position == point } &&
                    point != food &&
                    !newObstacles.contains(point) &&
                    buffs.none { it.position == point }
                ) {
                    newObstacles.add(point)
                    break
                }
            }
        }
        obstacles = newObstacles
    }
}