from data_pipeline import DataPipeline
from mongo_batch_writer import MongoBatchWriter
from parquet_chunk_reader import ParquetChunkReader
from taxi_config import TaxiTripConfig

if __name__ == "__main__":
    pipeline = DataPipeline(
        input_dir="data/",
        reader_kwargs={
            "chunk_size": 1000,
            "columns": TaxiTripConfig.FIELDS,
            "schema": TaxiTripConfig.SCHEMA,
        },
        writer_kwargs={
            "mongo_uri": "mongodb://localhost:27017/",
            "db_name": "census",
            "collection_name": "nyc_taxi",
            "batch_size": 1000,
        }
    )
    pipeline.run()