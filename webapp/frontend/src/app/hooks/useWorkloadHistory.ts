import useSWR from 'swr';

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
  loadingMemoryUsage: number;
  loadingIndexMemoryUsage: number;
}

export interface WorkloadHistoryItem {
  id: number;
  timestamp: number;
  workloadType: string;
  recordCount: number;
  operationCount: number;
  threadCount: number;
  mongoMetrics: DatabaseMetrics | null;
  couchbaseMetrics: DatabaseMetrics | null;
}

const fetcher = async (url: string) => {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error('Failed to fetch workload history');
  }
  return response.json();
};

export function useWorkloadHistory() {
  const { data, error, isLoading, mutate } = useSWR<WorkloadHistoryItem[]>(
    'http://localhost:8000/api/v1/histories',
    fetcher
  );

  return {
    history: data,
    isLoading,
    isError: error,
    mutate,
  };
} 