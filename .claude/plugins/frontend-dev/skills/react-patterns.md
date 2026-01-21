---
name: frontend-dev:react-patterns
description: React 19 patterns, hooks best practices, performance optimization, and modern React development techniques
---

# React Patterns & Best Practices

Comprehensive guide to modern React development patterns for HDIM frontend.

## Table of Contents
1. [Component Patterns](#component-patterns)
2. [Hooks Patterns](#hooks-patterns)
3. [Performance Optimization](#performance-optimization)
4. [State Management](#state-management)
5. [Error Handling](#error-handling)
6. [Code Organization](#code-organization)

---

## Component Patterns

### Functional Components (Preferred)

```typescript
// ✅ Good - Functional component with TypeScript
interface PatientCardProps {
  patient: Patient;
  onClick?: (id: string) => void;
}

export const PatientCard: React.FC<PatientCardProps> = ({ patient, onClick }) => {
  const handleClick = () => {
    onClick?.(patient.id);
  };

  return (
    <Card onClick={handleClick}>
      <Typography>{patient.name}</Typography>
    </Card>
  );
};
```

### Compound Components

```typescript
// Pattern for related components with shared context
interface TabsContextValue {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

const TabsContext = createContext<TabsContextValue | null>(null);

export const Tabs: React.FC<{ children: ReactNode; defaultTab: string }> = ({
  children,
  defaultTab
}) => {
  const [activeTab, setActiveTab] = useState(defaultTab);

  return (
    <TabsContext.Provider value={{ activeTab, setActiveTab }}>
      <Box>{children}</Box>
    </TabsContext.Provider>
  );
};

// Sub-components
Tabs.List = ({ children }: { children: ReactNode }) => (
  <Box role="tablist">{children}</Box>
);

Tabs.Tab = ({ value, children }: { value: string; children: ReactNode }) => {
  const context = useContext(TabsContext);
  if (!context) throw new Error('Tab must be used within Tabs');

  const isActive = context.activeTab === value;

  return (
    <Button
      role="tab"
      aria-selected={isActive}
      onClick={() => context.setActiveTab(value)}
    >
      {children}
    </Button>
  );
};

Tabs.Panel = ({ value, children }: { value: string; children: ReactNode }) => {
  const context = useContext(TabsContext);
  if (!context) throw new Error('Panel must be used within Tabs');

  if (context.activeTab !== value) return null;

  return <Box role="tabpanel">{children}</Box>;
};

// Usage
<Tabs defaultTab="details">
  <Tabs.List>
    <Tabs.Tab value="details">Details</Tabs.Tab>
    <Tabs.Tab value="history">History</Tabs.Tab>
  </Tabs.List>

  <Tabs.Panel value="details">
    <PatientDetails />
  </Tabs.Panel>

  <Tabs.Panel value="history">
    <PatientHistory />
  </Tabs.Panel>
</Tabs>
```

### Render Props Pattern

```typescript
// Pattern for sharing logic with flexible rendering
interface DataFetcherProps<T> {
  url: string;
  render: (data: T | null, loading: boolean, error: Error | null) => ReactNode;
}

function DataFetcher<T>({ url, render }: DataFetcherProps<T>) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    fetch(url)
      .then(res => res.json())
      .then(setData)
      .catch(setError)
      .finally(() => setLoading(false));
  }, [url]);

  return <>{render(data, loading, error)}</>;
}

// Usage
<DataFetcher<Patient>
  url="/api/patients/123"
  render={(patient, loading, error) => {
    if (loading) return <Skeleton />;
    if (error) return <Alert severity="error">{error.message}</Alert>;
    return <PatientCard patient={patient} />;
  }}
/>
```

### Higher-Order Components (HOC)

```typescript
// Pattern for enhancing components with additional props/behavior
function withLoading<P extends object>(
  Component: React.ComponentType<P>
): React.FC<P & { loading?: boolean }> {
  return ({ loading, ...props }) => {
    if (loading) {
      return <CircularProgress />;
    }

    return <Component {...(props as P)} />;
  };
}

// Usage
const PatientCardWithLoading = withLoading(PatientCard);
<PatientCardWithLoading patient={patient} loading={isLoading} />
```

---

## Hooks Patterns

### Custom Hooks

```typescript
// Extract reusable logic into custom hooks
function usePatientData(patientId: string) {
  const [data, setData] = useState<Patient | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    const fetchPatient = async () => {
      try {
        setLoading(true);
        const response = await fetch(`/api/patients/${patientId}`);
        const patient = await response.json();

        if (!cancelled) {
          setData(patient);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err as Error);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchPatient();

    return () => {
      cancelled = true; // Cleanup: prevent state updates after unmount
    };
  }, [patientId]);

  return { data, loading, error };
}
```

### useCallback Pattern

```typescript
// Memoize callbacks to prevent unnecessary re-renders
function PatientList({ patients }: { patients: Patient[] }) {
  // ❌ BAD - New function every render
  const handleDelete = (id: string) => {
    deletePatient(id);
  };

  // ✅ GOOD - Memoized callback
  const handleDelete = useCallback((id: string) => {
    deletePatient(id);
  }, []); // Empty deps if deletePatient is stable

  return (
    <>
      {patients.map(patient => (
        <PatientCard
          key={patient.id}
          patient={patient}
          onDelete={handleDelete}
        />
      ))}
    </>
  );
}
```

### useMemo Pattern

```typescript
// Memoize expensive calculations
function PatientStatistics({ patients }: { patients: Patient[] }) {
  // ✅ Memoize expensive computation
  const statistics = useMemo(() => {
    return {
      total: patients.length,
      active: patients.filter(p => p.status === 'active').length,
      averageAge: patients.reduce((sum, p) => sum + p.age, 0) / patients.length
    };
  }, [patients]); // Only recalculate when patients change

  return <StatisticsCard data={statistics} />;
}
```

### useRef Patterns

```typescript
// 1. Accessing DOM elements
function SearchInput() {
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus(); // Focus input on mount
  }, []);

  return <input ref={inputRef} />;
}

// 2. Storing mutable values (doesn't trigger re-render)
function WebSocketComponent() {
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    wsRef.current = new WebSocket('ws://localhost:8080');

    return () => {
      wsRef.current?.close();
    };
  }, []);

  const sendMessage = (msg: string) => {
    wsRef.current?.send(msg);
  };

  return <button onClick={() => sendMessage('Hello')}>Send</button>;
}

// 3. Tracking previous values
function usePrevious<T>(value: T): T | undefined {
  const ref = useRef<T>();

  useEffect(() => {
    ref.current = value;
  }, [value]);

  return ref.current;
}
```

### useEffect Patterns

```typescript
// ✅ Data fetching with cleanup
useEffect(() => {
  let cancelled = false;

  async function fetchData() {
    const data = await loadPatients();
    if (!cancelled) {
      setPatients(data);
    }
  }

  fetchData();

  return () => {
    cancelled = true; // Prevent state update after unmount
  };
}, []);

// ✅ Event listeners with cleanup
useEffect(() => {
  const handleResize = () => setWidth(window.innerWidth);

  window.addEventListener('resize', handleResize);

  return () => {
    window.removeEventListener('resize', handleResize);
  };
}, []);

// ✅ Timers with cleanup
useEffect(() => {
  const timer = setInterval(() => {
    setTime(Date.now());
  }, 1000);

  return () => {
    clearInterval(timer);
  };
}, []);
```

### useReducer for Complex State

```typescript
// Pattern for managing complex state logic
type State = {
  patients: Patient[];
  selectedId: string | null;
  loading: boolean;
  error: string | null;
};

type Action =
  | { type: 'LOAD_START' }
  | { type: 'LOAD_SUCCESS'; payload: Patient[] }
  | { type: 'LOAD_ERROR'; payload: string }
  | { type: 'SELECT'; payload: string };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'LOAD_START':
      return { ...state, loading: true, error: null };

    case 'LOAD_SUCCESS':
      return { ...state, loading: false, patients: action.payload };

    case 'LOAD_ERROR':
      return { ...state, loading: false, error: action.payload };

    case 'SELECT':
      return { ...state, selectedId: action.payload };

    default:
      return state;
  }
}

function PatientManager() {
  const [state, dispatch] = useReducer(reducer, {
    patients: [],
    selectedId: null,
    loading: false,
    error: null
  });

  useEffect(() => {
    dispatch({ type: 'LOAD_START' });

    fetchPatients()
      .then(data => dispatch({ type: 'LOAD_SUCCESS', payload: data }))
      .catch(error => dispatch({ type: 'LOAD_ERROR', payload: error.message }));
  }, []);

  return (
    // Component JSX
  );
}
```

---

## Performance Optimization

### React.memo

```typescript
// Prevent unnecessary re-renders of child components
export const PatientCard = React.memo(({ patient }: PatientCardProps) => {
  return <Card>{patient.name}</Card>;
});

// Custom comparison function
export const PatientCard = React.memo(
  ({ patient }: PatientCardProps) => {
    return <Card>{patient.name}</Card>;
  },
  (prevProps, nextProps) => {
    // Return true if props are equal (skip re-render)
    return prevProps.patient.id === nextProps.patient.id;
  }
);
```

### Code Splitting with React.lazy

```typescript
// Lazy load heavy components
const HeavyChart = lazy(() => import('./components/HeavyChart'));
const ReportViewer = lazy(() => import('./components/ReportViewer'));

function Dashboard() {
  return (
    <Suspense fallback={<CircularProgress />}>
      <HeavyChart data={data} />
    </Suspense>
  );
}
```

### Virtualization for Long Lists

```typescript
import { FixedSizeList } from 'react-window';

function PatientList({ patients }: { patients: Patient[] }) {
  return (
    <FixedSizeList
      height={600}
      itemCount={patients.length}
      itemSize={80}
      width="100%"
    >
      {({ index, style }) => (
        <div style={style}>
          <PatientCard patient={patients[index]} />
        </div>
      )}
    </FixedSizeList>
  );
}
```

---

## State Management

### Local State (useState)

```typescript
// Use for component-specific state
function SearchBar() {
  const [query, setQuery] = useState('');

  return (
    <input value={query} onChange={(e) => setQuery(e.target.value)} />
  );
}
```

### Global State (Zustand)

```typescript
// Use for app-wide state
import { create } from 'zustand';

interface AppStore {
  user: User | null;
  setUser: (user: User) => void;
  logout: () => void;
}

export const useAppStore = create<AppStore>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  logout: () => set({ user: null })
}));

// Usage
function UserProfile() {
  const user = useAppStore(state => state.user);
  const logout = useAppStore(state => state.logout);

  return (
    <div>
      <p>{user?.name}</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

---

## Error Handling

### Error Boundaries

```typescript
class ErrorBoundary extends React.Component<
  { children: ReactNode },
  { hasError: boolean; error: Error | null }
> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Alert severity="error">
          <AlertTitle>Something went wrong</AlertTitle>
          {this.state.error?.message}
        </Alert>
      );
    }

    return this.props.children;
  }
}

// Usage
<ErrorBoundary>
  <PatientDashboard />
</ErrorBoundary>
```

---

## Code Organization

### File Structure

```
src/
├── components/
│   ├── PatientCard/
│   │   ├── PatientCard.tsx
│   │   ├── PatientCard.test.tsx
│   │   └── index.ts
│   └── index.ts (barrel export)
├── hooks/
│   ├── usePatientData.ts
│   └── index.ts
├── pages/
│   ├── Dashboard.tsx
│   └── Patients.tsx
├── services/
│   └── api.ts
├── store/
│   └── app-store.ts
├── types/
│   └── patient.ts
└── utils/
    └── format.ts
```

### Barrel Exports

```typescript
// components/index.ts
export { PatientCard } from './PatientCard';
export { Dashboard } from './Dashboard';
export { SearchBar } from './SearchBar';

// Usage in other files
import { PatientCard, Dashboard } from '@/components';
```

---

**When to use this skill:**
- Building new React components
- Optimizing component performance
- Refactoring class components to functional
- Implementing complex state logic
- Code review and best practices
