package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    var name: String = "Player",
    var snake: MutableList<Point>,
    var direction: Direction,
    val color: String,
    var score: Int = 0,
    var ready: Boolean = false
)

@Serializable
data class Point(val x: Int, val y: Int)

@Serializable
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}