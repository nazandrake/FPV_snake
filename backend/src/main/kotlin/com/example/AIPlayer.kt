package com.example

object AIPlayer {
    fun getNextDirection(gameState: GameState, aiPlayerId: String): Direction {
        val aiPlayer = gameState.players[aiPlayerId] ?: return Direction.RIGHT
        val food = gameState.food
        val head = aiPlayer.snake.first()

        // 1. Determine all possible non-reversing moves.
        val possibleMoves = Direction.entries.toMutableSet()
        when (aiPlayer.direction) {
            Direction.UP -> possibleMoves.remove(Direction.DOWN)
            Direction.DOWN -> possibleMoves.remove(Direction.UP)
            Direction.LEFT -> possibleMoves.remove(Direction.RIGHT)
            Direction.RIGHT -> possibleMoves.remove(Direction.LEFT)
        }

        // 2. Find all safe moves from the possible moves.
        val safeMoves = possibleMoves.filter { isSafe(it, head, gameState, aiPlayer) }

        // If no moves are safe, well, we're doomed. Continue in the current direction.
        if (safeMoves.isEmpty()) {
            return aiPlayer.direction
        }

        // 3. From the safe moves, which ones move us closer to the food?
        val preferredMoves = safeMoves.filter {
            when (it) {
                Direction.UP -> head.y > food.y
                Direction.DOWN -> head.y < food.y
                Direction.LEFT -> head.x > food.x
                Direction.RIGHT -> head.x < food.x
            }
        }

        // 4. If there are preferred safe moves, choose one randomly. Otherwise, choose any safe move randomly.
        return (preferredMoves.ifEmpty { safeMoves }).random()
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