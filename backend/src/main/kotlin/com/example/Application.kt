package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val json = Json {
        classDiscriminator = "type"
        serializersModule = SerializersModule {
            polymorphic(ServerMessage::class) {
                subclass(ServerMessage.AssignPlayerId::class)
                subclass(ServerMessage.GameStateUpdate::class)
            }
            polymorphic(ClientMessage::class) {
                subclass(ClientMessage.SetPlayerName::class)
                subclass(ClientMessage.PlayerReady::class)
                subclass(ClientMessage.ResetGame::class)
                subclass(ClientMessage.StartMoving::class)
                subclass(ClientMessage.StopMoving::class)
                subclass(ClientMessage.AddAiPlayer::class)
                subclass(ClientMessage.HardResetGame::class)
            }
        }
        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(json)
    }
    install(WebSockets)

    val gameController = GameController(json)
    // Launch the game loop in a separate coroutine
    launch {
        gameController.gameLoop()
    }

    routing {
        webSocket("/ws") {
            val connection = gameController.onConnect(this)
            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    launch { gameController.onMessage(connection.id, receivedText) }
                }
            } catch (e: Exception) {
                println("Error during WebSocket session: ${e.message}")
            } finally {
                launch { gameController.onDisconnect(connection) }
            }
        }
        // Serve static files from resources
        staticResources("/", "static") {
            default("index.html")
        }
    }
}