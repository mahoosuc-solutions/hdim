---
name: frontend-dev:zustand-patterns
description: Zustand state management patterns, best practices, middleware, and optimization techniques for React applications
---

# Zustand State Management Patterns

Complete guide to using Zustand for global state management in the HDIM platform.

## Table of Contents
1. [Basic Store Creation](#basic-store-creation)
2. [Store Patterns](#store-patterns)
3. [Selectors & Performance](#selectors--performance)
4. [Middleware](#middleware)
5. [Async Actions](#async-actions)
6. [Store Slices](#store-slices)
7. [Testing](#testing)

---

## Basic Store Creation

### Simple Store

```typescript
// src/store/counter-store.ts
import { create } from 'zustand';

interface CounterStore {
  count: number;
  increment: () => void;
  decrement: () => void;
  reset: () => void;
}

export const useCounterStore = create<CounterStore>((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 })),
  decrement: () => set((state) => ({ count: state.count - 1 })),
  reset: () => set({ count: 0 }),
}));

// Usage in component
function Counter() {
  const count = useCounterStore((state) => state.count);
  const increment = useCounterStore((state) => state.increment);

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={increment}>Increment</button>
    </div>
  );
}
```

### Store with Complex State

```typescript
// src/store/patient-store.ts
import { create } from 'zustand';

interface Patient {
  id: string;
  name: string;
  age: number;
  status: 'active' | 'inactive';
}

interface PatientStore {
  patients: Patient[];
  selectedPatient: Patient | null;
  loading: boolean;
  error: string | null;

  // Actions
  setPatients: (patients: Patient[]) => void;
  selectPatient: (id: string) => void;
  clearSelection: () => void;
  updatePatient: (id: string, updates: Partial<Patient>) => void;
  deletePatient: (id: string) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const usePatientStore = create<PatientStore>((set, get) => ({
  patients: [],
  selectedPatient: null,
  loading: false,
  error: null,

  setPatients: (patients) => set({ patients }),

  selectPatient: (id) => {
    const patient = get().patients.find((p) => p.id === id);
    set({ selectedPatient: patient || null });
  },

  clearSelection: () => set({ selectedPatient: null }),

  updatePatient: (id, updates) =>
    set((state) => ({
      patients: state.patients.map((patient) =>
        patient.id === id ? { ...patient, ...updates } : patient
      ),
    })),

  deletePatient: (id) =>
    set((state) => ({
      patients: state.patients.filter((patient) => patient.id !== id),
      selectedPatient: state.selectedPatient?.id === id ? null : state.selectedPatient,
    })),

  setLoading: (loading) => set({ loading }),
  setError: (error) => set({ error }),
}));
```

---

## Store Patterns

### Immutable Updates

```typescript
// ✅ Good - Immutable update
set((state) => ({
  patients: [...state.patients, newPatient],
}));

// ✅ Good - Update nested object
set((state) => ({
  user: {
    ...state.user,
    profile: {
      ...state.user.profile,
      name: 'New Name',
    },
  },
}));

// ❌ Bad - Mutating state
set((state) => {
  state.patients.push(newPatient); // ❌ Mutation
  return state;
});
```

### Computed Values (Derived State)

```typescript
interface PatientStore {
  patients: Patient[];

  // Computed getters
  get activePatients(): Patient[] {
    return this.patients.filter((p) => p.status === 'active');
  },

  get patientCount(): number {
    return this.patients.length;
  },
}

export const usePatientStore = create<PatientStore>((set, get) => ({
  patients: [],

  // Expose computed values as getters
  get activePatients() {
    return get().patients.filter((p) => p.status === 'active');
  },

  get patientCount() {
    return get().patients.length;
  },
}));

// Usage - Will recompute on every access
const activePatients = usePatientStore((state) => state.activePatients);

// Better - Use useMemo in component for expensive computations
const activePatients = useMemo(() =>
  patients.filter(p => p.status === 'active'),
  [patients]
);
```

### Reset Pattern

```typescript
const initialState = {
  patients: [],
  selectedPatient: null,
  loading: false,
  error: null,
};

export const usePatientStore = create<PatientStore>((set) => ({
  ...initialState,

  reset: () => set(initialState),

  // Or reset specific slices
  resetError: () => set({ error: null }),
  resetSelection: () => set({ selectedPatient: null }),
}));
```

---

## Selectors & Performance

### ✅ Efficient Selectors

```typescript
// Good - Select only what you need
function PatientName() {
  const name = usePatientStore((state) => state.selectedPatient?.name);
  // Only re-renders when name changes
  return <Typography>{name}</Typography>;
}

// Good - Select multiple fields with shallow compare
import { shallow } from 'zustand/shallow';

function PatientInfo() {
  const { name, age } = usePatientStore(
    (state) => ({
      name: state.selectedPatient?.name,
      age: state.selectedPatient?.age,
    }),
    shallow // Shallow compare the returned object
  );

  return (
    <Box>
      <Typography>{name}</Typography>
      <Typography>{age}</Typography>
    </Box>
  );
}
```

### ❌ Inefficient Selectors

```typescript
// Bad - Returns new object every time (always re-renders)
function PatientInfo() {
  const data = usePatientStore((state) => ({
    name: state.selectedPatient?.name,
    age: state.selectedPatient?.age,
  }));
  // Re-renders on ANY store change because new object !== new object

  return <Box>{data.name}</Box>;
}

// Bad - Selecting entire store
function PatientName() {
  const store = usePatientStore(); // Re-renders on ANY change
  return <Typography>{store.selectedPatient?.name}</Typography>;
}
```

### useShallow Hook (Zustand v5+)

```typescript
import { useShallow } from 'zustand/react/shallow';

function PatientInfo() {
  const { name, age } = usePatientStore(
    useShallow((state) => ({
      name: state.selectedPatient?.name,
      age: state.selectedPatient?.age,
    }))
  );

  return (
    <Box>
      <Typography>{name}</Typography>
      <Typography>{age}</Typography>
    </Box>
  );
}
```

---

## Middleware

### Persist Middleware (localStorage)

```typescript
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export const useAuthStore = create(
  persist<AuthStore>(
    (set) => ({
      token: null,
      user: null,

      login: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null }),
    }),
    {
      name: 'auth-storage', // localStorage key
      storage: createJSONStorage(() => localStorage),
    }
  )
);
```

### Immer Middleware (Easier Immutability)

```typescript
import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';

export const usePatientStore = create(
  immer<PatientStore>((set) => ({
    patients: [],

    addPatient: (patient) =>
      set((state) => {
        state.patients.push(patient); // Direct mutation with Immer
      }),

    updatePatient: (id, updates) =>
      set((state) => {
        const patient = state.patients.find((p) => p.id === id);
        if (patient) {
          Object.assign(patient, updates); // Direct mutation
        }
      }),
  }))
);
```

### DevTools Middleware

```typescript
import { devtools } from 'zustand/middleware';

export const usePatientStore = create(
  devtools<PatientStore>(
    (set) => ({
      patients: [],
      addPatient: (patient) =>
        set(
          (state) => ({ patients: [...state.patients, patient] }),
          false, // Don't replace state
          'patients/add' // Action name in DevTools
        ),
    }),
    { name: 'PatientStore' } // Store name in DevTools
  )
);
```

### Combine Middleware

```typescript
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';

export const usePatientStore = create(
  devtools(
    persist(
      immer<PatientStore>((set) => ({
        patients: [],
        addPatient: (patient) =>
          set((state) => {
            state.patients.push(patient);
          }),
      })),
      { name: 'patients-storage' }
    ),
    { name: 'PatientStore' }
  )
);
```

---

## Async Actions

### Fetch Pattern

```typescript
export const usePatientStore = create<PatientStore>((set, get) => ({
  patients: [],
  loading: false,
  error: null,

  fetchPatients: async () => {
    set({ loading: true, error: null });

    try {
      const response = await fetch('/api/patients');
      if (!response.ok) throw new Error('Failed to fetch');

      const patients = await response.json();
      set({ patients, loading: false });
    } catch (error) {
      set({
        error: error.message,
        loading: false,
      });
    }
  },
}));

// Usage in component
function PatientList() {
  const { patients, loading, error, fetchPatients } = usePatientStore();

  useEffect(() => {
    fetchPatients();
  }, []);

  if (loading) return <CircularProgress />;
  if (error) return <Alert severity="error">{error}</Alert>;

  return (
    <List>
      {patients.map((patient) => (
        <ListItem key={patient.id}>{patient.name}</ListItem>
      ))}
    </List>
  );
}
```

### Optimistic Updates

```typescript
export const usePatientStore = create<PatientStore>((set, get) => ({
  patients: [],

  updatePatient: async (id: string, updates: Partial<Patient>) => {
    // Optimistic update
    const previousPatients = get().patients;
    set((state) => ({
      patients: state.patients.map((p) =>
        p.id === id ? { ...p, ...updates } : p
      ),
    }));

    try {
      const response = await fetch(`/api/patients/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(updates),
      });

      if (!response.ok) throw new Error('Update failed');

      const updated = await response.json();
      set((state) => ({
        patients: state.patients.map((p) => (p.id === id ? updated : p)),
      }));
    } catch (error) {
      // Rollback on error
      set({ patients: previousPatients });
      console.error('Failed to update patient:', error);
    }
  },
}));
```

---

## Store Slices

### Slice Pattern (Modular Stores)

```typescript
// src/store/slices/patient-slice.ts
export interface PatientSlice {
  patients: Patient[];
  selectedPatient: Patient | null;
  setPatients: (patients: Patient[]) => void;
  selectPatient: (id: string) => void;
}

