import uuid
import logging
from typing import Optional

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
            scope_name: Optional[str] = None,
            collection_name: Optional[str] = None,
            batch_size: int = 1000,
            key_prefix: str = "trip"
    ):
        self.cluster = Cluster(
            connection_string,
            ClusterOptions(PasswordAuthenticator(username, password))
        )
        bucket = self.cluster.bucket(bucket_name)
        if scope_name and collection_name:
            self.collection = bucket.scope(scope_name).collection(collection_name)
        else:
            self.collection = bucket.default_collection()

        self.batch_size = batch_size
        self.key_prefix = key_prefix

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
                try:
                    self.collection.upsert(key, rec)
                except CouchbaseException as e:
                    logger.error(f"Couchbase upsert failed for {key}: {e}")