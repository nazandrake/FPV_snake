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
  container.value.appendChild(renderer.domElement);

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
  scene.add(ambientLight);
  const directionalLight = new THREE.DirectionalLight(0xffffff, 1.0);
  directionalLight.position.set(10, 15, 10);
  scene.add(directionalLight);

  // Ground
  const groundGeometry = new THREE.PlaneGeometry(props.gameState.boardSize, props.gameState.boardSize);
  const groundMaterial = new THREE.MeshStandardMaterial({ color: 0x228b22, roughness: 0.9 }); // Forest green
  const ground = new THREE.Mesh(groundGeometry, groundMaterial);
  ground.rotation.x = -Math.PI / 2;
  ground.position.y = -0.5;
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
  scene.add(foodMesh);


  animate();
};

const createTree = (x, z) => {
    const tree = new THREE.Group();

    // Trunk
    const trunkGeometry = new THREE.CylinderGeometry(0.2, 0.3, 1.5, 8);
    const trunkMaterial = new THREE.MeshStandardMaterial({ color: 0x8B4513 }); // SaddleBrown
    const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
    trunk.position.y = 0.25;
    tree.add(trunk);

    // Canopy
    const canopyGeometry = new THREE.ConeGeometry(0.8, 2, 8);
    const canopyMaterial = new THREE.MeshStandardMaterial({ color: 0x228B22 }); // ForestGreen
    const canopy = new THREE.Mesh(canopyGeometry, canopyMaterial);
    canopy.position.y = 1.75;
    tree.add(canopy);

    tree.position.set(x, 0, z);
    return tree;
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
        const snakeMaterial = new THREE.MeshStandardMaterial({ color: otherPlayer.color, roughness: 0.3 });
        otherPlayer.snake.forEach(segment => {
            const segmentGeometry = new THREE.SphereGeometry(0.5, 16, 16);
            const segmentMesh = new THREE.Mesh(segmentGeometry, snakeMaterial);
            segmentMesh.position.set(segment.x - centerOffset, 0, segment.y - centerOffset);
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