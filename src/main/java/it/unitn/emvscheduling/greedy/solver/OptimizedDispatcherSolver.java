package it.unitn.emvscheduling.greedy.solver;

import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.domain.solver.ComputerPlan;
import it.unitn.emvscheduling.greedy.domain.solver.ComputingPlan;

import java.util.*;
import java.util.stream.Collectors;

public class OptimizedDispatcherSolver implements Solver {
    public ExecutionOutput solve(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        if (facts.isProposerMode) {
            return solveProposer(facts, settings, strategy);
        } else {
            return solveAttestor(facts, settings, strategy);
        }
    }

    private ExecutionOutput solveProposer(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        long startTime = System.nanoTime();
        ExecutionOutput output = new ExecutionOutput();
        ComputingPlan plan = new ComputingPlan(facts);

        // Sort processes by execution time (longest first)
        facts.sortProcesses(strategy.processSortType);

        // Phase 1: Loose Assignment - Try to assign processes without conflicts
        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            for (int round = 0; round < strategy.looseReviewRound; round++) {
                int unassignedProcesses = 0;
                for (Process process : facts.processes) {
                    if (process.computer == null) {
                        boolean couldAssign = false;
                        if (process.conflictingProcesses.isEmpty()) {
                            couldAssign = assignLoosely(process, plan);
                        } else {
                            boolean allPreviousConflictsAssigned = true;
                            for (Process conflict : process.conflictingProcesses) {
                                if (conflict.processId < process.processId && conflict.computer == null) {
                                    allPreviousConflictsAssigned = false;
                                    break;
                                }
                            }
                            if (allPreviousConflictsAssigned) {
                                couldAssign = assignLoosely(process, plan);
                            }
                        }
                        if (!couldAssign) {
                            unassignedProcesses++;
                        }
                    }
                }
                if (unassignedProcesses == 0) break;
            }

            // Phase 2: Strict Assignment for remaining processes
            for (Process process : facts.processes) {
                if (process.computer == null) {
                    assignStrictly(process, plan);
                }
            }
        } else {
            // If STRICT mode, assign all processes strictly in order
            for (Process process : facts.processes) {
                assignStrictly(process, plan);
            }
        }

        output.horizon = facts.processes.stream().mapToLong(p -> p.executionTime).sum();
        output.scheduleMakespan = plan.getScheduleMakespan();
        output.wallTimeInMs = (System.nanoTime() - startTime) / 1_000_000.0;
        output.resultStatus = "possible";
        output.processes = facts.processes;

        return output;
    }

    private boolean assignLoosely(Process process, ComputingPlan plan) {
        // Find the least loaded core
        ComputerPlan selectedCore = null;
        int firstFreeTime = Integer.MAX_VALUE;
        
        for (ComputerPlan core : plan.computerPlanList) {
            if (core.firstFreeTime < firstFreeTime) {
                selectedCore = core;
                firstFreeTime = core.firstFreeTime;
            }
        }
        
        if (selectedCore == null) {
            throw new RuntimeException("Could not find computer for process " + process.processId);
        }

        // Check if there are any conflicts at the start time
        int startTime = firstFreeTime;
        for (Process conflict : process.conflictingProcesses) {
            if (conflict.computer != null && !conflict.computer.equals(selectedCore.computer)) {
                if (startTime >= conflict.startTime && startTime <= conflict.endTime) {
                    return false;
                }
            }
        }

        // If no conflicts, assign the process
        process.computer = selectedCore.computer;
        process.startTime = startTime;
        process.endTime = startTime + process.executionTime;
        selectedCore.processList.add(process);
        selectedCore.firstFreeTime = process.endTime;
        selectedCore.busyTimeSum += process.executionTime;
        return true;
    }

    private void assignStrictly(Process process, ComputingPlan plan) {
        // Find the least loaded core
        ComputerPlan selectedCore = null;
        int firstFreeTime = Integer.MAX_VALUE;
        
        for (ComputerPlan core : plan.computerPlanList) {
            if (core.firstFreeTime < firstFreeTime) {
                selectedCore = core;
                firstFreeTime = core.firstFreeTime;
            }
        }
        
        if (selectedCore == null) {
            throw new RuntimeException("Could not find computer for process " + process.processId);
        }

        // Calculate start time considering conflicts
        int startTime = firstFreeTime;
        for (Process conflict : process.conflictingProcesses) {
            if (conflict.computer != null) {
                startTime = Math.max(startTime, conflict.endTime);
            }
        }

        // Assign the process with the calculated start time
        process.computer = selectedCore.computer;
        process.startTime = startTime;
        process.endTime = startTime + process.executionTime;
        process.idleDuration = startTime - firstFreeTime;
        selectedCore.processList.add(process);
        selectedCore.firstFreeTime = process.endTime;
        selectedCore.busyTimeSum += process.executionTime;
        selectedCore.idleTimeSum += process.idleDuration;
    }

    private ExecutionOutput solveAttestor(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        long startTime = System.nanoTime();
        ExecutionOutput output = new ExecutionOutput();
        ComputingPlan plan = new ComputingPlan(facts);

        // Move conflicting transactions to front
        facts.moveConflictingTransactionsToFront();

        // Phase 1: Loose Assignment with order preservation
        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            for (int round = 0; round < strategy.looseReviewRound; round++) {
                int unassignedProcesses = 0;
                for (Process process : facts.processes) {
                    if (process.computer == null) {
                        boolean couldAssign = false;
                        if (process.conflictingProcesses.isEmpty()) {
                            couldAssign = assignLoosely(process, plan);
                        } else {
                            boolean allPreviousConflictsAssigned = true;
                            for (Process conflict : process.conflictingProcesses) {
                                if (conflict.processId < process.processId && conflict.computer == null) {
                                    allPreviousConflictsAssigned = false;
                                    break;
                                }
                            }
                            if (allPreviousConflictsAssigned) {
                                couldAssign = assignLoosely(process, plan);
                            }
                        }
                        if (!couldAssign) {
                            unassignedProcesses++;
                        }
                    }
                }
                if (unassignedProcesses == 0) break;
            }

            // Phase 2: Strict Assignment with order preservation
            for (Process process : facts.processes) {
                if (process.computer == null) {
                    assignStrictly(process, plan);
                }
            }
        } else {
            // If STRICT mode or no conflicts, just assign all processes strictly
            for (Process process : facts.processes) {
                assignStrictly(process, plan);
            }
        }

        output.horizon = facts.processes.stream().mapToLong(p -> p.executionTime).sum();
        output.scheduleMakespan = plan.getScheduleMakespan();
        output.wallTimeInMs = (System.nanoTime() - startTime) / 1_000_000.0;
        output.resultStatus = "possible";
        output.processes = facts.processes;

        return output;
    }
} 