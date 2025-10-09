package com.example

object AIPlayer {
    fun getNextDirection(gameState: GameState, aiPlayerId: String): Direction {
        val aiPlayer = gameState.players[aiPlayerId] ?: return Direction.RIGHT
        val food = gameState.food
        val boardSize = gameState.boardSize
        val head = aiPlayer.snake.first()

        val possibleMoves = mutableListOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        // Avoid reversing
        when (aiPlayer.direction) {
            Direction.UP -> possibleMoves.remove(Direction.DOWN)
            Direction.DOWN -> possibleMoves.remove(Direction.UP)
            Direction.LEFT -> possibleMoves.remove(Direction.RIGHT)
            Direction.RIGHT -> possibleMoves.remove(Direction.LEFT)
        }

        // Simple pathfinding: move towards food
        val preferredMoves = mutableListOf<Direction>()
        if (head.x < food.x) preferredMoves.add(Direction.RIGHT)
        if (head.x > food.x) preferredMoves.add(Direction.LEFT)
        if (head.y < food.y) preferredMoves.add(Direction.DOWN)
        if (head.y > food.y) preferredMoves.add(Direction.UP)

        // Filter out unsafe moves
        val safeMoves = preferredMoves.filter { isSafe(it, head, gameState, aiPlayer) }
        if (safeMoves.isNotEmpty()) {
            return safeMoves.first()
        }

        // If preferred moves are unsafe, try any safe move
        val anySafeMove = possibleMoves.filter { isSafe(it, head, gameState, aiPlayer) }
        return anySafeMove.firstOrNull() ?: aiPlayer.direction // If no safe move, continue in the same direction
    }

    private fun isSafe(direction: Direction, head: Point, gameState: GameState, player: Player): Boolean {
        val nextHead = when (direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }

        // Wall collision
        if (nextHead.x < 0 || nextHead.x >= gameState.boardSize || nextHead.y < 0 || nextHead.y >= gameState.boardSize) {
            return false
        }

        // Self collision
        if (player.snake.any { it == nextHead }) {
            return false
        }

        // Other player collision
        gameState.players.values.filter { it.id != player.id }.forEach { otherPlayer ->
            if (otherPlayer.snake.any { it == nextHead }) {
                return false
            }
        }

        return true
    }
}