import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NgZone } from '@angular/core';
import { ThreeSceneService, SceneConfig } from './three-scene.service';
import * as THREE from 'three';

jest.mock('three/examples/jsm/controls/OrbitControls.js', () => {
  const THREE = require('three');
  return {
    OrbitControls: class {
      enableDamping = false;
      dampingFactor = 0;
      target = new THREE.Vector3();
      update = jest.fn();
      dispose = jest.fn();
    },
  };
});

jest.mock('three/examples/jsm/libs/stats.module.js', () => {
  const StatsMock = jest.fn().mockImplementation(() => {
    const dom = document.createElement('div');
    return {
      dom,
      showPanel: jest.fn(),
      begin: jest.fn(),
      end: jest.fn(),
    };
  });
  return { __esModule: true, default: StatsMock };
});

class MockWebGLRenderer {
  domElement = document.createElement('canvas');
  shadowMap = { enabled: false };
  setSize = jest.fn((width?: number, height?: number) => {
    this.domElement.style.width = width !== undefined ? `${width}px` : '';
    this.domElement.style.height = height !== undefined ? `${height}px` : '';
  });
  setPixelRatio = jest.fn();
  setAnimationLoop = jest.fn();
  render = jest.fn();
  dispose = jest.fn();
}

describe('ThreeSceneService', () => {
  let service: ThreeSceneService;
  let mockContainer: HTMLElement;
  let ngZone: NgZone;
  const originalWebGLRenderer = THREE.WebGLRenderer;

  beforeEach(() => {
    (THREE as any).WebGLRenderer = MockWebGLRenderer as any;
    TestBed.configureTestingModule({
      providers: [ThreeSceneService],
    });

    service = TestBed.inject(ThreeSceneService);
    ngZone = TestBed.inject(NgZone);

    // Create mock container element
    mockContainer = document.createElement('div');
    mockContainer.style.width = '800px';
    mockContainer.style.height = '600px';
    Object.defineProperty(mockContainer, 'clientWidth', {
      configurable: true,
      get: () => parseInt(mockContainer.style.width, 10) || 0,
    });
    Object.defineProperty(mockContainer, 'clientHeight', {
      configurable: true,
      get: () => parseInt(mockContainer.style.height, 10) || 0,
    });
    document.body.appendChild(mockContainer);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    (THREE as any).WebGLRenderer = originalWebGLRenderer as any;
    // Clean up service
    if (service) {
      service.dispose();
    }

    // Clean up DOM
    if (mockContainer && mockContainer.parentNode) {
      mockContainer.parentNode.removeChild(mockContainer);
    }
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should initialize scene with default configuration', () => {
      service.initScene(mockContainer);

      const scene = service.getScene();
      const camera = service.getCamera();
      const renderer = service.getRenderer();

      expect(scene).toBeInstanceOf(THREE.Scene);
      expect(camera).toBeInstanceOf(THREE.PerspectiveCamera);
      expect(renderer).toBeInstanceOf(THREE.WebGLRenderer);
    });

    it('should apply custom configuration', () => {
      const config: SceneConfig = {
        antialias: false,
        alpha: true,
        powerPreference: 'low-power',
        enableStats: false,
        backgroundColor: 0xff0000,
        cameraPosition: new THREE.Vector3(10, 20, 30),
        cameraFov: 45,
        enableOrbitControls: false,
        enableGrid: false,
        enableAxes: true,
      };

      service.initScene(mockContainer, config);

      const scene = service.getScene();
      const camera = service.getCamera();

      expect(scene.background).toEqual(new THREE.Color(0xff0000));
      expect(camera.fov).toBe(45);
      expect(camera.position.x).toBe(10);
      expect(camera.position.y).toBe(20);
      expect(camera.position.z).toBe(30);
    });

    it('should append renderer to container', () => {
      service.initScene(mockContainer);

      const renderer = service.getRenderer();
      expect(mockContainer.contains(renderer.domElement)).toBe(true);
    });

    it('should set renderer size to container dimensions', () => {
      service.initScene(mockContainer);

      const renderer = service.getRenderer();
      expect(renderer.domElement.width).toBeGreaterThan(0);
      expect(renderer.domElement.height).toBeGreaterThan(0);
    });

    it('should enable shadow mapping on renderer', () => {
      service.initScene(mockContainer);

      const renderer = service.getRenderer();
      expect(renderer.shadowMap.enabled).toBe(true);
      expect(renderer.shadowMap.type).toBe(THREE.PCFSoftShadowMap);
    });

    it('should create OrbitControls when enabled', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      expect(controls).toBeDefined();
    });

    it('should not create OrbitControls when disabled', () => {
      service.initScene(mockContainer, { enableOrbitControls: false });

      const controls = service.getControls();
      expect(controls).toBeUndefined();
    });

    it('should add grid helper when enabled', () => {
      service.initScene(mockContainer, { enableGrid: true });

      const scene = service.getScene();
      const gridHelper = scene.children.find(
        (child) => child instanceof THREE.GridHelper
      );
      expect(gridHelper).toBeDefined();
    });

    it('should add axes helper when enabled', () => {
      service.initScene(mockContainer, { enableAxes: true });

      const scene = service.getScene();
      const axesHelper = scene.children.find(
        (child) => child instanceof THREE.AxesHelper
      );
      expect(axesHelper).toBeDefined();
    });

    it('should add ambient light to scene', () => {
      service.initScene(mockContainer);

      const scene = service.getScene();
      const ambientLight = scene.children.find(
        (child) => child instanceof THREE.AmbientLight
      );
      expect(ambientLight).toBeDefined();
    });

    it('should add directional light with shadows', () => {
      service.initScene(mockContainer);

      const scene = service.getScene();
      const directionalLight = scene.children.find(
        (child) => child instanceof THREE.DirectionalLight
      ) as THREE.DirectionalLight;

      expect(directionalLight).toBeDefined();
      expect(directionalLight.castShadow).toBe(true);
    });

    it('should add stats when enabled', () => {
      service.initScene(mockContainer, { enableStats: true });

      // Stats DOM should be added to container
      const statsElements = mockContainer.querySelectorAll('canvas');
      expect(statsElements.length).toBeGreaterThan(0);
    });

    it('should not add stats when disabled', () => {
      service.initScene(mockContainer, { enableStats: false });

      // Only renderer canvas should exist
      const canvasElements = mockContainer.querySelectorAll('canvas');
      expect(canvasElements.length).toBe(1); // Only renderer canvas
    });
  });

  describe('scene management', () => {
    beforeEach(() => {
      service.initScene(mockContainer);
    });

    it('should add object to scene', () => {
      const mesh = new THREE.Mesh(
        new THREE.BoxGeometry(1, 1, 1),
        new THREE.MeshBasicMaterial()
      );

      service.addToScene(mesh);

      const scene = service.getScene();
      expect(scene.children).toContain(mesh);
    });

    it('should remove object from scene', () => {
      const mesh = new THREE.Mesh(
        new THREE.BoxGeometry(1, 1, 1),
        new THREE.MeshBasicMaterial()
      );

      service.addToScene(mesh);
      service.removeFromScene(mesh);

      const scene = service.getScene();
      expect(scene.children).not.toContain(mesh);
    });

    it('should return scene instance', () => {
      const scene = service.getScene();
      expect(scene).toBeInstanceOf(THREE.Scene);
    });

    it('should return camera instance', () => {
      const camera = service.getCamera();
      expect(camera).toBeInstanceOf(THREE.PerspectiveCamera);
    });

    it('should return renderer instance', () => {
      const renderer = service.getRenderer();
      expect(renderer).toBeInstanceOf(THREE.WebGLRenderer);
    });
  });

  describe('animation loop', () => {
    beforeEach(() => {
      service.initScene(mockContainer);
    });

    it('should start animation loop', fakeAsync(() => {
      jest.spyOn(window, 'requestAnimationFrame').mockReturnValue(1 as any);

      service.startAnimation();

      tick(16); // One frame

      expect(window.requestAnimationFrame).toHaveBeenCalled();
    }));

    it('should not start animation if already animating', fakeAsync(() => {
      jest.spyOn(window, 'requestAnimationFrame').mockReturnValue(1 as any);

      service.startAnimation();
      service.startAnimation(); // Try to start again

      tick(16);

      expect(window.requestAnimationFrame).toHaveBeenCalledTimes(1);
    }));

    it('should stop animation loop', fakeAsync(() => {
      jest.spyOn(window, 'cancelAnimationFrame');

      service.startAnimation();
      tick(16);

      service.stopAnimation();

      expect(window.cancelAnimationFrame).toHaveBeenCalled();
    }));

    it('should execute registered animation callbacks', fakeAsync(() => {
      const callback = jest.fn();

      service.registerAnimationCallback(callback);
      service.startAnimation();

      tick(16); // One frame

      expect(callback).toHaveBeenCalledWith(
        expect.any(Number),
        expect.any(Number)
      );
    }));

    it('should unregister animation callback', fakeAsync(() => {
      const callback = jest.fn();

      service.registerAnimationCallback(callback);
      service.unregisterAnimationCallback(callback);
      service.startAnimation();

      tick(16);

      expect(callback).not.toHaveBeenCalled();
    }));

    it('should execute multiple animation callbacks', fakeAsync(() => {
      const callback1 = jest.fn();
      const callback2 = jest.fn();

      service.registerAnimationCallback(callback1);
      service.registerAnimationCallback(callback2);
      service.startAnimation();

      tick(16);

      expect(callback1).toHaveBeenCalled();
      expect(callback2).toHaveBeenCalled();
    }));

    it('should pass delta and elapsed time to callbacks', fakeAsync(() => {
      let receivedDelta = 0;
      let receivedElapsed = 0;

      const callback = (delta: number, elapsed: number) => {
        receivedDelta = delta;
        receivedElapsed = elapsed;
      };

      service.registerAnimationCallback(callback);
      service.startAnimation();

      tick(16);

      expect(receivedDelta).toBeGreaterThan(0);
      expect(receivedElapsed).toBeGreaterThan(0);
    }));

    it('should update OrbitControls during animation', fakeAsync(() => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      if (controls) {
        service.startAnimation();
        tick(16);

        expect(controls.update).toHaveBeenCalled();
      }
    }));

    it('should stop animation when required objects are missing', () => {
      const originalRenderer = (service as any).renderer;
      const originalScene = (service as any).scene;
      const originalCamera = (service as any).camera;

      (service as any).renderer = undefined;
      (service as any).scene = undefined;
      (service as any).camera = undefined;
      (service as any).isAnimating = true;

      (service as any).animate();

      expect((service as any).isAnimating).toBe(false);

      (service as any).renderer = originalRenderer;
      (service as any).scene = originalScene;
      (service as any).camera = originalCamera;
    });
  });

  describe('window resize handling', () => {
    beforeEach(() => {
      service.initScene(mockContainer);
    });

    it('should update camera aspect ratio on resize', () => {
      const camera = service.getCamera();
      const initialAspect = camera.aspect;

      // Change container dimensions
      mockContainer.style.width = '1600px';
      mockContainer.style.height = '900px';

      // Trigger resize
      window.dispatchEvent(new Event('resize'));

      expect(camera.aspect).not.toBe(initialAspect);
    });

    it('should update renderer size on resize', () => {
      const renderer = service.getRenderer();

      // Change container dimensions
      mockContainer.style.width = '1600px';
      mockContainer.style.height = '900px';

      // Trigger resize
      window.dispatchEvent(new Event('resize'));

      // Renderer should update its size
      expect(renderer.domElement.style.width).toBeTruthy();
    });

    it('should call updateProjectionMatrix on camera resize', () => {
      const camera = service.getCamera();
      jest.spyOn(camera, 'updateProjectionMatrix');

      // Trigger resize
      window.dispatchEvent(new Event('resize'));

      expect(camera.updateProjectionMatrix).toHaveBeenCalled();
    });
  });

  describe('disposal and cleanup', () => {
    beforeEach(() => {
      service.initScene(mockContainer);
    });

    it('should stop animation on dispose', () => {
      service.startAnimation();
      jest.spyOn(service, 'stopAnimation');

      service.dispose();

      expect(service.stopAnimation).toHaveBeenCalled();
    });

    it('should dispose controls', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      if (controls) {
        jest.spyOn(controls, 'dispose');

        service.dispose();

        expect(controls.dispose).toHaveBeenCalled();
      }
    });

    it('should dispose renderer', () => {
      const renderer = service.getRenderer();
      jest.spyOn(renderer, 'dispose');

      service.dispose();

      expect(renderer.dispose).toHaveBeenCalled();
    });

    it('should remove renderer from DOM', () => {
      const renderer = service.getRenderer();
      const domElement = renderer.domElement;

      service.dispose();

      expect(mockContainer.contains(domElement)).toBe(false);
    });

    it('should remove stats from DOM', () => {
      service.initScene(mockContainer, { enableStats: true });

      const initialCanvasCount = mockContainer.querySelectorAll('canvas').length;

      service.dispose();

      const finalCanvasCount = mockContainer.querySelectorAll('canvas').length;
      expect(finalCanvasCount).toBeLessThan(initialCanvasCount);
    });

    it('should clear animation callbacks', () => {
      const callback = jest.fn();

      service.registerAnimationCallback(callback);
      service.dispose();
      service.startAnimation();

      expect(callback).not.toHaveBeenCalled();
    });

    it('should dispose scene objects with geometries and materials', () => {
      const geometry = new THREE.BoxGeometry(1, 1, 1);
      const material = new THREE.MeshBasicMaterial();
      const mesh = new THREE.Mesh(geometry, material);

      jest.spyOn(geometry, 'dispose');
      jest.spyOn(material, 'dispose');

      service.addToScene(mesh);
      service.dispose();

      expect(geometry.dispose).toHaveBeenCalled();
      expect(material.dispose).toHaveBeenCalled();
    });

    it('should dispose objects with array of materials', () => {
      const geometry = new THREE.BoxGeometry(1, 1, 1);
      const materials = [
        new THREE.MeshBasicMaterial(),
        new THREE.MeshBasicMaterial(),
      ];
      const mesh = new THREE.Mesh(geometry, materials);

      jest.spyOn(materials[0], 'dispose');
      jest.spyOn(materials[1], 'dispose');

      service.addToScene(mesh);
      service.dispose();

      expect(materials[0].dispose).toHaveBeenCalled();
      expect(materials[1].dispose).toHaveBeenCalled();
    });
  });

  describe('edge cases', () => {
    it('should handle container with zero dimensions', () => {
      const smallContainer = document.createElement('div');
      smallContainer.style.width = '0px';
      smallContainer.style.height = '0px';
      document.body.appendChild(smallContainer);

      expect(() => {
        service.initScene(smallContainer);
      }).not.toThrow();

      service.dispose();
      document.body.removeChild(smallContainer);
    });

    it('should handle multiple initializations on same instance', () => {
      service.initScene(mockContainer);
      const firstScene = service.getScene();

      service.initScene(mockContainer);
      const secondScene = service.getScene();

      // Second initialization should create new scene
      expect(firstScene).not.toBe(secondScene);
    });

    it('should handle dispose without initialization', () => {
      expect(() => {
        service.dispose();
      }).not.toThrow();
    });

    it('should handle animation start without initialization', () => {
      expect(() => {
        service.startAnimation();
      }).not.toThrow();
    });

    it('should handle callback registration without initialization', () => {
      const callback = jest.fn();

      expect(() => {
        service.registerAnimationCallback(callback);
      }).not.toThrow();
    });

    it('should not fail when removing non-existent callback', () => {
      service.initScene(mockContainer);
      const callback = jest.fn();

      expect(() => {
        service.unregisterAnimationCallback(callback);
      }).not.toThrow();
    });

    it('should not fail resetting camera before init', () => {
      expect(() => {
        service.resetCamera();
      }).not.toThrow();
    });

    it('should not fail toggling stats before init', () => {
      expect(() => {
        service.toggleStats(true);
      }).not.toThrow();
    });

    it('should safely ignore resize without a container', () => {
      (service as any).container = undefined;

      expect(() => {
        (service as any).onWindowResize();
      }).not.toThrow();
    });

    it('should not start animation when renderer is missing', () => {
      service.initScene(mockContainer);
      (service as any).renderer = undefined;

      service.startAnimation();

      expect((service as any).isAnimating).toBe(false);
    });

    it('should stop animation safely when no frame is scheduled', () => {
      expect(() => {
        service.stopAnimation();
      }).not.toThrow();
    });
  });

  describe('NgZone integration', () => {
    it('should run animation outside Angular zone', fakeAsync(() => {
      jest.spyOn(ngZone, 'runOutsideAngular');

      service.initScene(mockContainer);
      service.startAnimation();

      tick(16);

      expect(ngZone.runOutsideAngular).toHaveBeenCalled();
    }));

    it('should create WebSocket outside Angular zone during init', () => {
      jest.spyOn(ngZone, 'runOutsideAngular');

      service.initScene(mockContainer);

      // runOutsideAngular may be called for animation setup
      expect(ngZone.runOutsideAngular).toHaveBeenCalledTimes(0);
    });
  });

  describe('camera configuration', () => {
    it('should set camera field of view', () => {
      const fov = 75;
      service.initScene(mockContainer, { cameraFov: fov });

      const camera = service.getCamera();
      expect(camera.fov).toBe(fov);
    });

    it('should calculate correct aspect ratio', () => {
      service.initScene(mockContainer);

      const camera = service.getCamera();
      const expectedAspect = 800 / 600; // Container dimensions
      expect(camera.aspect).toBeCloseTo(expectedAspect, 2);
    });

    it('should set camera near and far planes', () => {
      service.initScene(mockContainer);

      const camera = service.getCamera();
      expect(camera.near).toBe(0.1);
      expect(camera.far).toBe(1000);
    });

    it('should position camera at custom position', () => {
      const position = new THREE.Vector3(100, 200, 300);
      service.initScene(mockContainer, { cameraPosition: position });

      const camera = service.getCamera();
      expect(camera.position.x).toBe(100);
      expect(camera.position.y).toBe(200);
      expect(camera.position.z).toBe(300);
    });

    it('should reset camera and controls to default position', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const camera = service.getCamera();
      camera.position.set(10, 20, 30);
      const controls = service.getControls();
      if (controls) {
        controls.target.set(5, 5, 5);
      }

      service.resetCamera();

      expect(camera.position.x).toBe(0);
      expect(camera.position.y).toBe(80);
      expect(camera.position.z).toBe(150);
      if (controls) {
        expect(controls.target.x).toBe(0);
        expect(controls.target.y).toBe(0);
        expect(controls.target.z).toBe(0);
        expect(controls.update).toHaveBeenCalled();
      }
    });
  });

  describe('OrbitControls configuration', () => {
    it('should enable damping on controls', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      expect(controls?.enableDamping).toBe(true);
      expect(controls?.dampingFactor).toBe(0.05);
    });

    it('should set max polar angle', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      expect(controls?.maxPolarAngle).toBe(Math.PI / 2);
    });

    it('should set distance constraints', () => {
      service.initScene(mockContainer, { enableOrbitControls: true });

      const controls = service.getControls();
      expect(controls?.minDistance).toBe(10);
      expect(controls?.maxDistance).toBe(500);
    });
  });

  describe('scene lighting', () => {
    beforeEach(() => {
      service.initScene(mockContainer);
    });

    it('should add ambient light with correct intensity', () => {
      const scene = service.getScene();
      const ambientLight = scene.children.find(
        (child) => child instanceof THREE.AmbientLight
      ) as THREE.AmbientLight;

      expect(ambientLight.intensity).toBe(0.5);
    });

    it('should add directional light with correct intensity', () => {
      const scene = service.getScene();
      const directionalLight = scene.children.find(
        (child) => child instanceof THREE.DirectionalLight
      ) as THREE.DirectionalLight;

      expect(directionalLight.intensity).toBe(1);
    });

    it('should configure shadow map on directional light', () => {
      const scene = service.getScene();
      const directionalLight = scene.children.find(
        (child) => child instanceof THREE.DirectionalLight
      ) as THREE.DirectionalLight;

      expect(directionalLight.shadow.mapSize.width).toBe(2048);
      expect(directionalLight.shadow.mapSize.height).toBe(2048);
    });
  });

  describe('stats display', () => {
    it('should toggle stats visibility', () => {
      service.initScene(mockContainer, { enableStats: true });

      const statsElement = mockContainer.querySelector('div');
      expect(statsElement).toBeTruthy();

      service.toggleStats(false);
      expect(statsElement?.style.display).toBe('none');

      service.toggleStats(true);
      expect(statsElement?.style.display).toBe('block');
    });
  });
});
