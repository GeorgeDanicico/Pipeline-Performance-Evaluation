import logging
from typing import List, Optional, Dict, Any

import pandas as pd
import pyarrow.parquet as pq

logger = logging.getLogger(__name__)

class ParquetChunkReader:
    """
    Reads a Parquet file in row batches using PyArrow's streaming API.
    """
    def __init__(
            self,
            file_path: str,
            chunk_size: int,
            columns: Optional[List[str]] = None,
            schema: Optional[Dict[str, Any]] = None
    ):
        self.file_path = file_path
        self.chunk_size = chunk_size
        self.columns = columns
        self.schema = schema or {}
        self._pq_file = pq.ParquetFile(self.file_path)

    def __iter__(self):
        """
        Yields pandas DataFrames of size up to chunk_size.
        """
        print("Reading Parquet file in chunks...")

        for batch in self._pq_file.iter_batches(
                batch_size=self.chunk_size,
                columns=self.columns
        ):
            df = batch.to_pandas()
            # enforce schema if provided
            for col, dtype in self.schema.items():
                if col not in df.columns:
                    continue
                try:
                    df[col] = df[col].astype(dtype)
                except (pd.errors.IntCastingNaNError, ValueError) as e:
                    logger.debug(
                        f"Skipping cast of column '{col}' to {dtype!r}: {e}"
                    )
                    # leave df[col] as‐is
            yield df

        print("Finished reading Parquet file.")