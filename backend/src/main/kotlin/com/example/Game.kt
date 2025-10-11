package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    private var tickCounter = 0

    private val turnSpeed = 0.1f // Radians per tick
    private val moveSpeed = 0.5f // Units per tick

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
            val point = Point(Random.nextInt(boardSize).toFloat(), Random.nextInt(boardSize).toFloat())
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
            direction = 0.0f, // Facing right
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
            direction = 0.0f,
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
        tickCounter = 0
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
            player.direction = 0.0f
            player.survivalTimer = 120
            player.timerBuffTimeGained = 0
            player.turning = TurnDirection.NONE
            player.isMovingForward = false
        }

        food = generateFood()

        val usedPositions = mutableSetOf<Point>(food)
        players.values.forEach { player ->
            var startPoint: Point
            do {
                startPoint = Point(Random.nextInt(boardSize).toFloat(), Random.nextInt(boardSize).toFloat())
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

    fun setTurning(id: String, turnDirection: TurnDirection) {
        players[id]?.turning = turnDirection
    }

    fun setMoving(id: String, isMoving: Boolean) {
        players[id]?.isMovingForward = isMoving
    }

    fun update() {
        if (phase != GamePhase.RUNNING) return

        // AI Player Logic
        val currentGameState = getGameState()
        players.values.filter { it.isAi }.forEach { aiPlayer ->
            AIPlayer.updateAiPlayer(aiPlayer, currentGameState)
        }

        // Process movement for all players
        players.values.forEach { player ->
            // 1. Update direction if turning
            when (player.turning) {
                TurnDirection.LEFT -> player.direction += turnSpeed
                TurnDirection.RIGHT -> player.direction -= turnSpeed
                TurnDirection.NONE -> {}
            }

            // 2. Update position if moving
            if (player.isMovingForward) {
                val speed = if (player.hasSpeedBuff) moveSpeed * 1.5f else moveSpeed
                val newX = player.position.x + cos(player.direction) * speed
                val newY = player.position.y + sin(player.direction) * speed
                val newPosition = Point(newX, newY)

                // 3. Collision detection
                if (isPositionValid(newPosition, player.id)) {
                    player.position = newPosition
                }
            }
        }

        // Update survival timers every second (20 ticks)
        tickCounter++
        if (tickCounter >= 20) {
            players.values.forEach { it.survivalTimer-- }
            tickCounter = 0
        }

        // Check for eliminations
        val eliminatedPlayers = mutableListOf<String>()
        players.values.forEach { player ->
            if (player.survivalTimer <= 0) {
                eliminatedPlayers.add(player.id)
            }
        }
        eliminatedPlayers.forEach { removePlayer(it) }

        checkGameOver()

        checkConsumables()
        updateBuffs()
    }

    private fun checkGameOver() {
        if (players.size <= 1 && phase == GamePhase.RUNNING) {
            phase = GamePhase.GAME_OVER
            winner = players.keys.firstOrNull()
        }
    }

    private fun updateBuffs() {
        val currentTime = System.currentTimeMillis()
        players.values.forEach { player ->
            if (player.hasSpeedBuff && currentTime >= player.speedBuffEndTime) {
                player.hasSpeedBuff = false
            }
        }
    }


    private fun isPositionValid(position: Point, playerId: String): Boolean {
        // Wall collision
        if (position.x < 0 || position.x >= boardSize || position.y < 0 || position.y >= boardSize) {
            return false
        }
        // Obstacle collision
        if (obstacles.any { distance(it, position) < 1.0f }) {
            return false
        }
        // Other player collision
        if (players.values.any { it.id != playerId && distance(it.position, position) < 1.0f }) {
            return false
        }
        return true
    }

    private fun checkConsumables() {
        players.values.forEach { player ->
            // Check for food
            if (distance(player.position, food) < 1.0f) {
                player.score++
                player.survivalTimer = 120 // Reset timer
                food = generateFood()
            }

            // Check for buffs
            val consumedBuff = buffs.find { distance(it.position, player.position) < 1.0f }
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

    private fun distance(p1: Point, p2: Point): Float {
        return sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
    }

    private fun generateFood(): Point {
        while (true) {
            val point = Point(Random.nextInt(boardSize).toFloat(), Random.nextInt(boardSize).toFloat())
            if (players.values.none { distance(it.position, point) < 2.0f } && obstacles.none { distance(it, point) < 2.0f } && buffs.none { distance(it.position, point) < 2.0f }) {
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
            val point = Point(Random.nextInt(boardSize).toFloat(), Random.nextInt(boardSize).toFloat())
            if (players.values.none { distance(it.position, point) < 2.0f } && obstacles.none { distance(it, point) < 2.0f } && distance(food, point) < 2.0f && buffs.none { distance(it.position, point) < 2.0f }) {
                buffs.add(Buff(point, type))
                break
            }
        }
    }

    private fun generateObstacles() {
        val newObstacles = mutableListOf<Point>()
        for (i in 0..10) {
            while (true) {
                val point = Point(Random.nextInt(boardSize).toFloat(), Random.nextInt(boardSize).toFloat())
                if (
                    players.values.none { distance(it.position, point) < 2.0f } &&
                    distance(food, point) > 2.0f &&
                    newObstacles.none { distance(it, point) < 2.0f } &&
                    buffs.none { distance(it.position, point) < 2.0f }
                ) {
                    newObstacles.add(point)
                    break
                }
            }
        }
        obstacles = newObstacles
    }
}