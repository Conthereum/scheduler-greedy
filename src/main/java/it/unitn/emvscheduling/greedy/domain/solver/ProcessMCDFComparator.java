package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.Comparator;

/**
 * @see Strategy MCCF //Most Conflicting Duration First (in terms of duration of conflicting transactions)
 */
public class ProcessMCDFComparator implements Comparator<Process> {
    public static final ProcessMCDFComparator INSTANCE = new ProcessMCDFComparator();

    private ProcessMCDFComparator() {
    }

    @Override
    public int compare(Process a, Process b) {
        return Long.compare(b.totalConflictDuration, a.totalConflictDuration); // Descending order
    }
}
