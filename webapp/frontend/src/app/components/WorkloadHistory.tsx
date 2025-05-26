'use client';

import { Paper, Typography, List, ListItem, ListItemButton, ListItemText, Box, CircularProgress, Alert } from '@mui/material';
import { useRouter } from 'next/navigation';
import { useWorkloadHistory } from '../hooks/useWorkloadHistory';

const formatTimestamp = (timestamp: number) => {
  const date = new Date(timestamp * 1000);
  return date.toLocaleString();
};

export default function WorkloadHistory() {
  const router = useRouter();
  const { history, isLoading, isError } = useWorkloadHistory();

  const handleHistoryItemClick = (id: number) => {
    router.push(`/history/${id}`);
  };

  return (
    <Paper elevation={3} sx={{ p: 3, height: '100%' }}>
      <Typography variant="h5" component="h2" gutterBottom>
        Workload History
      </Typography>
      <Box sx={{ 
        maxHeight: 'calc(100vh - 250px)', 
        overflow: 'auto',
        '&::-webkit-scrollbar': {
          width: '8px',
        },
        '&::-webkit-scrollbar-track': {
          background: '#f1f1f1',
          borderRadius: '4px',
        },
        '&::-webkit-scrollbar-thumb': {
          background: '#888',
          borderRadius: '4px',
          '&:hover': {
            background: '#555',
          },
        },
      }}>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
            <CircularProgress />
          </Box>
        ) : isError ? (
          <Alert severity="error" sx={{ m: 2 }}>
            Failed to load workload history
          </Alert>
        ) : (
          <List>
            {history?.map((item) => (
              <ListItem key={item.id} disablePadding>
                <ListItemButton onClick={() => handleHistoryItemClick(item.id)}>
                  <ListItemText
                    primary={`${item.workloadType} - Run #${item.id}`}
                    secondary={
                      <>
                        <Typography component="span" variant="body2" color="text.primary">
                          {formatTimestamp(item.timestamp)}
                        </Typography>
                        <br />
                        <Typography component="span" variant="body2" color="text.secondary">
                          Records: {item.recordCount.toLocaleString()} | 
                          Operations: {item.operationCount.toLocaleString()} | 
                          Threads: {item.threadCount}
                        </Typography>
                        <br />
                        <Typography component="span" variant="body2" color="text.secondary">
                          Mongo Exec: {item.mongo_metrics.executionTime}ms | 
                          Couchbase Exec: {item.couchbase_metrics.executionTime}ms
                        </Typography>
                      </>
                    }
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        )}
      </Box>
    </Paper>
  );
} 