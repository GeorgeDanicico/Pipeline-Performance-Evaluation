from typing import List, Dict, Any
import time
from config.mongo_config import MongoConfig
from benchmarks.base_benchmark import BaseBenchmark

class MongoBenchmark(BaseBenchmark):
    def __init__(self, config: Dict[str, Any]):
        super().__init__(config)
        self.db_config = MongoConfig(config)

    def initialize(self) -> None:
        """Initialize MongoDB connection"""
        self.db_config.connect()

    def perform_operation(self, batch: List[Dict[str, Any]]) -> float:
        """Perform batch insert operation"""
        start_time = time.time()
        self.db_config.collection_obj.insert_many(batch, ordered=False)
        return time.time() - start_time

    def cleanup(self) -> None:
        """Cleanup MongoDB connection"""
        self.db_config.disconnect() 