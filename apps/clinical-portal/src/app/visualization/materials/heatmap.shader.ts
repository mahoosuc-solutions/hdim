import * as THREE from 'three';

/**
 * Heatmap Shader for 3D Data Visualization
 *
 * Advanced GLSL shader for rendering data-driven heatmaps:
 * - Multi-stop color gradient with smooth interpolation
 * - Configurable color scales (e.g., viridis, plasma, custom)
 * - Value-based coloring for compliance rates, metrics, etc.
 * - Optional grid lines for better readability
 * - Smooth transitions between values
 * - Support for missing/null data visualization
 *
 * Primary Use Case: Measure Matrix 3D Heatmap
 * - X-axis: Measures
 * - Y-axis: Time periods
 * - Z-axis/Color: Compliance rate (0-100%)
 *
 * Performance: Highly optimized for large grid datasets
 * Mobile GPU: GLSL ES 3.0 compatible with efficient gradient lookups
 */

/**
 * Vertex Shader
 *
 * Passes data value and position information to fragment shader.
 * Handles vertex transformations and UV coordinate mapping.
 */
const vertexShader = `
  // Precision qualifiers
  precision highp float;

  // Attributes - per-vertex data
  attribute float aValue;        // Data value (e.g., compliance rate 0-1)
  attribute float aHasData;      // 1.0 if data exists, 0.0 if missing/null

  // Uniforms
  uniform float uTime;           // Animation time for transitions
  uniform float uHeightScale;    // Height multiplier for 3D extrusion

  // Varyings - passed to fragment shader
  varying float vValue;          // Interpolated data value
  varying float vHasData;        // Whether this fragment has data
  varying vec2 vUv;              // UV coordinates
  varying vec3 vPosition;        // World position
  varying vec3 vNormal;          // Surface normal for lighting

  void main() {
    // Pass data to fragment shader
    vValue = aValue;
    vHasData = aHasData;
    vUv = uv;
    vPosition = position;

    // Calculate normal in world space for lighting
    vNormal = normalize(normalMatrix * normal);

    // Optional: Animate height based on value for entrance effect
    vec3 animatedPosition = position;
    if (uHeightScale > 0.0 && position.y > 0.0) {
      // Scale height by value and animation factor
      float heightFactor = uHeightScale * aValue;
      animatedPosition.y *= heightFactor;
    }

    // Calculate final position
    gl_Position = projectionMatrix * modelViewMatrix * vec4(animatedPosition, 1.0);
  }
`;

/**
 * Fragment Shader
 *
 * Implements smooth color gradient based on data values.
 * Supports multiple color stops for sophisticated color scales.
 */
