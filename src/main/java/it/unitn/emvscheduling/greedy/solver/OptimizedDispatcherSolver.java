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
    private static class ProcessGroup {
        List<Process> processes;
        int totalDuration;
        Set<Process> conflictingProcesses;

        ProcessGroup() {
            processes = new ArrayList<>();
            totalDuration = 0;
            conflictingProcesses = new HashSet<>();
        }
    }

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

        // Phase 1: Loose Assignment - Try to assign processes without conflicts
        List<Process> unassigned = new ArrayList<>(facts.processes);
        int numCores = facts.computers.size();
        
        // Sort processes by execution time (longest first)
        unassigned.sort((a, b) -> Integer.compare(b.executionTime, a.executionTime));
        
        // Try multiple rounds of loose assignment
        for (int round = 0; round < strategy.looseReviewRound; round++) {
            List<Process> remaining = new ArrayList<>();
            for (Process p : unassigned) {
                if (!assignLoosely(p, plan, numCores)) {
                    remaining.add(p);
                }
            }
            unassigned = remaining;
            if (unassigned.isEmpty()) break;
        }

        // Phase 2: Strict Assignment - Assign remaining processes with proper timing
        for (Process p : unassigned) {
            assignStrictly(p, plan, numCores);
        }

        output.horizon = facts.processes.stream().mapToLong(p -> p.executionTime).sum();
        output.scheduleMakespan = plan.getScheduleMakespan();
        output.wallTimeInMs = (System.nanoTime() - startTime) / 1_000_000.0;
        output.resultStatus = "possible";
        output.processes = facts.processes;
        
        return output;
    }

    private boolean assignLoosely(Process p, ComputingPlan plan, int numCores) {
        // Find the best core that can execute this process without conflicts
        ComputerPlan bestCore = null;
        int minNewMakespan = Integer.MAX_VALUE;
        int bestStartTime = 0;
        
        // Sort cores by current load
        List<ComputerPlan> corePlans = new ArrayList<>(plan.computerPlanList);
        corePlans.sort(Comparator.comparingInt(cp -> cp.firstFreeTime));
        
        for (int i = 0; i < Math.min(numCores, corePlans.size()); i++) {
            ComputerPlan core = corePlans.get(i);
            
            // Calculate earliest possible start time considering conflicts
            int startTime = core.firstFreeTime;
            boolean hasConflicts = false;
            
            // Check for conflicts with already assigned processes
            for (Process conflict : p.conflictingProcesses) {
                if (conflict.computer != null) {
                    // If conflict is on same core, we can't assign here
                    if (conflict.computer.equals(core.computer)) {
                        hasConflicts = true;
                        break;
                    }
                    // If conflict is on different core, ensure proper timing
                    if (startTime < conflict.endTime) {
                        startTime = conflict.endTime;
                    }
                }
            }
            
            if (!hasConflicts) {
                // Calculate new makespan considering all cores
                int newMakespan = Math.max(startTime + p.executionTime,
                    corePlans.stream().mapToInt(cp -> cp.firstFreeTime).max().orElse(0));
                
                if (newMakespan < minNewMakespan) {
                    minNewMakespan = newMakespan;
                    bestCore = core;
                    bestStartTime = startTime;
                }
            }
        }
        
        if (bestCore != null) {
            p.computer = bestCore.computer;
            p.startTime = bestStartTime;
            p.endTime = bestStartTime + p.executionTime;
            bestCore.processList.add(p);
            bestCore.firstFreeTime = p.endTime;
            bestCore.busyTimeSum += p.executionTime;
            return true;
        }
        
        return false;
    }

    private void assignStrictly(Process p, ComputingPlan plan, int numCores) {
        // Find the best core that minimizes makespan while handling conflicts
        ComputerPlan bestCore = null;
        int minNewMakespan = Integer.MAX_VALUE;
        int bestStartTime = 0;
        
        // Sort cores by current load
        List<ComputerPlan> corePlans = new ArrayList<>(plan.computerPlanList);
        corePlans.sort(Comparator.comparingInt(cp -> cp.firstFreeTime));
        
        for (int i = 0; i < Math.min(numCores, corePlans.size()); i++) {
            ComputerPlan core = corePlans.get(i);
            
            // Calculate earliest possible start time considering conflicts
            int startTime = core.firstFreeTime;
            boolean hasConflicts = false;
            
            // Check for conflicts with already assigned processes
            for (Process conflict : p.conflictingProcesses) {
                if (conflict.computer != null) {
                    // If conflict is on same core, we can't assign here
                    if (conflict.computer.equals(core.computer)) {
                        hasConflicts = true;
                        break;
                    }
                    // If conflict is on different core, ensure proper timing
                    if (startTime < conflict.endTime) {
                        startTime = conflict.endTime;
                    }
                }
            }
            
            if (!hasConflicts) {
                // Calculate new makespan considering all cores
                int newMakespan = Math.max(startTime + p.executionTime,
                    corePlans.stream().mapToInt(cp -> cp.firstFreeTime).max().orElse(0));
                
                if (newMakespan < minNewMakespan) {
                    minNewMakespan = newMakespan;
                    bestCore = core;
                    bestStartTime = startTime;
                }
            }
        }
        
        if (bestCore != null) {
            p.computer = bestCore.computer;
            p.startTime = bestStartTime;
            p.endTime = bestStartTime + p.executionTime;
            bestCore.processList.add(p);
            bestCore.firstFreeTime = p.endTime;
            bestCore.busyTimeSum += p.executionTime;
        }
    }

    private ExecutionOutput solveAttestor(ExecutionFacts facts, ExecutionSettings settings, Strategy strategy) {
        // Similar to proposer but with order preservation
        long startTime = System.nanoTime();
        ExecutionOutput output = new ExecutionOutput();
        ComputingPlan plan = new ComputingPlan(facts);

        // Phase 1: Loose Assignment with order preservation
        List<Process> unassigned = new ArrayList<>(facts.processes);
        int numCores = facts.computers.size();
        
        // Move conflicting transactions to front
        facts.moveConflictingTransactionsToFront();
        
        // Try multiple rounds of loose assignment
        for (int round = 0; round < strategy.looseReviewRound; round++) {
            List<Process> remaining = new ArrayList<>();
            for (Process p : unassigned) {
                if (p.conflictingProcesses.isEmpty() || 
                    p.conflictingProcesses.stream().allMatch(c -> c.processId >= p.processId || c.computer != null)) {
                    if (!assignLoosely(p, plan, numCores)) {
                        remaining.add(p);
                    }
                } else {
                    remaining.add(p);
                }
            }
            unassigned = remaining;
            if (unassigned.isEmpty()) break;
        }

        // Phase 2: Strict Assignment with order preservation
        for (Process p : unassigned) {
            assignStrictly(p, plan, numCores);
        }

        output.horizon = facts.processes.stream().mapToLong(p -> p.executionTime).sum();
        output.scheduleMakespan = plan.getScheduleMakespan();
        output.wallTimeInMs = (System.nanoTime() - startTime) / 1_000_000.0;
        output.resultStatus = "possible";
        output.processes = facts.processes;
        
        return output;
    }
} 