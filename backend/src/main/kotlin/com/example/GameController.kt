package com.example

import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.decodeFromString

class GameController(private val json: Json) {
    private val game = Game()
    private val connections = ConcurrentHashMap<String, Connection>()

    suspend fun onConnect(session: DefaultWebSocketSession): Connection {
        val connection = Connection(session)
        connections[connection.id] = connection
        game.addPlayer(connection.id)
        println("Player ${connection.id} connected. Total players: ${connections.size}")

        // Assign player ID
        val assignPlayerIdMessage: ServerMessage = ServerMessage.AssignPlayerId(connection.id)
        connection.session.send(json.encodeToString(assignPlayerIdMessage))

        return connection
    }

    fun onDisconnect(connection: Connection) {
        connections.remove(connection.id)
        game.removePlayer(connection.id)
        println("Player ${connection.id} disconnected. Total players: ${connections.size}")
    }

    fun onMessage(id: String, message: String) {
        try {
            when (val clientMessage = json.decodeFromString<ClientMessage>(message)) {
                is ClientMessage.SetPlayerName -> game.setPlayerName(id, clientMessage.name)
                is ClientMessage.PlayerReady -> game.setPlayerReady(id, clientMessage.isReady)
                is ClientMessage.ResetGame -> game.resetGame()
                is ClientMessage.ChangeDirection -> game.changeDirection(id, clientMessage.direction)
                is ClientMessage.AddAiPlayer -> game.addAiPlayer()
            }
        } catch (e: Exception) {
            println("Error decoding message from player $id: $message")
        }
    }

    suspend fun gameLoop() {
        while (true) {
            game.update()
            val gameState = game.getGameState()
            val gameStateUpdateMessage: ServerMessage = ServerMessage.GameStateUpdate(gameState)
            val gameStateJson = json.encodeToString(gameStateUpdateMessage)

            connections.values.forEach { connection ->
                try {
                    connection.session.send(gameStateJson)
                } catch (e: Exception) {
                    println("Failed to send state to ${connection.id}, removing.")
                    // This can happen if the client disconnects abruptly
                    onDisconnect(connection)
                }
            }
            delay(150) // Game speed
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    val id: String = "player-${(1000..9999).random()}"
}