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
  Alert,
  Snackbar,
} from '@mui/material';
import WorkloadStatus from './WorkloadStatus';

interface DatabaseMetrics {
  executionTime: number;
  executionP50: number;
  executionP75: number;
  executionP90: number;
  executionP99: number;
  executeOperationsPerSec: number;
  memoryUsage: number;
  indexMemoryUsage: number;
  loadingTime: number;
  loadingP50: number;
  loadingP75: number;
  loadingP90: number;
  loadingP99: number;
  loadingOperationsPerSec: number;
  loading_memory_usage: number;
  loading_index_memory_usage: number;
}

interface BenchmarkResponse {
  id: number;
  timestamp: number;
  recordCount: number;
  operationCount: number;
  workloadType: string;
  threadCount: number;
  mongoMetrics: DatabaseMetrics | null;
  couchbaseMetrics: DatabaseMetrics | null;
}

const workloads = ['WORKLOAD_A', 'WORKLOAD_B', 'WORKLOAD_C', 'WORKLOAD_D'];

export default function WorkloadForm() {
  const [formData, setFormData] = useState({
    workload: '',
    recordCount: '',
    operationCount: '',
    threadCount: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [benchmarkResult, setBenchmarkResult] = useState<BenchmarkResponse | null>(null);

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
    setError(null);
    setBenchmarkResult(null);
    try {
      const response = await fetch('http://localhost:8000/api/v1/benchmark/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          workloadType: formData.workload,
          recordCount: parseInt(formData.recordCount),
          operationCount: parseInt(formData.operationCount),
          threadCount: parseInt(formData.threadCount),
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.detail || 'Failed to start benchmark');
      }

      const data: BenchmarkResponse = await response.json();
      console.log('Benchmark started:', data);
      setBenchmarkResult(data);
    } catch (error) {
      console.error('Error starting benchmark:', error);
      setError(error instanceof Error ? error.message : 'Failed to start benchmark');
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
                  {workload.replace('_', ' ')}
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
      {benchmarkResult && (
        <Paper elevation={3} sx={{ p: 3, mt: 2 }}>
          <Typography variant="h6" gutterBottom>
            Benchmark Results
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography>
              Workload: {benchmarkResult.workloadType.replace('_', ' ')}
            </Typography>
            <Typography>
              Record Count: {benchmarkResult.recordCount}
            </Typography>
            <Typography>
              Operation Count: {benchmarkResult.operationCount}
            </Typography>
            <Typography>
              Thread Count: {benchmarkResult.threadCount}
            </Typography>
            {benchmarkResult.mongoMetrics && (
              <>
                <Typography variant="subtitle1" sx={{ mt: 2 }}>
                  MongoDB Metrics:
                </Typography>
                <Box sx={{ pl: 2 }}>
                  <Typography>Execution Time: {benchmarkResult.mongoMetrics.executionTime}ms</Typography>
                  <Typography>Operations/sec: {benchmarkResult.mongoMetrics.executeOperationsPerSec.toFixed(2)}</Typography>
                  <Typography>Memory Usage: {benchmarkResult.mongoMetrics.memoryUsage}MB</Typography>
                </Box>
              </>
            )}
            {benchmarkResult.couchbaseMetrics && (
              <>
                <Typography variant="subtitle1" sx={{ mt: 2 }}>
                  Couchbase Metrics:
                </Typography>
                <Box sx={{ pl: 2 }}>
                  <Typography>Execution Time: {benchmarkResult.couchbaseMetrics.executionTime}ms</Typography>
                  <Typography>Operations/sec: {benchmarkResult.couchbaseMetrics.executeOperationsPerSec.toFixed(2)}</Typography>
                  <Typography>Memory Usage: {benchmarkResult.couchbaseMetrics.memoryUsage}MB</Typography>
                </Box>
              </>
            )}
          </Box>
        </Paper>
      )}
      <Snackbar 
        open={!!error} 
        autoHideDuration={6000} 
        onClose={() => setError(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setError(null)} severity="error" sx={{ width: '100%' }}>
          {error}
        </Alert>
      </Snackbar>
    </>
  );
} 