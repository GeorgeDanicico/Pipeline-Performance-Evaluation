from pipeline.data_pipeline import DataPipeline
from config.taxi_config import TaxiTripConfig

if __name__ == "__main__":

    # writer_kwargs = {
    #     "mongo_uri": "mongodb://localhost:27017/",
    #     "db_name": "census",
    #     "collection_name": "nyc_taxi",
    #     "batch_size": 5000,
    # }
    writer_kwargs = {
        "connection_string": "couchbase://127.0.0.1",
        "username": "admin",
        "password": "parola03",
        "bucket_name": "census", # optional
        "batch_size": 5000,
        "key_prefix": "trip"
    }

    pipeline = DataPipeline(
        input_dir="../data/",
        reader_kwargs={
            "chunk_size": 5000,
            "columns": TaxiTripConfig.FIELDS,
            "schema": TaxiTripConfig.SCHEMA,
        },
        writer_kwargs=writer_kwargs
    )
    pipeline.run()