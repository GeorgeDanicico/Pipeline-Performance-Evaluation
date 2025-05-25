'use client';

import { Box, CircularProgress, Typography, Paper } from '@mui/material';

interface WorkloadStatusProps {
  isVisible: boolean;
}

export default function WorkloadStatus({ isVisible }: WorkloadStatusProps) {
  if (!isVisible) return null;

  return (
    <Paper elevation={3} sx={{ p: 3, mt: 3 }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
        <CircularProgress size={40} />
        <Typography variant="body1">
          Running workload...
        </Typography>
      </Box>
    </Paper>
  );
} 