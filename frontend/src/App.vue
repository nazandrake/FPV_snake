<template>
  <div class="game-container">
    <!-- Connection Loading Screen -->
    <div v-if="!connected" class="loading">Connecting to server...</div>

    <!-- Name Input Screen -->
    <div v-if="connected && !isNameSet" class="name-input-container">
      <input v-model="playerName" @keyup.enter="setPlayerName" placeholder="Enter your name" />
      <button @click="setPlayerName">Join Game</button>
    </div>

    <!-- Game Content -->
    <div v-if="connected && isNameSet && gameState">
      <!-- Lobby View -->
      <div v-if="gameState.phase === 'LOBBY'" class="lobby-container">
        <h1>Lobby</h1>
        <ul class="player-list">
          <li v-for="player in gameState.players" :key="player.id">
            <span :style="{ color: player.color, fontWeight: 'bold' }">{{ player.name }}</span> - <span :class="{ 'ready': player.ready, 'not-ready': !player.ready }">{{ player.ready ? 'Ready' : 'Not Ready' }}</span>
          </li>
        </ul>
        <div class="lobby-buttons">
          <button @click="setReady" :disabled="isPlayerReady">
            {{ isPlayerReady ? 'Waiting for others...' : 'I\'m Ready!' }}
          </button>
          <button @click="addAiPlayer" v-if="canAddAiPlayer" class="add-ai-btn">Add AI Player</button>
          <button @click="hardResetGame" v-if="isLobbyNotEmpty" class="hard-reset-btn">Reset Lobby</button>
        </div>
      </div>

      <!-- Game View -->
      <div v-if="gameState.phase === 'RUNNING'" class="game-running-container">
        <div class="scores-container">
            <h2>Scores</h2>
            <ul>
                <li v-for="player in sortedPlayersByScore" :key="player.id" :style="{ color: player.color, fontWeight: 'bold' }">
                    {{ player.name }}: {{ player.score }}
                </li>
            </ul>
        </div>
        <div class="views-container">
          <TopDownView :game-state="gameState" />
          <div class="fpv-container">
            <FirstPersonView :game-state="gameState" :player-id="playerId" />
          </div>
        </div>
      </div>

      <!-- Game Over Screen -->
      <div v-if="gameState.phase === 'GAME_OVER'" class="game-over">
        <h1>Game Over</h1>
        <h2 v-if="winner">Winner: <span :style="{ color: winner.color, fontWeight: 'bold' }">{{ winner.name }}</span></h2>
        <h2 v-else>It's a tie!</h2>
        <div class="final-scores">
            <h3>Final Scores</h3>
            <ul>
                <li v-for="player in sortedPlayersByScore" :key="player.id" :style="{ color: player.color, fontWeight: 'bold' }">
                    {{ player.name }}: {{ player.score }}
                </li>
            </ul>
        </div>
        <button @click="resetGame">Start Again</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import TopDownView from './components/TopDownView.vue';
import FirstPersonView from './components/FirstPersonView.vue';

const connected = ref(false);
const gameState = ref(null);
const playerId = ref(null);
const playerName = ref('');
const isNameSet = ref(false);
let socket = null;

// Audio setup
const eatSound = new Audio('/eat.mp3');
const winSound = new Audio('/win.mp3');
const loseSound = new Audio('/lose.mp3');

const isPlayerReady = computed(() => {
  return gameState.value?.players[playerId.value]?.ready || false;
});

const canAddAiPlayer = computed(() => {
  // Allow adding AI players anytime in the lobby
  return !!gameState.value;
});

const isLobbyNotEmpty = computed(() => {
  return gameState.value && Object.keys(gameState.value.players).length > 0;
});

const winner = computed(() => {
    if (!gameState.value || !gameState.value.winner) return null;
    const winnerId = gameState.value.winner;
    return gameState.value.players[winnerId];
});

const sortedPlayersByScore = computed(() => {
    if (!gameState.value) return [];
    return Object.values(gameState.value.players).sort((a, b) => b.score - a.score);
});

const sendMessage = (message) => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(message));
  }
};

const setPlayerName = () => {
  if (playerName.value.trim()) {
    sendMessage({ type: 'SetPlayerName', name: playerName.value.trim() });
    isNameSet.value = true;
  }
};

const setReady = () => {
  sendMessage({ type: 'PlayerReady', isReady: true });
};

