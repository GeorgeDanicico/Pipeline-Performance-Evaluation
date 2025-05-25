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
  const xLabels = [
    'Execution Time',
    'P50',
    'P75',
    'P90',
    'P99',
    'Ops/sec',
    'Memory Usage',
    'Index Memory',
  ];

  const series = [
    {
      data: [
        mongoMetrics.executionTime,
        mongoMetrics.executionP50,
        mongoMetrics.executionP75,
        mongoMetrics.executionP90,
        mongoMetrics.executionP99,
        mongoMetrics.executeOperationsPerSec,
        mongoMetrics.memoryUsage,
        mongoMetrics.indexMemoryUsage,
      ],
      label: 'MongoDB',
      color: '#1976d2',
    },
    {
      data: [
        couchbaseMetrics.executionTime,
        couchbaseMetrics.executionP50,
        couchbaseMetrics.executionP75,
        couchbaseMetrics.executionP90,
        couchbaseMetrics.executionP99,
        couchbaseMetrics.executeOperationsPerSec,
        couchbaseMetrics.memoryUsage,
        couchbaseMetrics.indexMemoryUsage,
      ],
      label: 'Couchbase',
      color: '#2e7d32',
    },
  ];

  return (
    <Box sx={{ height: 400, width: '100%', mb: 4 }}>
      <BarChart
        series={series}
        xAxis={[{ scaleType: 'band', data: xLabels }]}
        height={400}
        margin={{ top: 20, bottom: 100, left: 40, right: 20 }}
        slotProps={{
          legend: {
            direction: 'row',
            position: { vertical: 'top', horizontal: 'right' },
          },
        }}
      />
    </Box>
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
            <Grid xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Workload Type</Typography>
              <Typography variant="h6">{historyItem.workloadType}</Typography>
            </Grid>
            <Grid xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Record Count</Typography>
              <Typography variant="h6">{historyItem.recordCount.toLocaleString()}</Typography>
            </Grid>
            <Grid xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Operation Count</Typography>
              <Typography variant="h6">{historyItem.operationCount.toLocaleString()}</Typography>
            </Grid>
            <Grid xs={12} sm={6} md={3}>
              <Typography color="text.secondary">Thread Count</Typography>
              <Typography variant="h6">{historyItem.threadCount}</Typography>
            </Grid>
            <Grid xs={12}>
              <Typography color="text.secondary">Timestamp</Typography>
              <Typography variant="h6">
                {new Date(historyItem.timestamp).toLocaleString()}
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