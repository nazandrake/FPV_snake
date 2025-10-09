<template>
  <div ref="container" class="fpv"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import * as THREE from 'three';

const props = defineProps({
  gameState: Object,
  playerId: String,
});

const container = ref(null);
let scene, camera, renderer, wall, otherSnake, foodMesh, obstaclesGroup;
let isInitialized = false;

const initThree = () => {
  if (!container.value || isInitialized) return;
  isInitialized = true;

  // Scene
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x000000);

  // Camera
  camera = new THREE.PerspectiveCamera(75, container.value.clientWidth / container.value.clientHeight, 0.1, 1000);
  scene.add(camera);

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(container.value.clientWidth, container.value.clientHeight);
  container.value.appendChild(renderer.domElement);

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);
  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
  directionalLight.position.set(5, 10, 7.5);
  scene.add(directionalLight);

  // Ground
  const groundGeometry = new THREE.PlaneGeometry(props.gameState.boardSize, props.gameState.boardSize);
  const groundMaterial = new THREE.MeshStandardMaterial({ color: 0x222222, roughness: 0.8 });
  const ground = new THREE.Mesh(groundGeometry, groundMaterial);
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.5;
  scene.add(ground);

  // Walls
  const wallGeometry = new THREE.BoxGeometry(props.gameState.boardSize, 2, props.gameState.boardSize);
  const wallMaterial = new THREE.MeshStandardMaterial({ color: 0x333333 }); // Dark grey solid color
  wall = new THREE.Mesh(wallGeometry, wallMaterial);
  wall.position.y = 0.5;
  scene.add(wall);

  // Other Snake Placeholder
  const snakeMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
  otherSnake = new THREE.Group();
  scene.add(otherSnake);

  // Obstacles Placeholder
  obstaclesGroup = new THREE.Group();
  scene.add(obstaclesGroup);

  // Food Placeholder
  const foodGeometry = new THREE.SphereGeometry(0.5, 16, 16);
  const foodMaterial = new THREE.MeshStandardMaterial({ color: 0xf1c40f });
  foodMesh = new THREE.Mesh(foodGeometry, foodMaterial);
  scene.add(foodMesh);


  animate();
};

const updateScene = () => {
    if (!props.gameState || !props.playerId) return;

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
        const snakeMaterial = new THREE.MeshStandardMaterial({ color: otherPlayer.color });
        otherPlayer.snake.forEach(segment => {
            const segmentGeometry = new THREE.BoxGeometry(1, 1, 1);
            const segmentMesh = new THREE.Mesh(segmentGeometry, snakeMaterial);
            segmentMesh.position.set(segment.x - centerOffset, 0, segment.y - centerOffset);
            otherSnake.add(segmentMesh);
        });
    }

    // Update obstacles
    obstaclesGroup.clear();
    if (obstacles) {
        const obstacleMaterial = new THREE.MeshStandardMaterial({ color: 0x654321 });
        obstacles.forEach(obstacle => {
            const obstacleGeometry = new THREE.BoxGeometry(1, 1, 1);
            const obstacleMesh = new THREE.Mesh(obstacleGeometry, obstacleMaterial);
            obstacleMesh.position.set(obstacle.x - centerOffset, 0, obstacle.y - centerOffset);
            obstaclesGroup.add(obstacleMesh);
        });
    }

    // Update food position
    foodMesh.position.set(food.x - centerOffset, 0, food.y - centerOffset);
};

const animate = () => {
  requestAnimationFrame(animate);
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