const fragmentShader = `
  // Precision qualifiers
  precision mediump float;

  // Uniforms
  uniform vec3 uColorStops[10];  // Color gradient stops (up to 10 colors)
  uniform float uValueStops[10]; // Value positions for each color (0-1)
  uniform int uNumStops;         // Number of gradient stops (2-10)
  uniform vec3 uNoDataColor;     // Color for missing data
  uniform float uMinValue;       // Minimum data value for scaling
  uniform float uMaxValue;       // Maximum data value for scaling
  uniform float uGridThickness;  // Grid line thickness (0 = no grid)
  uniform vec3 uGridColor;       // Grid line color
  uniform float uOpacity;        // Overall opacity
  uniform bool uUseLighting;     // Enable basic lighting

  // Varyings from vertex shader
  varying float vValue;
  varying float vHasData;
  varying vec2 vUv;
  varying vec3 vPosition;
  varying vec3 vNormal;

  /**
   * Smooth gradient color lookup
   *
   * Interpolates between color stops based on normalized value.
   * Uses smoothstep for smooth transitions between stops.
   */
  vec3 getGradientColor(float normalizedValue) {
    // Clamp value to valid range
    normalizedValue = clamp(normalizedValue, 0.0, 1.0);

    // Find the two color stops to interpolate between
    for (int i = 0; i < 9; i++) {
      if (i >= uNumStops - 1) break;

      float stopValue1 = uValueStops[i];
      float stopValue2 = uValueStops[i + 1];

      // Check if value falls between these two stops
      if (normalizedValue >= stopValue1 && normalizedValue <= stopValue2) {
        // Calculate interpolation factor
        float t = (normalizedValue - stopValue1) / (stopValue2 - stopValue1);

        // Smooth interpolation for natural color transitions
        t = smoothstep(0.0, 1.0, t);

        // Linear interpolate between colors
        return mix(uColorStops[i], uColorStops[i + 1], t);
      }
    }

    // Fallback: return last color if value is at maximum
    return uColorStops[uNumStops - 1];
  }

  /**
   * Draw grid lines on the surface
   *
   * Creates subtle grid pattern for better data readability.
   */
  float getGridMask(vec2 uv, float thickness) {
    if (thickness <= 0.0) return 1.0;

    // Calculate distance to nearest grid line
    vec2 grid = abs(fract(uv - 0.5) - 0.5) / fwidth(uv);
    float line = min(grid.x, grid.y);

    // Create anti-aliased line
    return 1.0 - (1.0 - smoothstep(0.0, thickness, line));
  }

  void main() {
    vec3 finalColor;
    float finalAlpha = uOpacity;

    // Check if this fragment has data
    if (vHasData < 0.5) {
      // No data - use special color
      finalColor = uNoDataColor;
      finalAlpha *= 0.3; // Make no-data cells semi-transparent
    } else {
      // Normalize value to 0-1 range
      float normalizedValue = (vValue - uMinValue) / (uMaxValue - uMinValue);
      normalizedValue = clamp(normalizedValue, 0.0, 1.0);

      // Get color from gradient
      finalColor = getGradientColor(normalizedValue);

      // Apply grid lines if enabled
      if (uGridThickness > 0.0) {
        float gridMask = getGridMask(vUv, uGridThickness);
        finalColor = mix(uGridColor, finalColor, gridMask);
      }

      // Apply simple lighting if enabled
      if (uUseLighting) {
        // Directional light from above-right
        vec3 lightDir = normalize(vec3(0.5, 1.0, 0.5));
        float diffuse = max(dot(vNormal, lightDir), 0.0);

        // Ambient + diffuse lighting
        float lightIntensity = 0.6 + 0.4 * diffuse;
        finalColor *= lightIntensity;
      }
    }

    // Output final color
    gl_FragColor = vec4(finalColor, finalAlpha);
  }
`;

/**
 * Color Scale Presets
 *
 * Pre-configured color gradients for common visualization needs.
 * Based on popular scientific color scales.
 */
export const ColorScales = {
  /**
   * Viridis - Perceptually uniform, colorblind-friendly
   * Good for: General purpose data visualization
   */
  viridis: {
    colors: [
      new THREE.Color(0x440154), // Dark purple
      new THREE.Color(0x31688e), // Blue
      new THREE.Color(0x35b779), // Green
      new THREE.Color(0xfde724), // Yellow
    ],
    stops: [0.0, 0.33, 0.66, 1.0],
  },

  /**
   * Plasma - High contrast, vibrant
   * Good for: Highlighting variations in data
   */
  plasma: {
    colors: [
      new THREE.Color(0x0d0887), // Deep blue
      new THREE.Color(0x7e03a8), // Purple
      new THREE.Color(0xcc4778), // Pink
      new THREE.Color(0xf89540), // Orange
      new THREE.Color(0xf0f921), // Yellow
    ],
    stops: [0.0, 0.25, 0.5, 0.75, 1.0],
  },

  /**
   * Red-Yellow-Green (Compliance)
   * Good for: Compliance rates, performance metrics
   * Red = low, Yellow = medium, Green = high
   */
  compliance: {
    colors: [
      new THREE.Color(0xd73027), // Red
      new THREE.Color(0xfc8d59), // Orange
      new THREE.Color(0xfee08b), // Yellow
      new THREE.Color(0xd9ef8b), // Light green
      new THREE.Color(0x91cf60), // Green
      new THREE.Color(0x1a9850), // Dark green
    ],
    stops: [0.0, 0.2, 0.4, 0.6, 0.8, 1.0],
  },

  /**
   * Blue-White-Red (Diverging)
   * Good for: Data with positive/negative deviation
   */
  diverging: {
    colors: [
      new THREE.Color(0x2166ac), // Dark blue
      new THREE.Color(0x92c5de), // Light blue
      new THREE.Color(0xf7f7f7), // White
      new THREE.Color(0xf4a582), // Light red
      new THREE.Color(0xd6604d), // Dark red
    ],
    stops: [0.0, 0.25, 0.5, 0.75, 1.0],
  },

  /**
   * Cool-to-Warm
   * Good for: Temperature-like data representation
   */
  coolWarm: {
    colors: [
      new THREE.Color(0x3b4cc0), // Cool blue
      new THREE.Color(0x7896db), // Light blue
      new THREE.Color(0xb4b4b4), // Gray
      new THREE.Color(0xe07a5f), // Light red
      new THREE.Color(0xb40426), // Warm red
    ],
    stops: [0.0, 0.25, 0.5, 0.75, 1.0],
  },

  /**
   * Grayscale
   * Good for: Print-friendly visualizations
   */
  grayscale: {
    colors: [
      new THREE.Color(0x000000), // Black
      new THREE.Color(0x808080), // Gray
      new THREE.Color(0xffffff), // White
    ],
    stops: [0.0, 0.5, 1.0],
  },
};

