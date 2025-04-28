from typing import ClassVar, List, Dict, Any

class TaxiTripConfig:
    # list of all fields
    FIELDS: ClassVar[List[str]] = [
        "VendorID",
        "tpep_pickup_datetime",
        "tpep_dropoff_datetime",
        "passenger_count",
        "trip_distance",
        "RatecodeID",
        "store_and_fwd_flag",
        "PULocationID",
        "DOLocationID",
        "payment_type",
        "fare_amount",
        "extra",
        "mta_tax",
        "tip_amount",
        "tolls_amount",
        "improvement_surcharge",
        "total_amount",
        "congestion_surcharge",
        "airport_fee",
        "cbd_congestion_fee",
    ]

    # pandas‐friendly schema: map each field to its desired dtype
    SCHEMA: ClassVar[Dict[str, Any]] = {
        "VendorID": int,
        "tpep_pickup_datetime": "datetime64[ns]",
        "tpep_dropoff_datetime": "datetime64[ns]",
        "passenger_count": int,
        "trip_distance": float,
        "RatecodeID": int,
        "store_and_fwd_flag": str,
        "PULocationID": int,
        "DOLocationID": int,
        "payment_type": int,
        "fare_amount": float,
        "extra": float,
        "mta_tax": float,
        "tip_amount": float,
        "tolls_amount": float,
        "improvement_surcharge": float,
        "total_amount": float,
        "congestion_surcharge": float,
        "airport_fee": float,
        "cbd_congestion_fee": float,
    }