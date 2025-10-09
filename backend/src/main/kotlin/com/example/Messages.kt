package com.example

import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    data class SetPlayerName(val name: String) : ClientMessage()
    @Serializable
    data class PlayerReady(val isReady: Boolean) : ClientMessage()
    @Serializable
    object ResetGame : ClientMessage()
    @Serializable
    data class ChangeDirection(val direction: Direction) : ClientMessage()
    @Serializable
    object AddAiPlayer : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    @Serializable
    data class AssignPlayerId(val id: String) : ServerMessage()
    @Serializable
    data class GameStateUpdate(val gameState: GameState) : ServerMessage()
}