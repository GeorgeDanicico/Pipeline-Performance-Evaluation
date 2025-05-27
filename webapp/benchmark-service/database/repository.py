from sqlalchemy.orm import Session
from . import models
from typing import List, Optional

class BenchmarkRepository:
    def __init__(self, db: Session):
        self.db = db

    def create_benchmark_result(self, result: models.BenchmarkResult) -> models.BenchmarkResult:
        self.db.add(result)
        self.db.commit()
        self.db.refresh(result)
        return result

    def get_all_benchmark_results(self) -> List[models.BenchmarkResult]:
        return self.db.query(models.BenchmarkResult).all()

    def get_benchmark_result_by_id(self, result_id: int) -> Optional[models.BenchmarkResult]:
        return self.db.query(models.BenchmarkResult).filter(models.BenchmarkResult.id == result_id).first() 