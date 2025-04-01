package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.Comparator;

/**
 * @see Strategy MCCF, //Most Conflicting Count First (in terms of number of conflicting transactions)
 */
public class ProcessMCCFComparator implements Comparator<Process> {
    public static final ProcessMCCFComparator INSTANCE = new ProcessMCCFComparator();

    private ProcessMCCFComparator() {
    }

    @Override
    public int compare(Process a, Process b) {
        return Integer.compare(b.conflictingProcesses.size(), a.conflictingProcesses.size()); // Descending order
    }
}
