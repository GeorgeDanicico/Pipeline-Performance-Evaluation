'use client';

import { useParams } from 'next/navigation';
import { useWorkloadHistoryItem } from '@/app/hooks/useWorkloadHistoryItem';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  CircularProgress,
  Paper,
  Divider,
  ThemeProvider,
  createTheme,
} from '@mui/material';
import { WorkloadHistoryItem } from '@/app/hooks/useWorkloadHistory';
import { BarChart } from '@mui/x-charts/BarChart';
import CssBaseline from '@mui/material/CssBaseline';
import { GridProps } from '@mui/material/Grid';

const theme = createTheme({
  palette: {
    mode: 'light',
    background: {
      default: '#ffffff',
      paper: '#ffffff',
    },
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#2e7d32',
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          boxShadow: '0px 2px 4px rgba(0, 0, 0, 0.1)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          boxShadow: '0px 2px 4px rgba(0, 0, 0, 0.1)',
        },
      },
    },
  },
});

function MetricCard({ title, value, unit }: { title: string; value: number; unit: string }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography color="text.secondary" gutterBottom>
          {title}
        </Typography>
        <Typography variant="h5" component="div">
          {value.toLocaleString()} {unit}
        </Typography>
      </CardContent>
    </Card>
  );
}

function MetricsSection({ title, metrics }: { title: string; metrics: WorkloadHistoryItem['mongo_metrics'] }) {
  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h6" gutterBottom>
        {title}
      </Typography>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Execution Time" value={metrics.executionTime} unit="ms" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="P50" value={metrics.executionP50} unit="ms" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="P75" value={metrics.executionP75} unit="ms" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="P90" value={metrics.executionP90} unit="ms" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="P99" value={metrics.executionP99} unit="ms" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Ops/sec" value={metrics.executeOperationsPerSec} unit="ops" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Memory Usage" value={metrics.memoryUsage} unit="MB" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Index Memory" value={metrics.indexMemoryUsage} unit="MB" />
        </Grid>
      </Grid>
    </Box>
  );
}

