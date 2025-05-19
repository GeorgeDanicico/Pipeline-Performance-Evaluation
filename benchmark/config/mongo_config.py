from typing import Dict, Any
from pymongo import MongoClient

class MongoConfig:
    def __init__(self, config: Dict[str, Any]):
        self.host = config.get('MONGODB_HOST', 'localhost')
        self.port = int(config.get('MONGODB_PORT', 27017))
        self.database = config.get('MONGODB_DATABASE', 'benchmark')
        self.collection = config.get('MONGODB_COLLECTION', 'test_collection')
        self.client = None
        self.db = None
        self.collection_obj = None

    def connect(self) -> None:
        """Initialize MongoDB connection"""
        self.client = MongoClient(f'mongodb://{self.host}:{self.port}')
        self.db = self.client[self.database]
        self.collection_obj = self.db[self.collection]

    def disconnect(self) -> None:
        """Close MongoDB connection"""
        if self.client:
            self.client.close()
            self.client = None
            self.db = None
            self.collection_obj = None 