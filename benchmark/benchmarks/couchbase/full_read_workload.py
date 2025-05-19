import pprint
import random
import time
from datetime import timedelta

from couchbase.cluster import Cluster, ClusterOptions
from couchbase.auth import PasswordAuthenticator
from couchbase.options import QueryOptions

from typing import List, Dict, Any

# Discrete value sets
VENDOR_IDS    = [1, 2, 6, 7]
RATE_CODE_IDS = [1, 2, 3, 4, 5, 6, 99]
PAYMENT_TYPES = [0, 1, 2, 3, 4, 5, 6]

class QueryBenchmark:
    """
    Runs a configurable number of N1QL query executions,
    measures per-run latency, and computes overall throughput.
    """

    def __init__(self,
                 connection_string: str,
                 username: str,
                 password: str,
                 bucket_name: str,
                 num_runs: int = 1000):
        # Connect to Couchbase
        auth = PasswordAuthenticator(username, password)
        self.cluster = Cluster(connection_string, ClusterOptions(auth))
        self.bucket = self.cluster.bucket(bucket_name)
        self.collection = self.bucket.default_collection()
        self.num_runs = num_runs

    def build_random_filter(self) -> str:
        vendor_id = random.choice(VENDOR_IDS)
        rate_code_id = random.choice(RATE_CODE_IDS)
        payment_type = random.choice(PAYMENT_TYPES)
        pu_loc = random.randint(1, 265)
        do_loc = random.randint(1, 265)
        passenger_count = random.randint(1, 6)
        trip_dist = random.uniform(0.1, 50.0)
        fare_amount = random.uniform(2.5, 200.0)

        return f"""
            VendorID = {vendor_id} AND
            RatecodeID = {rate_code_id} AND
            payment_type = {payment_type} AND
            PULocationID = {pu_loc} AND
            DOLocationID = {do_loc} AND
            passenger_count = {passenger_count} AND
            trip_distance >= {round(trip_dist, 2)} AND
            trip_distance < {round(trip_dist + 5.0, 2)} AND
            fare_amount >= {round(fare_amount, 2)} AND
            fare_amount < {round(fare_amount + 50.0, 2)}
        """

    def _run_once(self) -> float:
        """
        Execute the N1QL query once and return the elapsed time in seconds.
        Uses a high-resolution performance counter for timing.
        """
        start = time.perf_counter()
        
        # Build the query with random filters
        random_filters = self.build_random_filter()
        full_query = f"""
            SELECT * FROM `{self.bucket.name}`
            WHERE {random_filters}
        """
        
        # Execute the query
        result = self.cluster.query(full_query, QueryOptions(timeout=timedelta(seconds=200),metrics=True)  )
        
        # Force execution and fetch results
        # rows = [row for row in result]
        try:
            _ = list(result)  # fetch the first row, ignores its value
        except StopIteration:
            pass
        # Get execution stats
        metrics = result.metadata().metrics()

        pprint.pprint({
            'execution_time': metrics.execution_time(),
            'elapsed_time': metrics.elapsed_time(),
            'result_count': metrics.result_count(),
            'result_size': metrics.result_size(),
            'sort_count': metrics.sort_count(),
            'mutation_count': metrics.mutation_count(),
            'error_count': metrics.error_count(),
            'warning_count': metrics.warning_count()
        })
        
        end = time.perf_counter()
        return end - start

    def run(self) -> Dict[str, float]:
        """
        Executes the query self.num_runs times, collects latencies,
        and returns average latency (ms) and throughput (ops/sec).
        """
        latencies: List[float] = []

        # Warm-up: one run to mitigate cold-start effects
        self._run_once()

        # Benchmark loop
        # for _ in range(self.num_runs):
        #     elapsed = self._run_once()
        #     latencies.append(elapsed)

        # total_time = sum(latencies)
        # avg_latency_ms = (total_time / self.num_runs) * 1000
        # throughput = self.num_runs / total_time  # ops per second

        return {
            "runs": self.num_runs,
            "avg_latency_ms": 0,
            "throughput_ops_per_sec": 0
        }

if __name__ == "__main__":
    # Example complex N1QL query
    query = """
        SELECT 
            HOUR(tpep_pickup_datetime) as hourOfDay,
            AVG(trip_duration) as avgDuration,
            COUNT(*) as tripCount
        FROM `nyc_taxi`
        WHERE tpep_pickup_datetime >= "2025-01-01T00:00:00"
        AND tpep_pickup_datetime < "2025-02-01T00:00:00"
        GROUP BY HOUR(tpep_pickup_datetime)
        ORDER BY tripCount DESC
        LIMIT 5
    """

    benchmark = QueryBenchmark(
        connection_string="couchbase://localhost",
        username="admin",
        password="parola03",
        bucket_name="census",
        num_runs=1
    )

    results = benchmark.run()
    print(f"Executed {results['runs']} runs")
    print(f"Average Latency: {results['avg_latency_ms']:.2f} ms per query")
    print(f"Throughput: {results['throughput_ops_per_sec']:.2f} ops/sec") 