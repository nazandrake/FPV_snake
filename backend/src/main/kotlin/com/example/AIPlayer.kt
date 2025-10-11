package com.example

import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object AIPlayer {

    private const val LOW_TIMER_THRESHOLD = 45 // When to start panicking
    private const val ANGLE_TOLERANCE = 0.1f // Radians

    fun updateAiPlayer(player: Player, gameState: GameState) {
        // 1. Decide on a target
        val target = selectTarget(gameState, player)

        if (target == null) {
            player.isMovingForward = false
            player.turning = TurnDirection.NONE
            return
        }

        // 2. Calculate angle to target
        val dx = target.x - player.position.x
        val dy = target.y - player.position.y
        val targetAngle = atan2(dy, dx)

        // 3. Determine if we need to turn or move
        val angleDifference = normalizeAngle(targetAngle - player.direction)

        if (kotlin.math.abs(angleDifference) > ANGLE_TOLERANCE) {
            // Need to turn
            player.isMovingForward = false // Stop moving while turning
            player.turning = if (angleDifference > 0) TurnDirection.LEFT else TurnDirection.RIGHT
        } else {
            // Facing the target, so move forward
            player.turning = TurnDirection.NONE
            player.isMovingForward = true
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a <= -Math.PI) a += (2 * Math.PI).toFloat()
        while (a > Math.PI) a -= (2 * Math.PI).toFloat()
        return a
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

    private fun findClosestTarget(currentPos: Point, targets: List<Point>): Point? {
        return targets.minByOrNull { distance(currentPos, it) }
    }

    private fun distance(p1: Point, p2: Point): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
}