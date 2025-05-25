'use client';

import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { Container, Box } from '@mui/material';
import WorkloadForm from './components/WorkloadForm';
import WorkloadHistory from './components/WorkloadHistory';

const theme = createTheme({
  palette: {
    mode: 'light',
  },
});

export default function Home() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="lg">
        <Box sx={{ my: 4 }}>
          <Box sx={{ display: 'flex', gap: 3 }}>
            <Box sx={{ width: '40%' }}>
              <WorkloadForm />
            </Box>
            <Box sx={{ width: '60%' }}>
              <WorkloadHistory />
            </Box>
          </Box>
        </Box>
      </Container>
    </ThemeProvider>
  );
}
