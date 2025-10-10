<template>
  <div class="game-container">
    <!-- Connection Loading Screen -->
    <div v-if="!connected" class="loading">Connecting to server...</div>

    <!-- Name Input Screen -->
    <div v-if="connected && !isNameSet" class="name-input-container">
      <input v-model="playerName" @keyup.enter="setPlayerName" placeholder="Enter your name" />
      <button @click="setPlayerName" :disabled="!gameState">Join Game</button>
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
      <div v-if="gameState.phase === 'RUNNING'" class="game-view">
          <div class="ui-overlay">
              <div class="timer">Survival Time: {{ survivalTimer }}</div>
              <div class="scores">
                  <h2>Scores</h2>
                  <ul>
                      <li v-for="player in sortedPlayersByScore" :key="player.id" :style="{ color: player.color, fontWeight: 'bold' }">
                          {{ player.name }}: {{ player.score }}
                      </li>
                  </ul>
              </div>
          </div>
          <FirstPersonView :game-state="gameState" :player-id="playerId" @start-moving="handleStartMoving" @stop-moving="handleStopMoving" />
          <TopDownView :game-state="gameState" :player-id="playerId" />
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

// Audio setup (temporarily disabled for debugging)

const isPlayerReady = computed(() => {
  return gameState.value?.players[playerId.value]?.ready || false;
});

const canAddAiPlayer = computed(() => {
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

const survivalTimer = computed(() => {
    if (!gameState.value || !playerId.value) return 0;
    const player = gameState.value.players[playerId.value];
    return player ? player.survivalTimer : 0;
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

const handleStartMoving = (direction) => {
    sendMessage({ type: 'StartMoving', direction });
};

const handleStopMoving = () => {
    sendMessage({ type: 'StopMoving' });
};

const connectWebSocket = () => {
  const wsUrl = 'ws://localhost:8080/ws';
  console.log(`Attempting to connect to WebSocket at: ${wsUrl}`);
  socket = new WebSocket(wsUrl);

  socket.onopen = () => {
    console.log("WebSocket connection established successfully.");
    connected.value = true;
  };

  socket.onmessage = (event) => {
    console.log("Received message from server:", event.data);
    const message = JSON.parse(event.data);
    const oldState = gameState.value;

    if (message.type.endsWith('.AssignPlayerId')) {
      playerId.value = message.id;
    } else if (message.type.endsWith('.GameStateUpdate')) {
      gameState.value = message.gameState;

      if (playerId.value && !gameState.value.players[playerId.value]) {
        isNameSet.value = false;
      }
    }
  };

  socket.onclose = (event) => {
    console.log(`WebSocket disconnected. Code: ${event.code}, Reason: ${event.reason}`);
    connected.value = false;
    isNameSet.value = false;
    gameState.value = null;
  };

  socket.onerror = (error) => {
    console.error("WebSocket error:", error);
    connected.value = false;
  };
};

onMounted(() => {
  connectWebSocket();
});

onUnmounted(() => {
  if (socket) {
    socket.close();
  }
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
  background-color: #1a1a1a;
  height: 100vh;
  width: 100vw;
}

.loading, .name-input-container, .lobby-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 2em;
}

.name-input-container input, .lobby-container button, .game-over button {
  font-size: 1em;
  padding: 10px 20px;
  border-radius: 5px;
  border: none;
  margin: 5px;
}

.name-input-container input {
    border: 1px solid #ccc;
}

.name-input-container button, .lobby-container button, .game-over button {
  background-color: #2ecc71;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s;
}

.name-input-container button:hover, .lobby-container button:hover, .game-over button:hover {
  background-color: #27ae60;
}

.lobby-buttons {
  display: flex;
  gap: 10px;
}

.game-view {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
}

.ui-overlay {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 10;
    color: white;
    background-color: rgba(0, 0, 0, 0.5);
    padding: 15px;
    border-radius: 8px;
    font-family: 'Courier New', Courier, monospace;
}

.timer {
    font-size: 1.8em;
    font-weight: bold;
    margin-bottom: 15px;
    color: #f1c40f;
}

.scores h2 {
    margin-top: 0;
    font-size: 1.5em;
    border-bottom: 2px solid white;
    padding-bottom: 5px;
}

.scores ul {
    list-style: none;
    padding: 0;
}

.game-over {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.85);
  padding: 40px;
  border-radius: 10px;
  text-align: center;
  z-index: 20;
}
</style>