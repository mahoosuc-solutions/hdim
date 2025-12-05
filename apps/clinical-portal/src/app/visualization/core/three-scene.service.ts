import { Injectable, NgZone } from '@angular/core';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import Stats from 'three/examples/jsm/libs/stats.module.js';

/**
 * Scene Configuration Interface
 */
export interface SceneConfig {
  antialias?: boolean;
  alpha?: boolean;
  powerPreference?: 'high-performance' | 'low-power' | 'default';
  enableStats?: boolean;
  backgroundColor?: number;
  cameraPosition?: THREE.Vector3;
  cameraFov?: number;
  enableOrbitControls?: boolean;
  enableGrid?: boolean;
  enableAxes?: boolean;
}

/**
 * Three.js Scene Service
 *
 * Core service for managing Three.js scene, camera, renderer, and animation loop.
 * Provides a foundation for all 3D visualizations in the Clinical Portal.
 *
 * Features:
 * - Scene initialization with customizable settings
 * - Automatic resize handling
 * - OrbitControls for camera interaction
 * - Animation loop management
 * - Performance monitoring with Stats.js
 * - Proper cleanup on destroy
 */
@Injectable({
  providedIn: 'root'
})
export class ThreeSceneService {
  // Three.js core objects
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private controls?: OrbitControls;
  private stats?: Stats;

  // Animation state
  private animationFrameId?: number;
  private isAnimating = false;
  private animationCallbacks: Array<(delta: number, elapsed: number) => void> = [];

  // Timing
  private clock = new THREE.Clock();

  // Container reference
  private container?: HTMLElement;

  constructor(private ngZone: NgZone) {}

  /**
   * Initialize the Three.js scene
   */
  initScene(container: HTMLElement, config: SceneConfig = {}): void {
    this.container = container;

    // Default configuration
    const finalConfig: Required<SceneConfig> = {
      antialias: config.antialias ?? true,
      alpha: config.alpha ?? false,
      powerPreference: config.powerPreference ?? 'high-performance',
      enableStats: config.enableStats ?? true,
      backgroundColor: config.backgroundColor ?? 0x0a0e1a,
      cameraPosition: config.cameraPosition ?? new THREE.Vector3(0, 50, 100),
      cameraFov: config.cameraFov ?? 60,
      enableOrbitControls: config.enableOrbitControls ?? true,
      enableGrid: config.enableGrid ?? true,
      enableAxes: config.enableAxes ?? false,
    };

    // Create scene
    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(finalConfig.backgroundColor);
    this.scene.fog = new THREE.FogExp2(finalConfig.backgroundColor, 0.002);

    // Create camera
    const aspect = container.clientWidth / container.clientHeight;
    this.camera = new THREE.PerspectiveCamera(finalConfig.cameraFov, aspect, 0.1, 1000);
    this.camera.position.copy(finalConfig.cameraPosition);
    this.camera.lookAt(0, 0, 0);

    // Create renderer
    this.renderer = new THREE.WebGLRenderer({
      antialias: finalConfig.antialias,
      alpha: finalConfig.alpha,
      powerPreference: finalConfig.powerPreference,
    });
    this.renderer.setSize(container.clientWidth, container.clientHeight);
    this.renderer.setPixelRatio(window.devicePixelRatio);
    this.renderer.shadowMap.enabled = true;
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    container.appendChild(this.renderer.domElement);

    // Add OrbitControls
    if (finalConfig.enableOrbitControls) {
      this.controls = new OrbitControls(this.camera, this.renderer.domElement);
      this.controls.enableDamping = true;
      this.controls.dampingFactor = 0.05;
      this.controls.maxPolarAngle = Math.PI / 2;
      this.controls.minDistance = 10;
      this.controls.maxDistance = 500;
    }

    // Add grid helper
    if (finalConfig.enableGrid) {
      const gridHelper = new THREE.GridHelper(200, 20, 0x444444, 0x222222);
      this.scene.add(gridHelper);
    }

    // Add axes helper
    if (finalConfig.enableAxes) {
      const axesHelper = new THREE.AxesHelper(50);
      this.scene.add(axesHelper);
    }

    // Add ambient light
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
    this.scene.add(ambientLight);

    // Add directional light
    const directionalLight = new THREE.DirectionalLight(0xffffff, 1);
    directionalLight.position.set(50, 50, 50);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 2048;
    directionalLight.shadow.mapSize.height = 2048;
    directionalLight.shadow.camera.near = 0.5;
    directionalLight.shadow.camera.far = 500;
    this.scene.add(directionalLight);

    // Add Stats.js
    if (finalConfig.enableStats) {
      this.stats = new Stats();
      this.stats.showPanel(0); // 0: fps, 1: ms, 2: mb
      this.stats.dom.style.position = 'absolute';
      this.stats.dom.style.top = '0px';
      this.stats.dom.style.left = '0px';
      container.appendChild(this.stats.dom);
    }

    // Handle window resize
    window.addEventListener('resize', this.onWindowResize.bind(this));

    // Reset clock
    this.clock.start();
  }

