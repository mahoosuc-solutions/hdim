import { Box, Card, CardContent, Stack, Typography } from '@mui/material';

export function ExternalAuthMockPage() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 520 }}>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h5" component="h1">
              External Auth Login
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Mock external IdP endpoint for local session-flow e2e validation.
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
