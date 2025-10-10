package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    var name: String = "Player",
    var position: Point,
    var direction: Direction,
    val color: String,
    var score: Int = 0,
    var ready: Boolean = false,
    val isAi: Boolean = false,
    var survivalTimer: Int = 120, // 2 minutes
    var timerBuffTimeGained: Int = 0, // Max 60 seconds
    var lastBuffConsumptionTime: Long = 0,
    var hasSpeedBuff: Boolean = false,
    var speedBuffEndTime: Long = 0,
    var isMoving: Boolean = false,
    var movingDirection: Direction? = null
)

@Serializable
data class Point(val x: Int, val y: Int)

@Serializable
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}