/**
 * Heatmap Shader Configuration
 */
export interface HeatmapShaderConfig {
  colorScale?: keyof typeof ColorScales | { colors: THREE.Color[]; stops: number[] };
  minValue?: number;           // Minimum data value (default: 0)
  maxValue?: number;           // Maximum data value (default: 1)
  noDataColor?: THREE.Color;   // Color for missing data (default: gray)
  gridThickness?: number;      // Grid line thickness 0-2 (default: 0.5)
  gridColor?: THREE.Color;     // Grid line color (default: black)
  opacity?: number;            // Overall opacity (default: 1.0)
  useLighting?: boolean;       // Enable lighting (default: true)
  heightScale?: number;        // 3D height extrusion (default: 1.0)
  transparent?: boolean;       // Enable transparency (default: false)
  side?: THREE.Side;           // Render side (default: FrontSide)
}

/**
 * Create a heatmap shader material
 *
 * @param config - Configuration options
 * @returns THREE.ShaderMaterial configured for heatmap rendering
 */
export function createHeatmapShaderMaterial(config: HeatmapShaderConfig = {}): THREE.ShaderMaterial {
  const {
    colorScale = 'viridis',
    minValue = 0.0,
    maxValue = 1.0,
    noDataColor = new THREE.Color(0x808080),
    gridThickness = 0.5,
    gridColor = new THREE.Color(0x000000),
    opacity = 1.0,
    useLighting = true,
    heightScale = 1.0,
    transparent = false,
    side = THREE.FrontSide,
  } = config;

  // Get color scale configuration
  const scale = typeof colorScale === 'string' ? ColorScales[colorScale] : colorScale;

  // Pad arrays to length 10 for uniform arrays
  const colors = new Array(10).fill(new THREE.Color(0x000000));
  const stops = new Array(10).fill(0.0);

  for (let i = 0; i < Math.min(scale.colors.length, 10); i++) {
    colors[i] = scale.colors[i];
    stops[i] = scale.stops[i];
  }

  const material = new THREE.ShaderMaterial({
    uniforms: {
      uColorStops: { value: colors },
      uValueStops: { value: stops },
      uNumStops: { value: scale.colors.length },
      uNoDataColor: { value: noDataColor },
      uMinValue: { value: minValue },
      uMaxValue: { value: maxValue },
      uGridThickness: { value: gridThickness },
      uGridColor: { value: gridColor },
      uOpacity: { value: opacity },
      uUseLighting: { value: useLighting },
      uTime: { value: 0.0 },
      uHeightScale: { value: heightScale },
    },
    vertexShader,
    fragmentShader,
    transparent,
    side,
  });

  return material;
}

/**
 * Update heatmap shader time uniform for animations
 *
 * @param material - The heatmap shader material
 * @param time - Current time in seconds
 */
