<template>
  <div class="game-container">
    <div v-if="!connected" class="loading">Connecting to server...</div>
    <div v-if="connected && !gameStarted" class="loading">Waiting for another player...</div>

    <div v-if="gameState">
      <div class="views-container">
        <!-- First Person View -->
        <div class="fpv-container">
          <FirstPersonView :game-state="gameState" :player-id="playerId" />
        </div>

        <!-- Top Down View -->
        <TopDownView :game-state="gameState" />
      </div>

      <!-- Game Over Screen -->
      <div v-if="gameState.gameOver" class="game-over">
        <h1>Game Over</h1>
        <h2 v-if="gameState.winner">Winner: {{ gameState.winner }}</h2>
        <h2 v-else>It's a tie!</h2>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import TopDownView from './components/TopDownView.vue';
import FirstPersonView from './components/FirstPersonView.vue';

const connected = ref(false);
const gameStarted = ref(false);
const gameState = ref(null);
const playerId = ref(null);
let socket = null;

// Audio setup
const eatSound = new Audio('/eat.mp3');
const winSound = new Audio('/win.mp3');
const loseSound = new Audio('/lose.mp3');

const connectWebSocket = () => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  socket = new WebSocket(`${wsProtocol}//${window.location.host}/ws`);

  socket.onopen = () => {
    console.log("WebSocket connected!");
    connected.value = true;
  };

  socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    const oldState = gameState.value;
    gameState.value = data;

    if (!playerId.value) {
        // very hacky way to get player id
        if (Object.keys(data.players).length == 1)
            playerId.value = Object.keys(data.players)[0];
        else if (Object.keys(data.players).length == 2 && playerId.value == null){
            // this is the second player to join, we need to find our id
            // this is not a good way to do this
            // but we don't have a good way to get the player id from the server
            // so we will just find the player that is not the other player
            const otherPlayerId = Object.keys(gameState.value.players).find(id => id !== playerId.value);
            if(otherPlayerId)
                playerId.value = otherPlayerId;
        }
    }

    if (Object.keys(data.players).length === 2) {
      gameStarted.value = true;
    }

    // Sound logic
    if (oldState) {
      // Food eaten
      const myPlayer = playerId.value ? data.players[playerId.value] : null;
      const oldPlayer = playerId.value ? oldState.players[playerId.value] : null;
      if (myPlayer && oldPlayer && myPlayer.score > oldPlayer.score) {
        eatSound.play();
      }

      // Game over
      if (data.gameOver && !oldState.gameOver) {
        if (data.winner === playerId.value) {
          winSound.play();
        } else {
          loseSound.play();
        }
      }
    }
  };

  socket.onclose = () => {
    console.log("WebSocket disconnected.");
    connected.value = false;
    gameStarted.value = false;
    gameState.value = null; // Reset game state
  };

  socket.onerror = (error) => {
    console.error("WebSocket error:", error);
    connected.value = false;
  };
};

const handleKeyPress = (e) => {
  if (!socket || socket.readyState !== WebSocket.OPEN) return;

  let direction = null;
  switch (e.key) {
    case 'ArrowUp':
      direction = 'UP';
      break;
    case 'ArrowDown':
      direction = 'DOWN';
      break;
    case 'ArrowLeft':
      direction = 'LEFT';
      break;
    case 'ArrowRight':
      direction = 'RIGHT';
      break;
  }

  if (direction) {
    socket.send(JSON.stringify(direction));
  }
};

onMounted(() => {
  connectWebSocket();
  window.addEventListener('keydown', handleKeyPress);
});

onUnmounted(() => {
  if (socket) {
    socket.close();
  }
  window.removeEventListener('keydown', handleKeyPress);
});
</script>

<style scoped>
.game-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.loading {
  font-size: 2em;
}

.views-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 20px;
}

.fpv-container {
  position: absolute;
  top: 10px;
  left: 10px;
  border: 2px solid #ccc;
  border-radius: 5px;
  background-color: #000;
  width: 300px; /* Adjust size as needed */
  height: 200px; /* Adjust size as needed */
}

.game-over {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.8);
  padding: 40px;
  border-radius: 10px;
  text-align: center;
}

.game-over h1 {
  color: #e74c3c;
  margin-bottom: 20px;
}
</style>