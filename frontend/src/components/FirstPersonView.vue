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

const emit = defineEmits(['set-turning', 'set-moving']);

const container = ref(null);
let scene, camera, renderer;
let isInitialized = false;
let playersGroup, foodMesh, obstaclesGroup, buffsGroup;
let rainParticles, snowParticles, lightning;

const initThree = () => {
    if (!container.value || !props.gameState || isInitialized) return;
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
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
    scene.add(ambientLight);
    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(20, 30, 20);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 2048;
    directionalLight.shadow.mapSize.height = 2048;
    directionalLight.shadow.camera.near = 0.5;
    directionalLight.shadow.camera.far = 500;
    scene.add(directionalLight);

    // Ground
    const textureLoader = new THREE.TextureLoader();
    const grassTexture = textureLoader.load('src/assets/grass.jpg');
    grassTexture.wrapS = THREE.RepeatWrapping;
    grassTexture.wrapT = THREE.RepeatWrapping;
    grassTexture.repeat.set(20, 20);

    const groundGeometry = new THREE.PlaneGeometry(props.gameState.boardSize, props.gameState.boardSize);
    const groundMaterial = new THREE.MeshStandardMaterial({ map: grassTexture, roughness: 0.9 });
    const ground = new THREE.Mesh(groundGeometry, groundMaterial);
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.5;
    ground.receiveShadow = true;
    scene.add(ground);

    // Boundary Trees
    const boundaryTrees = new THREE.Group();
    const boardSize = props.gameState.boardSize;
    const centerOffset = boardSize / 2;
    const treeSpacing = 2; // Denser trees
    for (let i = -centerOffset; i <= centerOffset; i += treeSpacing) {
        boundaryTrees.add(createTree(i, -centerOffset));
        boundaryTrees.add(createTree(i, centerOffset));
        boundaryTrees.add(createTree(-centerOffset, i));
        boundaryTrees.add(createTree(centerOffset, i));
    }
    scene.add(boundaryTrees);

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

    // Weather systems
    initWeatherSystems();

    animate();
};

const initWeatherSystems = () => {
    const boardSize = props.gameState.boardSize;

    // Rain
    const rainGeometry = new THREE.BufferGeometry();
    const rainVertices = [];
    for (let i = 0; i < 1000; i++) {
        rainVertices.push(
            Math.random() * boardSize - boardSize / 2,
            Math.random() * 20,
            Math.random() * boardSize - boardSize / 2
        );
    }
    rainGeometry.setAttribute('position', new THREE.Float32BufferAttribute(rainVertices, 3));
    const rainMaterial = new THREE.PointsMaterial({ color: 0xaaaaaa, size: 0.1, transparent: true });
    rainParticles = new THREE.Points(rainGeometry, rainMaterial);
    scene.add(rainParticles);

    // Snow
    const snowGeometry = new THREE.BufferGeometry();
    const snowVertices = [];
    for (let i = 0; i < 1000; i++) {
        snowVertices.push(
            Math.random() * boardSize - boardSize / 2,
            Math.random() * 20,
            Math.random() * boardSize - boardSize / 2
        );
    }
    snowGeometry.setAttribute('position', new THREE.Float32BufferAttribute(snowVertices, 3));
    const snowMaterial = new THREE.PointsMaterial({ color: 0xffffff, size: 0.1, transparent: true });
    snowParticles = new THREE.Points(snowGeometry, snowMaterial);
    scene.add(snowParticles);

    // Lightning
    lightning = new THREE.PointLight(0xffffff, 0, 100);
    scene.add(lightning);
};

const createCharacter = (color) => {
    const character = new THREE.Group();
    const head = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.5, 0.5), new THREE.MeshStandardMaterial({ color }));
    head.position.y = 0.75;
    head.castShadow = true;
    character.add(head);

    const body = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.8, 0.4), new THREE.MeshStandardMaterial({ color }));
    body.position.y = 0;
    body.castShadow = true;
    character.add(body);

    return character;
};

