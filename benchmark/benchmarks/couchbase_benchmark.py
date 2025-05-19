from typing import List, Dict, Any
import time
from config.couchbase_config import CouchbaseConfig
from benchmarks.base_benchmark import BaseBenchmark

class CouchbaseBenchmark(BaseBenchmark):
    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        self.db_config = CouchbaseConfig(config)

    def initialize(self) -> None:
        """Initialize Couchbase connection"""
        self.db_config.connect()

    def perform_operation(self, batch: List[Dict[str, Any]]) -> float:
        """Perform batch upsert operation"""
        start_time = time.time()
        
        # Perform upsert operations
        for doc in batch:
            doc_id = f"doc_{doc['id']}"
            self.db_config.collection.upsert(doc_id, doc)
            
        return time.time() - start_time

    def cleanup(self) -> None:
        """Cleanup Couchbase connection"""
        self.db_config.disconnect() 