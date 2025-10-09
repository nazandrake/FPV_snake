package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class ClientMessage {
    @Serializable
    @SerialName("SetPlayerName")
    data class SetPlayerName(val name: String) : ClientMessage()
    @Serializable
    @SerialName("PlayerReady")
    data class PlayerReady(val isReady: Boolean) : ClientMessage()
    @Serializable
    @SerialName("ResetGame")
    object ResetGame : ClientMessage()
    @Serializable
    @SerialName("ChangeDirection")
    data class ChangeDirection(val direction: Direction) : ClientMessage()
    @Serializable
    @SerialName("AddAiPlayer")
    object AddAiPlayer : ClientMessage()
    @Serializable
    @SerialName("HardResetGame")
    object HardResetGame : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    @Serializable
    data class AssignPlayerId(val id: String) : ServerMessage()
    @Serializable
    data class GameStateUpdate(val gameState: GameState) : ServerMessage()
}