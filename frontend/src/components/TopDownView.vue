<template>
  <div class="top-down-container">
    <canvas ref="canvas" :width="canvasSize" :height="canvasSize"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';

const props = defineProps({
  gameState: Object,
});

const canvas = ref(null);
const canvasSize = 600; // The display size of the canvas
let ctx = null;

const draw = () => {
  if (!ctx || !props.gameState) return;

  const { players, food, boardSize } = props.gameState;
  const scale = canvasSize / boardSize;

  // Clear canvas
  ctx.fillStyle = '#1a1a1a'; // Dark background
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  // Draw food
  ctx.fillStyle = '#f1c40f'; // Yellow for food
  ctx.fillRect(food.x * scale, food.y * scale, scale, scale);

  // Draw players
  for (const playerId in players) {
    const player = players[playerId];
    ctx.fillStyle = player.color;
    player.snake.forEach((segment, index) => {
      // Make head slightly different
      ctx.globalAlpha = index === 0 ? 1.0 : 0.8;
      ctx.fillRect(segment.x * scale, segment.y * scale, scale, scale);
    });
    ctx.globalAlpha = 1.0;
  }
};

onMounted(() => {
  ctx = canvas.value.getContext('2d');
  draw();
});

watch(() => props.gameState, draw, { deep: true });
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