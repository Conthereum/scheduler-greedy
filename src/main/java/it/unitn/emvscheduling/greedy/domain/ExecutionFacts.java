package it.unitn.emvscheduling.greedy.domain;

import it.unitn.emvscheduling.greedy.domain.solver.ProcessLCCFComparator;
import it.unitn.emvscheduling.greedy.domain.solver.ProcessLCDFComparator;
import it.unitn.emvscheduling.greedy.domain.solver.ProcessMCCFComparator;
import it.unitn.emvscheduling.greedy.domain.solver.ProcessMCDFComparator;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecutionFacts {
    //facts
    public List<Computer> computers;
    public List<Process> processes;
    public Integer conflictPercentage;
    public List<UnorderedPair> conflictingProcesses;
    public Integer timeWeight;//out of 100
    //proposer or attestor: for attestors do not change the order of conflicting transactions
    public Boolean isProposerMode = true;

    //dependant variable
//    private Integer costWeight;
//    private Integer score;

    /**
     * Note: relies on incremental id from 0
     *
     * @param processId
     * @return
     */
    public Process getProcess(Integer processId) {
        return processes.get(processId);
    }

    @Override
    public String toString() {
        return "ExecutionFacts{" +
                "computers=" + computers +
                ", processes=" + processes +
                ", conflictingProcesses=" + conflictingProcesses +
                ", timeWeight=" + timeWeight +
                ", isProposerMode=" + isProposerMode +
                '}';
    }

    /**
     * a method to do some preparations on the facts
     */
    public void setTotalConflictDurationForEachProcess() {
        for (Process process : processes) {
            process.setTotalConflictDuration();
        }
    }

    public void sortProcesses(Strategy.ProcessSortType processSortType) {
        if (processSortType.equals(processSortType.MCDF)) {//Most Conflicting Duration First
            this.setTotalConflictDurationForEachProcess();
            Collections.sort(processes, ProcessMCDFComparator.INSTANCE);
        } else if (processSortType.equals(processSortType.LCDF)) {//Least Conflicting Duration First
            this.setTotalConflictDurationForEachProcess();
            Collections.sort(processes, ProcessLCDFComparator.INSTANCE);
        } else if (processSortType.equals(processSortType.MCCF)) {//Most Conflicting Count First
            Collections.sort(processes, ProcessMCCFComparator.INSTANCE);
        } else if (processSortType.equals(processSortType.LCCF)) {//Least Conflicting Count First
            Collections.sort(processes, ProcessLCCFComparator.INSTANCE);
        } else if (processSortType.equals(processSortType.FIFO)) {//First In First Out
            //do not change the order of processes
        }
    }

    /**
     * this sort method will call if the execution is for a validator and bring all the conflicting processes
     * in their original order in the beginning of the list and then bring the non conflicting transactions in their
     * original order
     */
    public void moveConflictingTransactionsToFront() {
        List<Process> conflictingProcesses = new ArrayList<>(processes.size());
        List<Process> nonConflictingProcesses = new ArrayList<>(processes.size());
        for (Process p : processes) {
            if (p.conflictingProcesses.size() != 0) {
                conflictingProcesses.add(p);
            } else {
                nonConflictingProcesses.add(p);
            }
        }
        //Collections.sort(nonConflictingProcesses, comparator);
        processes.clear();
        processes.addAll(conflictingProcesses);
        processes.addAll(nonConflictingProcesses);
    }
}
