# Pipeline Performance Evaluation

This project evaluates the performance of data ingestion pipelines for MongoDB and Couchbase using Apache Spark.

## Project Structure

```
Pipeline-Performance-Evaluation/
├── pom.xml                           # Parent Maven POM
├── docker-compose.yml                # Docker Compose configuration
├── data/                             # Data directory
├── .local_docker_run/                # Docker data persistence
├── couchbase/                        # Couchbase Docker configuration
├── mongo-ingestion/                  # MongoDB ingestion module
│   ├── pom.xml                       # MongoDB module POM
│   └── src/                          # Source code
│       └── main/java/com/ubb/master/
│           ├── Main.java             # MongoDB ingestion entry point
│           ├── AbstractDatabaseJob.java  # Abstract job class
│           └── MongoDBJob.java       # MongoDB-specific job implementation
└── couchbase-ingestion/              # Couchbase ingestion module
    ├── pom.xml                       # Couchbase module POM
    └── src/                          # Source code
        └── main/java/com/ubb/master/
            ├── Main.java             # Couchbase ingestion entry point
            ├── AbstractDatabaseJob.java  # Abstract job class
            └── CouchbaseJob.java     # Couchbase-specific job implementation
```

## Building the Project

To build the entire project:

```bash
mvn clean package
```

To build a specific module:

```bash
# Build MongoDB ingestion module
cd mongo-ingestion
mvn clean package

# Build Couchbase ingestion module
cd couchbase-ingestion
mvn clean package
```

## Running the Ingestion Pipelines

### MongoDB Ingestion

```bash
cd mongo-ingestion
java -cp target/mongo-ingestion-1.0-SNAPSHOT.jar com.ubb.master.CouchbaseMain
```

### Couchbase Ingestion

```bash
cd couchbase-ingestion
java -cp target/couchbase-ingestion-1.0-SNAPSHOT.jar com.ubb.master.CouchbaseMain
```

## Docker Setup

To start the MongoDB and Couchbase containers:

```bash
docker-compose up -d
```

To stop the containers:

```bash
docker-compose down
```
