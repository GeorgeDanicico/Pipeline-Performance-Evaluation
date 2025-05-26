'use client';

import { useState } from 'react';
import {
  Box,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Paper,
  Typography,
  SelectChangeEvent,
  Button,
} from '@mui/material';
import WorkloadStatus from './WorkloadStatus';

const workloads = ['Workload A', 'Workload B', 'Workload C', 'Workload D'];

export default function WorkloadForm() {
  const [formData, setFormData] = useState({
    workload: '',
    recordCount: '',
    operationCount: '',
    threadCount: '',
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSelectChange = (event: SelectChangeEvent) => {
    setFormData({
      ...formData,
      workload: event.target.value,
    });
  };

  const handleNumberChange = (field: string) => (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [field]: event.target.value,
    });
  };

  const handleStartWorkload = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/v1/start_benchmark', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          workloadType: formData.workload.replace(' ', '_').toUpperCase(),
          recordCount: parseInt(formData.recordCount),
          operationCount: parseInt(formData.operationCount),
          threadCount: parseInt(formData.threadCount),
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to start benchmark');
      }

      const data = await response.json();
      console.log('Benchmark started:', data);
    } catch (error) {
      console.error('Error starting benchmark:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h5" component="h2" gutterBottom>
          Workload Configuration
        </Typography>
        <Box component="form" sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <FormControl fullWidth>
            <InputLabel id="workload-label">Workload</InputLabel>
            <Select
              labelId="workload-label"
              id="workload"
              value={formData.workload}
              label="Workload"
              onChange={handleSelectChange}
            >
              {workloads.map((workload) => (
                <MenuItem key={workload} value={workload}>
                  {workload}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            fullWidth
            label="Record Count"
            type="number"
            value={formData.recordCount}
            onChange={handleNumberChange('recordCount')}
            InputProps={{ inputProps: { min: 0 } }}
          />

          <TextField
            fullWidth
            label="Operation Count"
            type="number"
            value={formData.operationCount}
            onChange={handleNumberChange('operationCount')}
            InputProps={{ inputProps: { min: 0 } }}
          />

          <TextField
            fullWidth
            label="Thread Count"
            type="number"
            value={formData.threadCount}
            onChange={handleNumberChange('threadCount')}
            InputProps={{ inputProps: { min: 1 } }}
          />

          <Button
            variant="contained"
            color="primary"
            size="large"
            onClick={handleStartWorkload}
            disabled={isLoading}
            sx={{ mt: 2 }}
          >
            Start Workload
          </Button>
        </Box>
      </Paper>
      <WorkloadStatus isVisible={isLoading} />
    </>
  );
} 