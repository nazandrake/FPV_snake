package com.example

import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class GameController {
    private val game = Game()
    private val connections = ConcurrentHashMap<String, Connection>()

    fun onConnect(session: DefaultWebSocketSession): Connection {
        val connection = Connection(session)
        connections[connection.id] = connection
        game.addPlayer(connection.id)
        println("Player ${connection.id} connected. Total players: ${connections.size}")
        return connection
    }

    fun onDisconnect(connection: Connection) {
        connections.remove(connection.id)
        game.removePlayer(connection.id)
        println("Player ${connection.id} disconnected. Total players: ${connections.size}")
    }

    fun onMessage(id: String, message: String) {
        try {
            val direction = Json.decodeFromString<Direction>(message)
            game.changeDirection(id, direction)
        } catch (e: Exception) {
            println("Error decoding direction from player $id: $message")
        }
    }

    suspend fun gameLoop() {
        while (true) {
            if (connections.isNotEmpty()) {
                game.update()
                val gameState = game.getGameState()
                val gameStateJson = Json.encodeToString(gameState)
                connections.values.forEach { connection ->
                    try {
                        connection.session.send(gameStateJson)
                    } catch (e: Exception) {
                        println("Failed to send state to ${connection.id}, removing.")
                        onDisconnect(connection)
                    }
                }
            }
            delay(150) // Game speed
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    val id: String = "player-${(1000..9999).random()}"
}