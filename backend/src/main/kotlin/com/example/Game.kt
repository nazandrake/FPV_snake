package com.example

import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Serializable
data class GameState(
    val players: Map<String, Player>,
    val food: Point,
    val boardSize: Int,
    val gameOver: Boolean = false,
    val winner: String? = null
)

class Game {
    private val players = ConcurrentHashMap<String, Player>()
    private var food = generateFood()
    private val boardSize = 20
    private var gameOver = false
    private var winner: String? = null

    fun getGameState(): GameState {
        return GameState(players, food, boardSize, gameOver, winner)
    }

    fun addPlayer(id: String) {
        if (players.size < 2) {
            val color = if (players.isEmpty()) "#ff0000" else "#0000ff"
            val startX = if (players.isEmpty()) 5 else 15
            val snake = mutableListOf(Point(startX, 10))
            players[id] = Player(id, snake, Direction.RIGHT, color)
        }
    }

    fun removePlayer(id: String) {
        players.remove(id)
        if (players.size < 2) {
            gameOver = true
            winner = players.keys.firstOrNull()
        }
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
        if (gameOver || players.size < 2) return

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
        }

        if (losers.isNotEmpty()) {
            gameOver = true
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
            if (players.values.none { player -> player.snake.any { it == point } }) {
                return point
            }
        }
    }
}