function ComparisonChart({ mongoMetrics, couchbaseMetrics }: { mongoMetrics: WorkloadHistoryItem['mongo_metrics']; couchbaseMetrics: WorkloadHistoryItem['couchbase_metrics'] }) {
  const metrics = [
    // Execution metrics
    {
      name: 'Execution Time',
      mongoValue: mongoMetrics?.executionTime ?? 0,
      couchbaseValue: couchbaseMetrics?.executionTime ?? 0,
      unit: 'ms'
    },
    {
      name: 'Execution P50',
      mongoValue: mongoMetrics?.executionP50 ?? 0,
      couchbaseValue: couchbaseMetrics?.executionP50 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Execution P75',
      mongoValue: mongoMetrics?.executionP75 ?? 0,
      couchbaseValue: couchbaseMetrics?.executionP75 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Execution P90',
      mongoValue: mongoMetrics?.executionP90 ?? 0,
      couchbaseValue: couchbaseMetrics?.executionP90 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Execution P99',
      mongoValue: mongoMetrics?.executionP99 ?? 0,
      couchbaseValue: couchbaseMetrics?.executionP99 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Execution Operations/sec',
      mongoValue: mongoMetrics?.executeOperationsPerSec ?? 0,
      couchbaseValue: couchbaseMetrics?.executeOperationsPerSec ?? 0,
      unit: 'ops'
    },
    // Data loading metrics
    {
      name: 'Data Load Time',
      mongoValue: mongoMetrics?.loadingTime ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingTime ?? 0,
      unit: 'ms'
    },
    {
      name: 'Data Load P50',
      mongoValue: mongoMetrics?.loadingP50 ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingP50 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Data Load P75',
      mongoValue: mongoMetrics?.loadingP75 ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingP75 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Data Load P90',
      mongoValue: mongoMetrics?.loadingP90 ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingP90 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Data Load P99',
      mongoValue: mongoMetrics?.loadingP99 ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingP99 ?? 0,
      unit: 'ms'
    },
    {
      name: 'Data Load Operations/sec',
      mongoValue: mongoMetrics?.loadingOperationsPerSec ?? 0,
      couchbaseValue: couchbaseMetrics?.loadingOperationsPerSec ?? 0,
      unit: 'ops'
    },
    // Memory metrics
    {
      name: 'Memory Usage',
      mongoValue: mongoMetrics?.memoryUsage ?? 0,
      couchbaseValue: couchbaseMetrics?.memoryUsage ?? 0,
      unit: 'MB'
    },
    {
      name: 'Index Memory',
      mongoValue: mongoMetrics?.indexMemoryUsage ?? 0,
      couchbaseValue: couchbaseMetrics?.indexMemoryUsage ?? 0,
      unit: 'MB'
    }
  ];

  return (
    <Grid container spacing={3}>
      {metrics.map((metric) => (
        <Grid component="div" key={metric.name} xs={12} md={6}>
          <Paper sx={{ p: 2, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              {metric.name}
            </Typography>
            <Box sx={{ height: 300, width: '100%' }}>
              <BarChart
                series={[
                  {
                    data: [metric.mongoValue],
                    label: 'MongoDB',
                    color: '#2e7d32', // Green for MongoDB
                  },
                  {
                    data: [metric.couchbaseValue],
                    label: 'Couchbase',
                    color: '#1976d2', // Blue for Couchbase
                  },
                ]}
                xAxis={[{ scaleType: 'band', data: [''] }]}
                height={300}
                margin={{ top: 20, bottom: 30, left: 40, right: 100 }}
                slotProps={{
                  legend: {
                    direction: 'column',
                    position: { vertical: 'top', horizontal: 'right' },
                    padding: 20,
                  },
                }}
                yAxis={[
                  {
                    label: metric.unit,
                  },
                ]}
              />
            </Box>
            <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-around' }}>
              <Typography sx={{ color: '#2e7d32' }}>
                MongoDB: {metric.mongoValue.toLocaleString()} {metric.unit}
              </Typography>
              <Typography sx={{ color: '#1976d2' }}>
                Couchbase: {metric.couchbaseValue.toLocaleString()} {metric.unit}
              </Typography>
            </Box>
          </Paper>
        </Grid>
      ))}
    </Grid>
  );
}

export default function HistoryDetail() {
  const params = useParams();
  const { historyItem, isLoading, isError } = useWorkloadHistoryItem(params.id as string);

  if (isLoading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      </ThemeProvider>
    );
  }

  if (isError || !historyItem) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box sx={{ p: 4 }}>
          <Typography color="error">Failed to load workload history details</Typography>
        </Box>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ p: 4, backgroundColor: 'background.default' }}>
        <Paper sx={{ p: 3, mb: 4 }}>
          <Typography variant="h4" gutterBottom>
            Workload Details
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Workload Type</Typography>
              <Typography variant="h6">{historyItem.workloadType}</Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Record Count</Typography>
              <Typography variant="h6">{historyItem.recordCount.toLocaleString()}</Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Operation Count</Typography>
              <Typography variant="h6">{historyItem.operationCount.toLocaleString()}</Typography>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Thread Count</Typography>
              <Typography variant="h6">{historyItem.threadCount}</Typography>
            </Grid>
            <Grid item xs={12}>
              <Typography color="text.secondary">Timestamp</Typography>
              <Typography variant="h6">
                {new Date(historyItem.timestamp * 1000).toLocaleString()}
              </Typography>
            </Grid>
          </Grid>
        </Paper>

        <Divider sx={{ my: 4 }} />

        <Typography variant="h5" gutterBottom>
          Performance Comparison
        </Typography>
        <ComparisonChart mongoMetrics={historyItem.mongo_metrics} couchbaseMetrics={historyItem.couchbase_metrics} />

        <Divider sx={{ my: 4 }} />

        <MetricsSection title="MongoDB Metrics" metrics={historyItem.mongo_metrics} />
        <MetricsSection title="Couchbase Metrics" metrics={historyItem.couchbase_metrics} />
      </Box>
    </ThemeProvider>
  );
} 