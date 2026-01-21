---
name: frontend-dev:mui-customization
description: Material UI v7 theming, component customization, styling patterns, and best practices for HDIM frontend
---

# MUI Customization Guide

Comprehensive guide to customizing Material UI v7 for the HDIM healthcare platform.

## Table of Contents
1. [Theming](#theming)
2. [Component Customization](#component-customization)
3. [Styling Patterns](#styling-patterns)
4. [Responsive Design](#responsive-design)
5. [Accessibility](#accessibility)
6. [Performance](#performance)

---

## Theming

### Create Custom Theme

```typescript
// src/theme.ts
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2', // Healthcare blue
      light: '#42a5f5',
      dark: '#1565c0',
      contrastText: '#fff',
    },
    secondary: {
      main: '#00796b', // Medical green
      light: '#48a999',
      dark: '#004d40',
    },
    error: {
      main: '#d32f2f', // Critical alerts
    },
    warning: {
      main: '#f57c00', // Warnings
    },
    success: {
      main: '#388e3c', // Success states
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
    text: {
      primary: '#212121',
      secondary: '#757575',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
      lineHeight: 1.2,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
      lineHeight: 1.3,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
      lineHeight: 1.4,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 600,
      lineHeight: 1.5,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 600,
      lineHeight: 1.5,
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.5,
    },
    body2: {
      fontSize: '0.875rem',
      lineHeight: 1.5,
    },
    button: {
      textTransform: 'none', // Don't uppercase buttons
      fontWeight: 500,
    },
  },
  spacing: 8, // Base spacing unit (8px)
  shape: {
    borderRadius: 8, // Rounded corners
  },
  components: {
    // Global component overrides
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 16px',
        },
        contained: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: 'none',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
      },
    },
  },
});
```

### Dark Mode Theme

```typescript
// src/theme.ts
import { createTheme, ThemeProvider, useMediaQuery } from '@mui/material';
import { useMemo } from 'react';

export function useAppTheme() {
  const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)');

  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: prefersDarkMode ? 'dark' : 'light',
          primary: {
            main: prefersDarkMode ? '#90caf9' : '#1976d2',
          },
          background: {
            default: prefersDarkMode ? '#121212' : '#f5f5f5',
            paper: prefersDarkMode ? '#1e1e1e' : '#ffffff',
          },
        },
      }),
    [prefersDarkMode]
  );

  return theme;
}

// Usage in App.tsx
function App() {
  const theme = useAppTheme();

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {/* App content */}
    </ThemeProvider>
  );
}
```

### Custom Color Palette

```typescript
// Extend palette with custom colors
declare module '@mui/material/styles' {
  interface Palette {
    hipaa: Palette['primary'];
    medical: Palette['primary'];
  }

  interface PaletteOptions {
    hipaa?: PaletteOptions['primary'];
    medical?: PaletteOptions['primary'];
  }
}

const theme = createTheme({
  palette: {
    hipaa: {
      main: '#ff9800', // HIPAA-related warnings
      light: '#ffb74d',
      dark: '#f57c00',
      contrastText: '#000',
    },
    medical: {
      main: '#00796b',
      light: '#48a999',
      dark: '#004d40',
      contrastText: '#fff',
    },
  },
});

// Usage
<Button color="hipaa">HIPAA Alert</Button>
<Chip color="medical" label="Medical Record" />
```

---

## Component Customization

### Custom Button Variants

```typescript
// Extend Button component
declare module '@mui/material/Button' {
  interface ButtonPropsVariantOverrides {
    dashed: true;
    elevated: true;
  }
}

const theme = createTheme({
  components: {
    MuiButton: {
      variants: [
        {
          props: { variant: 'dashed' },
          style: {
            border: '2px dashed',
            backgroundColor: 'transparent',
            '&:hover': {
              backgroundColor: 'rgba(25, 118, 210, 0.04)',
            },
          },
        },
        {
          props: { variant: 'elevated' },
          style: {
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            '&:hover': {
              boxShadow: '0 6px 16px rgba(0,0,0,0.2)',
            },
          },
        },
      ],
    },
  },
});

// Usage
<Button variant="dashed">Upload</Button>
<Button variant="elevated">Primary Action</Button>
```

### Styled Components

```typescript
import { styled } from '@mui/material/styles';
import { Card, CardProps } from '@mui/material';

// Custom styled Card
export const PatientCard = styled(Card)<CardProps>(({ theme }) => ({
  borderRadius: theme.spacing(2),
  padding: theme.spacing(3),
  boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  transition: 'all 0.3s ease',

  '&:hover': {
    boxShadow: '0 4px 16px rgba(0,0,0,0.12)',
    transform: 'translateY(-2px)',
  },

  // Responsive
  [theme.breakpoints.down('sm')]: {
    padding: theme.spacing(2),
  },
}));

// With custom props
interface StatusCardProps extends CardProps {
  status: 'active' | 'inactive' | 'pending';
}

export const StatusCard = styled(Card, {
  shouldForwardProp: (prop) => prop !== 'status',
})<StatusCardProps>(({ theme, status }) => ({
  borderLeft: `4px solid ${
    status === 'active'
      ? theme.palette.success.main
      : status === 'inactive'
      ? theme.palette.error.main
      : theme.palette.warning.main
  }`,
}));

// Usage
<StatusCard status="active">
  <Typography>Patient is active</Typography>
</StatusCard>
```

### Component Override

```typescript
// Global overrides for all instances
const theme = createTheme({
  components: {
    MuiCard: {
      defaultProps: {
        elevation: 0, // No shadow by default
      },
      styleOverrides: {
        root: {
          borderRadius: 12,
          border: '1px solid',
          borderColor: '#e0e0e0',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 500,
        },
        colorPrimary: {
          backgroundColor: '#e3f2fd',
          color: '#1976d2',
        },
      },
    },
  },
});
```

---

## Styling Patterns

### 1. sx Prop (Recommended for one-offs)

```typescript
// Inline styling with theme access
<Box
  sx={{
    p: 2, // padding: theme.spacing(2)
    bgcolor: 'primary.main',
    color: 'primary.contrastText',
    borderRadius: 2,
    '&:hover': {
      bgcolor: 'primary.dark',
    },
    // Responsive
    [theme => theme.breakpoints.down('sm')]: {
      p: 1,
    },
  }}
>
  Content
</Box>
```

### 2. styled() API (Reusable components)

```typescript
const FlexBox = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(2),
  padding: theme.spacing(2),

  [theme.breakpoints.down('md')]: {
    flexDirection: 'column',
  },
}));
```

### 3. Theme-aware Components

```typescript
function PatientCard({ patient }: { patient: Patient }) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Card
      sx={{
        p: isMobile ? 2 : 3,
        borderLeft: `4px solid ${
          patient.status === 'active'
            ? theme.palette.success.main
            : theme.palette.error.main
        }`,
      }}
    >
      <Typography variant={isMobile ? 'h6' : 'h5'}>
        {patient.name}
      </Typography>
    </Card>
  );
}
```

### 4. Reusable Styles

```typescript
// Create reusable style objects
export const commonStyles = {
  card: {
    p: 3,
    borderRadius: 2,
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  },
  flexBetween: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  scrollable: {
    overflowY: 'auto',
    maxHeight: '500px',
    '&::-webkit-scrollbar': {
      width: '8px',
    },
    '&::-webkit-scrollbar-thumb': {
      backgroundColor: '#ccc',
      borderRadius: '4px',
    },
  },
};

// Usage
<Box sx={{ ...commonStyles.card, ...commonStyles.flexBetween }}>
  Content
</Box>
```

---

## Responsive Design

### Breakpoints

```typescript
// Default MUI breakpoints
xs: 0px      // Phone
sm: 600px    // Tablet
md: 900px    // Small laptop
lg: 1200px   // Desktop
xl: 1536px   // Large desktop

// Usage in sx prop
<Box
  sx={{
    width: {
      xs: '100%',  // 0-599px
      sm: '80%',   // 600-899px
      md: '60%',   // 900-1199px
      lg: '50%',   // 1200-1535px
      xl: '40%',   // 1536px+
    },
  }}
/>

// Usage in styled components
const ResponsiveBox = styled(Box)(({ theme }) => ({
  padding: theme.spacing(3),

  [theme.breakpoints.down('md')]: {
    padding: theme.spacing(2),
  },

  [theme.breakpoints.down('sm')]: {
    padding: theme.spacing(1),
  },
}));
```

### Responsive Grid

```typescript
<Grid container spacing={3}>
  <Grid item xs={12} sm={6} md={4} lg={3}>
    <PatientCard /> {/* Full width mobile, half tablet, 1/3 laptop, 1/4 desktop */}
  </Grid>
</Grid>

// Or with Grid2 (new in MUI v6+)
import Grid2 from '@mui/material/Unstable_Grid2';

<Grid2 container spacing={2}>
  <Grid2 xs={12} md={6}>
    <Card>Left half</Card>
  </Grid2>
  <Grid2 xs={12} md={6}>
    <Card>Right half</Card>
  </Grid2>
</Grid2>
```

### useMediaQuery Hook

```typescript
import { useTheme, useMediaQuery } from '@mui/material';

function ResponsiveComponent() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.between('sm', 'md'));
  const isDesktop = useMediaQuery(theme.breakpoints.up('lg'));

  return (
    <Box>
      {isMobile && <MobileLayout />}
      {isTablet && <TabletLayout />}
      {isDesktop && <DesktopLayout />}
    </Box>
  );
}
```

---

## Accessibility

### Accessible Components

```typescript
// Button with proper ARIA
<Button
  aria-label="Delete patient record"
  aria-describedby="delete-description"
  onClick={handleDelete}
>
  <DeleteIcon aria-hidden="true" />
</Button>

// TextField with proper labels
<TextField
  id="patient-name"
  label="Patient Name"
  required
  aria-required="true"
  error={!!errors.name}
  helperText={errors.name?.message}
  aria-describedby={errors.name ? "name-error" : undefined}
/>

// Accessible Dialog
<Dialog
  open={open}
  onClose={onClose}
  aria-labelledby="dialog-title"
  aria-describedby="dialog-description"
>
  <DialogTitle id="dialog-title">Confirm Action</DialogTitle>
  <DialogContent id="dialog-description">
    Are you sure you want to proceed?
  </DialogContent>
</Dialog>

// Accessible Table
<Table aria-label="Patient evaluation results">
  <TableHead>
    <TableRow>
      <TableCell>Patient ID</TableCell>
      <TableCell>Status</TableCell>
    </TableRow>
  </TableHead>
  <TableBody>
    {/* ... */}
  </TableBody>
</Table>
```

### Focus Indicators

```typescript
const theme = createTheme({
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          '&:focus-visible': {
            outline: '2px solid',
            outlineColor: 'primary.main',
            outlineOffset: '2px',
          },
        },
      },
    },
  },
});
```

---

## Performance

### Optimize Re-renders

```typescript
// Memoize styled components
const MemoizedCard = React.memo(styled(Card)(({ theme }) => ({
  padding: theme.spacing(2),
})));

// Use sx prop sparingly in render-heavy components
// ❌ Bad - Creates new object every render
<Box sx={{ p: 2, bgcolor: 'primary.main' }} />

// ✅ Good - Extract to constant
const boxStyles = { p: 2, bgcolor: 'primary.main' };
<Box sx={boxStyles} />
```

### Tree Shaking

```typescript
// ✅ Good - Import only what you need
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';

// ❌ Bad - Imports everything
import { Button, TextField } from '@mui/material';
```

### Lazy Load Icons

```typescript
// Lazy load heavy icon sets
const DeleteIcon = lazy(() =>
  import('@mui/icons-material/Delete').then(module => ({
    default: module.default,
  }))
);

<Suspense fallback={<Box width={24} height={24} />}>
  <DeleteIcon />
</Suspense>
```

---

## Healthcare-Specific Patterns

### Status Indicators

```typescript
function PatientStatusChip({ status }: { status: string }) {
  const getColor = () => {
    switch (status) {
      case 'active': return 'success';
      case 'inactive': return 'error';
      case 'pending': return 'warning';
      default: return 'default';
    }
  };

  return (
    <Chip
      label={status.toUpperCase()}
      color={getColor()}
      size="small"
      sx={{ fontWeight: 600 }}
    />
  );
}
```

### Data Tables

```typescript
<TableContainer component={Paper}>
  <Table sx={{ minWidth: 650 }} aria-label="patient data table">
    <TableHead>
      <TableRow>
        <TableCell>Patient ID</TableCell>
        <TableCell>Name</TableCell>
        <TableCell align="right">Age</TableCell>
        <TableCell>Status</TableCell>
        <TableCell>Actions</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      {patients.map((patient) => (
        <TableRow
          key={patient.id}
          hover
          sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
        >
          <TableCell>{patient.id}</TableCell>
          <TableCell>{patient.name}</TableCell>
          <TableCell align="right">{patient.age}</TableCell>
          <TableCell>
            <PatientStatusChip status={patient.status} />
          </TableCell>
          <TableCell>
            <IconButton aria-label="view details">
              <VisibilityIcon />
            </IconButton>
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>
</TableContainer>
```

### Loading States

```typescript
function PatientCard({ patient, loading }: Props) {
  if (loading) {
    return (
      <Card sx={{ p: 2 }}>
        <Skeleton variant="text" width="60%" height={32} />
        <Skeleton variant="text" width="40%" />
        <Skeleton variant="rectangular" height={80} sx={{ mt: 2 }} />
      </Card>
    );
  }

  return (
    <Card sx={{ p: 2 }}>
      {/* Actual content */}
    </Card>
  );
}
```

---

**When to use this skill:**
- Theming the application
- Customizing MUI components
- Implementing responsive layouts
- Ensuring accessibility compliance
- Optimizing MUI performance
- Creating reusable styled components