const resetGame = () => {
  sendMessage({ type: 'ResetGame' });
};

const hardResetGame = () => {
  sendMessage({ type: 'HardResetGame' });
};

const addAiPlayer = () => {
  sendMessage({ type: 'AddAiPlayer' });
};

const connectWebSocket = () => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  socket = new WebSocket(`${wsProtocol}//${window.location.host}/ws`);

  socket.onopen = () => {
    console.log("WebSocket connected!");
    connected.value = true;
  };

  socket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    const oldState = gameState.value;

    if (message.type.endsWith('.AssignPlayerId')) {
      playerId.value = message.id;
    } else if (message.type.endsWith('.GameStateUpdate')) {
      gameState.value = message.gameState;

      // If the player is no longer in the game state (e.g., after a hard reset),
      // send them back to the name input screen.
      if (playerId.value && !gameState.value.players[playerId.value]) {
        isNameSet.value = false;
      }

      // Sound logic
      if (oldState) {
        const myPlayer = playerId.value ? gameState.value.players[playerId.value] : null;
        const oldPlayer = playerId.value ? oldState.players[playerId.value] : null;

        // Food eaten
        if (myPlayer && oldPlayer && myPlayer.score > oldPlayer.score) {
          eatSound.play();
        }

        // Game over
        if (gameState.value.phase === 'GAME_OVER' && oldState.phase === 'RUNNING') {
          if (gameState.value.winner === playerId.value) {
            winSound.play();
          } else {
            loseSound.play();
          }
        }
      }
    }
  };

  socket.onclose = () => {
    console.log("WebSocket disconnected.");
    connected.value = false;
    isNameSet.value = false;
    gameState.value = null;
  };

  socket.onerror = (error) => {
    console.error("WebSocket error:", error);
    connected.value = false;
  };
};

const handleKeyPress = (e) => {
  if (!socket || socket.readyState !== WebSocket.OPEN || gameState.value?.phase !== 'RUNNING') return;

  let direction = null;
  switch (e.key) {
    case 'ArrowUp': direction = 'UP'; break;
    case 'ArrowDown': direction = 'DOWN'; break;
    case 'ArrowLeft': direction = 'LEFT'; break;
    case 'ArrowRight': direction = 'RIGHT'; break;
  }

  if (direction) {
    sendMessage({ type: 'ChangeDirection', direction: direction });
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
  font-family: 'Arial', sans-serif;
  color: #fff;
}

.loading, .name-input-container, .lobby-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  font-size: 2em;
}

.name-input-container input {
  font-size: 1em;
  padding: 10px;
  margin-bottom: 20px;
  border-radius: 5px;
  border: 1px solid #ccc;
}

.name-input-container button, .lobby-container button, .game-over button {
  font-size: 1em;
  padding: 10px 20px;
  border-radius: 5px;
  border: none;
  background-color: #2ecc71;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s;
}

.name-input-container button:hover, .lobby-container button:hover, .game-over button:hover {
  background-color: #27ae60;
}

.lobby-container h1 {
  margin-bottom: 40px;
}

.player-list {
  list-style: none;
  padding: 0;
  margin-bottom: 40px;
  font-size: 0.8em;
  text-align: center;
}

.player-list li {
  margin-bottom: 10px;
}

.ready {
  color: #2ecc71;
  font-weight: bold;
}

.not-ready {
  color: #e74c3c;
  font-weight: bold;
}

.lobby-buttons {
  display: flex;
  gap: 20px;
}

.add-ai-btn {
  background-color: #3498db !important;
}

.add-ai-btn:hover {
  background-color: #2980b9 !important;
}

.hard-reset-btn {
  background-color: #e74c3c !important;
}

.hard-reset-btn:hover {
  background-color: #c0392b !important;
}

.game-running-container {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 30px;
}

.scores-container {
  padding: 20px;
  background-color: #2c3e50;
  border-radius: 8px;
  min-width: 200px;
}

.scores-container h2 {
    margin-top: 0;
}

.scores-container ul {
    list-style: none;
    padding: 0;
}

.views-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
}

.fpv-container {
  border: 2px solid #ccc;
  border-radius: 5px;
  background-color: #000;
  width: 400px; /* Adjusted width */
  height: 300px; /* Adjusted height */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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

.game-over button {
  margin-top: 20px;
}
</style>