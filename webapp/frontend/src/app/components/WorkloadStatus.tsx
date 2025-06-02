'use client';

import { Box, CircularProgress, Typography, Paper } from '@mui/material';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

interface WorkloadStatusProps {
  isVisible: boolean;
  historyId?: string;
  isComplete?: boolean;
}

export default function WorkloadStatus({ isVisible, historyId, isComplete }: WorkloadStatusProps) {
  const router = useRouter();

  useEffect(() => {
    if (isComplete && historyId) {
      const timer = setTimeout(() => {
        router.push(`/history/${historyId}`);
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [isComplete, historyId, router]);

  if (!isVisible) return null;

  if (isComplete) {
    return (
      <Paper elevation={3} sx={{ p: 3, mt: 3 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
          <CheckCircleIcon color="success" sx={{ fontSize: 60 }} />
          <Typography variant="h6" color="success.main">
            Benchmark Completed Successfully
          </Typography>
          <Typography variant="body1">
            Redirecting to results in a few seconds...
          </Typography>
        </Box>
      </Paper>
    );
  }

  return (
    <Paper elevation={3} sx={{ p: 3, mt: 3 }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
        <CircularProgress size={40} />
        <Typography variant="body1">
          Running benchmark...
        </Typography>
      </Box>
    </Paper>
  );
} 