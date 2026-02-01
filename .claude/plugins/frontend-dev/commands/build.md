---
name: /frontend-dev:build
description: Build the frontend application with Vite and analyze bundle size
---

# Build Frontend Application

Build the production-optimized frontend bundle with Vite.

## Build Process

### 1. Clean Previous Build

```bash
cd frontend
rm -rf dist
```

### 2. Run Production Build

```bash
npm run build
```

This executes:
1. `tsc -b` - TypeScript compilation check
2. `vite build` - Production bundle creation with:
   - Minification
   - Tree shaking
   - Code splitting
   - Asset optimization

### 3. Analyze Build Output

```bash
# List generated files with sizes
ls -lh dist/assets/*.js | awk '{print $5 "\t" $9}'

# Check total bundle size
du -sh dist
```

### 4. Bundle Size Analysis

**Target Sizes:**
- Initial JS bundle: < 200KB gzipped
- Total JS (all chunks): < 500KB gzipped
- CSS: < 50KB gzipped

**Check if targets met:**
```bash
# Find gzipped sizes (estimate: ~30% of uncompressed)
for file in dist/assets/*.js; do
  size=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file")
  gzipped=$((size * 30 / 100))
  echo "$(basename $file): ${gzipped} bytes (estimated gzipped)"
done
```

### 5. Verify Build

```bash
# Preview production build locally
npm run preview

# Access at http://localhost:4173
```

### 6. Test Production Build (Optional)

```bash
# Run E2E tests against production build
npm run e2e
```

## Common Build Issues

### TypeScript Errors

**Error:**
```
src/components/Example.tsx:10:5 - error TS2322: Type 'string' is not assignable to type 'number'.
```

**Fix:**
```bash
# Run type checking separately
npx tsc --noEmit

# Fix type errors before building
```

### Bundle Size Warnings

**Warning:**
```
(!) Some chunks are larger than 500 kB after minification.
```

**Fix:**
1. Check for large dependencies:
   ```bash
   # Install bundle analyzer
   npm install --save-dev rollup-plugin-visualizer

   # Add to vite.config.ts
   import { visualizer } from 'rollup-plugin-visualizer';

   plugins: [
     react(),
     visualizer({ open: true })
   ]
   ```

2. Implement code splitting:
   ```typescript
   // Use dynamic imports
   const HeavyComponent = lazy(() => import('./HeavyComponent'));
   ```

### Missing Dependencies

**Error:**
```
Could not resolve "./components/Example"
```

**Fix:**
```bash
# Check import paths (case-sensitive on Linux)
# Ensure file exists
ls -la src/components/Example.tsx
```

## Build Optimization

### Vite Configuration

**File: `frontend/vite.config.ts`**
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    // Enable source maps for debugging
    sourcemap: true,

    // Optimize bundle splitting
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-mui': ['@mui/material', '@mui/icons-material'],
          'vendor-charts': ['recharts'],
          'vendor-utils': ['date-fns', 'zustand']
        }
      }
    },

    // Chunk size warnings
    chunkSizeWarningLimit: 500,

    // Minification
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // Remove console.logs in production
        drop_debugger: true
      }
    }
  }
});
```

## Output Example

```
✅ Build successful!

📦 Bundle Analysis:
   main.js:           180 KB (65 KB gzipped) ✅
   vendor-react.js:   140 KB (45 KB gzipped) ✅
   vendor-mui.js:     280 KB (85 KB gzipped) ⚠️
   vendor-charts.js:  150 KB (48 KB gzipped) ✅
   styles.css:        45 KB  (12 KB gzipped) ✅

   Total:            795 KB (255 KB gzipped)
   Status:           ⚠️ MUI bundle large - consider optimization

📊 Build Stats:
   Files:            18
   Assets:           dist/assets/
   Build time:       23.4s

🔍 Recommendations:
   1. MUI bundle is large (85KB gzipped)
      - Consider tree shaking optimization
      - Use named imports: import { Button } from '@mui/material'

   2. Enable compression on server (Gzip/Brotli)

✅ Preview: npm run preview
✅ Deploy: Ready for production
```

## Post-Build Checklist

- [ ] TypeScript compilation successful
- [ ] Bundle sizes within targets
- [ ] No console warnings
- [ ] Source maps generated (for debugging)
- [ ] Assets properly hashed (cache busting)
- [ ] Preview build works locally

## Deployment

After successful build:

```bash
# Build artifacts in dist/
ls -la dist/

# Contents:
# - index.html
# - assets/ (JS, CSS, images)
# - vite.svg, etc.

# Deploy to hosting (examples):
# Netlify: drag dist/ folder or use CLI
# Vercel: vercel --prod
# S3: aws s3 sync dist/ s3://bucket-name
```

## CI/CD Integration

**Example GitHub Actions:**
```yaml
- name: Build frontend
  run: |
    cd frontend
    npm ci
    npm run build

- name: Check bundle size
  run: |
    cd frontend
    # Fail if main bundle > 250KB gzipped
    size=$(stat -c%s dist/assets/main-*.js)
    gzipped=$((size * 30 / 100))
    if [ $gzipped -gt 256000 ]; then
      echo "Bundle too large: ${gzipped} bytes"
      exit 1
    fi
```

---

**When to Run:**
- Before deployment to production
- After significant dependency updates
- When investigating bundle size issues
- As part of CI/CD pipeline
