<template>
  <div class="top-down-container">
    <canvas ref="canvas" :width="canvasSize" :height="canvasSize"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';
import foodApple from '../assets/food-apple.png';

const props = defineProps({
  gameState: Object,
});

const canvas = ref(null);
const canvasSize = 600;
let ctx = null;
let animationFrameId = null;
let foodImage = null;

// For interpolation
const previousGameState = ref(null);
const currentGameState = ref(null);
let lastUpdateTime = 0;
const serverUpdateInterval = 50; // Corresponds to the backend delay

const draw = (interpolationFactor) => {
  if (!ctx || !currentGameState.value) return;

  const { players, food, obstacles, boardSize } = currentGameState.value;
  const scale = canvasSize / boardSize;

  // Draw grass background
  ctx.fillStyle = '#27ae60'; // Grassy green
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  // Draw food (apple)
  if (foodImage && foodImage.complete) {
    ctx.drawImage(foodImage, food.x * scale, food.y * scale, scale, scale);
  } else {
    // Fallback to drawing a red circle if the image hasn't loaded
    ctx.fillStyle = '#e74c3c'; // Vibrant red
    ctx.beginPath();
    ctx.arc(food.x * scale + scale / 2, food.y * scale + scale / 2, scale / 2, 0, 2 * Math.PI);
    ctx.fill();
  }

  // Draw obstacles (trees)
  if (obstacles) {
    obstacles.forEach(obstacle => {
      const x = obstacle.x * scale;
      const y = obstacle.y * scale;

      // Draw trunk
      ctx.fillStyle = '#8B4513'; // SaddleBrown
      ctx.fillRect(x + scale * 0.4, y + scale * 0.4, scale * 0.2, scale * 0.6);

      // Draw canopy
      ctx.fillStyle = '#228B22'; // ForestGreen
      ctx.beginPath();
      ctx.arc(x + scale / 2, y + scale / 2, scale / 2, 0, 2 * Math.PI);
      ctx.fill();
    });
  }

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
  foodImage = new Image();
  foodImage.src = foodApple;

  foodImage.onload = () => {
    if (!animationFrameId) {
      animationFrameId = requestAnimationFrame(animationLoop);
    }
  };

  // If the image is already cached and loaded, start the loop immediately.
  if (foodImage.complete && !animationFrameId) {
    animationFrameId = requestAnimationFrame(animationLoop);
  }
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