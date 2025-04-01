package it.unitn.emvscheduling.greedy.domain;

import java.util.List;

public class ExecutionOutput {
    public Double wallTimeInMs;
    public Integer scheduleMakespan;
    public Long horizon;
    public String resultStatus;//Optimal, possible, unknown
    public List<Process> processes;

    @Override
    public String toString() {
        return "SolverOutput{" +
                "solverWallTime(ms)=" + wallTimeInMs +
                ", scheduleMakespan=" + scheduleMakespan +
                ", horizon=" + horizon +
                ", solverStatus=" + resultStatus +
                '}';
    }
}
