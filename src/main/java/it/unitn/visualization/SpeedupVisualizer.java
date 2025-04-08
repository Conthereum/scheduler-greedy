package it.unitn.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.awt.geom.Rectangle2D;

public class SpeedupVisualizer {
    // Make chart square to ensure equal unit sizes
    private static final int CHART_SIZE = 600;
    
    public static void createSpeedupChart(SpeedupDataReader.SpeedupData data, String title, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        XYSeries proposerSeries = new XYSeries("Proposer");
        XYSeries attestorSeries = new XYSeries("Attestor");
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        
        // Add data points starting from (1,1)
        for (int i = 0; i < data.cores.length; i++) {
            proposerSeries.add(data.cores[i], data.proposerSpeedup[i]);
            attestorSeries.add(data.cores[i], data.attestorSpeedup[i]);
            linearSeries.add(data.cores[i], data.cores[i]);
        }
        
        dataset.addSeries(proposerSeries);
        dataset.addSeries(attestorSeries);
        dataset.addSeries(linearSeries);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        
        // Customize legend (guidance table)
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, 20));  // increased legend font
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        
        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, 24);
        Font tickFont = new Font("SansSerif", Font.PLAIN, 20);
        
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        
        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);
        
        // Increase line thickness for better visibility
        XYLineAndShapeRenderer baseRenderer = new XYLineAndShapeRenderer();
        baseRenderer.setDefaultStroke(new BasicStroke(2.0f));
        plot.setRenderer(baseRenderer);
        
        // Configure axes to show numbers
        domainAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis = (NumberAxis) plot.getRangeAxis();
        
        // Show all integer ticks
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        // Ensure ticks are visible
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);
        
        // Set axis ranges
        domainAxis.setRange(1, 9);
        rangeAxis.setRange(1, 9);
        
        // Set tick marks visible
        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);
        
        // Adjust legend position more precisely
        chart.getLegend().setItemLabelPadding(new org.jfree.chart.ui.RectangleInsets(2, 2, 2, 2));
        chart.getLegend().setBorder(0, 0, 0, 0);
        
        // Custom renderer with error bars
        ErrorBarRenderer renderer = new ErrorBarRenderer(data);
        plot.setRenderer(renderer);
        
        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);
        
        // Increase image quality
        ChartUtils.saveChartAsPNG(
            outputDir.resolve(outputPath).toFile(), 
            chart, 
            1200,  // Doubled width for higher quality
            1200,  // Doubled height for higher quality
            null,
            true,  // Enable anti-aliasing
            9      // Highest compression quality
        );
    }
    
    private static class ErrorBarRenderer extends XYLineAndShapeRenderer {
        private final SpeedupDataReader.SpeedupData data;
        
        public ErrorBarRenderer(SpeedupDataReader.SpeedupData data) {
            this.data = data;
            
            // Configure line styles with thicker lines
            setDefaultStroke(new BasicStroke(2.5f));  // slightly thicker lines
            
            // Configure colors
            setSeriesPaint(0, new Color(0, 114, 189));  // Richer blue
            setSeriesPaint(1, new Color(255, 140, 0));  // Deeper orange
            setSeriesPaint(2, Color.GRAY);
            
            // Configure markers - larger and filled
            setSeriesShapesVisible(0, true);
            setSeriesShapesVisible(1, true);
            setSeriesShapesVisible(2, false);
            
            // Larger markers for mean points
            int markerSize = 8;  // increased from 4
            setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-markerSize, -markerSize, markerSize*2, markerSize*2));
            setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-markerSize, -markerSize, markerSize*2, markerSize*2));
            
            // Fill the markers
            setSeriesFillPaint(0, new Color(0, 114, 189));
            setSeriesFillPaint(1, new Color(255, 140, 0));
            setSeriesShapesFilled(0, true);
            setSeriesShapesFilled(1, true);
            
            // Dashed line for linear speedup
            setSeriesStroke(2, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                             10.0f, new float[]{10.0f}, 0.0f));
        }
        
        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                            PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
                            ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                            CrosshairState crosshairState, int pass) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
            
            // Draw error bars only for proposer and attestor
            if (series < 2 && item > 0) { // Skip first point (1,1)
                double x = data.cores[item];
                double y = series == 0 ? data.proposerSpeedup[item] : data.attestorSpeedup[item];
                double stdDev = series == 0 ? data.proposerStdDev[item] : data.attestorStdDev[item];
                
                double xJava2D = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
                double topJava2D = rangeAxis.valueToJava2D(y + stdDev, dataArea, plot.getRangeAxisEdge());
                double bottomJava2D = rangeAxis.valueToJava2D(y - stdDev, dataArea, plot.getRangeAxisEdge());
                
                // Draw error bar with thicker stroke
                g2.setStroke(new BasicStroke(1.5f));  // slightly thicker error bars
                g2.setColor(series == 0 ? new Color(0, 114, 189) : new Color(255, 140, 0));
                g2.draw(new java.awt.geom.Line2D.Double(xJava2D, topJava2D, xJava2D, bottomJava2D));
                
                // Draw caps with increased width
                double capWidth = 6.0;  // increased from 4.0
                g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, topJava2D, 
                                                       xJava2D + capWidth, topJava2D));
                g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, bottomJava2D, 
                                                       xJava2D + capWidth, bottomJava2D));
            }
        }
    }

    public static void main(String[] args) {
        try {
            Map<Integer, SpeedupDataReader.SpeedupData> allData = SpeedupDataReader.readSpeedupData();
            
            // Create individual charts for each conflict percentage
            int[] conflictPercentages = {15, 25, 35, 45};
            for (int percentage : conflictPercentages) {
                SpeedupDataReader.SpeedupData data = allData.get(percentage);
                if (data != null) {
                    createSpeedupChart(data, 
                                     "Speedup Analysis (" + percentage + "% Conflict)",
                                     "speedup_chart_" + percentage + "percent.png");
                }
            }
            
            // Create general chart combining all percentages
            SpeedupDataReader.SpeedupData generalData = SpeedupDataReader.combineData(allData);
            createSpeedupChart(generalData, 
                             "General Speedup Analysis (All Conflict Percentages)",
                             "speedup_chart_general.png");
            
            System.out.println("Charts generated successfully in target/charts directory!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 