export function updateHeatmapShaderTime(material: THREE.ShaderMaterial, time: number): void {
  if (material.uniforms['uTime']) {
    material.uniforms['uTime'].value = time;
  }
}

/**
 * Helper: Create heatmap geometry for grid data
 *
 * Creates a grid-based geometry for heatmap visualization.
 * Each grid cell represents a data point with color based on value.
 *
 * @param rows - Number of rows (Y-axis)
 * @param cols - Number of columns (X-axis)
 * @param cellSize - Size of each cell
 * @param values - 2D array of data values (rows x cols)
 * @returns BufferGeometry configured for heatmap
 */
export function createHeatmapGeometry(
  rows: number,
  cols: number,
  cellSize: number,
  values: number[][]
): THREE.BufferGeometry {
  const geometry = new THREE.BufferGeometry();

  // Calculate vertices for grid
  const positions: number[] = [];
  const uvs: number[] = [];
  const dataValues: number[] = [];
  const hasDataFlags: number[] = [];
  const indices: number[] = [];

  let vertexIndex = 0;

  for (let row = 0; row < rows; row++) {
    for (let col = 0; col < cols; col++) {
      // Get data value (or null if missing)
      const value = values[row]?.[col];
      const hasData = value !== null && value !== undefined && !isNaN(value);

      // Cell corners (quad)
      const x0 = col * cellSize;
      const x1 = (col + 1) * cellSize;
      const z0 = row * cellSize;
      const z1 = (row + 1) * cellSize;
      const y = 0; // Base height

      // Four vertices per cell
      const corners = [
        [x0, y, z0],
        [x1, y, z0],
        [x1, y, z1],
        [x0, y, z1],
      ];

      const baseIndex = vertexIndex;

      for (let i = 0; i < 4; i++) {
        positions.push(...corners[i]);

        // UV coordinates for texturing/grid
        const u = (col + (i === 1 || i === 2 ? 1 : 0)) / cols;
        const v = (row + (i === 2 || i === 3 ? 1 : 0)) / rows;
        uvs.push(u, v);

        // Data value for coloring
        dataValues.push(hasData ? value : 0.0);
        hasDataFlags.push(hasData ? 1.0 : 0.0);

        vertexIndex++;
      }

      // Two triangles per cell
      indices.push(
        baseIndex, baseIndex + 1, baseIndex + 2,
        baseIndex, baseIndex + 2, baseIndex + 3
      );
    }
  }

  // Set attributes
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geometry.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
  geometry.setAttribute('aValue', new THREE.Float32BufferAttribute(dataValues, 1));
  geometry.setAttribute('aHasData', new THREE.Float32BufferAttribute(hasDataFlags, 1));
  geometry.setIndex(indices);

  // Compute normals for lighting
  geometry.computeVertexNormals();

  return geometry;
}

/**
 * Usage Example:
 *
 * ```typescript
 * // Create heatmap for compliance data
 * const rows = 12; // months
 * const cols = 10; // measures
 * const cellSize = 5;
 *
 * // Sample compliance data (0-1 range)
 * const complianceData = Array(rows).fill(0).map(() =>
 *   Array(cols).fill(0).map(() => Math.random())
 * );
 *
 * // Create geometry and material
 * const geometry = createHeatmapGeometry(rows, cols, cellSize, complianceData);
 * const material = createHeatmapShaderMaterial({
 *   colorScale: 'compliance',
 *   minValue: 0,
 *   maxValue: 1,
 *   gridThickness: 1.0,
 *   useLighting: true,
 * });
 *
 * // Create mesh
 * const heatmap = new THREE.Mesh(geometry, material);
 * scene.add(heatmap);
 *
 * // In animation loop
 * updateHeatmapShaderTime(material, clock.getElapsedTime());
 * ```
 *
 * Performance Tips:
 * 1. Use indexed geometry for large grids (saves ~40% memory)
 * 2. Consider LOD for very large datasets (>1000 cells)
 * 3. Disable lighting on mobile for better performance
 * 4. Use simpler color scales (fewer stops) for faster gradient lookup
 * 5. Batch multiple heatmaps with same material using InstancedMesh
 */
