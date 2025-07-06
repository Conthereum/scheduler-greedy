package it.unitn.emvscheduling.greedy.solver;

import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.domain.solver.ComputerPlan;
import it.unitn.emvscheduling.greedy.domain.solver.ComputingPlan;

import java.util.*;

public class ScalableDispatcherSolver implements Solver {
    
    // Configuration for improved scheduling
    private static final int MAX_LOOKAHEAD_DEPTH = 3;
    private static final double LOAD_BALANCE_WEIGHT = 0.3;
    private static final double CONFLICT_AVOIDANCE_WEIGHT = 0.7;
    
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
        long horizon = 0;

        // Create computing plan
        ComputingPlan computingPlan = new ComputingPlan(facts);

        // Sort processes based on strategy
        facts.sortProcesses(strategy.processSortType);

        // Pre-process: Calculate conflict density for each process
        calculateConflictDensity(facts);

        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            // Improved loose assignment with better load balancing
            for (int round = 0; round < strategy.looseReviewRound; round++) {
                int unassignedProcesses = 0;
                
                // Sort unassigned processes by priority (conflict density + execution time)
                List<Process> unassignedProcessesList = getUnassignedProcesses(facts.processes);
                if (unassignedProcessesList.isEmpty()) break;
                
                sortUnassignedProcessesByPriority(unassignedProcessesList);
                
                for (Process process : unassignedProcessesList) {
                    horizon += process.executionTime;
                    boolean couldAssign = assignProcessOptimally(computingPlan, process, facts);
                    if (!couldAssign) {
                        unassignedProcesses++;
                    }
                }
                
                if (unassignedProcesses == 0) break;
            }
            
