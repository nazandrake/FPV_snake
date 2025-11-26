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

// Weather particles
let rainParticles = [];
let snowParticles = [];
let lightningOpacity = 0;

// For interpolation
let lastGameState = null;
let lastUpdateTime = 0;
const serverUpdateInterval = 50; // Corresponds to the backend delay

const updateWeatherParticles = () => {
    // Rain
    if (rainParticles.length < 100) {
        rainParticles.push({ x: Math.random() * canvasSize, y: Math.random() * canvasSize, l: Math.random() * 1, xs: -4 + Math.random() * 4 + 2, ys: Math.random() * 10 + 10 });
    }
    for (let i = 0; i < rainParticles.length; i++) {
        const p = rainParticles[i];
        p.x += p.xs;
        p.y += p.ys;
        if (p.x > canvasSize || p.y > canvasSize) {
            p.x = Math.random() * canvasSize;
            p.y = -20;
        }
    }

    // Snow
    if (snowParticles.length < 100) {
        snowParticles.push({ x: Math.random() * canvasSize, y: Math.random() * canvasSize, r: Math.random() * 2 + 1, d: Math.random() * 100 });
    }
    for (let i = 0; i < snowParticles.length; i++) {
        const p = snowParticles[i];
        p.d += Math.random() > 0.5 ? 1 : -1;
        p.y += Math.cos(p.d) + p.r / 2;
        p.x += Math.sin(p.d) * 2;

        if (p.x > canvasSize + 5 || p.x < -5 || p.y > canvasSize) {
            snowParticles.splice(i, 1);
            snowParticles.push({ x: Math.random() * canvasSize, y: -10, r: p.r, d: p.d });
        }
    }
};


const drawWeather = (interpolationFactor) => {
    if (!props.gameState || !props.gameState.weather) return;

    const { weather, nextWeather, weatherTransitionProgress } = props.gameState;

    const applyWeather = (type, alpha) => {
        if (type === 'RAIN') {
            ctx.strokeStyle = `rgba(174,194,224,${alpha})`;
            ctx.lineWidth = 1;
            ctx.lineCap = 'round';
            for (let i = 0; i < rainParticles.length; i++) {
                const p = rainParticles[i];
                ctx.beginPath();
                ctx.moveTo(p.x, p.y);
                ctx.lineTo(p.x + p.l * p.xs, p.y + p.l * p.ys);
                ctx.stroke();
            }
        } else if (type === 'SNOW') {
            ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`;
            ctx.beginPath();
            for (let i = 0; i < snowParticles.length; i++) {
                const p = snowParticles[i];
                ctx.moveTo(p.x, p.y);
                ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2, true);
            }
            ctx.fill();
        } else if (type === 'FOG') {
            ctx.fillStyle = `rgba(200, 200, 200, ${0.5 * alpha})`;
            ctx.fillRect(0, 0, canvasSize, canvasSize);
        } else if (type === 'THUNDERSTORM') {
            if (Math.random() > 0.99) {
                lightningOpacity = 1;
            }
            if (lightningOpacity > 0) {
                ctx.fillStyle = `rgba(255, 255, 255, ${lightningOpacity * alpha})`;
                ctx.fillRect(0, 0, canvasSize, canvasSize);
                lightningOpacity -= 0.1;
            }
        }
    };

    applyWeather(weather, 1 - weatherTransitionProgress);
    if (nextWeather && weatherTransitionProgress > 0) {
        applyWeather(nextWeather, weatherTransitionProgress);
    }
};

const draw = (interpolationFactor) => {
  if (!ctx || !props.gameState) return;

  const { players, food, obstacles, buffs, boardSize } = props.gameState;
  const scale = canvasSize / boardSize;

  // Draw background
  ctx.fillStyle = 'rgba(39, 174, 96, 0.7)'; // Semi-transparent grassy green
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  // Draw weather
  drawWeather(interpolationFactor);

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

    updateWeatherParticles();
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