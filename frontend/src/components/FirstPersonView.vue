<template>
  <div ref="container" class="fpv"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as THREE from 'three';
import foodApple from '../assets/food-apple.png';

const props = defineProps({
  gameState: Object,
  playerId: String,
});

const container = ref(null);
let scene, camera, renderer, wall, otherSnake, foodMesh, obstaclesGroup;
let rainParticles, snowParticles;
let isInitialized = false;

const initThree = () => {
  if (!container.value || isInitialized) return;
  isInitialized = true;

  // Scene
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x87ceeb); // Sky blue

  // Camera
  camera = new THREE.PerspectiveCamera(75, container.value.clientWidth / container.value.clientHeight, 0.1, 1000);
  scene.add(camera);

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(container.value.clientWidth, container.value.clientHeight);
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  container.value.appendChild(renderer.domElement);

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
  scene.add(ambientLight);
  const directionalLight = new THREE.DirectionalLight(0xffffff, 1.0);
  directionalLight.position.set(10, 15, 10);
  directionalLight.castShadow = true;
  directionalLight.shadow.mapSize.width = 2048;
  directionalLight.shadow.mapSize.height = 2048;
  scene.add(directionalLight);

  // Ground
  const groundGeometry = new THREE.PlaneGeometry(props.gameState.boardSize, props.gameState.boardSize);
  const groundMaterial = new THREE.MeshStandardMaterial({ color: 0x228b22, roughness: 0.9 }); // Forest green
  const ground = new THREE.Mesh(groundGeometry, groundMaterial);
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.5;
  ground.receiveShadow = true;
  scene.add(ground);

  // Boundary Trees
  const boundaryTrees = new THREE.Group();
  const boardSize = props.gameState.boardSize;
  const centerOffset = boardSize / 2;
  // Top and bottom walls
  for (let i = 0; i < boardSize; i++) {
    boundaryTrees.add(createTree(i - centerOffset, -centerOffset));
    boundaryTrees.add(createTree(i - centerOffset, centerOffset - 1));
  }
  // Left and right walls (excluding corners)
  for (let i = 1; i < boardSize - 1; i++) {
    boundaryTrees.add(createTree(-centerOffset, i - centerOffset));
    boundaryTrees.add(createTree(centerOffset - 1, i - centerOffset));
  }
  scene.add(boundaryTrees);

  // Other Snake Placeholder
  otherSnake = new THREE.Group();
  scene.add(otherSnake);

  // Obstacles Placeholder
  obstaclesGroup = new THREE.Group();
  scene.add(obstaclesGroup);

  // Food Placeholder (Apple)
  const textureLoader = new THREE.TextureLoader();
  const foodTexture = textureLoader.load(foodApple);
  const foodGeometry = new THREE.SphereGeometry(0.4, 16, 16);
  const foodMaterial = new THREE.MeshStandardMaterial({ map: foodTexture });
  foodMesh = new THREE.Mesh(foodGeometry, foodMaterial);
  foodMesh.castShadow = true;
  foodMesh.receiveShadow = true;
  scene.add(foodMesh);

  initWeatherEffects();
  animate();
};

const initWeatherEffects = () => {
  // Rain
  const rainGeometry = new THREE.BufferGeometry();
  const rainCount = 10000;
  const rainVertices = new Float32Array(rainCount * 3);
  for (let i = 0; i < rainCount * 3; i++) {
    rainVertices[i] = (Math.random() - 0.5) * 100;
  }
  rainGeometry.setAttribute('position', new THREE.BufferAttribute(rainVertices, 3));
  const rainMaterial = new THREE.PointsMaterial({
    color: 0xaaaaaa,
    size: 0.1,
    transparent: true,
  });
  rainParticles = new THREE.Points(rainGeometry, rainMaterial);
  rainParticles.visible = false;
  scene.add(rainParticles);

  // Snow
  const snowGeometry = new THREE.BufferGeometry();
  const snowCount = 10000;
  const snowVertices = new Float32Array(snowCount * 3);
  for (let i = 0; i < snowCount * 3; i++) {
    snowVertices[i] = (Math.random() - 0.5) * 100;
  }
  snowGeometry.setAttribute('position', new THREE.BufferAttribute(snowVertices, 3));
  const snowMaterial = new THREE.PointsMaterial({
    color: 0xffffff,
    size: 0.1,
    transparent: true,
  });
  snowParticles = new THREE.Points(snowGeometry, snowMaterial);
  snowParticles.visible = false;
  scene.add(snowParticles);
};