const createTree = (x, z) => {
    const tree = new THREE.Group();
    const trunkGeometry = new THREE.CylinderGeometry(0.2, 0.3, 1.5, 8);
    const trunkMaterial = new THREE.MeshStandardMaterial({ color: 0x8B4513 });
    const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
    trunk.position.y = 0.25;
    trunk.castShadow = true;
    tree.add(trunk);

    const canopyGeometry = new THREE.ConeGeometry(0.8, 2, 8);
    const canopyMaterial = new THREE.MeshStandardMaterial({ color: 0x228B22 });
    const canopy = new THREE.Mesh(canopyGeometry, canopyMaterial);
    canopy.position.y = 1.75;
    canopy.castShadow = true;
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
        character.rotation.y = -player.direction; // Rotate character
        playersGroup.add(character);
    }

    // Update camera to player's head
    const mainPlayer = players[props.playerId];
    if (mainPlayer) {
        const head = mainPlayer.position;
        camera.position.set(head.x - centerOffset, 0.5, head.y - centerOffset);

        // Point camera in the direction of movement
        const lookAtPosition = new THREE.Vector3(
            head.x - centerOffset + Math.cos(mainPlayer.direction),
            0.5,
            head.y - centerOffset + Math.sin(mainPlayer.direction)
        );
        camera.lookAt(lookAtPosition);
    }

    // Update food
    foodMesh.position.set(food.x - centerOffset, 0, food.y - centerOffset);
    foodMesh.castShadow = true;

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
            buffMesh.castShadow = true;
            buffsGroup.add(buffMesh);
        });
    }
};

const animate = () => {
    requestAnimationFrame(animate);
    updateScene();
    updateWeather();
    if (renderer && scene && camera) {
        renderer.render(scene, camera);
    }
};

const updateWeather = () => {
    if (!props.gameState || !props.gameState.weather) return;

    const { weather, nextWeather, weatherTransitionProgress } = props.gameState;
    const boardSize = props.gameState.boardSize;

    const applyWeather = (type, alpha) => {
        if (type === 'RAIN') {
            rainParticles.visible = true;
            rainParticles.material.opacity = alpha;
            const positions = rainParticles.geometry.attributes.position.array;
            for (let i = 1; i < positions.length; i += 3) {
                positions[i] -= 0.1;
                if (positions[i] < 0) {
                    positions[i] = 20;
                }
            }
            rainParticles.geometry.attributes.position.needsUpdate = true;
        } else if (type === 'SNOW') {
            snowParticles.visible = true;
            snowParticles.material.opacity = alpha;
            const positions = snowParticles.geometry.attributes.position.array;
            for (let i = 1; i < positions.length; i += 3) {
                positions[i] -= 0.05;
                if (positions[i] < 0) {
                    positions[i] = 20;
                }
            }
            snowParticles.geometry.attributes.position.needsUpdate = true;
        } else if (type === 'FOG') {
            scene.fog = new THREE.Fog(0xcccccc, 0.015, 100 * alpha);
        } else if (type === 'THUNDERSTORM') {
            if (Math.random() > 0.99) {
                lightning.intensity = 2 * alpha;
                lightning.position.set(
                    Math.random() * boardSize - boardSize / 2,
                    Math.random() * 10 + 10,
                    Math.random() * boardSize - boardSize / 2
                );
            } else {
                lightning.intensity *= 0.9;
            }
        }
    };

    // Hide all weather effects by default
    rainParticles.visible = false;
    snowParticles.visible = false;
    scene.fog = null;
    lightning.intensity = 0;

    applyWeather(weather, 1 - weatherTransitionProgress);
    if (nextWeather && weatherTransitionProgress > 0) {
        applyWeather(nextWeather, weatherTransitionProgress);
    }
};

const onKeyDown = (event) => {
    if (event.repeat) return;
    switch (event.key.toLowerCase()) {
        case 'w': emit('set-moving', true); break;
        case 's': emit('set-moving', false); break; // Or handle backward movement
        case 'a': emit('set-turning', 'LEFT'); break;
        case 'd': emit('set-turning', 'RIGHT'); break;
    }
};

const onKeyUp = (event) => {
    switch (event.key.toLowerCase()) {
        case 'w':
        case 's':
            emit('set-moving', false);
            break;
        case 'a':
        case 'd':
            emit('set-turning', 'NONE');
            break;
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
}, { deep: true, immediate: true });

onMounted(() => {
    window.addEventListener('resize', onResize);
    // The watcher will handle initialization once gameState is ready.
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
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #000;
}
</style>