import glob
import logging
import os
import time
from typing import Dict, Any

from mongo_batch_writer import MongoBatchWriter
from parquet_chunk_reader import ParquetChunkReader
from resource_monitor import ResourceMonitor

logger = logging.getLogger(__name__)

class DataPipeline:
    def __init__(
            self,
            input_dir: str,
            reader_kwargs: Dict[str, Any],
            writer_kwargs: Dict[str, Any]
    ):
        self.input_dir = input_dir
        self.reader_kwargs = reader_kwargs
        self.writer_kwargs = writer_kwargs

    def run(self):
        # Set up logging
        logging.basicConfig(
            level=logging.INFO,
            format="%(asctime)s %(levelname)s %(message)s"
        )

        # Start resource monitor
        monitor = ResourceMonitor(interval=5.0)
        monitor.start()

        total_start = time.perf_counter()
        parquet_files = glob.glob(os.path.join(self.input_dir, "*.parquet"))

        logger.info(f"Found {len(parquet_files)} files to process.")

        writer = MongoBatchWriter(**self.writer_kwargs)

        for file_path in parquet_files:
            fname = os.path.basename(file_path)
            file_start = time.perf_counter()
            logger.info(f"→ Starting: {fname}")

            reader = ParquetChunkReader(file_path=file_path, **self.reader_kwargs)
            for chunk in reader:
                writer.write(chunk)

            file_end = time.perf_counter()
            logger.info(f"← Finished: {fname} in {file_end - file_start:.2f}s")

        total_end = time.perf_counter()
        logger.info(f"All done in {total_end - total_start:.2f}s")

        monitor.stop()