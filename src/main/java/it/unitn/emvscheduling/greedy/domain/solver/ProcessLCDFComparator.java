package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.util.Comparator;

/**
 * @see Strategy LCDF //Least Conflicting Duration First (in terms of overall conflict duration with
 * other transactions)
 */
public class ProcessLCDFComparator implements Comparator<Process> {

    public static final ProcessLCDFComparator INSTANCE = new ProcessLCDFComparator();

    private ProcessLCDFComparator() {
    }

    @Override
    public int compare(Process a, Process b) {
        return Long.compare(a.totalConflictDuration, b.totalConflictDuration); // Ascending order
    }
}