export const createPatientSlice = (set, get): PatientSlice => ({
  patients: [],
  selectedPatient: null,

  setPatients: (patients) => set({ patients }),
  selectPatient: (id) => {
    const patient = get().patients.find((p) => p.id === id);
    set({ selectedPatient: patient || null });
  },
});

// src/store/slices/auth-slice.ts
export interface AuthSlice {
  token: string | null;
  user: User | null;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const createAuthSlice = (set): AuthSlice => ({
  token: null,
  user: null,

  login: (token, user) => set({ token, user }),
  logout: () => set({ token: null, user: null }),
});

// src/store/index.ts - Combine slices
import { create } from 'zustand';
import { createPatientSlice, PatientSlice } from './slices/patient-slice';
import { createAuthSlice, AuthSlice } from './slices/auth-slice';

type AppStore = PatientSlice & AuthSlice;

export const useAppStore = create<AppStore>()((...args) => ({
  ...createPatientSlice(...args),
  ...createAuthSlice(...args),
}));

// Usage
const patients = useAppStore((state) => state.patients);
const user = useAppStore((state) => state.user);
```

---

## Testing

### Test Store

```typescript
// patient-store.test.ts
import { renderHook, act } from '@testing-library/react';
import { usePatientStore } from './patient-store';

describe('PatientStore', () => {
  beforeEach(() => {
    // Reset store before each test
    usePatientStore.setState({
      patients: [],
      selectedPatient: null,
      loading: false,
      error: null,
    });
  });

  it('should add patient', () => {
    const { result } = renderHook(() => usePatientStore());

    act(() => {
      result.current.setPatients([
        { id: '1', name: 'John Doe', age: 45, status: 'active' },
      ]);
    });

    expect(result.current.patients).toHaveLength(1);
    expect(result.current.patients[0].name).toBe('John Doe');
  });

  it('should select patient', () => {
    const { result } = renderHook(() => usePatientStore());

    act(() => {
      result.current.setPatients([
        { id: '1', name: 'John Doe', age: 45, status: 'active' },
        { id: '2', name: 'Jane Smith', age: 32, status: 'active' },
      ]);
    });

    act(() => {
      result.current.selectPatient('2');
    });

    expect(result.current.selectedPatient?.name).toBe('Jane Smith');
  });
});
```

### Mock Store in Tests

```typescript
// Mock store for component tests
vi.mock('./store/patient-store', () => ({
  usePatientStore: vi.fn((selector) =>
    selector({
      patients: [{ id: '1', name: 'Test Patient', age: 45, status: 'active' }],
      selectedPatient: null,
      loading: false,
      error: null,
      setPatients: vi.fn(),
      selectPatient: vi.fn(),
    })
  ),
}));

describe('PatientList Component', () => {
  it('should render patients from store', () => {
    render(<PatientList />);
    expect(screen.getByText('Test Patient')).toBeInTheDocument();
  });
});
```

---

## Best Practices

### ✅ DO

```typescript
// Separate state and actions clearly
interface Store {
  // State
  count: number;

  // Actions
  increment: () => void;
  reset: () => void;
}

// Use selective subscriptions
const count = useStore((state) => state.count);

// Keep actions pure
increment: () => set((state) => ({ count: state.count + 1 }))

// Use TypeScript
interface Store { ... }
export const useStore = create<Store>(...)
```

### ❌ DON'T

```typescript
// Don't select entire store
const store = useStore(); // ❌ Re-renders on any change

// Don't mutate state directly
set((state) => {
  state.count++; // ❌ (unless using Immer middleware)
  return state;
});

// Don't put non-serializable values in persisted store
persist(..., {
  name: 'storage',
  // ❌ Functions, dates, etc. won't serialize correctly
});
```

---

**When to use this skill:**
- Setting up global state management
- Optimizing component re-renders
- Managing async state (loading, error)
- Persisting application state
- Creating modular stores with slices
- Testing Zustand stores
