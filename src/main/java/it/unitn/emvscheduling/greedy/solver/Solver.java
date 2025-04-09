package it.unitn.emvscheduling.greedy.solver;

import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;

public interface Solver {
    ExecutionOutput solve(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy);
}
