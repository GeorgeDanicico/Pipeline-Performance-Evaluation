import os
from dotenv import load_dotenv
from benchmarks.mongo_benchmark import MongoBenchmark
from benchmarks.couchbase_benchmark import CouchbaseBenchmark

def load_config():
    """Load configuration from .env file"""
    load_dotenv()
    return {
        'MONGODB_HOST': os.getenv('MONGODB_HOST', 'localhost'),
        'MONGODB_PORT': os.getenv('MONGODB_PORT', '27017'),
        'MONGODB_DATABASE': os.getenv('MONGODB_DATABASE', 'benchmark'),
        'MONGODB_COLLECTION': os.getenv('MONGODB_COLLECTION', 'test_collection'),
        
        'COUCHBASE_HOST': os.getenv('COUCHBASE_HOST', 'localhost'),
        'COUCHBASE_PORT': os.getenv('COUCHBASE_PORT', '8091'),
        'COUCHBASE_BUCKET': os.getenv('COUCHBASE_BUCKET', 'benchmark'),
        'COUCHBASE_USERNAME': os.getenv('COUCHBASE_USERNAME', 'Administrator'),
        'COUCHBASE_PASSWORD': os.getenv('COUCHBASE_PASSWORD', 'password'),
        
        'BENCHMARK_ITERATIONS': os.getenv('BENCHMARK_ITERATIONS', '1000'),
        'BENCHMARK_BATCH_SIZE': os.getenv('BENCHMARK_BATCH_SIZE', '100'),
        'BENCHMARK_THREADS': os.getenv('BENCHMARK_THREADS', '4')
    }

def main():
    """Run both MongoDB and Couchbase benchmarks"""
    config = load_config()
    
    # Run MongoDB benchmark
    mongo_benchmark = MongoBenchmark(config)
    mongo_benchmark.run()
    
    # Run Couchbase benchmark
    couchbase_benchmark = CouchbaseBenchmark(config)
    couchbase_benchmark.run()

if __name__ == '__main__':
    main() 