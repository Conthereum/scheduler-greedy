package it.unitn.emvscheduling.greedy.application;

import it.unitn.emvscheduling.greedy.data.DataGenerator;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.solver.DispatcherSolver;
import it.unitn.emvscheduling.greedy.solver.OptimizedDispatcherSolver;
import it.unitn.emvscheduling.greedy.solver.Solver;
import it.unitn.emvscheduling.greedy.solver.Strategy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileBasedExecutor {
    private static final String inputFile = "input.csv";
    private static final String outputFile = "output.csv";
    private static final String accumulativeOutputFile = "output-accumulative.csv";
    private static final String outputHeader = "no, groupNo, randomSeed, numberOfWorkers," +
            "maxSolverExecutionTimeInSeconds, processCount, processExecutionTimeMin(ms), processExecutionTimeMax(ms)," +
            " computerCount, conflictPercentage, timeWeight," +
            "processSortType, looseReviewRound, " +
            "solverWallTime(ms), makespan(ms), parallelTimeSum(ms), " +
            "serialTimeHorizon(ms), solverStatus, speedupFactor, currentTimestamp";

    public static void executeUsingFiles(boolean isNewOptimalSolution) {
        Solver solver;

        // Use the updated method to read inputs from "input.csv"
        List<List<Integer>> inputs = readInputsFromCSV(inputFile);
        String outputFilePath = "src/main/resources/" + outputFile;

        List<ExecutionOutput> outputs = new ArrayList<>();

        String outputLines = "";
        System.out.println("Output:\n\n" + "---------------------------\n" + outputHeader);
        Path outputPath = Paths.get(outputFilePath);
        try {
            // Delete the file if it exists, then create a new one
            if (Files.exists(outputPath)) {
                Files.delete(outputPath);
            }
            Files.createFile(outputPath);
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
                writer.println(outputHeader);
                for (List<Integer> input : inputs) {
                    int i = 2;
                    int randomSeed = input.get(i++);
                    int numberOfWorkers = input.get(i++);
                    int maxSolverExecutionTimeInSeconds = input.get(i++);
                    ExecutionSettings settings = new ExecutionSettings(numberOfWorkers, maxSolverExecutionTimeInSeconds, randomSeed);

                    ExecutionFacts facts = DataGenerator.getBenchmark(randomSeed, input.get(i++), input.get(i++),
                            input.get(i++), input.get(i++), input.get(i++), input.get(i++));
                    solver = isNewOptimalSolution? new OptimizedDispatcherSolver() : new DispatcherSolver();
                    Strategy.ProcessSortType processSortType = Strategy.ProcessSortType.getByValue(input.get(i++));
                    int looseReviewRound = input.get(i++);
                    Strategy strategy = new Strategy(processSortType, looseReviewRound);
                    ExecutionOutput output = solver.solve(facts, settings, strategy);
                    outputs.add(output);

                    String outputLine = getOutputLine(input.get(0), // No.
                            input.get(1),// Group id
                            randomSeed,
                            numberOfWorkers,
                            maxSolverExecutionTimeInSeconds,
                            facts.processes.size(), // processCount
                            input.get(6), // processExecutionTimeMin
                            input.get(7), // processExecutionTimeMax
                            facts.computers.size(), // computerCount
                            input.get(9), // conflictPercentage
                            input.get(10), // timeWeight
                            input.get(11), // processSortType
                            //input.get(12), // looseReviewRound
                            strategy.looseReviewRound, // in case of changed by the heuristic approach
                            output.wallTimeInMs,
                            output.scheduleMakespan,
                            output.horizon,
                            output.resultStatus); // serial time (horizon));
                    writer.println(outputLine);
                    writer.flush();
                    writeInAccumulativeOutFileWithTimestamp(outputLine);
                    System.out.println(outputLine);
                }
            }
            System.out.println("---------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getOutputLine(Integer no, Integer groupId, Integer randomSeed, Integer numberOfWorkers,
                                     Integer maxSolverExecutionTimeInSeconds,
                                     Integer processCount, Integer processExecutionTimeMin,
                                     Integer processExecutionTimeMax, Integer computerCount,
                                     Integer conflictPercentage, Integer timeWeight, Integer processSortType,
                                     Integer looseReviewRound,
                                     Double solverWallTimeMs,
                                     Integer makeSpan, Long serialTimeHorizon,
                                     String solverStatus) {
        // Speedup is serial time divided by parallel time (makespan)
        Double speedUpFactor = (double)serialTimeHorizon / makeSpan;

        String line = String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%d,%.6f,%d,%s,%.6f",
                no, groupId, randomSeed, numberOfWorkers, maxSolverExecutionTimeInSeconds, processCount,
                processExecutionTimeMin, processExecutionTimeMax, computerCount, conflictPercentage,
                timeWeight, processSortType, looseReviewRound, solverWallTimeMs, makeSpan, solverWallTimeMs + makeSpan,
                serialTimeHorizon,
                solverStatus, speedUpFactor
        );
        return line;
    }

    private static List<List<Integer>> readInputsFromCSV(String fileName) {
        List<List<Integer>> inputs = new ArrayList<>();
        try (InputStream inputStream = FileBasedExecutor.class.getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                System.out.println("File not found: " + fileName);
                return inputs;
            }

            // Skip header
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                List<Integer> inputList = new ArrayList<>();
                String[] parts = line.trim().split(",");
                for (String part : parts) {
                    inputList.add(Integer.parseInt(part));
                }
                inputs.add(inputList);
            }
        } catch (IOException e) {
            System.out.println("Error reading input file " + e);
        }
        return inputs;
    }

    public static void writeInAccumulativeOutFileWithTimestamp(String outputLines) throws IOException {
        String accumulativeOutputFilePath = "src/main/resources/" + accumulativeOutputFile;
        Path accumOutputPath = Paths.get(accumulativeOutputFilePath);
        boolean accumCreated = false;

        if (!Files.exists(accumOutputPath)) {
            Files.createFile(accumOutputPath);
            accumCreated = true;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                accumOutputPath,
                StandardOpenOption.CREATE, // Creates the file if it doesn't exist
                StandardOpenOption.APPEND  // Appends to the file if it exists
        )) {
            if (accumCreated) {
                writer.write(outputHeader); // Only write the header if needed
                writer.newLine();
            }
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


            writer.write(outputLines + "," + now.format(formatter));
            writer.newLine();
        }
    }
}
