package it.unitn.emvscheduling.greedy.domain;

public class ExecutionSettings {
    public Integer numberOfWorkers;
    public Integer maxSolverExecutionTimeInSeconds;
    public Integer randomSeed;

    public ExecutionSettings(Integer numberOfWorkers, Integer maxSolverExecutionTimeInSeconds, Integer randomSeed) {
        this.numberOfWorkers = numberOfWorkers;
        this.maxSolverExecutionTimeInSeconds = maxSolverExecutionTimeInSeconds;
        this.randomSeed = randomSeed;
    }
}
