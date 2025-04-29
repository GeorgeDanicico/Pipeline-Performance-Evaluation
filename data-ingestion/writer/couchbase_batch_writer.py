import datetime
import uuid
import logging
from typing import Optional, Any, Dict

import numpy as np
import pandas as pd
from couchbase.cluster import Cluster, ClusterOptions
from couchbase.auth import PasswordAuthenticator
from couchbase.exceptions import CouchbaseException

logger = logging.getLogger(__name__)

class CouchbaseBatchWriter:
    """
    Writes pandas DataFrame chunks to a Couchbase collection in upsert batches.
    """
    def __init__(
            self,
            connection_string: str,
            username: str,
            password: str,
            bucket_name: str,
            batch_size: int = 5000,
            key_prefix: str = "trip"
    ):
        self.cluster = Cluster(
            connection_string,
            ClusterOptions(PasswordAuthenticator(username, password))
        )
        bucket = self.cluster.bucket(bucket_name)
        self.collection = bucket.default_collection()

        self.batch_size = batch_size
        self.key_prefix = key_prefix

    def _serialize_value(self, v: Any) -> Any:
        """
        Convert pandas/NumPy/datetime types into JSON-serializable ones.
        """
        if isinstance(v, (pd.Timestamp, datetime.date)):
            return v.isoformat()
        if isinstance(v, np.generic):
            return v.item()
        return v

    def _serialize_record(self, rec: Dict[str, Any]) -> Dict[str, Any]:
        """
        Apply _serialize_value to every field in the record.
        """
        return {k: self._serialize_value(v) for k, v in rec.items()}

    def write(self, df: pd.DataFrame):
        """
        Upserts each row in `df` into Couchbase.
        Splits into sub-batches of size `self.batch_size`.
        """
        records = df.to_dict(orient="records")
        for start in range(0, len(records), self.batch_size):
            sub_batch = records[start : start + self.batch_size]
            for rec in sub_batch:
                # generate unique key per doc; swap this out for a real ID field if you have one
                key = f"{self.key_prefix}::{uuid.uuid4().hex}"
                serialized = self._serialize_record(rec)
                try:
                    self.collection.upsert(key, serialized)
                except CouchbaseException as e:
                    logger.error(f"Couchbase upsert failed for {key}: {e}")