<template>
  <div class="minimap-container">
    <canvas ref="canvas" :width="canvasSize" :height="canvasSize"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';

const props = defineProps({
  gameState: Object,
  playerId: String,
});

const canvas = ref(null);
const canvasSize = 200;
let ctx = null;
let animationFrameId = null;

// For interpolation
let lastGameState = null;
let lastUpdateTime = 0;
const serverUpdateInterval = 50; // Corresponds to the backend delay

const draw = (interpolationFactor) => {
  if (!ctx || !props.gameState) return;

  const { players, food, obstacles, buffs, boardSize } = props.gameState;
  const scale = canvasSize / boardSize;

  // Draw background
  ctx.fillStyle = 'rgba(39, 174, 96, 0.7)'; // Semi-transparent grassy green
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  // Draw food
  ctx.fillStyle = '#e74c3c'; // Vibrant red
  ctx.beginPath();
  ctx.arc(food.x * scale + scale / 2, food.y * scale + scale / 2, scale, 0, 2 * Math.PI);
  ctx.fill();

  // Draw obstacles
  if (obstacles) {
    ctx.fillStyle = '#8B4513'; // SaddleBrown
    obstacles.forEach(obstacle => {
      ctx.fillRect(obstacle.x * scale, obstacle.y * scale, scale, scale);
    });
  }

  // Draw buffs
  if (buffs) {
    buffs.forEach(buff => {
      ctx.fillStyle = buff.type === 'SPEED' ? '#f1c40f' : '#2ecc71'; // Yellow for speed, Green for timer
      ctx.beginPath();
      ctx.arc(buff.position.x * scale + scale / 2, buff.position.y * scale + scale / 2, scale * 0.8, 0, 2 * Math.PI);
      ctx.fill();
    });
  }

  // Draw players
  for (const id in players) {
    const player = players[id];
    let x = player.position.x;
    let y = player.position.y;

    const lastPlayer = lastGameState?.players[id];
    if (lastPlayer && interpolationFactor < 1) {
        const dx = player.position.x - lastPlayer.position.x;
        const dy = player.position.y - lastPlayer.position.y;
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) { // Only interpolate for small movements
            x = lastPlayer.position.x + dx * interpolationFactor;
            y = lastPlayer.position.y + dy * interpolationFactor;
        }
    }

    ctx.fillStyle = player.color;
    // Highlight current player
    if (id === props.playerId) {
        ctx.strokeStyle = 'white';
        ctx.lineWidth = 2;
    } else {
        ctx.strokeStyle = 'black';
        ctx.lineWidth = 1;
    }

    ctx.beginPath();
    ctx.arc(x * scale + scale / 2, y * scale + scale / 2, scale * 1.2, 0, 2 * Math.PI);
    ctx.fill();
    ctx.stroke();
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
    lastGameState = props.gameState;
    lastUpdateTime = Date.now();
}, { deep: true, immediate: true });

</script>

<style scoped>
.minimap-container {
  position: absolute;
  top: 20px;
  right: 20px;
  border: 2px solid #34495e;
  border-radius: 8px;
  background-color: rgba(44, 62, 80, 0.5);
  padding: 5px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.5);
  z-index: 10;
}

canvas {
  display: block;
  border-radius: 4px;
}
</style>