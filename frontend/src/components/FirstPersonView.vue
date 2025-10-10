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
let scene, camera, renderer;
let isInitialized = false;

const initThree = () => {
    if (!container.value || isInitialized) return;
    isInitialized = true;

    // Scene
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x333333);

    // Camera
    camera = new THREE.PerspectiveCamera(75, container.value.clientWidth / container.value.clientHeight, 0.1, 1000);
    camera.position.z = 5;
    scene.add(camera);

    // Renderer
    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(container.value.clientWidth, container.value.clientHeight);
    container.value.appendChild(renderer.domElement);

    // Cube
    const geometry = new THREE.BoxGeometry(1, 1, 1);
    const material = new THREE.MeshBasicMaterial({ color: 0x00ff00 });
    const cube = new THREE.Mesh(geometry, material);
    scene.add(cube);

    animate();
};

const animate = () => {
    requestAnimationFrame(animate);
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
    if (newGameState && container.value && !isInitialized) {
        initThree();
    }
}, { deep: true });

onMounted(() => {
    window.addEventListener('resize', onResize);
    if (props.gameState && !isInitialized) {
        initThree();
    }
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