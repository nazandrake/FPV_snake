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

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)

    val gameController = GameController()
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
                    gameController.onMessage(connection.id, receivedText)
                }
            } catch (e: Exception) {
                println("Error during WebSocket session: ${e.message}")
            } finally {
                gameController.onDisconnect(connection)
            }
        }
        // Serve static files from resources
        staticResources("/", "static") {
            default("index.html")
        }
    }
}