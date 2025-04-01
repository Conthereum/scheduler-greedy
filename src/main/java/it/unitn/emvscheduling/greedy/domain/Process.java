package it.unitn.emvscheduling.greedy.domain;

import java.util.ArrayList;
import java.util.List;

public class Process {
    public int processId;
    public int executionTime; // facts; in millisecond

    //calculate based on facts
    public List<Process> conflictingProcesses = new ArrayList<>();
    //calculated based on facts just in case of some specific solving strategies (LCDF, MCDF)
    public Integer totalConflictDuration;

    //planning variable
    public Computer computer;
    public int startTime;
    public int endTime;

    //calculating based on planning
    public int idleDuration; //equal or greater than zero, default value as zero

    public void setTotalConflictDuration() {
        totalConflictDuration = 0;
        for (Process conflict : this.conflictingProcesses) {
            if (conflict.computer == null) {
                /** consider their conflicting time, just if is still under process */
                totalConflictDuration += Math.min(this.executionTime, conflict.executionTime);
            }
        }
    }
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Process)) return false;
//        Process that = (Process) o;
//        return processId == that.processId;
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(processId);
//    }

    public Process(int processId, int executionTime) {
        this.processId = processId;
        this.executionTime = executionTime;
    }

    public boolean isActiveAt(int askedTime) {
        if (computer != null & startTime <= askedTime && startTime + executionTime >= askedTime)
            return true;
        else
            return false;
    }

    /*//     the time process starts. the base time is starting the block process, in millisecond
    private Integer startTime;

//    Next Phase will be involved
    private static final int GAS_TO_OPERATION_CONSTANT = 10;
    private String hash;
    private int estimatedGas;
    private int estimatedOperationCount;*/
}
