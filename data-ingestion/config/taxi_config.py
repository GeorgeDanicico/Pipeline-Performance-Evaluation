from typing import ClassVar, List, Dict, Any

class TaxiTripConfig:
    # list of all fields
    FIELDS: ClassVar[List[str]] = [
        "VendorID",
        "RatecodeID",
        "payment_type",
        "PULocationID",
        "DOLocationID",
        "passenger_count",
        "trip_distance",
        "fare_amount"
    ]
    # pandas‐friendly schema: map each field to its desired dtype
    SCHEMA: ClassVar[Dict[str, Any]] = {
        "VendorID": int,
        "RatecodeID": int,
        "payment_type": int,
        "PULocationID": int,
        "DOLocationID": int,
        "passenger_count": int,
        "trip_distance": float,
        "fare_amount": float
    }