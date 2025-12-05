/**
 * Error Boundary component for catching and logging React errors
 */

import { Component, ReactNode } from 'react';
import { Box, Typography, Button, Paper } from '@mui/material';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: any;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null
    };
    console.log('[ErrorBoundary] Initialized');
  }

  static getDerivedStateFromError(error: Error) {
    console.error('[ErrorBoundary] getDerivedStateFromError:', error);
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: any) {
    console.error('[ErrorBoundary] componentDidCatch - Error:', error);
    console.error('[ErrorBoundary] componentDidCatch - Error Info:', errorInfo);
    console.error('[ErrorBoundary] Stack trace:', error.stack);

    this.setState({
      error,
      errorInfo
    });
  }

  handleReset = () => {
    console.log('[ErrorBoundary] Resetting error boundary');
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      console.log('[ErrorBoundary] Rendering error UI');
      return (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '100vh',
            bgcolor: 'background.default',
            p: 3
          }}
        >
          <Paper
            elevation={3}
            sx={{
              p: 4,
              maxWidth: 600,
              bgcolor: 'error.dark',
              color: 'error.contrastText'
            }}
          >
            <Typography variant="h4" gutterBottom>
              Something went wrong
            </Typography>
            <Typography variant="body1" paragraph>
              The application encountered an error. Check the browser console for details.
            </Typography>
            {this.state.error && (
              <Box
                component="pre"
                sx={{
                  bgcolor: 'rgba(0,0,0,0.2)',
                  p: 2,
                  borderRadius: 1,
                  overflow: 'auto',
                  fontSize: '0.875rem'
                }}
              >
                <Typography variant="body2" component="div">
                  <strong>Error:</strong> {this.state.error.toString()}
                </Typography>
                <Typography variant="body2" component="div" sx={{ mt: 1 }}>
                  <strong>Stack:</strong>
                  {this.state.error.stack}
                </Typography>
                {this.state.errorInfo && (
                  <Typography variant="body2" component="div" sx={{ mt: 1 }}>
                    <strong>Component Stack:</strong>
                    {this.state.errorInfo.componentStack}
                  </Typography>
                )}
              </Box>
            )}
            <Button
              variant="contained"
              color="inherit"
              onClick={this.handleReset}
              sx={{ mt: 2 }}
            >
              Reload Application
            </Button>
          </Paper>
        </Box>
      );
    }

    console.log('[ErrorBoundary] Rendering children');
    return this.props.children;
  }
}
