# Nx Measure Builder Workflow

## Workflow objective

Ensure the system is operational before test execution, then gate release with deterministic Nx/Playwright flows.

## Workflow stages

1. Operational readiness
2. Group A static/unit checks
3. Group B focused Playwright checks
4. Centralized summary
5. Queue repeatability

## Commands by stage

### 1) Readiness
```bash
npm run test:measure-builder:readiness
```

### 2) Group A
```bash
npm run test:measure-builder:group-a
```

### 3) Group B local
```bash
GROUP_B_SERVER_MODE=existing BASE_URL=http://localhost:4210 npm run test:measure-builder:group-b:local
```

### 4) Centralized
```bash
npm run test:measure-builder:centralized:local
```

### 5) Queue to release point
```bash
npm run test:measure-builder:queue:local
```

## Mode selection

- `GROUP_B_SERVER_MODE=existing`: use already-running UI server
- `GROUP_B_SERVER_MODE=managed`: let script manage startup
- `GROUP_B_SERVER_MODE=auto`: detect existing server and choose mode

## Release-go decision

Promote release candidate only when:

- readiness is green,
- Group B is green,
- centralized summary is green,
- queue summary is green twice consecutively.
