package com.example

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach

class GameTest {

    private lateinit var game: Game

    @BeforeEach
    fun setUp() {
        game = Game()
    }

    @Test
    fun `test addPlayer`() {
        game.addPlayer("player1")
        val gameState = game.getGameState()
        assertEquals(1, gameState.players.size)
        assertEquals("Player 1", gameState.players["player1"]?.name)
    }

    @Test
    fun `test player movement`() {
        game.addPlayer("player1")
        game.setPlayerReady("player1", true)
        game.addAiPlayer() // To start the game

        val initialPosition = game.getGameState().players["player1"]?.position
        assertTrue(initialPosition != null, "Initial position should not be null")

        game.setMoving("player1", true)
        game.update()

        val newPosition = game.getGameState().players["player1"]?.position
        assertTrue(newPosition != null, "New position should not be null")
        assertNotEquals(initialPosition, newPosition, "Player should move when setMoving(true)")
    }

    @Test
    fun `test player turning`() {
        game.addPlayer("player1")
        game.setPlayerReady("player1", true)
        game.addAiPlayer() // To start the game

        val initialDirection = game.getGameState().players["player1"]?.direction
        assertTrue(initialDirection != null, "Initial direction should not be null")

        game.setTurning("player1", TurnDirection.LEFT)
        game.update()

        val newDirection = game.getGameState().players["player1"]?.direction
        assertTrue(newDirection != null, "New direction should not be null")
        assertNotEquals(initialDirection, newDirection, "Player should turn when setTurning(LEFT)")
    }

    @Test
    fun `test game over when one player remains`() {
        game.addPlayer("player1")
        game.setPlayerReady("player1", true)
        game.addAiPlayer() // To start the game

        val player = game.getGameState().players["player1"]
        assertTrue(player != null, "Player should exist")
        player.survivalTimer = 0

        game.update() // This should trigger the elimination check
        game.update() // This should process the elimination and end the game

        val gameState = game.getGameState()
        assertEquals(GamePhase.GAME_OVER, gameState.phase, "Game should be in GAME_OVER phase")
        assertEquals(1, gameState.players.size, "There should be one player remaining")
        assertEquals("ai-player", gameState.winner?.substring(0, 9), "The AI player should be the winner")
    }
}