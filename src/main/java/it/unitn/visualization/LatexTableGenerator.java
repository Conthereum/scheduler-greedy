package it.unitn.visualization;

import java.io.IOException;
import java.util.Map;

public class LatexTableGenerator {
    
    public static void main(String[] args) {
        try {
            String latexTable = generateLatexTable();
            System.out.println(latexTable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String generateLatexTable() throws IOException {
        // Read data from speedup-auto.xlsx
        Map<Integer, SpeedupDataReader.SpeedupData> allData = SpeedupDataReader.readSpeedupData("speedup-auto.xlsx");
        
        // Define the core sizes we want to show (8 cores as requested)
        int[] coreSizes = {2, 7, 12, 17, 22, 27, 32};
        
        // Define transaction counts and conflict percentages
        int[] transactionCounts = {50, 100, 150, 200};
        int[] conflictPercentages = {15, 25, 35, 45};
        
        StringBuilder latex = new StringBuilder();
        
        // Start the table
        latex.append("\\begin{table*}[]\n");
        latex.append("\\caption{Performance Metrics - Proposers and Attestors speedups from 2 to 32 cores}\n");
        latex.append("\\label{tab:performance_metrics_greedy}\n");
        latex.append("\\begin{tabular}{|ccc|cc|cc|cc|cc|cc|cc|cc|}\n");
        latex.append("\\hline\n");
        
        // Header row with core counts
        latex.append("\\multicolumn{3}{|c|}{\\textbf{Datasets}}");
        for (int core : coreSizes) {
            latex.append(" & \\multicolumn{2}{c|}{\\textbf{").append(core).append(" cores}}");
        }
        latex.append(" \\\\ \\hline\n");
        
        // Subheader row with proposer/attestor labels
        latex.append("\\rotatebox{90}{\\textbf{Group No.}} & \\rotatebox{90}{\\textbf{Process Count}} & \\rotatebox{90}{\\textbf{Conflict \\%}}");
        for (int core : coreSizes) {
            latex.append(" & \\rotatebox{90}{\\textbf{proposer}} & \\rotatebox{90}{\\textbf{attestor}}");
        }
        latex.append(" \\\\ \\hline\n");
        
        // Data rows
        int groupNo = 1;
        for (int txCount : transactionCounts) {
            for (int conflict : conflictPercentages) {
                latex.append(groupNo).append(" & ").append(txCount).append(" & ").append(conflict);
                
                // Get data for this conflict percentage
                SpeedupDataReader.SpeedupData data = allData.get(conflict);
                if (data != null) {
                    for (int core : coreSizes) {
                        // Find the index for this core count
                        int coreIndex = -1;
                        for (int i = 0; i < data.cores.length; i++) {
                            if (data.cores[i] == core) {
                                coreIndex = i;
                                break;
                            }
                        }
                        
                        if (coreIndex >= 0) {
                            double proposerSpeedup = data.proposerSpeedup[coreIndex];
                            double attestorSpeedup = data.attestorSpeedup[coreIndex];
                            latex.append(" & ").append(String.format("%.2f", proposerSpeedup))
                                 .append(" & ").append(String.format("%.2f", attestorSpeedup));
                        } else {
                            // If core count not found, use interpolation or default values
                            latex.append(" & 0.00 & 0.00");
                        }
                    }
                } else {
                    // If no data for this conflict percentage, fill with zeros
                    for (int core : coreSizes) {
                        latex.append(" & 0.00 & 0.00");
                    }
                }
                
                latex.append("\\\\\n");
                groupNo++;
            }
            // Add horizontal line after each transaction count group
            if (groupNo <= 16) { // Only add lines between groups, not after the last group
                latex.append("\\hline\n");
            }
        }
        
        // Calculate and add average row
        latex.append("\\textbf{AVG:} & \\textbf{125} & \\textbf{30}");
        for (int core : coreSizes) {
            double avgProposer = 0.0;
            double avgAttestor = 0.0;
            int count = 0;
            
            for (int conflict : conflictPercentages) {
                SpeedupDataReader.SpeedupData data = allData.get(conflict);
                if (data != null) {
                    int coreIndex = -1;
                    for (int i = 0; i < data.cores.length; i++) {
                        if (data.cores[i] == core) {
                            coreIndex = i;
                            break;
                        }
                    }
                    
                    if (coreIndex >= 0) {
                        avgProposer += data.proposerSpeedup[coreIndex];
                        avgAttestor += data.attestorSpeedup[coreIndex];
                        count++;
                    }
                }
            }
            
            if (count > 0) {
                avgProposer /= count;
                avgAttestor /= count;
            }
            
            latex.append(" & \\textbf{").append(String.format("%.2f", avgProposer)).append("}")
                 .append(" & \\textbf{").append(String.format("%.2f", avgAttestor)).append("}");
        }
        latex.append(" \\\\ \\hline\n");
        
        // End the table
        latex.append("\\end{tabular}\n");
        latex.append("\\end{table*}");
        
        return latex.toString();
    }
}
