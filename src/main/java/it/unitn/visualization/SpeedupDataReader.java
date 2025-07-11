package it.unitn.visualization;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SpeedupDataReader {
    private static final int GROUP_COL = 0;
    private static final int PROCESS_COUNT_COL = 1;
    private static final int CONFLICT_PERCENTAGE_COL = 2;
    private static final int FIRST_CORE_DATA_COL = 3;

    public static class SpeedupData {
        public final double[] cores;
        public final double[] proposerSpeedup;
        public final double[] attestorSpeedup;
        public final double[] proposerStdDev;
        public final double[] attestorStdDev;
        public final int processCount; // Transaction count

        public SpeedupData(int size, int processCount) {
            cores = new double[size];
            proposerSpeedup = new double[size];
            attestorSpeedup = new double[size];
            proposerStdDev = new double[size];
            attestorStdDev = new double[size];
            this.processCount = processCount;
            
            // Initialize first point to (1,1)
            cores[0] = 1.0;
            proposerSpeedup[0] = 1.0;
            attestorSpeedup[0] = 1.0;
            proposerStdDev[0] = 0.0;
            attestorStdDev[0] = 0.0;
        }
    }

    private static double getCellNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }

    public static Map<Integer, SpeedupData> readSpeedupData() throws IOException {
        return readSpeedupData("speedup-auto.xlsx");
    }
    
    public static Map<Integer, SpeedupData> readSpeedupData(String filename) throws IOException {
        Map<Integer, List<double[]>> conflictData = new HashMap<>();
        Map<Integer, Integer> conflictToProcessCount = new HashMap<>(); // Map conflict percentage to process count
        
        try (InputStream is = SpeedupDataReader.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new IOException("Could not find " + filename + " in resources directory");
            
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Get process count (transaction count)
                Cell processCountCell = row.getCell(PROCESS_COUNT_COL);
                if (processCountCell == null) continue;
                int processCount = (int) getCellNumericValue(processCountCell);
                
                // Get conflict percentage
                Cell conflictCell = row.getCell(CONFLICT_PERCENTAGE_COL);
                if (conflictCell == null) continue;
                
                int conflictPercentage = (int) getCellNumericValue(conflictCell);
                if (conflictPercentage < 0) continue; // Changed from <= 0 to < 0 to include 0%
                
                // Store the process count for this conflict percentage
                conflictToProcessCount.put(conflictPercentage, processCount);
                
                // Determine number of core pairs based on file format
                int maxCol = row.getLastCellNum();
                int numCorePairs = (maxCol - FIRST_CORE_DATA_COL) / 2;
                double[] rowData = new double[numCorePairs * 2]; // Pairs of proposer/attestor values
                
                for (int i = 0; i < numCorePairs * 2; i++) {
                    Cell cell = row.getCell(FIRST_CORE_DATA_COL + i);
                    rowData[i] = getCellNumericValue(cell);
                }
                
                conflictData.computeIfAbsent(conflictPercentage, k -> new ArrayList<>()).add(rowData);
            }
            
            workbook.close();
        }
        
        Map<Integer, SpeedupData> result = new HashMap<>();
        
        for (Map.Entry<Integer, List<double[]>> conflictEntry : conflictData.entrySet()) {
            int conflictPercentage = conflictEntry.getKey();
            int numCores = conflictEntry.getValue().get(0).length / 2; // Number of core pairs
            int processCount = conflictToProcessCount.getOrDefault(conflictPercentage, 0);
            SpeedupData speedupData = new SpeedupData(numCores, processCount);
            
            // Process each core count (1 to numCores)
            for (int coreIdx = 0; coreIdx < numCores; coreIdx++) {
                List<Double> proposerValues = new ArrayList<>();
                List<Double> attestorValues = new ArrayList<>();
                
                // Collect all measurements for this core count
                for (double[] measurement : conflictEntry.getValue()) {
                    proposerValues.add(measurement[coreIdx * 2]);
                    attestorValues.add(measurement[coreIdx * 2 + 1]);
                }
                
                speedupData.cores[coreIdx] = coreIdx + 1;
                speedupData.proposerSpeedup[coreIdx] = calculateMean(proposerValues);
                speedupData.attestorSpeedup[coreIdx] = calculateMean(attestorValues);
                speedupData.proposerStdDev[coreIdx] = calculateStdDev(proposerValues);
                speedupData.attestorStdDev[coreIdx] = calculateStdDev(attestorValues);
            }
            
            result.put(conflictPercentage, speedupData);
        }
        
        return result;
    }
    
    /**
     * Read speedup data grouped by transaction count (process count) instead of conflict percentage
     */
    public static Map<Integer, SpeedupData> readSpeedupDataByTransactionCount(String filename) throws IOException {
        Map<Integer, List<double[]>> transactionData = new HashMap<>();
        
        try (InputStream is = SpeedupDataReader.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new IOException("Could not find " + filename + " in resources directory");
            
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Get process count (transaction count)
                Cell processCountCell = row.getCell(PROCESS_COUNT_COL);
                if (processCountCell == null) continue;
                int processCount = (int) getCellNumericValue(processCountCell);
                
                // Get conflict percentage
                Cell conflictCell = row.getCell(CONFLICT_PERCENTAGE_COL);
                if (conflictCell == null) continue;
                
                int conflictPercentage = (int) getCellNumericValue(conflictCell);
                if (conflictPercentage < 0) continue;
                
                // Determine number of core pairs based on file format
                int maxCol = row.getLastCellNum();
                int numCorePairs = (maxCol - FIRST_CORE_DATA_COL) / 2;
                double[] rowData = new double[numCorePairs * 2]; // Pairs of proposer/attestor values
                
                for (int i = 0; i < numCorePairs * 2; i++) {
                    Cell cell = row.getCell(FIRST_CORE_DATA_COL + i);
                    rowData[i] = getCellNumericValue(cell);
                }
                
                transactionData.computeIfAbsent(processCount, k -> new ArrayList<>()).add(rowData);
            }
            
            workbook.close();
        }
        
        Map<Integer, SpeedupData> result = new HashMap<>();
        
        for (Map.Entry<Integer, List<double[]>> transactionEntry : transactionData.entrySet()) {
            int transactionCount = transactionEntry.getKey();
            int numCores = transactionEntry.getValue().get(0).length / 2; // Number of core pairs
            SpeedupData speedupData = new SpeedupData(numCores, transactionCount);
            
            // Process each core count (1 to numCores)
            for (int coreIdx = 0; coreIdx < numCores; coreIdx++) {
                List<Double> proposerValues = new ArrayList<>();
                List<Double> attestorValues = new ArrayList<>();
                
                // Collect all measurements for this core count
                for (double[] measurement : transactionEntry.getValue()) {
                    proposerValues.add(measurement[coreIdx * 2]);
                    attestorValues.add(measurement[coreIdx * 2 + 1]);
                }
                
                speedupData.cores[coreIdx] = coreIdx + 1;
                speedupData.proposerSpeedup[coreIdx] = calculateMean(proposerValues);
                speedupData.attestorSpeedup[coreIdx] = calculateMean(attestorValues);
                speedupData.proposerStdDev[coreIdx] = calculateStdDev(proposerValues);
                speedupData.attestorStdDev[coreIdx] = calculateStdDev(attestorValues);
            }
            
            result.put(transactionCount, speedupData);
        }
        
        return result;
    }
    
    public static SpeedupData combineData(Map<Integer, SpeedupData> allData) {
        if (allData.isEmpty()) {
            return new SpeedupData(1, 0);
        }
        
        // Get the number of cores from the first data entry
        int numCores = allData.values().iterator().next().cores.length;
        SpeedupData combined = new SpeedupData(numCores, 0);
        
        // Process each core count (1 to numCores)
        for (int coreIdx = 0; coreIdx < numCores; coreIdx++) {
            List<Double> proposerValues = new ArrayList<>();
            List<Double> attestorValues = new ArrayList<>();
            
            // Collect values from all conflict percentages
            for (SpeedupData data : allData.values()) {
                if (coreIdx < data.cores.length) {
                    proposerValues.add(data.proposerSpeedup[coreIdx]);
                    attestorValues.add(data.attestorSpeedup[coreIdx]);
                }
            }
            
            combined.cores[coreIdx] = coreIdx + 1;
            combined.proposerSpeedup[coreIdx] = calculateMean(proposerValues);
            combined.attestorSpeedup[coreIdx] = calculateMean(attestorValues);
            combined.proposerStdDev[coreIdx] = calculateStdDev(proposerValues);
            combined.attestorStdDev[coreIdx] = calculateStdDev(attestorValues);
        }
        
        return combined;
    }

    private static double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double calculateStdDev(List<Double> values) {
        double mean = calculateMean(values);
        return Math.sqrt(values.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0));
    }
} 