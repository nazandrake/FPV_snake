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

    private suspend fun broadcastGameState() {
        val gameState = game.getGameState()
        val gameStateUpdateMessage: ServerMessage = ServerMessage.GameStateUpdate(gameState)
        val gameStateJson = json.encodeToString(gameStateUpdateMessage)

        connections.values.forEach { connection ->
            try {
                connection.session.send(gameStateJson)
            } catch (e: Exception) {
                println("Failed to send state to ${connection.id}, removing.")
                onDisconnect(connection)
            }
        }
    }

    suspend fun onConnect(session: DefaultWebSocketSession): Connection {
        val connection = Connection(session)
        connections[connection.id] = connection
        game.addPlayer(connection.id)
        println("Player ${connection.id} connected. Total players: ${connections.size}")

        // Assign player ID and send initial state
        val assignPlayerIdMessage: ServerMessage = ServerMessage.AssignPlayerId(connection.id)
        connection.session.send(json.encodeToString(assignPlayerIdMessage))
        broadcastGameState()

        return connection
    }

    suspend fun onDisconnect(connection: Connection) {
        connections.remove(connection.id)
        game.removePlayer(connection.id)
        println("Player ${connection.id} disconnected. Total players: ${connections.size}")
        broadcastGameState()
    }

    suspend fun onMessage(id: String, message: String) {
        println("Received message from $id: $message") // Log raw message
        try {
            val clientMessage = json.decodeFromString<ClientMessage>(message)
            when (clientMessage) {
                is ClientMessage.SetPlayerName -> game.setPlayerName(id, clientMessage.name)
                is ClientMessage.PlayerReady -> game.setPlayerReady(id, clientMessage.isReady)
                is ClientMessage.ResetGame -> game.resetGame()
                is ClientMessage.HardResetGame -> game.hardResetGame()
                is ClientMessage.StartMoving -> game.startMoving(id, clientMessage.direction)
                is ClientMessage.StopMoving -> game.stopMoving(id)
                is ClientMessage.AddAiPlayer -> game.addAiPlayer()
            }

            // Don't wait for the next tick for lobby updates.
            // Movement updates will be sent by the game loop.
            if (clientMessage !is ClientMessage.StartMoving && clientMessage !is ClientMessage.StopMoving) {
                broadcastGameState()
            }
        } catch (e: Exception) {
            println("Error processing message from player $id: $message")
            e.printStackTrace() // Log the full exception
        }
    }

    suspend fun gameLoop() {
        while (true) {
            game.update()
            broadcastGameState()
            delay(50) // Game speed
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    val id: String = "player-${(1000..9999).random()}"
}