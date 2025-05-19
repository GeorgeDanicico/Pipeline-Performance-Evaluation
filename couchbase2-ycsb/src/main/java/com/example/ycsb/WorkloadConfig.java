package com.example.ycsb;

public class WorkloadConfig {
    private final double readProportion;
    private final double updateProportion;
    private final double insertProportion;
    private final double scanProportion;

    private WorkloadConfig(double readProportion, double updateProportion, double insertProportion, double scanProportion) {
        this.readProportion = readProportion;
        this.updateProportion = updateProportion;
        this.insertProportion = insertProportion;
        this.scanProportion = scanProportion;
    }

    public static WorkloadConfig createDefault(WorkloadType type) {
        switch (type) {
            case READ_HEAVY:
                return new WorkloadConfig(0.5, 0.25, 0.15, 0.1);
            case WRITE_HEAVY:
                return new WorkloadConfig(0.25, 0.35, 0.35, 0.05);
            case READ_ONLY:
                return new WorkloadConfig(0.95, 0.0, 0.0, 0.05);
            case WRITE_ONLY:
                return new WorkloadConfig(0.0, 0.5, 0.5, 0.0);
            case MIXED:
                return new WorkloadConfig(0.4, 0.3, 0.2, 0.1);
            default:
                return new WorkloadConfig(0.5, 0.25, 0.15, 0.1);
        }
    }

    public static WorkloadConfig createCustom(double readProportion, double updateProportion, 
                                            double insertProportion, double scanProportion) {
        return new WorkloadConfig(readProportion, updateProportion, insertProportion, scanProportion);
    }

    public double getReadProportion() {
        return readProportion;
    }

    public double getUpdateProportion() {
        return updateProportion;
    }

    public double getInsertProportion() {
        return insertProportion;
    }

    public double getScanProportion() {
        return scanProportion;
    }
} 