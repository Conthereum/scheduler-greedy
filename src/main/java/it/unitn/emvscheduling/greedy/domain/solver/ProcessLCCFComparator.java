package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.Comparator;

/**
 * @see Strategy LCCF, //Least Conflicting Count First (in terms of number of conflicting transactions)
 */
public class ProcessLCCFComparator implements Comparator<Process> {
    public static final ProcessLCCFComparator INSTANCE = new ProcessLCCFComparator();

    private ProcessLCCFComparator() {
    }

    @Override
    public int compare(Process a, Process b) {
        return Integer.compare(a.conflictingProcesses.size(), b.conflictingProcesses.size()); // Ascending order
    }
}
