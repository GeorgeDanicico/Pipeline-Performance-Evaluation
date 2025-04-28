import logging

import pandas as pd
from pymongo import MongoClient
from pymongo.errors import BulkWriteError

logger = logging.getLogger(__name__)

class MongoBatchWriter:
    """
    Writes pandas DataFrame chunks to MongoDB in batches.
    """
    def __init__(
            self,
            mongo_uri: str,
            db_name: str,
            collection_name: str,
            batch_size: int = 1000
    ):
        self.client = MongoClient(mongo_uri)
        self.collection = self.client[db_name][collection_name]
        self.batch_size = batch_size

    def write(self, df: pd.DataFrame):
        """
        Converts DataFrame to dict records and inserts them in batches.
        """
        records = df.to_dict(orient="records")
        for i in range(0, len(records), self.batch_size):
            batch = records[i : i + self.batch_size]
            if not batch:
                continue
            try:
                self.collection.insert_many(batch, ordered=False)
            except BulkWriteError as bwe:
                # handle duplicate key or other bulk errors gracefully
                logger.error("Bulk write error:", bwe.details)

