<template>
  <div class="top-down-container">
    <canvas ref="canvas" :width="canvasSize" :height="canvasSize"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';

const props = defineProps({
  gameState: Object,
});

const canvas = ref(null);
const canvasSize = 600;
let ctx = null;
let animationFrameId = null;

// For interpolation
const previousGameState = ref(null);
const currentGameState = ref(null);
let lastUpdateTime = 0;
const serverUpdateInterval = 50; // Corresponds to the backend delay

const draw = (interpolationFactor) => {
  if (!ctx || !currentGameState.value) return;

  const { players, food, boardSize } = currentGameState.value;
  const scale = canvasSize / boardSize;

  ctx.fillStyle = '#1a1a1a';
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  ctx.fillStyle = '#f1c40f';
  ctx.fillRect(food.x * scale, food.y * scale, scale, scale);

  for (const playerId in players) {
    const player = players[playerId];
    const prevPlayer = previousGameState.value?.players[playerId];

    ctx.fillStyle = player.color;
    player.snake.forEach((segment, index) => {
      let x = segment.x;
      let y = segment.y;

      const prevSegment = prevPlayer?.snake[index];
      if (prevSegment && interpolationFactor < 1) {
        const dx = segment.x - prevSegment.x;
        const dy = segment.y - prevSegment.y;

        // Don't interpolate on large jumps (e.g., wrapping around the board)
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
            x = segment.x;
            y = segment.y;
        } else {
            x = prevSegment.x + dx * interpolationFactor;
            y = prevSegment.y + dy * interpolationFactor;
        }
      }

      ctx.globalAlpha = index === 0 ? 1.0 : 0.8;
      ctx.fillRect(x * scale, y * scale, scale, scale);
    });
    ctx.globalAlpha = 1.0;
  }
};

const animationLoop = () => {
    const now = Date.now();
    const timeSinceUpdate = now - lastUpdateTime;
    let interpolationFactor = timeSinceUpdate / serverUpdateInterval;
    if (interpolationFactor > 1) interpolationFactor = 1;

    draw(interpolationFactor);
    animationFrameId = requestAnimationFrame(animationLoop);
};

onMounted(() => {
  ctx = canvas.value.getContext('2d');
  animationFrameId = requestAnimationFrame(animationLoop);
});

onUnmounted(() => {
    cancelAnimationFrame(animationFrameId);
});

watch(() => props.gameState, (newGameState) => {
    previousGameState.value = currentGameState.value;
    currentGameState.value = newGameState;
    if (!previousGameState.value) {
        previousGameState.value = newGameState; // Initial setup
    }
    lastUpdateTime = Date.now();
}, { deep: true });
</script>

<style scoped>
.top-down-container {
  border: 4px solid #34495e;
  border-radius: 8px;
  background-color: #2c3e50;
  padding: 10px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

canvas {
  display: block;
}
</style>