  /**
   * Start the animation loop
   */
  startAnimation(): void {
    if (this.isAnimating || !this.renderer || !this.scene || !this.camera) return;

    this.isAnimating = true;
    this.clock.start();

    // Run animation outside Angular zone for better performance
    this.ngZone.runOutsideAngular(() => {
      this.animate();
    });
  }

  /**
   * Stop the animation loop
   */
  stopAnimation(): void {
    this.isAnimating = false;
    if (this.animationFrameId !== undefined) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = undefined;
    }
  }

  /**
   * Animation loop
   */
  private animate(): void {
    if (!this.isAnimating || !this.renderer || !this.scene || !this.camera) {
      this.isAnimating = false;
      return;
    }

    this.animationFrameId = requestAnimationFrame(() => this.animate());

    // Update stats
    this.stats?.begin();

    // Get delta time
    const delta = this.clock.getDelta();
    const elapsed = this.clock.getElapsedTime();

    // Update controls
    this.controls?.update();

    // Execute registered animation callbacks
    this.animationCallbacks.forEach(callback => callback(delta, elapsed));

    // Render scene
    this.renderer.render(this.scene, this.camera);

    // Update stats
    this.stats?.end();
  }

  /**
   * Register an animation callback
   */
  registerAnimationCallback(callback: (delta: number, elapsed: number) => void): void {
    this.animationCallbacks.push(callback);
  }

  /**
   * Unregister an animation callback
   */
  unregisterAnimationCallback(callback: (delta: number, elapsed: number) => void): void {
    const index = this.animationCallbacks.indexOf(callback);
    if (index !== -1) {
      this.animationCallbacks.splice(index, 1);
    }
  }

  /**
   * Handle window resize
   */
  private onWindowResize(): void {
    if (!this.container) return;

    const width = this.container.clientWidth;
    const height = this.container.clientHeight;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();

    this.renderer.setSize(width, height);
  }

  /**
   * Add object to scene
   */
  addToScene(object: THREE.Object3D): void {
    this.scene.add(object);
  }

  /**
   * Remove object from scene
   */
  removeFromScene(object: THREE.Object3D): void {
    this.scene.remove(object);
  }

  /**
   * Get the scene
   */
  getScene(): THREE.Scene {
    return this.scene;
  }

  /**
   * Get the camera
   */
  getCamera(): THREE.PerspectiveCamera {
    return this.camera;
  }

  /**
   * Get the renderer
   */
  getRenderer(): THREE.WebGLRenderer {
    return this.renderer;
  }

  /**
   * Get the controls
   */
  getControls(): OrbitControls | undefined {
    return this.controls;
  }

  /**
   * Reset camera to initial position
   */
  resetCamera(): void {
    if (!this.camera) return;

    // Reset to default position
    this.camera.position.set(0, 80, 150);
    this.camera.lookAt(0, 0, 0);

    // Reset controls target
    if (this.controls) {
      this.controls.target.set(0, 0, 0);
      this.controls.update();
    }
  }

  /**
   * Toggle stats display
   */
  toggleStats(enabled: boolean): void {
    if (!this.stats || !this.stats.dom) return;

    this.stats.dom.style.display = enabled ? 'block' : 'none';
  }

  /**
   * Clean up resources
   */
  dispose(): void {
    // Stop animation
    this.stopAnimation();

    // Remove resize listener
    window.removeEventListener('resize', this.onWindowResize.bind(this));

    // Dispose controls
    this.controls?.dispose();

    // Dispose renderer
    if (this.renderer) {
      this.renderer.dispose();
    }

    // Remove renderer DOM element
    if (this.container && this.renderer?.domElement && this.container.contains(this.renderer.domElement)) {
      this.container.removeChild(this.renderer.domElement);
    }

    // Remove stats
    if (this.container && this.stats?.dom && this.container.contains(this.stats.dom)) {
      this.container.removeChild(this.stats.dom);
    }

    // Clear scene
    this.scene?.traverse((object) => {
      if (object instanceof THREE.Mesh) {
        object.geometry?.dispose();
        if (Array.isArray(object.material)) {
          object.material.forEach(material => material.dispose());
        } else {
          object.material?.dispose();
        }
      }
    });

    // Clear animation callbacks
    this.animationCallbacks = [];
  }
}
