<template>
  <div class="top-down-container">
    <canvas ref="canvas" :width="canvasSize" :height="canvasSize"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue';
import foodApple from '../assets/food-apple.png';
import grassTexture from '../assets/grass.png';

const props = defineProps({
  gameState: Object,
});

const canvas = ref(null);
const canvasSize = 600;
let ctx = null;
let animationFrameId = null;
let foodImage = null;
let grassPattern = null;
let rainParticles = [];
let snowParticles = [];

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
  if (grassPattern) {
    ctx.fillStyle = grassPattern;
  } else {
    ctx.fillStyle = '#27ae60'; // Grassy green
  }
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  drawWeather(interpolationFactor);

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

const drawWeather = (interpolationFactor) => {
  const { weather, nextWeather, weatherTransitionProgress } = currentGameState.value;

  const weatherConfigs = {
    SUNNY: { overlay: 'rgba(0,0,0,0)' },
    RAIN: { overlay: 'rgba(0, 0, 0, 0.2)', particles: rainParticles, particleStyle: 'rgba(174,194,224,0.5)' },
    SNOW: { overlay: 'rgba(255, 255, 255, 0.2)', particles: snowParticles, particleStyle: 'white' },
    FOG: { overlay: 'rgba(200, 200, 200, 0.5)' },
    THUNDERSTORM: { overlay: 'rgba(0, 0, 0, 0.5)' },
  };

  const drawParticles = (particles, style, opacity) => {
    ctx.globalAlpha = opacity;
    if (Array.isArray(particles)) {
        particles.forEach(p => {
            p.y += p.speed;
            if (p.y > canvasSize) {
                p.y = 0;
                p.x = Math.random() * canvasSize;
            }
            ctx.fillStyle = style;
            if (p.length) { // Rain
                ctx.fillRect(p.x, p.y, 1, p.length);
            } else { // Snow
                ctx.beginPath();
                ctx.arc(p.x, p.y, p.radius, 0, 2 * Math.PI);
                ctx.fill();
            }
        });
    }
    ctx.globalAlpha = 1.0;
  };

  const currentConfig = weatherConfigs[weather];
  const nextConfig = nextWeather ? weatherConfigs[nextWeather] : null;

  if (nextConfig) {
    // Draw current weather effects, fading out
    const currentOpacity = 1 - weatherTransitionProgress;
    ctx.fillStyle = currentConfig.overlay;
    ctx.globalAlpha = currentOpacity;
    ctx.fillRect(0, 0, canvasSize, canvasSize);
    if (currentConfig.particles) {
        drawParticles(currentConfig.particles, currentConfig.particleStyle, currentOpacity);
    }

    // Draw next weather effects, fading in
    const nextOpacity = weatherTransitionProgress;
    ctx.fillStyle = nextConfig.overlay;
    ctx.globalAlpha = nextOpacity;
    ctx.fillRect(0, 0, canvasSize, canvasSize);
    if (nextConfig.particles) {
        drawParticles(nextConfig.particles, nextConfig.particleStyle, nextOpacity);
    }
    ctx.globalAlpha = 1.0;

  } else {
    // Draw normal weather
    ctx.fillStyle = currentConfig.overlay;
    ctx.fillRect(0, 0, canvasSize, canvasSize);
    if (currentConfig.particles) {
        drawParticles(currentConfig.particles, currentConfig.particleStyle, 1.0);
    }
  }

  // Flashes for thunderstorm
  if ((weather === 'THUNDERSTORM' && (!nextWeather || weatherTransitionProgress < 0.5)) ||
      (nextWeather === 'THUNDERSTORM' && weatherTransitionProgress >= 0.5)) {
    if (Math.random() < 0.05) {
      ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
      ctx.fillRect(0, 0, canvasSize, canvasSize);
    }
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

  const grassImage = new Image();
  grassImage.src = grassTexture;
  grassImage.onload = () => {
    grassPattern = ctx.createPattern(grassImage, 'repeat');
  };

  for (let i = 0; i < 500; i++) {
    rainParticles.push({
      x: Math.random() * canvasSize,
      y: Math.random() * canvasSize,
      length: Math.random() * 20,
      speed: Math.random() * 5 + 2
    });
    snowParticles.push({
      x: Math.random() * canvasSize,
      y: Math.random() * canvasSize,
      radius: Math.random() * 2 + 1,
      speed: Math.random() * 1 + 0.5
    });
  }

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