const createTree = (x, z) => {
    const tree = new THREE.Group();

    // Trunk
    const trunkGeometry = new THREE.CylinderGeometry(0.2, 0.3, 1.5, 8);
    const trunkMaterial = new THREE.MeshStandardMaterial({ color: 0x8B4513 }); // SaddleBrown
    const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
    trunk.position.y = 0.25;
    trunk.castShadow = true;
    trunk.receiveShadow = true;
    tree.add(trunk);

    // Canopy
    const canopyGeometry = new THREE.ConeGeometry(0.8, 2, 8);
    const canopyMaterial = new THREE.MeshStandardMaterial({ color: 0x228B22 }); // ForestGreen
    const canopy = new THREE.Mesh(canopyGeometry, canopyMaterial);
    canopy.position.y = 1.75;
    canopy.castShadow = true;
    canopy.receiveShadow = true;
    tree.add(canopy);

    tree.position.set(x, 0, z);
    return tree;
};

const updateScene = () => {
    if (!props.gameState || !props.playerId) return;

    updateWeatherVisuals();

    const { players, food, obstacles, boardSize } = props.gameState;
    const player = players[props.playerId];
    if (!player) return;

    // Center of the board
    const centerOffset = boardSize / 2;

    // Update camera to player's head
    const head = player.snake[0];
    camera.position.set(head.x - centerOffset, 0.5, head.y - centerOffset);

    // Point camera in the direction of movement
    const lookAtPosition = new THREE.Vector3();
    switch (player.direction) {
        case 'UP':
            lookAtPosition.set(head.x - centerOffset, 0.5, head.y - centerOffset - 1);
            break;
        case 'DOWN':
            lookAtPosition.set(head.x - centerOffset, 0.5, head.y - centerOffset + 1);
            break;
        case 'LEFT':
            lookAtPosition.set(head.x - centerOffset - 1, 0.5, head.y - centerOffset);
            break;
        case 'RIGHT':
            lookAtPosition.set(head.x - centerOffset + 1, 0.5, head.y - centerOffset);
            break;
    }
    camera.lookAt(lookAtPosition);

    // Update other snake
    otherSnake.clear();
    const otherPlayerId = Object.keys(players).find(id => id !== props.playerId);
    if (otherPlayerId) {
        const otherPlayer = players[otherPlayerId];
        const snakeMaterial = new THREE.MeshStandardMaterial({ color: otherPlayer.color, roughness: 0.3 });
        otherPlayer.snake.forEach(segment => {
            const segmentGeometry = new THREE.SphereGeometry(0.5, 16, 16);
            const segmentMesh = new THREE.Mesh(segmentGeometry, snakeMaterial);
            segmentMesh.position.set(segment.x - centerOffset, 0, segment.y - centerOffset);
            segmentMesh.castShadow = true;
            segmentMesh.receiveShadow = true;
            otherSnake.add(segmentMesh);
        });
    }

    // Update obstacles
    obstaclesGroup.clear();
    if (obstacles) {
        obstacles.forEach(obstacle => {
            obstaclesGroup.add(createTree(obstacle.x - centerOffset, obstacle.y - centerOffset));
        });
    }

    // Update food position
    foodMesh.position.set(food.x - centerOffset, 0, food.y - centerOffset);
};