            // Assign remaining processes strictly
            for (Process process : facts.processes) {
                if (process.computer == null) {
                    assignProcessStrictlyOptimized(computingPlan, process, facts);
                }
            }
        } else if (strategy.assignmentType.equals(Strategy.AssignmentType.STRICT) || 
                   (facts.conflictingProcesses == null || facts.conflictingProcesses.size() == 0)) {
            // Improved strict assignment
            for (Process process : facts.processes) {
                horizon += process.executionTime;
                assignProcessStrictlyOptimized(computingPlan, process, facts);
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
        
        return solverOutput;
    }

    private ExecutionOutput solveAttestor(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        long startSystemTimeNanoSecond = System.nanoTime();

        ExecutionOutput solverOutput = new ExecutionOutput();
        long horizon = 0;

        ComputingPlan computingPlan = new ComputingPlan(facts);

        // Move conflicting transactions to front for attestor mode
        facts.moveConflictingTransactionsToFront();
        
        // Pre-process: Calculate conflict density
        calculateConflictDensity(facts);

        if (strategy.assignmentType.equals(Strategy.AssignmentType.LOOSE)) {
            for (Process process : facts.processes) {
                horizon += process.executionTime;
            }
            
            for (int round = 0; round < strategy.looseReviewRound; round++) {
                int unassignedProcesses = 0;
                
                List<Process> unassignedProcessesList = getUnassignedProcesses(facts.processes);
                if (unassignedProcessesList.isEmpty()) break;
                
                for (Process process : unassignedProcessesList) {
                    boolean couldAssign = false;
                    
                    if (process.conflictingProcesses.size() == 0) {
                        // No conflicts, assign optimally
                        couldAssign = assignProcessOptimally(computingPlan, process, facts);
                    } else {
                        // Check if all previous conflicting processes are assigned
                        boolean allPreviousConflictsAssigned = true;
                        for (Process conflictProcess : process.conflictingProcesses) {
                            if (conflictProcess.processId < process.processId && conflictProcess.computer == null) {
                                allPreviousConflictsAssigned = false;
                                break;
                            }
                        }
                        
                        if (allPreviousConflictsAssigned) {
                            couldAssign = assignProcessOptimally(computingPlan, process, facts);
                        }
                    }
                    
                    if (!couldAssign) {
                        unassignedProcesses++;
                    }
                }
                
                if (unassignedProcesses == 0) break;
            }
            
            // Assign remaining processes strictly
            for (Process process : facts.processes) {
                if (process.computer == null) {
                    assignProcessStrictlyOptimized(computingPlan, process, facts);
                }
            }
        } else if (strategy.assignmentType.equals(Strategy.AssignmentType.STRICT) || 
                   (facts.conflictingProcesses == null || facts.conflictingProcesses.size() == 0)) {
            for (Process process : facts.processes) {
                horizon += process.executionTime;
                assignProcessStrictlyOptimized(computingPlan, process, facts);
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
        
        return solverOutput;
    }

    /**
     * Calculate conflict density for each process to prioritize scheduling
     */
    private void calculateConflictDensity(ExecutionFacts facts) {
        for (Process process : facts.processes) {
            process.totalConflictDuration = 0;
            for (Process conflict : process.conflictingProcesses) {
                process.totalConflictDuration += Math.min(process.executionTime, conflict.executionTime);
            }
        }
    }

    /**
     * Get list of unassigned processes
     */
    private List<Process> getUnassignedProcesses(List<Process> processes) {
        List<Process> unassigned = new ArrayList<>();
        for (Process process : processes) {
            if (process.computer == null) {
                unassigned.add(process);
            }
        }
        return unassigned;
    }

    /**
     * Sort unassigned processes by priority (conflict density + execution time)
     */
    private void sortUnassignedProcessesByPriority(List<Process> processes) {
        processes.sort((p1, p2) -> {
            // Higher conflict density and longer execution time get higher priority
            double priority1 = (p1.totalConflictDuration != null ? p1.totalConflictDuration : 0) + p1.executionTime;
            double priority2 = (p2.totalConflictDuration != null ? p2.totalConflictDuration : 0) + p2.executionTime;
            return Double.compare(priority2, priority1); // Descending order
        });
    }

    /**
     * Optimally assign a process considering load balancing and conflict avoidance
     */
    private boolean assignProcessOptimally(ComputingPlan computingPlan, Process process, ExecutionFacts facts) {
        double bestScore = Double.NEGATIVE_INFINITY;
        ComputerPlan bestComputerPlan = null;
        int bestStartTime = -1;

        // Evaluate each computer
        for (int i = 0; i < computingPlan.computerPlanList.size(); i++) {
            ComputerPlan computerPlan = computingPlan.computerPlanList.get(i);
            
            // Find earliest possible start time on this computer
            int startTime = findEarliestStartTime(computerPlan, process, facts);
            if (startTime == -1) continue; // Cannot assign to this computer
            
            // Calculate assignment score
            double score = calculateAssignmentScore(computingPlan, computerPlan, process, startTime, facts);
            
            if (score > bestScore) {
                bestScore = score;
                bestComputerPlan = computerPlan;
                bestStartTime = startTime;
            }
        }

        if (bestComputerPlan != null) {
            // Assign the process
            process.computer = bestComputerPlan.computer;
            process.startTime = bestStartTime;
            process.endTime = bestStartTime + process.executionTime;
            process.idleDuration = bestStartTime - bestComputerPlan.firstFreeTime;
            
            bestComputerPlan.processList.add(process);
            bestComputerPlan.firstFreeTime = process.endTime;
            bestComputerPlan.busyTimeSum += process.executionTime;
            bestComputerPlan.idleTimeSum += process.idleDuration;
            
            return true;
        }
        
        return false;
    }

    /**
     * Find the earliest possible start time for a process on a given computer
     */
    private int findEarliestStartTime(ComputerPlan computerPlan, Process process, ExecutionFacts facts) {
        int startTime = computerPlan.firstFreeTime;
        
        // Check conflicts with already assigned processes
        for (Process conflictProcess : process.conflictingProcesses) {
            if (conflictProcess.computer != null && !conflictProcess.computer.equals(computerPlan.computer)) {
                // Check for overlap
                if (startTime < conflictProcess.endTime && (startTime + process.executionTime) > conflictProcess.startTime) {
                    startTime = conflictProcess.endTime;
                }
            }
        }
        
        return startTime;
    }

    /**
     * Calculate the score for assigning a process to a computer at a specific time
     */
    private double calculateAssignmentScore(ComputingPlan computingPlan, ComputerPlan computerPlan, 
                                          Process process, int startTime, ExecutionFacts facts) {
        double loadBalanceScore = 0.0;
        double conflictAvoidanceScore = 0.0;
        
        // Load balancing score: prefer computers with lower current load
        int totalBusyTime = 0;
        for (ComputerPlan cp : computingPlan.computerPlanList) {
            totalBusyTime += cp.busyTimeSum;
        }
        double avgBusyTime = (double) totalBusyTime / computingPlan.computerPlanList.size();
        loadBalanceScore = -Math.abs(computerPlan.busyTimeSum - avgBusyTime);
        
        // Conflict avoidance score: prefer assignments that minimize future conflicts
        int conflictCount = 0;
        for (Process conflictProcess : process.conflictingProcesses) {
            if (conflictProcess.computer != null && !conflictProcess.computer.equals(computerPlan.computer)) {
                // Check if this assignment would create immediate conflicts
                if (startTime < conflictProcess.endTime && (startTime + process.executionTime) > conflictProcess.startTime) {
                    conflictCount++;
                }
            }
        }
        conflictAvoidanceScore = -conflictCount * 100; // Heavy penalty for conflicts
        
        // Combine scores with weights
        return LOAD_BALANCE_WEIGHT * loadBalanceScore + CONFLICT_AVOIDANCE_WEIGHT * conflictAvoidanceScore;
    }

    /**
     * Optimized strict assignment with better conflict resolution
     */
    private void assignProcessStrictlyOptimized(ComputingPlan computingPlan, Process process, ExecutionFacts facts) {
        // Find the computer with the earliest completion time considering conflicts
        ComputerPlan bestComputerPlan = null;
        int bestCompletionTime = Integer.MAX_VALUE;
        
        for (ComputerPlan computerPlan : computingPlan.computerPlanList) {
            int startTime = findEarliestStartTime(computerPlan, process, facts);
            int completionTime = startTime + process.executionTime;
            
            if (completionTime < bestCompletionTime) {
                bestCompletionTime = completionTime;
                bestComputerPlan = computerPlan;
            }
        }
        
        if (bestComputerPlan != null) {
            int startTime = findEarliestStartTime(bestComputerPlan, process, facts);
            
            process.computer = bestComputerPlan.computer;
            process.startTime = startTime;
            process.endTime = startTime + process.executionTime;
            process.idleDuration = startTime - bestComputerPlan.firstFreeTime;
            
            bestComputerPlan.processList.add(process);
            bestComputerPlan.firstFreeTime = process.endTime;
            bestComputerPlan.busyTimeSum += process.executionTime;
            bestComputerPlan.idleTimeSum += process.idleDuration;
        } else {
            throw new RuntimeException("could not find computer for process " + process.processId);
        }
    }
}
