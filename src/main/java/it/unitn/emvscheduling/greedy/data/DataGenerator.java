package it.unitn.emvscheduling.greedy.data;

import it.unitn.emvscheduling.greedy.domain.Computer;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.Process;
import it.unitn.emvscheduling.greedy.domain.UnorderedPair;

import java.util.*;

public class DataGenerator {
    /**
     * execution time of th processes is a random number in [processExecutionTimeMin, processTimeExecutionMax]
     *
     * @param processCount
     * @param processExecutionTimeMin
     * @param processExecutionTimeMax
     * @param computerCount
     * @param conflictPercentage
     * @param timeWeight
     * @return
     */
    public static ExecutionFacts getBenchmark(Integer randomSeed, Integer processCount, Integer processExecutionTimeMin,
                                              Integer processExecutionTimeMax,
                                              Integer computerCount, Integer conflictPercentage, Integer timeWeight) {
        Random random = new Random(randomSeed);
        if (processExecutionTimeMin > processExecutionTimeMax)
            throw new RuntimeException("processExecutionTimeMin must be less than or equal to processTimeExecutionMax");

//        System.out.println("start generating it.unitn.emvscheduling.declarative.data(processCount=" + processCount + ", computerCount=" + computerCount +
//                ", conflictPercentage=" + conflictPercentage + ", timeWeight=" + timeWeight + ")");
        ExecutionFacts facts = new ExecutionFacts();

        List<Computer> computers = new ArrayList<>();
        facts.computers = computers;
        for (int computerId = 0; computerId < computerCount; computerId++) {
            Computer computer = new Computer(computerId);
            computers.add(computer);
        }

        List<Process> processes = new ArrayList<>();
        facts.processes = processes;

        // String processDurations = "";
        for (int processIdx = 0; processIdx < processCount; processIdx++) {

            Integer executionTime =
                    random.nextInt(processExecutionTimeMax - processExecutionTimeMin + 1) + processExecutionTimeMin;
            Process process = new Process(processIdx, executionTime);
            processes.add(process);
            // processDurations += "\t" + executionTime;
        }
        // System.out.println("processDurations: " + processDurations);

        List<UnorderedPair> conflicts = generateConflictPairs(randomSeed, processCount, conflictPercentage);
        facts.conflictingProcesses = conflicts;
        for (UnorderedPair pair : conflicts) {
            facts.processes.get(pair.i).conflictingProcesses.add(processes.get(pair.j));
            facts.processes.get(pair.j).conflictingProcesses.add(processes.get(pair.i));
        }
        facts.conflictPercentage = conflictPercentage;
        facts.timeWeight = timeWeight;

        /*
        String conflictsStr = "";
        for (UnorderedPair pair: conflicts){
            conflictsStr += "("+pair.i+","+pair.j+"),";
        }
       System.out.println("finished generating it.unitn.emvscheduling.declarative.data, conflicts are:\n"+conflictsStr);
       */
        return facts;
    }

    public static List<UnorderedPair> generateConflictPairs(Integer randomSeed, Integer processCount,
                                                            Integer conflictPercentage) {
        Random random = new Random(randomSeed);
        List<UnorderedPair> conflictPairs = new ArrayList<>();
        Set<String> uniquePairs = new HashSet<>();
        int totalPairs = processCount * (processCount - 1) / 2; // Total unique pairs
        int requiredConflicts = totalPairs * conflictPercentage / 100; // Number of conflicts based on percentage

        while (uniquePairs.size() < requiredConflicts) {
            int processA = random.nextInt(processCount);
            int processB = random.nextInt(processCount);

            // Ensure processA and processB are different and order pair consistently to avoid duplicates
            if (processA != processB) {
                int minProcess = Math.min(processA, processB);
                int maxProcess = Math.max(processA, processB);
                String pairKey = minProcess + "-" + maxProcess;

                if (!uniquePairs.contains(pairKey)) {
                    uniquePairs.add(pairKey);
                    conflictPairs.add(new UnorderedPair(minProcess, maxProcess));
                }
            }
        }
        Collections.sort(conflictPairs);
        return conflictPairs;
    }
}
