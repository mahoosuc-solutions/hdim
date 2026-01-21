# Quick Start Guide - Micro Frontend Architecture

## 🚀 Run the Micro Frontend Architecture

### Option 1: Run Both Apps (Recommended for Development)

Open two terminal windows:

**Terminal 1 - Shell App (Host)**:
```bash
cd /home/webemo-aaron/projects/hdim-master
npx nx serve shell-app
```

**Terminal 2 - Patient MFE (Remote)**:
```bash
cd /home/webemo-aaron/projects/hdim-master
npx nx serve mfePatients
```

Then open: **http://localhost:4200**

### Option 2: Run with Nx CLI (Parallel)

```bash
cd /home/webemo-aaron/projects/hdim-master
npx nx run-many --target=serve --projects=shell-app,mfePatients --parallel=2
```

### Option 3: Production Build & Serve

```bash
cd /home/webemo-aaron/projects/hdim-master

# Build both
npx nx run-many --target=build --projects=shell-app,mfePatients

# Serve builds (requires http-server)
npx http-server dist/apps/shell-app/browser -p 4200
```

## 📱 What You'll See

### Home Page (/)
- Welcome hero section
- Feature cards:
  - 🏥 Patient Management (active link to MFE)
  - 📊 Analytics (coming soon)
  - 🔒 Security (coming soon)
- Architecture overview

### Patient MFE (/mfePatients)
- Nx Welcome component (placeholder)
- Demonstrates successful Module Federation loading
- Will be replaced with actual patient management UI in Phase 3

## 🔍 Testing Module Federation

### 1. Verify Remote Loading
Open browser DevTools → Network tab:
- Navigate to http://localhost:4200
- Click "Access Patients Module"
- You should see `remoteEntry.mjs` being loaded dynamically

### 2. Verify Shared Dependencies
Check the console for Module Federation logs (in development mode):
- Shared Angular modules loaded once
- Remote chunks loaded on demand

### 3. Verify Navigation
- Click between "Home" and "Patients" links
- Notice instant navigation (no page reload)
- URL updates correctly

## 🧪 Run Tests

### Unit Tests (Shared Libraries)
```bash
# Test all libraries
npx nx run-many --target=test --projects=data-access,util-auth,ui-common,feature-shell

# Test specific library
npx nx test data-access
npx nx test util-auth
```

### E2E Tests
```bash
# Shell app E2E
npx nx e2e shell-app-e2e

# Patient MFE E2E
npx nx e2e mfePatients-e2e
```

## 🏗️ Build for Production

```bash
# Build all
npx nx run-many --target=build --projects=shell-app,mfePatients

# Build affected only
npx nx affected --target=build

# Check output
ls -lh dist/apps/shell-app/browser
ls -lh dist/apps/mfe-patients/browser
```

## 📊 Nx Project Graph

Visualize the dependency graph:

```bash
npx nx graph
```

This will open an interactive graph showing:
- Shell app depends on mfePatients (Module Federation)
- Both apps depend on shared libraries
- Nx task execution order

## 🔧 Troubleshooting

### Port Already in Use
```bash
# Find and kill process on port 4200
lsof -ti:4200 | xargs kill -9

# Or use different port
npx nx serve shell-app --port=4300
```

### Module Federation Not Loading Remote
1. Ensure mfe-patients is running on port 4201
2. Check browser console for CORS errors
3. Verify module-federation.config.ts has correct remote URL

### Build Errors
```bash
# Clean and rebuild
rm -rf dist .nx
npx nx reset
npx nx build shell-app
```

## 📝 Development Workflow

### Making Changes to Shared Libraries

1. **Edit shared library** (e.g., `libs/shared/data-access`)
2. **Changes auto-rebuild** (watch mode)
3. **Apps auto-reload** (HMR)

Example:
```bash
# Terminal 1: Watch shared library
npx nx build data-access --watch

# Terminal 2: Serve shell
npx nx serve shell-app

# Terminal 3: Serve patient MFE
npx nx serve mfePatients
```

### Adding New Routes to Patient MFE

Edit: `apps/mfe-patients/src/app/remote-entry/entry.routes.ts`

```typescript
import { Route } from '@angular/router';

export const remoteRoutes: Route[] = [
  {
    path: '',
    loadComponent: () => import('./entry').then((m) => m.RemoteEntry),
  },
  {
    path: 'list',
    loadComponent: () => import('../pages/patient-list.page').then((m) => m.PatientListPage),
  },
  {
    path: ':id',
    loadComponent: () => import('../pages/patient-detail.page').then((m) => m.PatientDetailPage),
  },
];
```

Shell automatically loads these routes under `/mfePatients/*`.

## 🎯 Next Steps

1. **Test local setup**: Run both apps and verify Module Federation
2. **Review architecture**: Check MICRO_FRONTEND_PHASE_1_2_COMPLETE.md
3. **Plan Phase 3**: State management and patient UI migration
4. **Set up CI/CD**: Update pipelines for multi-app builds

## 📚 Documentation

- [MICRO_FRONTEND_MIGRATION.md](./MICRO_FRONTEND_MIGRATION.md) - Full migration plan
- [MICRO_FRONTEND_PHASE_1_2_COMPLETE.md](./MICRO_FRONTEND_PHASE_1_2_COMPLETE.md) - Completion summary
- [AGENTS.md](./AGENTS.md) - Repository guidelines

## 💡 Tips

- Use `nx affected` commands to only build/test changed projects
- Enable Nx Cloud for distributed caching (optional)
- Keep shared libraries small and focused
- Document breaking changes in shared libraries
- Use semantic versioning for shared libraries if publishing externally

---

**Status**: ✅ Fully functional micro frontend architecture  
**Apps**: Shell (host) + Patient MFE (remote)  
**Shared Libraries**: 4 libraries ready for use  
**Next**: Migrate patient management UI from monolith
