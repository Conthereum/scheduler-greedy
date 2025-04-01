package it.unitn.emvscheduling.greedy.application;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args[0].equals("args"))
            executeUsingArgs(args);
        else if (args[0].equals("files"))
            executeUsingFiles(args);
        else
            System.out.println("Two options of 'args' or 'file' are supported");
    }

    private static void executeUsingArgs(String[] args) throws IOException {
        if (args.length != 10) {
            System.out.println("Usage: args <randomSeed> <numberOfWorkers> <maxSolverExecutionTimeInSeconds> <processCount> " +
                    "<processExecutionTimeMin> <processExecutionTimeMax> <computerCount> <conflictPercentage> " +
                    "<timeWeight>");
            System.exit(1);
        }
        Integer i = 1;
        int randomSeed = Integer.parseInt(args[i++]);
        int numberOfWorkers = Integer.parseInt(args[i++]);
        int maxSolverExecutionTimeInSeconds = Integer.parseInt(args[i++]);
        int processCount = Integer.parseInt(args[i++]);
        int processExecutionTimeMin = Integer.parseInt(args[i++]);
        int processExecutionTimeMax = Integer.parseInt(args[i++]);
        int computerCount = Integer.parseInt(args[i++]);
        int conflictPercentage = Integer.parseInt(args[i++]);
        int timeWeight = Integer.parseInt(args[i++]);
        ArgsBasedExecutor.executeUsingArgs(randomSeed, numberOfWorkers, maxSolverExecutionTimeInSeconds
                , processCount, processExecutionTimeMin, processExecutionTimeMax, computerCount, conflictPercentage, timeWeight);
    }

    private static void executeUsingFiles(String[] args) {
        if (args.length != 1) {
            //todo you can include file name or postfix and ...
            System.out.println("Usage: file");
            System.exit(1);
        }
        FileBasedExecutor.executeUsingFiles();
    }
}
