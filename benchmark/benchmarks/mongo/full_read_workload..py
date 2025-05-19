import pprint
import random
import time
from pymongo import MongoClient
from typing import List, Dict, Any

# Discrete value sets
VENDOR_IDS    = [1, 2, 6, 7]
RATE_CODE_IDS = [1, 2, 3, 4, 5, 6, 99]
PAYMENT_TYPES = [0, 1, 2, 3, 4, 5, 6]

class AggregationBenchmark:
    """
    Runs a configurable number of aggregation pipeline executions,
    measures per-run latency, and computes overall throughput.
    """

    def __init__(self,
                 mongo_uri: str,
                 db_name: str,
                 collection_name: str,
                 pipeline: List[Dict[str, Any]],
                 num_runs: int = 1000):
        # Connect to MongoDB
        self.client = MongoClient(mongo_uri)
        self.collection = self.client[db_name][collection_name]
        self.pipeline = pipeline
        self.num_runs = num_runs

    def build_random_filter(self):
        vendor_id = random.choice(VENDOR_IDS)
        rate_code_id = random.choice(RATE_CODE_IDS)
        payment_type = random.choice(PAYMENT_TYPES)
        pu_loc = random.randint(1, 265)
        do_loc = random.randint(1, 265)
        passenger_count = random.randint(1, 6)
        trip_dist = random.uniform(0.1, 50.0)
        fare_amount = random.uniform(2.5, 200.0)

        return {
            "VendorID": vendor_id,
            "RatecodeID": rate_code_id,
            "payment_type": payment_type,
            "PULocationID": pu_loc,
            "DOLocationID": do_loc,
            "passenger_count": passenger_count,
            "trip_distance": {"$gte": round(trip_dist, 2), "$lt": round(trip_dist + 5.0, 2)},
            "fare_amount": {"$gte": round(fare_amount, 2), "$lt": round(fare_amount + 50.0, 2)},
        }

    def _run_once(self) -> float:
        """
        Execute the aggregation pipeline once and return the elapsed time in seconds.
        Uses a high-resolution performance counter for timing.
        """
        start = time.perf_counter()
        filter_query = {"VendorID": "1"}  # e.g. find documents where status == "active"

        random_filters = self.build_random_filter()
        # 3. Run the query with explain in 'executionStats' mode
        # explain_output = self.collection.find(filter_query).explain()
        # self.collection.getPlanCache().clear()  # Clear the plan cache to ensure a fresh execution

        explain_output = self.collection.find(filter_query).explain()  # aggregate() applies the multi-stage pipeline :contentReference[oaicite:6]{index=6}

        # high-resolution timer :contentReference[oaicite:5]{index=5}
        pprint.pprint(explain_output)
        # print("Execution stats: " + explain_output)
        end = time.perf_counter()
        return end - start

    def run(self) -> Dict[str, float]:
        """
        Executes the pipeline self.num_runs times, collects latencies,
        and returns average latency (ms) and throughput (ops/sec).
        """
        latencies: List[float] = []

        # Warm-up: one run to mitigate cold-start effects :contentReference[oaicite:7]{index=7}
        self._run_once()

        # Benchmark loop
        for _ in range(self.num_runs):
            elapsed = self._run_once()
            latencies.append(elapsed)

        total_time = sum(latencies)
        avg_latency_ms = (total_time / self.num_runs) * 1000
        throughput = self.num_runs / total_time  # ops per second :contentReference[oaicite:8]{index=8}

        return {
            "runs": self.num_runs,
            "avg_latency_ms": avg_latency_ms,
            "throughput_ops_per_sec": throughput
        }

if __name__ == "__main__":
    # Define a complex aggregation pipeline
    pipeline = [
        {"$match": {
            "field1": {"$gte": "2025-01-01T00:00:00", "$lt": "2025-02-01T00:00:00"}
        }},
        {"$project": {
            "passenger_count": 1,
            "tripDistance": "$field4",
            "tripDuration": {
                "$divide": [{"$subtract": ["$field2", "$field1"]}, 1000]
            },
            "total_amount": "$field16",
            "hourOfDay": {"$hour": "$field1"}
        }},
        {"$bucketAuto": {
            "groupBy": "$tripDistance",
            "buckets": 5,
            "output": {
                "avgFare": {"$avg": "$total_amount"},
                "count": {"$sum": 1}
            }
        }},
        {"$unwind": "$count"},
        {"$group": {
            "_id": "$hourOfDay",
            "avgDuration": {"$avg": "$tripDuration"},
            "tripCount": {"$sum": 1}
        }},
        {"$sort": {"tripCount": -1}},
        {"$limit": 5}
    ]

    benchmark = AggregationBenchmark(
        mongo_uri="mongodb://localhost:27017/",
        db_name="census",
        collection_name="nyc_taxi",
        pipeline=pipeline,
        num_runs=1  # e.g., 500 runs
    )

    results = benchmark.run()
    print(f"Executed {results['runs']} runs")
    print(f"Average Latency: {results['avg_latency_ms']:.2f} ms per query")
    print(f"Throughput: {results['throughput_ops_per_sec']:.2f} ops/sec")