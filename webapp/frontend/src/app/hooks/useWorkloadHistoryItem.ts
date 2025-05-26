import useSWR from 'swr';
import { WorkloadHistoryItem } from './useWorkloadHistory';

const fetcher = async (url: string) => {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error('Failed to fetch workload history item');
  }
  return response.json();
};

export function useWorkloadHistoryItem(id: string) {
  const { data, error, isLoading, mutate } = useSWR<WorkloadHistoryItem>(
    `http://localhost:8080/api/v1/histories/${id}`,
    fetcher
  );

  return {
    historyItem: data,
    isLoading,
    isError: error,
    mutate,
  };
} 