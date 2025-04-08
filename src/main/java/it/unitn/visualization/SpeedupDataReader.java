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

        public SpeedupData(int size) {
            cores = new double[size];
            proposerSpeedup = new double[size];
            attestorSpeedup = new double[size];
            proposerStdDev = new double[size];
            attestorStdDev = new double[size];
            
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
        Map<Integer, List<double[]>> conflictData = new HashMap<>();
        
        try (InputStream is = SpeedupDataReader.class.getClassLoader().getResourceAsStream("speedups.xlsx")) {
            if (is == null) throw new IOException("Could not find speedups.xlsx in resources directory");
            
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Get conflict percentage
                Cell conflictCell = row.getCell(CONFLICT_PERCENTAGE_COL);
                if (conflictCell == null) continue;
                
                int conflictPercentage = (int) getCellNumericValue(conflictCell);
                if (conflictPercentage <= 0) continue;
                
                double[] rowData = new double[18]; // 9 pairs of proposer/attestor values
                for (int i = 0; i < 18; i++) {
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
            SpeedupData speedupData = new SpeedupData(9);
            
            // Process each core count (2-9)
            for (int coreIdx = 1; coreIdx < 9; coreIdx++) {
                List<Double> proposerValues = new ArrayList<>();
                List<Double> attestorValues = new ArrayList<>();
                
                // Collect all measurements for this core count
                for (double[] measurement : conflictEntry.getValue()) {
                    proposerValues.add(measurement[(coreIdx-1) * 2]);
                    attestorValues.add(measurement[(coreIdx-1) * 2 + 1]);
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
    
    public static SpeedupData combineData(Map<Integer, SpeedupData> allData) {
        SpeedupData combined = new SpeedupData(9);
        
        // Process each core count (2-9)
        for (int coreIdx = 1; coreIdx < 9; coreIdx++) {
            List<Double> proposerValues = new ArrayList<>();
            List<Double> attestorValues = new ArrayList<>();
            
            // Collect values from all conflict percentages
            for (SpeedupData data : allData.values()) {
                proposerValues.add(data.proposerSpeedup[coreIdx]);
                attestorValues.add(data.attestorSpeedup[coreIdx]);
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