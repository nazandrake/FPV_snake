package com.example

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object AIPlayer {

    private const val LOW_TIMER_THRESHOLD = 45 // When to start panicking

    fun getMove(gameState: GameState, aiPlayerId: String): Direction? {
        val aiPlayer = gameState.players[aiPlayerId] ?: return null

        // 1. Decide on a target
        val target = selectTarget(gameState, aiPlayer) ?: return null // No target, do nothing

        // 2. Find the best direction towards the target
        return findBestDirection(gameState, aiPlayer, target)
    }

    private fun selectTarget(gameState: GameState, aiPlayer: Player): Point? {
        val foodPosition = gameState.food
        val timerBuffs = gameState.buffs.filter { it.type == BuffType.TIMER }
        val speedBuffs = gameState.buffs.filter { it.type == BuffType.SPEED }

        // Priority 1: Survival
        if (aiPlayer.survivalTimer < LOW_TIMER_THRESHOLD) {
            val survivalTargets = mutableListOf(foodPosition)
            timerBuffs.forEach { survivalTargets.add(it.position) }
            return findClosestTarget(aiPlayer.position, survivalTargets)
        }

        // Priority 2: Get buffs if available
        val allBuffs = (timerBuffs + speedBuffs).map { it.position }
        if (allBuffs.isNotEmpty()) {
            return findClosestTarget(aiPlayer.position, allBuffs)
        }

        // Priority 3: Go for food
        return foodPosition
    }

    private fun findBestDirection(gameState: GameState, player: Player, target: Point): Direction? {
        val currentPos = player.position
        val dx = target.x - currentPos.x
        val dy = target.y - currentPos.y

        val preferredDirections = mutableListOf<Direction>()
        if (abs(dx) > abs(dy)) {
            if (dx > 0) preferredDirections.add(Direction.RIGHT) else preferredDirections.add(Direction.LEFT)
            if (dy > 0) preferredDirections.add(Direction.DOWN) else if (dy < 0) preferredDirections.add(Direction.UP)
        } else {
            if (dy > 0) preferredDirections.add(Direction.DOWN) else preferredDirections.add(Direction.UP)
            if (dx > 0) preferredDirections.add(Direction.RIGHT) else if (dx < 0) preferredDirections.add(Direction.LEFT)
        }

        // Add remaining directions as fallbacks
        Direction.entries.forEach {
            if (!preferredDirections.contains(it)) {
                preferredDirections.add(it)
            }
        }

        // Find the first safe direction from the preferred list
        for (direction in preferredDirections) {
            val nextPos = getNextPosition(currentPos, direction)
            if (isSafe(nextPos, gameState, player.id)) {
                return direction
            }
        }

        return null // No safe move found
    }

    private fun getNextPosition(current: Point, direction: Direction): Point {
        return when (direction) {
            Direction.UP -> Point(current.x, current.y - 1)
            Direction.DOWN -> Point(current.x, current.y + 1)
            Direction.LEFT -> Point(current.x - 1, current.y)
            Direction.RIGHT -> Point(current.x + 1, current.y)
        }
    }

    private fun isSafe(position: Point, gameState: GameState, playerId: String): Boolean {
        // Wall collision
        if (position.x < 0 || position.x >= gameState.boardSize || position.y < 0 || position.y >= gameState.boardSize) {
            return false
        }

        // Obstacle collision
        if (gameState.obstacles.any { it == position }) {
            return false
        }

        // Other player collision
        if (gameState.players.values.any { it.id != playerId && it.position == position }) {
            return false
        }

        return true
    }

    private fun findClosestTarget(currentPos: Point, targets: List<Point>): Point? {
        return targets.minByOrNull { distance(currentPos, it) }
    }

    private fun distance(p1: Point, p2: Point): Double {
        return sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
    }
}