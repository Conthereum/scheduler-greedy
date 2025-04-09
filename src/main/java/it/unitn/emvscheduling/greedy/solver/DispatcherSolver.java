package it.unitn.emvscheduling.greedy.solver;

import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.domain.solver.ComputingPlan;

public class DispatcherSolver implements Solver {
    public ExecutionOutput solve(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        if (facts.isProposerMode) {
            return solveProposer(facts, settings, strategy);
        } else {
            return solveAttestor(facts, settings, strategy);
        }
    }

    private ExecutionOutput solveProposer(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {

        long startSystemTimeNanoSecond = System.nanoTime();

        ExecutionOutput solverOutput = new ExecutionOutput();
        // Computes horizon dynamically as the sum of all durations.
//        long horizon = facts.processes.stream().mapToLong(p -> p.executionTime).sum();
//        solverOutput.horizon = horizon;

        long horizon = 0;
        //ComputingPlan as a singleton class
        ComputingPlan computingPlan = new ComputingPlan(facts);

        facts.sortProcesses(strategy.processSortType);
        Integer heuristicLooseReviewRound = strategy.looseReviewRound;
        /*Integer heuristicLooseReviewRound = LooseReviewRoundCalculator.getValue(facts.processes.size(),
                facts.conflictPercentage, facts.computers.size(),
                LooseReviewRoundCalculator.HeuristicType.getByValue(5));
        strategy.looseReviewRound = heuristicLooseReviewRound;*/
//        Integer heuristicLooseReviewRound = 100000;

        //strategy
        // .looseReviewRound;
        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            for (int round = 0; round <heuristicLooseReviewRound ; round++) {
                int unassignedProcesses = 0;
                for (Process process : facts.processes) {
                    if (round == 0) {
                        horizon += process.executionTime;
                    } else {
//                        computingPlan.sortUnassignedProcesses(strategy.processSortType);
                    }
                    if (process.computer == null) {// if the process is not assigned yet
                        boolean couldAssign = computingPlan.assignLoosely(process);
                        if (couldAssign == false)
                            unassignedProcesses++;
                    }
                }
                if (unassignedProcesses == 0)
                    break; // do not go for next round if there is no unassigned process
            }
            //after looseRound times attempt of assigning loosely, assign the remaining strictly
            for (Process process : facts.processes) {
                if (process.computer == null) {// if the process is not assigned yet
                    computingPlan.assignStrictly(process);
                }
            }
        } else if (strategy.assignmentType.equals(Strategy.AssignmentType.STRICT) || (facts.conflictingProcesses == null || facts.conflictingProcesses.size() == 0)) {
            /* accommodating the next process in the first available processor and wait to solve any conflicts */
            for (Process process : facts.processes) {
                horizon += process.executionTime;
                computingPlan.assignStrictly(process);
            }
        } else {
            throw new RuntimeException("not supported assignment type of: " + strategy.assignmentType);
        }
        solverOutput.horizon = horizon;
        solverOutput.scheduleMakespan = computingPlan.getScheduleMakespan();
        long endSystemTimeNanoSecond = System.nanoTime();
        solverOutput.wallTimeInMs = (endSystemTimeNanoSecond - startSystemTimeNanoSecond) / 1000000.0;
        solverOutput.resultStatus = "possible";
        solverOutput.processes = facts.processes;
        //System.out.println(computingPlan);
        return solverOutput;
        //todo you can implement the timeout based on the it.unitn.emvscheduling.declarative.data from settings
    }


    private ExecutionOutput solveAttestor(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {

        long startSystemTimeNanoSecond = System.nanoTime();

        ExecutionOutput solverOutput = new ExecutionOutput();
        // Computes horizon dynamically as the sum of all durations.
        long horizon = 0;

        //ComputingPlan as a singleton class
        ComputingPlan computingPlan = new ComputingPlan(facts);

//        facts.sortProcesses(strategy.processSortType);
        facts.moveConflictingTransactionsToFront();
        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            for (Process process : facts.processes) {
                horizon += process.executionTime;
            }
            for (int round = 0; round < strategy.looseReviewRound; round++) {
                int unassignedProcesses = 0;
                for (Process process : facts.processes) {
                    if (process.computer == null) {// if the process is not assigned yet
                        Boolean couldAssign;
                        if (process.conflictingProcesses.size() == 0) {// If does not have any conflict try to assign it
                            /*does not have conflict */
                            couldAssign = computingPlan.assignLoosely(process);
                        } else { //if this transaction has any conflict, their beforehands have been executed and now its order
                            /*
                             * the process has conflict
                             */
                            Boolean allPreviousConflictAreAssigned = true;
                            for (Process processC : process.conflictingProcesses) {
                                if (processC.processId < process.processId && processC.computer == null) {
                                    allPreviousConflictAreAssigned = false;
                                    break;
                                }
                            }
                            /*boolean allPreviousConflictAreAssigned = process.conflictingProcesses.stream()
                                    .allMatch(p -> p.processId >= process.processId || p.computer != null);*/ //slower
                            if (allPreviousConflictAreAssigned) {
                                couldAssign = computingPlan.assignLoosely(process);
                            } else {
                                couldAssign = false;
                            }
                        }

                        if (couldAssign == false) {
                            unassignedProcesses++;
                        }
                    }
                }
                if (unassignedProcesses == 0)
                    break; // do not go for next round if there is no unassigned process
            }
            //after looseRound times attempt of assigning loosely, assign the remaining strictly
            for (Process process : facts.processes) {
                if (process.computer == null) {// if the process is not assigned yet
                    computingPlan.assignStrictly(process);
                }
            }
        } else if (strategy.assignmentType.equals(Strategy.AssignmentType.STRICT) || (facts.conflictingProcesses == null || facts.conflictingProcesses.size() == 0)) {
            /* accommodating the next process in the first available processor and wait to solve any conflicts */
            for (Process process : facts.processes) {
                horizon += process.executionTime;
                computingPlan.assignStrictly(process);
            }
        } else {
            throw new RuntimeException("not supported assignment type of: " + strategy.assignmentType);
        }
        solverOutput.horizon = horizon;
        solverOutput.scheduleMakespan = computingPlan.getScheduleMakespan();
        long endSystemTimeNanoSecond = System.nanoTime();
        solverOutput.wallTimeInMs = (endSystemTimeNanoSecond - startSystemTimeNanoSecond) / 1_000_000.0;
        solverOutput.resultStatus = "possible";
        solverOutput.processes = facts.processes;
        //System.out.println(computingPlan);
        return solverOutput;
        //todo you can implement the timeout based on the it.unitn.emvscheduling.declarative.data from settings
    }
}
