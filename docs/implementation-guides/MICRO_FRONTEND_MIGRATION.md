# Micro Frontend Migration

This document tracks the migration of the Health Data Platform from a monolithic Angular application to a micro frontend architecture using Nx Module Federation.

## Architecture Overview

### Current State (Monolithic)
- **Bundle Size**: 1.5MB (exceeding 800KB warning threshold)
- **Services**: 100+ services in a single app
- **Pages**: 29 feature pages
- **Deployment**: Single Docker container with Nginx

### Target State (Micro Frontends)
- **Shell App**: Core layout, auth, tenant context, routing orchestration
- **Patient Management MFE**: Patient list, patient chart
- **Quality Measures MFE**: Measures, evaluations, results
- **Care Gaps MFE**: Care gap management
- **Reports MFE**: Analytics and reporting
- **Admin MFE**: Settings and user management

## Shared Libraries

### @health-platform/shared/data-access
**Purpose**: Core API communication, interceptors, and data access patterns

**Exports**:
- `tenantInterceptor`: Adds X-Tenant-ID header to backend requests
- `authInterceptor`: Handles authentication (HttpOnly cookies)
- `ApiConfigService`: Manages API endpoints and tenant configuration

**Location**: `libs/shared/data-access`

### @health-platform/shared/ui-common
**Purpose**: Reusable UI components across all MFEs

**Planned Components**:
- Buttons, modals, dialogs
- Data tables and grids
- Form controls and validators
- Loading spinners and skeletons

**Location**: `libs/shared/ui-common`

### @health-platform/shared/util-auth
**Purpose**: Authentication utilities and guards

**Planned Exports**:
- `AuthGuard`: Route protection
- `AuthService`: Login, logout, token management
- `User` models and interfaces

**Location**: `libs/shared/util-auth`

### @health-platform/shared/feature-shell
**Purpose**: Shell application layout and navigation

**Planned Components**:
- App shell layout
- Top navigation bar
- Side navigation menu
- Footer

**Location**: `libs/shared/feature-shell`

## Migration Phases

### ✅ Phase 1: Foundation & Shared Libraries (COMPLETED)
- [x] Create shared library structure with Nx
- [x] Extract tenant interceptor to shared/data-access
- [x] Extract auth interceptor to shared/data-access
- [x] Create ApiConfigService for centralized config
- [x] Extract AuthService to shared/util-auth
- [x] Create auth guards (authGuard, roleGuard, permissionGuard)
- [x] Set up buildable libraries with Jest testing
- [x] Configure tsconfig path mappings

### ✅ Phase 2: Module Federation Setup (COMPLETED)
- [x] Install `@nx/angular:module-federation` plugin
- [x] Create shell-app as Module Federation host
- [x] Configure webpack Module Federation
- [x] Create mfe-patients as first remote
- [x] Set up routing federation
- [x] Create shell layout component with navigation
- [x] Create home page component
- [x] Configure HTTP interceptors in shell
- [x] Test successful builds for both apps

### 📋 Phase 3: State Management Federation
- [x] Create shared global state library (`@health-platform/shared/state`)
- [x] Provide shared NgRx bootstrap in shell-app
- [x] Connect AuthService to NgRx effects for session sync
- [ ] Refactor existing NgRx features to federated pattern
- [ ] Implement lazy-loaded feature stores for MFEs
- [ ] Establish inter-MFE event bus

### 📋 Phase 4: Migrate Core MFEs
- [ ] Create mfe-quality (measures, evaluations)
- [ ] Create mfe-care-gaps
- [ ] Create mfe-reports
- [ ] Migrate routes from clinical-portal

### 📋 Phase 5: Build & Deployment Pipeline
- [ ] Update CI/CD for independent MFE builds
- [ ] Configure Nx affected commands
- [ ] Create Dockerfiles for each MFE
- [ ] Set up Kubernetes manifests

### 📋 Phase 6: Testing & Observability
- [ ] Refactor E2E tests for MFE integration
- [ ] Add contract tests for MFE APIs
- [ ] Instrument performance monitoring
- [ ] Set up error boundaries

## Benefits

1. **Team Autonomy**: Teams can work on different MFEs independently
2. **Faster Builds**: Only affected MFEs rebuild on changes
3. **Independent Deployment**: Deploy features without full app deployment
4. **Bundle Optimization**: Lazy load MFEs on demand
5. **Technology Flexibility**: Upgrade dependencies per MFE
6. **Scalability**: Scale individual MFEs based on traffic

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Service duplication across MFEs | High | Centralize in shared libraries |
| State synchronization complexity | High | Use NgRx federated stores + event bus |
| Version skew between MFEs | Medium | Single shared Angular version initially |
| Bundle size overhead (Module Federation) | Medium | Careful dependency management |
| Developer experience complexity | Medium | Good documentation + local dev tooling |

## Local Development

### Running Shared Libraries
```bash
# Build all shared libraries
nx run-many --target=build --projects=data-access,ui-common,util-auth,feature-shell

# Watch mode for development
nx build data-access --watch
```

### Testing
```bash
# Test all shared libraries
nx run-many --target=test --projects=data-access,ui-common,util-auth,feature-shell

# Test specific library
nx test data-access
```

## Architecture Decision Records

### ADR-001: Use Nx Module Federation over npm packages
**Decision**: Use Nx Module Federation instead of publishing shared code as npm packages

**Rationale**:
- Real-time updates in monorepo without publishing
- Better DX with hot reload across MFEs
- Easier dependency management
- No version coordination overhead

**Trade-offs**: Requires running module federation dev server

### ADR-002: HttpOnly cookies for authentication
**Decision**: Continue using HttpOnly cookies for JWT storage

**Rationale**:
- XSS protection (already implemented)
- Works seamlessly across MFEs (same domain)
- HIPAA compliance maintained

**Trade-offs**: Must be same-origin deployment

### ADR-003: Centralized tenant context
**Decision**: Manage tenant ID in ApiConfigService (shared library)

**Rationale**:
- Single source of truth across MFEs
- Interceptor can inject tenant header consistently
- Easy to swap tenant in development

**Trade-offs**: All MFEs must import shared/data-access

## Next Steps

1. ✅ Complete shared library foundation
2. Extract AuthService from clinical-portal
3. Set up Module Federation for shell-app + mfe-patients
4. Test end-to-end routing between shell and first remote

## Resources

- [Nx Module Federation Documentation](https://nx.dev/recipes/module-federation)
- [Angular Module Federation Guide](https://www.angulararchitects.io/en/blog/module-federation-with-angular/)
- [Micro Frontends Best Practices](https://martinfowler.com/articles/micro-frontends.html)
