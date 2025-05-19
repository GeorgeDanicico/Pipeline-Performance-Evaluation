from abc import ABC, abstractmethod
import time
from concurrent.futures import ThreadPoolExecutor
from typing import List, Dict, Any
import pandas as pd
import matplotlib.pyplot as plt
from tqdm import tqdm

class BaseBenchmark(ABC):
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.iterations = int(config.get('BENCHMARK_ITERATIONS', 1000))
        self.batch_size = int(config.get('BENCHMARK_BATCH_SIZE', 100))
        self.threads = int(config.get('BENCHMARK_THREADS', 4))
        self.results = []
        self.executor = ThreadPoolExecutor(max_workers=self.threads)

    @abstractmethod
    def initialize(self) -> None:
        """Initialize database connection"""
        pass

    @abstractmethod
    def cleanup(self) -> None:
        """Cleanup database connection"""
        pass

    @abstractmethod
    def perform_operation(self, batch: List[Dict[str, Any]]) -> float:
        """Perform the benchmark operation on a batch of documents"""
        pass

    def generate_test_documents(self, count: int) -> List[Dict[str, Any]]:
        """Generate test documents for benchmarking"""
        documents = []
        timestamp = int(time.time() * 1000)
        for i in range(count):
            doc = {
                'id': f"{timestamp}_{i}",
                'value': f"test_value_{i}",
                'timestamp': timestamp
            }
            documents.append(doc)
        return documents

    def run_benchmark(self) -> None:
        """Run the benchmark with progress bar"""
        print(f"\nRunning {self.__class__.__name__} benchmark...")
        
        with tqdm(total=self.iterations, desc="Progress") as pbar:
            for _ in range(self.iterations):
                batch = self.generate_test_documents(self.batch_size)
                start_time = time.time()
                
                # Submit the operation to the thread pool
                future = self.executor.submit(self.perform_operation, batch)
                operation_time = future.result()
                
                self.results.append({
                    'batch_size': self.batch_size,
                    'operation_time': operation_time,
                    'operations_per_second': self.batch_size / operation_time
                })
                
                pbar.update(1)

    def print_results(self) -> None:
        """Print benchmark results"""
        if not self.results:
            print("No results to display")
            return

        df = pd.DataFrame(self.results)
        
        print(f"\n{self.__class__.__name__} Results:")
        print(f"Total Operations: {len(self.results) * self.batch_size}")
        print(f"Total Time: {df['operation_time'].sum():.2f} seconds")
        print(f"Average Time per Operation: {df['operation_time'].mean() / self.batch_size:.4f} seconds")
        print(f"Average Operations per Second: {df['operations_per_second'].mean():.2f}")

    def plot_results(self) -> None:
        """Plot benchmark results"""
        if not self.results:
            return

        df = pd.DataFrame(self.results)
        
        plt.figure(figsize=(12, 6))
        
        # Plot operation times
        plt.subplot(1, 2, 1)
        plt.plot(df['operation_time'])
        plt.title('Operation Time per Batch')
        plt.xlabel('Batch Number')
        plt.ylabel('Time (seconds)')
        
        # Plot operations per second
        plt.subplot(1, 2, 2)
        plt.plot(df['operations_per_second'])
        plt.title('Operations per Second')
        plt.xlabel('Batch Number')
        plt.ylabel('Ops/sec')
        
        plt.tight_layout()
        plt.savefig(f'{self.__class__.__name__}_results.png')
        plt.close()

    def run(self) -> None:
        """Run the complete benchmark process"""
        try:
            self.initialize()
            self.run_benchmark()
            self.print_results()
            self.plot_results()
        finally:
            self.cleanup()
            self.executor.shutdown() 