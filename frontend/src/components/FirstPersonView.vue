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

const emit = defineEmits(['start-moving', 'stop-moving']);

const container = ref(null);
let scene, camera, renderer, controls;
let isInitialized = false;
let playersGroup, foodMesh, obstaclesGroup, buffsGroup;
let skybox;

const keyState = {};

const initThree = () => {
    if (!container.value || !props.gameState || isInitialized) return;
    isInitialized = true;

    // Scene
    scene = new THREE.Scene();

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
    const groundMaterial = new THREE.MeshStandardMaterial({ color: 0x228b22, roughness: 0.9 });
    const ground = new THREE.Mesh(groundGeometry, groundMaterial);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.5;
    scene.add(ground);

    // Skybox
    const loader = new THREE.CubeTextureLoader();
    const texture = loader.load([
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/pos-x.jpg',
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/neg-x.jpg',
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/pos-y.jpg',
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/neg-y.jpg',
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/pos-z.jpg',
        'https://threejsfundamentals.org/threejs/resources/images/cubemaps/computer-history-museum/neg-z.jpg',
    ]);
    scene.background = texture;
    skybox = scene.background;


    // Groups for objects
    playersGroup = new THREE.Group();
    scene.add(playersGroup);

    obstaclesGroup = new THREE.Group();
    scene.add(obstaclesGroup);

    buffsGroup = new THREE.Group();
    scene.add(buffsGroup);

    // Food
    const foodGeometry = new THREE.SphereGeometry(0.4, 16, 16);
    const foodMaterial = new THREE.MeshStandardMaterial({ color: 0xff0000 });
    foodMesh = new THREE.Mesh(foodGeometry, foodMaterial);
    scene.add(foodMesh);

    // Event Listeners
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    window.addEventListener('resize', onResize);

    animate();
};

const createCharacter = (color) => {
    const character = new THREE.Group();
    const head = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.5, 0.5), new THREE.MeshStandardMaterial({ color }));
    head.position.y = 0.75;
    character.add(head);

    const body = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.8, 0.4), new THREE.MeshStandardMaterial({ color }));
    body.position.y = 0;
    character.add(body);

    return character;
};

const createTree = (x, z) => {
    const tree = new THREE.Group();
    const trunkGeometry = new THREE.CylinderGeometry(0.2, 0.3, 1.5, 8);
    const trunkMaterial = new THREE.MeshStandardMaterial({ color: 0x8B4513 });
    const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
    trunk.position.y = 0.25;
    tree.add(trunk);

    const canopyGeometry = new THREE.ConeGeometry(0.8, 2, 8);
    const canopyMaterial = new THREE.MeshStandardMaterial({ color: 0x228B22 });
    const canopy = new THREE.Mesh(canopyGeometry, canopyMaterial);
    canopy.position.y = 1.75;
    tree.add(canopy);

    tree.position.set(x, 0, z);
    return tree;
};

const updateScene = () => {
    if (!props.gameState || !props.playerId) return;

    const { players, food, obstacles, buffs, boardSize } = props.gameState;
    const centerOffset = boardSize / 2;

    // Update players
    playersGroup.clear();
    for (const id in players) {
        const player = players[id];
        const character = createCharacter(player.color);
        character.position.set(player.position.x - centerOffset, 0, player.position.y - centerOffset);
        playersGroup.add(character);
    }

    // Update camera to player's head
    const mainPlayer = players[props.playerId];
    if (mainPlayer) {
        const head = mainPlayer.position;
        camera.position.set(head.x - centerOffset, 0.5, head.y - centerOffset);

        // Point camera in the direction of movement
        const lookAtPosition = new THREE.Vector3();
        switch (mainPlayer.direction) {
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
    }

    // Update food
    foodMesh.position.set(food.x - centerOffset, 0, food.y - centerOffset);

    // Update obstacles
    obstaclesGroup.clear();
    if (obstacles) {
        obstacles.forEach(obstacle => {
            obstaclesGroup.add(createTree(obstacle.x - centerOffset, obstacle.y - centerOffset));
        });
    }

    // Update buffs
    buffsGroup.clear();
    if (buffs) {
        buffs.forEach(buff => {
            const buffGeometry = new THREE.SphereGeometry(0.3, 16, 16);
            let buffMaterial;
            if (buff.type === 'SPEED') {
                buffMaterial = new THREE.MeshStandardMaterial({ color: 0xffff00, emissive: 0xffff00 }); // Yellow
            } else { // TIMER
                buffMaterial = new THREE.MeshStandardMaterial({ color: 0x00ff00, emissive: 0x00ff00 }); // Green
            }
            const buffMesh = new THREE.Mesh(buffGeometry, buffMaterial);
            buffMesh.position.set(buff.position.x - centerOffset, 0, buff.position.y - centerOffset);
            buffsGroup.add(buffMesh);
        });
    }
};

const animate = () => {
    requestAnimationFrame(animate);
    if (scene) {
        scene.rotation.y += 0.0001;
    }
    updateScene();
    if (renderer && scene && camera) {
        renderer.render(scene, camera);
    }
};

const onKeyDown = (event) => {
    if (event.repeat) return;
    let direction = null;
    switch (event.key) {
        case 'w': case 'W': direction = 'UP'; break;
        case 's': case 'S': direction = 'DOWN'; break;
        case 'a': case 'A': direction = 'LEFT'; break;
        case 'd': case 'D': direction = 'RIGHT'; break;
    }
    if (direction) {
        emit('start-moving', direction);
    }
};

const onKeyUp = (event) => {
    let direction = null;
    switch (event.key) {
        case 'w': case 'W': direction = 'UP'; break;
        case 's': case 'S': direction = 'DOWN'; break;
        case 'a': case 'A': direction = 'LEFT'; break;
        case 'd': case 'D': direction = 'RIGHT'; break;
    }
    if (direction) {
        emit('stop-moving');
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
    if (newGameState && container.value && !isInitialized) {
        initThree();
    }
}, { deep: true });

onMounted(() => {
    // The watcher will handle initialization once gameState is ready.
    // This hook ensures container.value is available for the watcher.
});

onUnmounted(() => {
    window.removeEventListener('keydown', onKeyDown);
    window.removeEventListener('keyup', onKeyUp);
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