package it.unitn.emvscheduling.greedy.application;

import it.unitn.emvscheduling.greedy.data.DataGenerator;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.solver.DispatcherSolver;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.io.IOException;

public class ArgsBasedExecutor {
    public static void executeUsingArgs(int randomSeed, int numberOfWorkers, int maxSolverExecutionTimeInSeconds
            , int processCount, int processExecutionTimeMin, int processExecutionTimeMax, int computerCount, int conflictPercentage, int timeWeight) throws IOException {
        DispatcherSolver solver = new DispatcherSolver();
        ExecutionFacts facts = DataGenerator.getBenchmark(randomSeed, processCount, processExecutionTimeMin,
                processExecutionTimeMax, computerCount, conflictPercentage, timeWeight);

        ExecutionSettings settings = new ExecutionSettings(numberOfWorkers, maxSolverExecutionTimeInSeconds, randomSeed);

        // Solve the problem
        Strategy strategy = new Strategy(Strategy.AssignmentType.LOOSE, Strategy.ProcessSortType.MCDF, 9);
        ExecutionOutput solverOutput = solver.solve(facts, settings, strategy);
        String line = FileBasedExecutor.getOutputLine(0, 0, randomSeed, numberOfWorkers,
                maxSolverExecutionTimeInSeconds,
                processCount, processExecutionTimeMin, processExecutionTimeMax, computerCount, conflictPercentage,
                timeWeight, strategy.processSortType.getValue(), strategy.looseReviewRound, solverOutput.wallTimeInMs,
                solverOutput.scheduleMakespan,

                solverOutput.horizon, solverOutput.resultStatus);
        FileBasedExecutor.writeInAccumulativeOutFileWithTimestamp(line);
        System.out.println("Result: " + line);
    }
}
