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
  Alert,
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

function MetricsSection({ title, metrics }: { title: string; metrics: NonNullable<WorkloadHistoryItem['mongoMetrics']> }) {
  if (!metrics) return null;

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

function ComparisonChart({ mongoMetrics, couchbaseMetrics }: { 
  mongoMetrics: WorkloadHistoryItem['mongoMetrics']; 
  couchbaseMetrics: WorkloadHistoryItem['couchbaseMetrics'] 
}) {
  if (!mongoMetrics || !couchbaseMetrics) return null;

  const metrics = [
    // Execution metrics
    {
      name: 'Execution Time',
      mongoValue: mongoMetrics.executionTime,
      couchbaseValue: couchbaseMetrics.executionTime,
      unit: 'ms'
    },
    {
      name: 'Execution P50',
      mongoValue: mongoMetrics.executionP50,
      couchbaseValue: couchbaseMetrics.executionP50,
      unit: 'ms'
    },
    {
      name: 'Execution P75',
      mongoValue: mongoMetrics.executionP75,
      couchbaseValue: couchbaseMetrics.executionP75,
      unit: 'ms'
    },
    {
      name: 'Execution P90',
      mongoValue: mongoMetrics.executionP90,
      couchbaseValue: couchbaseMetrics.executionP90,
      unit: 'ms'
    },
    {
      name: 'Execution P99',
      mongoValue: mongoMetrics.executionP99,
      couchbaseValue: couchbaseMetrics.executionP99,
      unit: 'ms'
    },
    {
      name: 'Execution Operations/sec',
      mongoValue: mongoMetrics.executeOperationsPerSec,
      couchbaseValue: couchbaseMetrics.executeOperationsPerSec,
      unit: 'ops'
    },
    // Data loading metrics
    {
      name: 'Data Load Time',
      mongoValue: mongoMetrics.loadingTime,
      couchbaseValue: couchbaseMetrics.loadingTime,
      unit: 'ms'
    },
    {
      name: 'Data Load P50',
      mongoValue: mongoMetrics.loadingP50,
      couchbaseValue: couchbaseMetrics.loadingP50,
      unit: 'ms'
    },
    {
      name: 'Data Load P75',
      mongoValue: mongoMetrics.loadingP75,
      couchbaseValue: couchbaseMetrics.loadingP75,
      unit: 'ms'
    },
    {
      name: 'Data Load P90',
      mongoValue: mongoMetrics.loadingP90,
      couchbaseValue: couchbaseMetrics.loadingP90,
      unit: 'ms'
    },
    {
      name: 'Data Load P99',
      mongoValue: mongoMetrics.loadingP99,
      couchbaseValue: couchbaseMetrics.loadingP99,
      unit: 'ms'
    },
    {
      name: 'Data Load Operations/sec',
      mongoValue: mongoMetrics.loadingOperationsPerSec,
      couchbaseValue: couchbaseMetrics.loadingOperationsPerSec,
      unit: 'ops'
    },
    // Memory metrics
    {
      name: 'Memory Usage',
      mongoValue: mongoMetrics.memoryUsage,
      couchbaseValue: couchbaseMetrics.memoryUsage,
      unit: 'MB'
    },
    {
      name: 'Index Memory',
      mongoValue: mongoMetrics.indexMemoryUsage,
      couchbaseValue: couchbaseMetrics.indexMemoryUsage,
      unit: 'MB'
    }
  ];

  return (
    <Grid container spacing={3}>
      {metrics.map((metric) => (
        <Grid item xs={12} md={6} key={metric.name}>
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
                    direction: 'column' as 'column',
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
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError || !historyItem) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">Failed to load benchmark history</Alert>
      </Box>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ p: 3 }}>
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h4" gutterBottom>
            {historyItem.workloadType.replace('_', ' ')} - Run #{historyItem.id}
          </Typography>
          <Typography variant="subtitle1" color="text.secondary" gutterBottom>
            {new Date(historyItem.timestamp * 1000).toLocaleString()}
          </Typography>
          <Divider sx={{ my: 2 }} />
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <MetricCard title="Record Count" value={historyItem.recordCount} unit="records" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <MetricCard title="Operation Count" value={historyItem.operationCount} unit="operations" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <MetricCard title="Thread Count" value={historyItem.threadCount} unit="threads" />
            </Grid>
          </Grid>
        </Paper>

        {historyItem.mongoMetrics && (
          <MetricsSection title="MongoDB Metrics" metrics={historyItem.mongoMetrics} />
        )}

        {historyItem.couchbaseMetrics && (
          <MetricsSection title="Couchbase Metrics" metrics={historyItem.couchbaseMetrics} />
        )}

        {historyItem.mongoMetrics && historyItem.couchbaseMetrics && (
          <ComparisonChart 
            mongoMetrics={historyItem.mongoMetrics} 
            couchbaseMetrics={historyItem.couchbaseMetrics} 
          />
        )}
      </Box>
    </ThemeProvider>
  );
} 