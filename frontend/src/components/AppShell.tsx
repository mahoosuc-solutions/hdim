/**
 * AppShell Component
 *
 * Provides the main navigation shell for the HDIM platform.
 * Includes navigation between Evaluations and Approvals dashboards.
 */

import { useState, useMemo, useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  ThemeProvider,
  createTheme,
  Divider,
  Tooltip,
  useMediaQuery,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Assessment as AssessmentIcon,
  Approval as ApprovalIcon,
  ChevronLeft as ChevronLeftIcon,
  TrendingUp as SalesIcon,
} from '@mui/icons-material';
import { DarkModeToggle } from './DarkModeToggle';
import { useDarkMode } from '../hooks/useDarkMode';
import { shouldRequireSessionReauth } from '../app/integrations/sessionExpiry';

const DRAWER_WIDTH = 240;

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
  description: string;
}

const navItems: NavItem[] = [
  {
    path: '/evaluations',
    label: 'Evaluations',
    icon: <AssessmentIcon />,
    description: 'CQL measure evaluation dashboard',
  },
  {
    path: '/approvals',
    label: 'Approvals',
    icon: <ApprovalIcon />,
    description: 'Human-in-the-Loop approval queue',
  },
  {
    path: '/sales',
    label: 'Sales',
    icon: <SalesIcon />,
    description: 'Sales automation and CRM',
  },
];

export function AppShell() {
  const [drawerOpen, setDrawerOpen] = useState(true);
  const location = useLocation();
  const navigate = useNavigate();
  const { isDarkMode } = useDarkMode();
  const isMobile = useMediaQuery('(max-width:600px)');

  // Create theme dynamically based on dark mode
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: isDarkMode ? 'dark' : 'light',
          primary: {
            main: '#1976d2',
          },
          secondary: {
            main: '#dc004e',
          },
        },
      }),
    [isDarkMode]
  );

  const handleDrawerToggle = () => {
    setDrawerOpen(!drawerOpen);
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    if (isMobile) {
      setDrawerOpen(false);
    }
  };

  useEffect(() => {
    if (shouldRequireSessionReauth()) {
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  // Get current page title
  const currentPage = navItems.find(item => location.pathname.startsWith(item.path));
  const pageTitle = currentPage?.label || 'HDIM Platform';

  const drawer = (
    <Box>
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="h6" noWrap component="div">
          HDIM
        </Typography>
        {!isMobile && (
          <IconButton onClick={handleDrawerToggle} size="small">
            <ChevronLeftIcon />
          </IconButton>
        )}
      </Toolbar>
      <Divider />
      <List>
        {navItems.map((item) => (
          <ListItem key={item.path} disablePadding>
            <Tooltip title={item.description} placement="right" arrow>
              <ListItemButton
                selected={location.pathname.startsWith(item.path)}
                onClick={() => handleNavigation(item.path)}
                sx={{
                  '&.Mui-selected': {
                    backgroundColor: 'primary.main',
                    color: 'primary.contrastText',
                    '&:hover': {
                      backgroundColor: 'primary.dark',
                    },
                    '& .MuiListItemIcon-root': {
                      color: 'primary.contrastText',
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            </Tooltip>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', height: '100vh' }}>
        {/* App Bar - Only show on mobile or when drawer is closed */}
        {(!drawerOpen || isMobile) && (
          <AppBar
            position="fixed"
            sx={{
              width: { sm: `calc(100% - ${drawerOpen ? DRAWER_WIDTH : 0}px)` },
              ml: { sm: `${drawerOpen ? DRAWER_WIDTH : 0}px` },
            }}
          >
            <Toolbar>
              <IconButton
                color="inherit"
                aria-label="open drawer"
                edge="start"
                onClick={handleDrawerToggle}
                sx={{ mr: 2 }}
              >
                <MenuIcon />
              </IconButton>
              <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
                {pageTitle}
              </Typography>
              <DarkModeToggle />
            </Toolbar>
          </AppBar>
        )}

        {/* Navigation Drawer */}
        <Drawer
          variant={isMobile ? 'temporary' : 'persistent'}
          open={drawerOpen}
          onClose={handleDrawerToggle}
          sx={{
            width: DRAWER_WIDTH,
            flexShrink: 0,
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              boxSizing: 'border-box',
            },
          }}
        >
          {drawer}
        </Drawer>

        {/* Main Content */}
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            width: { sm: `calc(100% - ${drawerOpen ? DRAWER_WIDTH : 0}px)` },
            ml: { sm: `${drawerOpen ? 0 : -DRAWER_WIDTH}px` },
            transition: theme.transitions.create(['margin', 'width'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen,
            }),
            ...(drawerOpen && {
              transition: theme.transitions.create(['margin', 'width'], {
                easing: theme.transitions.easing.easeOut,
                duration: theme.transitions.duration.enteringScreen,
              }),
            }),
            pt: (!drawerOpen || isMobile) ? '64px' : 0,
            height: '100vh',
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </ThemeProvider>
  );
}
