from typing import Dict, Any
from couchbase.cluster import Cluster, ClusterOptions
from couchbase.auth import PasswordAuthenticator

class CouchbaseConfig:
    def __init__(self, config: Dict[str, Any]):
        self.host = config.get('COUCHBASE_HOST', 'localhost')
        self.port = int(config.get('COUCHBASE_PORT', 8091))
        self.bucket = config.get('COUCHBASE_BUCKET', 'benchmark')
        self.username = config.get('COUCHBASE_USERNAME', 'Administrator')
        self.password = config.get('COUCHBASE_PASSWORD', 'password')
        self.cluster = None
        self.bucket_obj = None
        self.collection = None

    def connect(self) -> None:
        """Initialize Couchbase connection"""
        connection_string = f'couchbase://{self.host}:{self.port}'
        auth = PasswordAuthenticator(self.username, self.password)
        options = ClusterOptions(auth)
        
        self.cluster = Cluster(connection_string, options)
        self.bucket_obj = self.cluster.bucket(self.bucket)
        self.collection = self.bucket_obj.default_collection()

    def disconnect(self) -> None:
        """Close Couchbase connection"""
        if self.cluster:
            self.cluster.disconnect()
            self.cluster = None
            self.bucket_obj = None
            self.collection = None 