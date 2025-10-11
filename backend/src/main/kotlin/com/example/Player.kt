package com.example

import kotlinx.serialization.Serializable

@Serializable
enum class TurnDirection {
    LEFT, RIGHT, NONE
}

@Serializable
data class Player(
    val id: String,
    var name: String = "Player",
    var position: Point,
    var direction: Float, // Angle in radians
    val color: String,
    var score: Int = 0,
    var ready: Boolean = false,
    val isAi: Boolean = false,
    var survivalTimer: Int = 120, // 2 minutes
    var timerBuffTimeGained: Int = 0, // Max 60 seconds
    var lastBuffConsumptionTime: Long = 0,
    var hasSpeedBuff: Boolean = false,
    var speedBuffEndTime: Long = 0,
    var turning: TurnDirection = TurnDirection.NONE,
    var isMovingForward: Boolean = false
)

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}