const updateWeatherVisuals = () => {
    const { weather, nextWeather, weatherTransitionProgress } = props.gameState;

    const weatherConfigs = {
        SUNNY: { color: 0x87ceeb, fog: null },
        RAIN: { color: 0x46494b, fog: new THREE.Fog(0x46494b, 1, 50) },
        SNOW: { color: 0x9ca3a8, fog: new THREE.Fog(0x9ca3a8, 1, 50) },
        FOG: { color: 0xcccccc, fog: new THREE.Fog(0xcccccc, 1, 30) },
        THUNDERSTORM: { color: 0x1d1f21, fog: new THREE.Fog(0x1d1f21, 1, 40) }
    };

    const currentConfig = weatherConfigs[weather];
    const nextConfig = nextWeather ? weatherConfigs[nextWeather] : null;

    if (nextConfig && weatherTransitionProgress > 0) {
        // Interpolate background color
        const currentColor = new THREE.Color(currentConfig.color);
        const nextColor = new THREE.Color(nextConfig.color);
        scene.background.copy(currentColor).lerp(nextColor, weatherTransitionProgress);

        // Interpolate fog
        if (currentConfig.fog && nextConfig.fog) {
            scene.fog = currentConfig.fog.clone();
            scene.fog.color.lerp(nextConfig.fog.color, weatherTransitionProgress);
            scene.fog.near = currentConfig.fog.near + (nextConfig.fog.near - currentConfig.fog.near) * weatherTransitionProgress;
            scene.fog.far = currentConfig.fog.far + (nextConfig.fog.far - currentConfig.fog.far) * weatherTransitionProgress;
        } else if (nextConfig.fog) {
            scene.fog = nextConfig.fog.clone();
            scene.fog.color.lerp(new THREE.Color(0xffffff), 1 - weatherTransitionProgress); // Fake starting color
        } else if (currentConfig.fog) {
            // No easy way to fade out fog, so just keep it until it's gone
        } else {
             scene.fog = null;
        }

    } else {
        scene.background.set(currentConfig.color);
        scene.fog = currentConfig.fog;
    }

    // Handle particles and flashes
    rainParticles.visible = weather === 'RAIN' || nextWeather === 'RAIN';
    rainParticles.material.opacity = weather === 'RAIN' ? (1 - weatherTransitionProgress) : (nextWeather === 'RAIN' ? weatherTransitionProgress : 0);

    snowParticles.visible = weather === 'SNOW' || nextWeather === 'SNOW';
    snowParticles.material.opacity = weather === 'SNOW' ? (1 - weatherTransitionProgress) : (nextWeather === 'SNOW' ? weatherTransitionProgress : 0);

    if (weather === 'THUNDERSTORM' || (nextWeather === 'THUNDERSTORM' && weatherTransitionProgress > 0.5)) {
        if (Math.random() < 0.05) {
            const flash = new THREE.PointLight(0xffffff, 100, 0, 2);
            flash.position.set(Math.random() * 50 - 25, 20 + Math.random() * 10, Math.random() * 50 - 25);
            scene.add(flash);
            setTimeout(() => scene.remove(flash), 100 + Math.random() * 100);
        }
    }
};

const animate = () => {
  requestAnimationFrame(animate);

  if (rainParticles.visible) {
    rainParticles.position.y -= 0.2;
    if (rainParticles.position.y < -50) {
      rainParticles.position.y = 50;
    }
  }

  if (snowParticles.visible) {
    snowParticles.position.y -= 0.05;
    if (snowParticles.position.y < -50) {
      snowParticles.position.y = 50;
    }
  }

  updateScene();
  if (renderer && scene && camera) {
    renderer.render(scene, camera);
  }
};

const onResize = () => {
  if (container.value && renderer) {
    camera.aspect = container.value.clientWidth / container.value.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.value.clientWidth, container.value.clientHeight);
  }
};

watch(() => props.gameState, (newGameState) => {
    if (newGameState && !isInitialized) {
        initThree();
    }
    updateScene();
}, { deep: true });

onMounted(() => {
    window.addEventListener('resize', onResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
  if (renderer) {
    renderer.dispose();
  }
});
</script>

<style scoped>
.fpv {
  width: 100%;
  height: 100%;
  background-color: #000;
}
</style>