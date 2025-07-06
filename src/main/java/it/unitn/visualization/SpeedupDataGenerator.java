package it.unitn.visualization;

import it.unitn.emvscheduling.greedy.data.DataGenerator;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.solver.DispatcherSolver;
import it.unitn.emvscheduling.greedy.solver.Solver;
import it.unitn.emvscheduling.greedy.solver.Strategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SpeedupDataGenerator {
    
    // Configuration parameters
    private static final int[] CORE_COUNTS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
    private static final int[] CONFLICT_PERCENTAGES = {0, 5, 10, 15, 25, 35, 45};
    private static final int[] PROCESS_COUNTS = {50, 100, 150, 200};
    private static final int PROCESS_EXECUTION_TIME_MIN = 5;
    private static final int PROCESS_EXECUTION_TIME_MAX = 10;
    private static final int TIME_WEIGHT = 100;
    private static final int PROCESS_SORT_TYPE = 3; // MCDF
    private static final int LOOSE_REVIEW_ROUND = 29;
    private static final int RANDOM_SEEDS_PER_CONFIG = 5; // Number of random seeds for each configuration
    private static final int MAX_SOLVER_EXECUTION_TIME = -1;
    private static final int NUMBER_OF_WORKERS = -1;
    
    public static void main(String[] args) {
        try {
            System.out.println("Starting automated speedup data generation...");
            generateSpeedupData();
            System.out.println("Speedup data generation completed successfully!");
            System.out.println("Output file: speedup-auto.xlsx");
        } catch (Exception e) {
            System.err.println("Error generating speedup data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void generateSpeedupData() throws IOException {
        // Create workbook and sheet
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Speedup Data");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Group", "ProcessCount", "ConflictPercentage", 
                "Core1_Proposer", "Core1_Attestor",
                "Core2_Proposer", "Core2_Attestor",
                "Core3_Proposer", "Core3_Attestor",
                "Core4_Proposer", "Core4_Attestor",
                "Core5_Proposer", "Core5_Attestor",
                "Core6_Proposer", "Core6_Attestor",
                "Core7_Proposer", "Core7_Attestor",
                "Core8_Proposer", "Core8_Attestor",
                "Core9_Proposer", "Core9_Attestor",
                "Core10_Proposer", "Core10_Attestor",
                "Core11_Proposer", "Core11_Attestor",
                "Core12_Proposer", "Core12_Attestor",
                "Core13_Proposer", "Core13_Attestor",
                "Core14_Proposer", "Core14_Attestor",
                "Core15_Proposer", "Core15_Attestor",
                "Core16_Proposer", "Core16_Attestor",
                "Core17_Proposer", "Core17_Attestor",
                "Core18_Proposer", "Core18_Attestor",
                "Core19_Proposer", "Core19_Attestor",
                "Core20_Proposer", "Core20_Attestor",
                "Core21_Proposer", "Core21_Attestor",
                "Core22_Proposer", "Core22_Attestor",
                "Core23_Proposer", "Core23_Attestor",
                "Core24_Proposer", "Core24_Attestor",
                "Core25_Proposer", "Core25_Attestor",
                "Core26_Proposer", "Core26_Attestor",
                "Core27_Proposer", "Core27_Attestor",
                "Core28_Proposer", "Core28_Attestor",
                "Core29_Proposer", "Core29_Attestor",
                "Core30_Proposer", "Core30_Attestor",
                "Core31_Proposer", "Core31_Attestor",
                "Core32_Proposer", "Core32_Attestor"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowIndex = 1;
            
            // Generate data for each process count and conflict percentage combination
            for (int processCount : PROCESS_COUNTS) {
                for (int conflictPercentage : CONFLICT_PERCENTAGES) {
                    System.out.println("Processing: " + processCount + " processes, " + conflictPercentage + "% conflicts");
                    
                    // Calculate average speedup for each core count
                    double[] proposerSpeedups = new double[CORE_COUNTS.length];
                    double[] attestorSpeedups = new double[CORE_COUNTS.length];
                    
                    // For each core count, run multiple random seeds and calculate average
                    for (int coreIndex = 0; coreIndex < CORE_COUNTS.length; coreIndex++) {
                        int coreCount = CORE_COUNTS[coreIndex];
                        
                        List<Double> proposerSpeedupValues = new ArrayList<>();
                        List<Double> attestorSpeedupValues = new ArrayList<>();
                        
                        // Run multiple random seeds for statistical significance
                        for (int seed = 1; seed <= RANDOM_SEEDS_PER_CONFIG; seed++) {
                            // Test proposer mode
                            double proposerSpeedup = calculateSpeedup(seed, processCount, coreCount, conflictPercentage, true);
                            proposerSpeedupValues.add(proposerSpeedup);
                            
                            // Test attestor mode
                            double attestorSpeedup = calculateSpeedup(seed, processCount, coreCount, conflictPercentage, false);
                            attestorSpeedupValues.add(attestorSpeedup);
                        }
                        
                        // Calculate average speedup for this core count
                        proposerSpeedups[coreIndex] = proposerSpeedupValues.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
                        attestorSpeedups[coreIndex] = attestorSpeedupValues.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
                    }
                    
                    // Create data row
                    Row dataRow = sheet.createRow(rowIndex++);
                    
                    // Set group, process count, and conflict percentage
                    dataRow.createCell(0).setCellValue(rowIndex - 1); // Group number
                    dataRow.createCell(1).setCellValue(processCount);
                    dataRow.createCell(2).setCellValue(conflictPercentage);
                    
                    // Set speedup values for each core count
                    for (int coreIndex = 0; coreIndex < CORE_COUNTS.length; coreIndex++) {
                        int colIndex = 3 + (coreIndex * 2); // Start from column 3, each core takes 2 columns
                        dataRow.createCell(colIndex).setCellValue(proposerSpeedups[coreIndex]);
                        dataRow.createCell(colIndex + 1).setCellValue(attestorSpeedups[coreIndex]);
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Save the workbook
            Path outputPath = Paths.get("src/main/resources/speedup-auto.xlsx");
            Files.createDirectories(outputPath.getParent());
            
            try (FileOutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fileOut);
            }
        }
    }
    
    private static double calculateSpeedup(int randomSeed, int processCount, int computerCount, 
                                         int conflictPercentage, boolean isProposerMode) {
        try {
            // Create execution settings
            ExecutionSettings settings = new ExecutionSettings(NUMBER_OF_WORKERS, MAX_SOLVER_EXECUTION_TIME, randomSeed);
            
            // Generate execution facts
            ExecutionFacts facts = DataGenerator.getBenchmark(
                randomSeed, processCount, PROCESS_EXECUTION_TIME_MIN, 
                PROCESS_EXECUTION_TIME_MAX, computerCount, conflictPercentage, TIME_WEIGHT
            );
            
            // Set proposer/attestor mode
            facts.isProposerMode = isProposerMode;
            
            // Create solver and strategy
            Solver solver = new DispatcherSolver();
            Strategy strategy = new Strategy(Strategy.ProcessSortType.getByValue(PROCESS_SORT_TYPE), LOOSE_REVIEW_ROUND);
            
            // Solve the scheduling problem
            ExecutionOutput output = solver.solve(facts, settings, strategy);
            
            // Calculate speedup factor
            if (output.scheduleMakespan > 0) {
                return (double) output.horizon / output.scheduleMakespan;
            } else {
                return 1.0; // Default speedup if makespan is 0
            }
            
        } catch (Exception e) {
            System.err.println("Error calculating speedup for seed " + randomSeed + 
                             ", cores " + computerCount + ", conflicts " + conflictPercentage + 
                             ", proposer " + isProposerMode + ": " + e.getMessage());
            return 1.0; // Return default speedup on error
        }
    }
}
