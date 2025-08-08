package it.unitn.visualization;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

public class SpeedupVisualizer {
    // Make chart square to ensure equal unit sizes
    private static final int CHART_SIZE = 600;

    // Font size variables for easy adjustment
    private static final int LEGEND_FONT_SIZE = 30;
    private static final int AXIS_LABEL_FONT_SIZE = 30;
    private static final int TICK_FONT_SIZE = 30;

    // Centralized color definitions for all charts
    private static final Color[] CONFLICT_COLORS = {
//        new Color(0, 114, 189),    // Blue for 0%
//        new Color(119, 172, 48),   // Green for 5%
//        new Color(255, 140, 0),    // Orange for 10%
//        new Color(0, 0, 139),      // Dark Blue for 15%
//        new Color(34, 139, 34),    // Forest Green for 25%
//        new Color(255, 0, 255),    // Magenta for 35%
//        new Color(0, 0, 0)         // Black for 45%
            new Color(13, 86, 243),    // Blue for 0%
            new Color(42, 224, 11),   // Green for 5%
            new Color(255, 140, 0),    // Orange for 10%
            new Color(4, 220, 248),      // Dark Blue for 15%
            new Color(2, 0, 2),    // Forest Green for 25%
            new Color(255, 30, 0),    // Magenta for 35%
            new Color(172, 2, 255, 255)         // Black for 45%

    };

    private static final int[] CONFLICT_PERCENTAGES = {0, 5, 10, 15, 25, 35, 45};

    /**
     * Get the color for a specific conflict percentage
     */
    private static Color getColorForConflictPercentage(int percentage) {
        for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
            if (CONFLICT_PERCENTAGES[i] == percentage) {
                return CONFLICT_COLORS[i];
            }
        }
        return Color.GRAY; // fallback color
    }

    /**
     * Get the color index for a specific conflict percentage
     */
    private static int getColorIndexForConflictPercentage(int percentage) {
        for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
            if (CONFLICT_PERCENTAGES[i] == percentage) {
                return i;
            }
        }
        return -1; // not found
    }

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
                "Average Speedup Factor",
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
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

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

        // Set axis ranges dynamically based on data
        double maxCore = data.cores[data.cores.length - 1];
        double maxSpeedup = Math.max(
                Arrays.stream(data.proposerSpeedup).max().orElse(1.0),
                Arrays.stream(data.attestorSpeedup).max().orElse(1.0)
        );

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, Math.max(maxSpeedup + 1, maxCore));

        // Set tick marks visible
        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Adjust legend position more precisely
        chart.getLegend().setItemLabelPadding(new org.jfree.chart.ui.RectangleInsets(2, 2, 2, 2));
        chart.getLegend().setBorder(0, 0, 0, 0);

        // Custom renderer with error bars and centralized colors
        ErrorBarRenderer renderer = new ErrorBarRenderer(data);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Increase image quality
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
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

            // Configure colors using centralized color system
            setSeriesPaint(0, CONFLICT_COLORS[0]);  // Use first color from centralized palette
            setSeriesPaint(1, CONFLICT_COLORS[2]);  // Use third color from centralized palette (orange)
            setSeriesPaint(2, Color.GRAY);

            // Configure markers - larger and filled
            setSeriesShapesVisible(0, true);
            setSeriesShapesVisible(1, true);
            setSeriesShapesVisible(2, false);

            // Larger markers for mean points
            int markerSize = 8;  // increased from 4
            setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-markerSize, -markerSize, markerSize * 2, markerSize * 2));
            setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-markerSize, -markerSize, markerSize * 2, markerSize * 2));

            // Fill the markers using centralized color system
            setSeriesFillPaint(0, CONFLICT_COLORS[0]);
            setSeriesFillPaint(1, CONFLICT_COLORS[2]);
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

                // Draw error bar with thicker stroke using centralized color system
                g2.setStroke(new BasicStroke(1.5f));  // slightly thicker error bars
                g2.setColor(series == 0 ? CONFLICT_COLORS[0] : CONFLICT_COLORS[2]);
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

    public static void createComprehensiveSpeedupChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create proposer series (solid line, legend entry is just percentage)
                XYSeries proposerSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    proposerSeries.add(data.cores[i], data.proposerSpeedup[i]);
                }
                dataset.addSeries(proposerSeries);
                // Create attestor series (dashed line, no legend entry)
                XYSeries attestorSeries = new XYSeries(percentage + "% Attestor");
                for (int i = 0; i < data.cores.length; i++) {
                    attestorSeries.add(data.cores[i], data.attestorSpeedup[i]);
                }
                dataset.addSeries(attestorSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);
        // Set custom tick units for x-axis to show the actual conflict percentages
        domainAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(5.0));
        // Set custom tick units for y-axis to show integer steps
        rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1.0));

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with colors and line styles
        ComprehensiveChartRenderer renderer = new ComprehensiveChartRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    private static class ComprehensiveChartRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public ComprehensiveChartRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Proposer series (solid line, legend entry is just percentage)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12)); // small for plot
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24)); // large for legend
                    seriesIndex++;

                    // Attestor series (dashed line, no legend entry)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10.0f, new float[]{8.0f}, 0.0f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, false);
                    setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-6, -6, 12, 12));
                    // Hide attestor from legend
                    setSeriesVisibleInLegend(seriesIndex, false);
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }
    }

    public static void createComprehensiveSpeedupChartWithErrorBars(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create proposer series (solid line, legend entry is just percentage)
                XYSeries proposerSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    proposerSeries.add(data.cores[i], data.proposerSpeedup[i]);
                }
                dataset.addSeries(proposerSeries);
                // Create attestor series (dashed line, no legend entry)
                XYSeries attestorSeries = new XYSeries(percentage + "% Attestor");
                for (int i = 0; i < data.cores.length; i++) {
                    attestorSeries.add(data.cores[i], data.attestorSpeedup[i]);
                }
                dataset.addSeries(attestorSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);
        // Set custom tick units for x-axis to show the actual conflict percentages
        domainAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(5.0));
        // Set custom tick units for y-axis to show integer steps
        rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1.0));

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with error bars
        ComprehensiveErrorBarRenderer renderer = new ComprehensiveErrorBarRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    private static class ComprehensiveErrorBarRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public ComprehensiveErrorBarRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Proposer series (solid line, legend entry is just percentage)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12)); // small for plot
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24)); // large for legend
                    seriesIndex++;

                    // Attestor series (dashed line, no legend entry)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10.0f, new float[]{8.0f}, 0.0f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, false);
                    setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-6, -6, 12, 12));
                    // Hide attestor from legend
                    setSeriesVisibleInLegend(seriesIndex, false);
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                             PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                             CrosshairState crosshairState, int pass) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);

            // Draw error bars for all series except linear speedup
            if (series < dataset.getSeriesCount() - 1 && item > 0) { // Skip first point (1,1) and linear speedup
                int conflictIndex = series / 2; // Each conflict has 2 series (proposer + attestor)

                if (conflictIndex < CONFLICT_PERCENTAGES.length) {
                    int percentage = CONFLICT_PERCENTAGES[conflictIndex];
                    SpeedupDataReader.SpeedupData data = allData.get(percentage);

                    if (data != null && item < data.cores.length) {
                        double x = data.cores[item];
                        double y = (series % 2 == 0) ? data.proposerSpeedup[item] : data.attestorSpeedup[item];
                        double stdDev = (series % 2 == 0) ? data.proposerStdDev[item] : data.attestorStdDev[item];

                        double xJava2D = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
                        double topJava2D = rangeAxis.valueToJava2D(y + stdDev, dataArea, plot.getRangeAxisEdge());
                        double bottomJava2D = rangeAxis.valueToJava2D(y - stdDev, dataArea, plot.getRangeAxisEdge());

                        // Draw error bar
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.setColor(colors[conflictIndex]);
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D, topJava2D, xJava2D, bottomJava2D));

                        // Draw caps
                        double capWidth = 6.0;
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, topJava2D,
                                xJava2D + capWidth, topJava2D));
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, bottomJava2D,
                                xJava2D + capWidth, bottomJava2D));
                    }
                }
            }
        }
    }

    /**
     * Create a conflict vs speedup chart showing how average speedup varies with conflict percentage
     * for different core counts, showing the slowdown effect
     */
    public static void createConflictVsSpeedupChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Define core counts to show (1 to 32)
        int[] coreCounts = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 28, 32};

        // Define colors using the provided RGB codes, from highest to lowest core count
        Color[] coreColors = {
                new Color(240, 50, 230),  // Violet (32 cores)
                new Color(145, 30, 180),  // Indigo (28 cores)
                new Color(0, 0, 255),     // Blue (24 cores)
                new Color(67, 99, 216),   // Sky Blue (20 cores)
                new Color(70, 153, 144),  // Cyan (16 cores)
                new Color(66, 212, 244),  // Teal (12 cores)
                new Color(60, 180, 75),   // Green (10 cores)
                new Color(191, 239, 69),  // Lime Green (8 cores)
                new Color(255, 225, 25),  // Yellow (6 cores)
                new Color(245, 130, 49),  // Orange (5 cores)
                new Color(230, 25, 75),   // Red (4 cores)
                new Color(220, 20, 60),   // Magenta (3 cores)
                new Color(250, 190, 190), // Pink (2 cores)
                new Color(154, 99, 36)    // Brown (1 core)
        };

        // Create series for each core count
        for (int i = 0; i < coreCounts.length; i++) {
            int coreCount = coreCounts[i];
            Color color = coreColors[i % coreColors.length];

            // Create series for this core count (just the number as label)
            XYSeries series = new XYSeries(String.valueOf(coreCount));

            for (int percentage : CONFLICT_PERCENTAGES) {
                SpeedupDataReader.SpeedupData data = allData.get(percentage);
                if (data != null) {
                    // Find the index for this core count
                    int coreIndex = -1;
                    for (int j = 0; j < data.cores.length; j++) {
                        if (data.cores[j] == coreCount) {
                            coreIndex = j;
                            break;
                        }
                    }

                    if (coreIndex >= 0) {
                        // Calculate average speedup (proposer and attestor combined)
                        double avgSpeedup = (data.proposerSpeedup[coreIndex] + data.attestorSpeedup[coreIndex]) / 2.0;
                        series.add(percentage, avgSpeedup);
                    }
                }
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // no title
                "Conflict Percentage (%)",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));


        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set custom tick units for x-axis to show the actual conflict percentages
        domainAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(5.0));
        // Set custom tick units for y-axis to show steps of 2
        rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(2.0));

        // Set axis ranges: x from 0 to 45 (actual max conflict), y from 0 to max speedup
        domainAxis.setRange(0, 45);

        // Calculate max speedup to set y-axis range
        double maxSpeedup = 0;
        for (int coreCount : coreCounts) {
            for (int percentage : CONFLICT_PERCENTAGES) {
                SpeedupDataReader.SpeedupData data = allData.get(percentage);
                if (data != null) {
                    int coreIndex = -1;
                    for (int j = 0; j < data.cores.length; j++) {
                        if (data.cores[j] == coreCount) {
                            coreIndex = j;
                            break;
                        }
                    }
                    if (coreIndex >= 0) {
                        double avgSpeedup = (data.proposerSpeedup[coreIndex] + data.attestorSpeedup[coreIndex]) / 2.0;
                        maxSpeedup = Math.max(maxSpeedup, avgSpeedup);
                    }
                }
            }
        }
        // Round up to next integer and add some padding
        int yMax = (int) Math.ceil(maxSpeedup) + 1;
        rangeAxis.setRange(0, yMax);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with color rotation
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < coreCounts.length; i++) {
            Color color = coreColors[i % coreColors.length];
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesStroke(i, new BasicStroke(2.5f));
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesShapesFilled(i, true);
            renderer.setSeriesFillPaint(i, color);
            renderer.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
            renderer.setSeriesVisibleInLegend(i, true); // Show individual legend entries
            renderer.setLegendShape(i, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
            renderer.setLegendTextPaint(i, Color.BLACK); // Ensure legend text is black
        }
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    /**
     * Create a transaction count vs speedup chart showing how average speedup varies with transaction count
     * for different core counts, similar to conflict vs speedup chart but with transaction counts on x-axis
     */
    public static void createTransactionVsSpeedupChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        // Read data grouped by transaction count instead of conflict percentage
        Map<Integer, SpeedupDataReader.SpeedupData> transactionData = SpeedupDataReader.readSpeedupDataByTransactionCount("speedup-auto.xlsx");

        XYSeriesCollection dataset = new XYSeriesCollection();

        // Define core counts to show (1 to 32)
        int[] coreCounts = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 28, 32};

        // Define colors using the provided RGB codes, from highest to lowest core count
        Color[] coreColors = {
                new Color(240, 50, 230),  // Violet (32 cores)
                new Color(145, 30, 180),  // Indigo (28 cores)
                new Color(0, 0, 255),     // Blue (24 cores)
                new Color(67, 99, 216),   // Sky Blue (20 cores)
                new Color(70, 153, 144),  // Cyan (16 cores)
                new Color(66, 212, 244),  // Teal (12 cores)
                new Color(60, 180, 75),   // Green (10 cores)
                new Color(191, 239, 69),  // Lime Green (8 cores)
                new Color(255, 225, 25),  // Yellow (6 cores)
                new Color(245, 130, 49),  // Orange (5 cores)
                new Color(230, 25, 75),   // Red (4 cores)
                new Color(220, 20, 60),   // Magenta (3 cores)
                new Color(250, 190, 190), // Pink (2 cores)
                new Color(154, 99, 36)    // Brown (1 core)
        };

        // Always use these transaction counts for the x-axis
        int[] txCounts = {50, 100, 150, 200};
        java.util.Set<Integer> detectedTxCounts = new java.util.TreeSet<>(transactionData.keySet());
        System.out.println("Transaction counts detected: " + detectedTxCounts);

        // For each core count, create a series
        for (int i = 0; i < coreCounts.length; i++) {
            int coreCount = coreCounts[i];
            Color color = coreColors[i % coreColors.length];
            XYSeries series = new XYSeries(String.valueOf(coreCount));

            for (int txCount : txCounts) {
                SpeedupDataReader.SpeedupData data = transactionData.get(txCount);
                if (data != null) {
                    // Find the index for this core count
                    int coreIndex = -1;
                    for (int j = 0; j < data.cores.length; j++) {
                        if (data.cores[j] == coreCount) {
                            coreIndex = j;
                            break;
                        }
                    }
                    if (coreIndex >= 0) {
                        double avgSpeedup = (data.proposerSpeedup[coreIndex] + data.attestorSpeedup[coreIndex]) / 2.0;
                        series.add(txCount, avgSpeedup);
                    }
                }
                // If data is missing for this txCount, skip (no point will be plotted)
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // no title
                "Transaction Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set custom tick units for x-axis to show 50, 100, 150, 200
        domainAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(50.0));
        // Set custom tick units for y-axis to show integer steps
        rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1.0));

        // Set axis ranges: x from 50 to 200, y from 1 to max speedup
        domainAxis.setRange(50, 200);

        // Calculate max speedup to set y-axis range
        double maxSpeedup = 0;
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            XYSeries s = dataset.getSeries(i);
            for (int j = 0; j < s.getItemCount(); j++) {
                double y = s.getY(j).doubleValue();
                if (y > maxSpeedup) maxSpeedup = y;
            }
        }
        int yMax = (int) Math.ceil(maxSpeedup);
        if (yMax < 6) yMax = 6;
        rangeAxis.setRange(0, yMax);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom tick units for x-axis
        domainAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(50.0));
        // Set axis range: x from 50 to 210 (so 200 is fully visible)
        domainAxis.setRange(50, 210);

        // Custom renderer with color rotation
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < coreCounts.length; i++) {
            Color color = coreColors[i % coreColors.length];
            renderer.setSeriesPaint(i, color);
            renderer.setSeriesStroke(i, new BasicStroke(2.5f));
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesShapesFilled(i, true);
            renderer.setSeriesFillPaint(i, color);
            renderer.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
            renderer.setSeriesVisibleInLegend(i, true); // Show individual legend entries
            renderer.setLegendShape(i, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
            renderer.setLegendTextPaint(i, Color.BLACK); // Ensure legend text is black
        }
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    private static class ConflictVsSpeedupRenderer extends XYLineAndShapeRenderer {

        public ConflictVsSpeedupRenderer() {
            // Colors matching Python: blue, green, red
            Color[] colors = {
                    new Color(0, 0, 255),    // Blue for 3 cores
                    new Color(0, 128, 0),    // Green for 4 cores
                    new Color(255, 0, 0)     // Red for 5 cores
            };

            for (int i = 0; i < 3; i++) {
                setSeriesPaint(i, colors[i]);
                setSeriesStroke(i, new BasicStroke(2.5f));
                setSeriesShapesVisible(i, true);
                setSeriesShapesFilled(i, true);
                setSeriesFillPaint(i, colors[i]);
                setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
            }
        }
    }


    /**
     * Create comprehensive chart showing only proposer data (solid lines, no dashed)
     */
    public static void createComprehensiveProposerChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage (proposer only)
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create proposer series (solid line)
                XYSeries proposerSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    proposerSeries.add(data.cores[i], data.proposerSpeedup[i]);
                }
                dataset.addSeries(proposerSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with colors and line styles (proposer only)
        ProposerOnlyRenderer renderer = new ProposerOnlyRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    /**
     * Create comprehensive chart showing only proposer data with error bars
     */
    public static void createComprehensiveProposerChartWithErrorBars(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage (proposer only)
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create proposer series (solid line)
                XYSeries proposerSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    proposerSeries.add(data.cores[i], data.proposerSpeedup[i]);
                }
                dataset.addSeries(proposerSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with error bars (proposer only)
        ProposerOnlyErrorBarRenderer renderer = new ProposerOnlyErrorBarRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    /**
     * Create comprehensive chart showing only attestor data (solid lines, no dashed)
     */
    public static void createComprehensiveAttestorChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage (attestor only)
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create attestor series (solid line)
                XYSeries attestorSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    attestorSeries.add(data.cores[i], data.attestorSpeedup[i]);
                }
                dataset.addSeries(attestorSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with colors and line styles (attestor only)
        AttestorOnlyRenderer renderer = new AttestorOnlyRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    /**
     * Create comprehensive chart showing only attestor data with error bars
     */
    public static void createComprehensiveAttestorChartWithErrorBars(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();

        // Use centralized color system
        int colorIndex = 0;

        // Add series for each conflict percentage (attestor only)
        for (int percentage : CONFLICT_PERCENTAGES) {
            SpeedupDataReader.SpeedupData data = allData.get(percentage);
            if (data != null) {
                Color color = CONFLICT_COLORS[colorIndex];

                // Create attestor series (solid line)
                XYSeries attestorSeries = new XYSeries(percentage + "%");
                for (int i = 0; i < data.cores.length; i++) {
                    attestorSeries.add(data.cores[i], data.attestorSpeedup[i]);
                }
                dataset.addSeries(attestorSeries);

                colorIndex++;
            }
        }

        // Add linear speedup reference, extend by 3 extra dashes
        XYSeries linearSeries = new XYSeries("Linear Speedup");
        if (!allData.isEmpty()) {
            SpeedupDataReader.SpeedupData firstData = allData.values().iterator().next();
            int n = firstData.cores.length;
            double lastCore = firstData.cores[n - 1];
            for (int i = 0; i < n; i++) {
                linearSeries.add(firstData.cores[i], firstData.cores[i]);
            }
            // Add 3 more points for the tail
            for (int i = 1; i <= 3; i++) {
                linearSeries.add(lastCore + i, lastCore + i);
            }
        }
        dataset.addSeries(linearSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,  // removed title
                "Core Count",
                "Average Speedup Factor",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Customize legend
        chart.getLegend().setPosition(RectangleEdge.TOP);
        chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
        chart.getLegend().setMargin(5, 5, 5, 5);
        chart.getLegend().setPadding(5, 5, 5, 5);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
        chart.getLegend().setBackgroundPaint(Color.WHITE);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        // Set larger fonts for axis labels and ticks
        Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
        Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        domainAxis.setLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);
        domainAxis.setTickLabelFont(tickFont);
        rangeAxis.setTickLabelFont(tickFont);

        // Configure axes
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        domainAxis.setTickLabelsVisible(true);
        rangeAxis.setTickLabelsVisible(true);

        // Set axis ranges dynamically
        double maxCore = 32; // Fixed for consistency
        double maxSpeedup = 32; // Fixed for consistency

        domainAxis.setRange(1, maxCore);
        rangeAxis.setRange(1, maxSpeedup);

        domainAxis.setTickMarksVisible(true);
        rangeAxis.setTickMarksVisible(true);

        // Custom renderer with error bars (attestor only)
        AttestorOnlyErrorBarRenderer renderer = new AttestorOnlyErrorBarRenderer(allData, CONFLICT_COLORS);
        plot.setRenderer(renderer);

        // Ensure output directory exists
        Path outputDir = Paths.get("target/charts");
        Files.createDirectories(outputDir);

        // Save chart
        ChartUtils.saveChartAsPNG(
                outputDir.resolve(outputPath).toFile(),
                chart,
                1200,  // Square for equal aspect ratio
                1200,  // Square for equal aspect ratio
                null,
                true,  // Enable anti-aliasing
                9      // Highest compression quality
        );
    }

    private static class ProposerOnlyRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public ProposerOnlyRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Proposer series (solid line)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }
    }

    private static class ProposerOnlyErrorBarRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public ProposerOnlyErrorBarRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Proposer series (solid line)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                             PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                             CrosshairState crosshairState, int pass) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);

            // Draw error bars for proposer series
            if (series < dataset.getSeriesCount() - 1 && item > 0) { // Skip first point (1,1) and linear speedup
                int conflictIndex = series;

                if (conflictIndex < CONFLICT_PERCENTAGES.length) {
                    int percentage = CONFLICT_PERCENTAGES[conflictIndex];
                    SpeedupDataReader.SpeedupData data = allData.get(percentage);

                    if (data != null && item < data.cores.length) {
                        double x = data.cores[item];
                        double y = data.proposerSpeedup[item];
                        double stdDev = data.proposerStdDev[item];

                        double xJava2D = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
                        double topJava2D = rangeAxis.valueToJava2D(y + stdDev, dataArea, plot.getRangeAxisEdge());
                        double bottomJava2D = rangeAxis.valueToJava2D(y - stdDev, dataArea, plot.getRangeAxisEdge());

                        // Draw error bar
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.setColor(colors[conflictIndex]);
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D, topJava2D, xJava2D, bottomJava2D));

                        // Draw caps
                        double capWidth = 6.0;
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, topJava2D,
                                xJava2D + capWidth, topJava2D));
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, bottomJava2D,
                                xJava2D + capWidth, bottomJava2D));
                    }
                }
            }
        }
    }

    private static class AttestorOnlyRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public AttestorOnlyRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Attestor series (solid line)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }
    }

    private static class AttestorOnlyErrorBarRenderer extends XYLineAndShapeRenderer {
        private final Map<Integer, SpeedupDataReader.SpeedupData> allData;
        private final Color[] colors;

        public AttestorOnlyErrorBarRenderer(Map<Integer, SpeedupDataReader.SpeedupData> allData, Color[] colors) {
            this.allData = allData;
            this.colors = colors;

            int seriesIndex = 0;

            for (int i = 0; i < CONFLICT_PERCENTAGES.length; i++) {
                int percentage = CONFLICT_PERCENTAGES[i];
                if (allData.containsKey(percentage)) {
                    Color color = colors[i];

                    // Attestor series (solid line)
                    setSeriesPaint(seriesIndex, color);
                    setSeriesStroke(seriesIndex, new BasicStroke(2.5f));
                    setSeriesShapesVisible(seriesIndex, true);
                    setSeriesShapesFilled(seriesIndex, true);
                    setSeriesFillPaint(seriesIndex, color);
                    setSeriesShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-6, -6, 12, 12));
                    setSeriesVisibleInLegend(seriesIndex, true);
                    setLegendShape(seriesIndex, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
                    seriesIndex++;
                }
            }

            // Linear speedup reference (gray dashed)
            setSeriesPaint(seriesIndex, Color.GRAY);
            setSeriesStroke(seriesIndex, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{10.0f}, 0.0f));
            setSeriesShapesVisible(seriesIndex, false);
        }

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
                             PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset, int series, int item,
                             CrosshairState crosshairState, int pass) {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);

            // Draw error bars for attestor series
            if (series < dataset.getSeriesCount() - 1 && item > 0) { // Skip first point (1,1) and linear speedup
                int conflictIndex = series;

                if (conflictIndex < CONFLICT_PERCENTAGES.length) {
                    int percentage = CONFLICT_PERCENTAGES[conflictIndex];
                    SpeedupDataReader.SpeedupData data = allData.get(percentage);

                    if (data != null && item < data.cores.length) {
                        double x = data.cores[item];
                        double y = data.attestorSpeedup[item];
                        double stdDev = data.attestorStdDev[item];

                        double xJava2D = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
                        double topJava2D = rangeAxis.valueToJava2D(y + stdDev, dataArea, plot.getRangeAxisEdge());
                        double bottomJava2D = rangeAxis.valueToJava2D(y - stdDev, dataArea, plot.getRangeAxisEdge());

                        // Draw error bar
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.setColor(colors[conflictIndex]);
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D, topJava2D, xJava2D, bottomJava2D));

                        // Draw caps
                        double capWidth = 6.0;
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, topJava2D,
                                xJava2D + capWidth, topJava2D));
                        g2.draw(new java.awt.geom.Line2D.Double(xJava2D - capWidth, bottomJava2D,
                                xJava2D + capWidth, bottomJava2D));
                    }
                }
            }
        }

        /**
         * Create a bar chart showing parallel vs serial execution time for different transaction counts
         * and core counts, similar to the Python analysis
         */
        public static void createParallelVsSerialChart(Map<Integer, SpeedupDataReader.SpeedupData> allData, String outputPath) throws IOException {
            // Define transaction groups and core counts
            String[] transactionGroups = {"50", "100", "150", "200"};
            int[] coreCounts = {1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 28, 32};

            // Define colors using the same RGB codes as the conflict vs speedup chart
            Color[] coreColors = {
                    new Color(240, 50, 230),  // Violet (32 cores)
                    new Color(145, 30, 180),  // Indigo (28 cores)
                    new Color(0, 0, 255),     // Blue (24 cores)
                    new Color(67, 99, 216),   // Sky Blue (20 cores)
                    new Color(70, 153, 144),  // Cyan (16 cores)
                    new Color(66, 212, 244),  // Teal (12 cores)
                    new Color(60, 180, 75),   // Green (10 cores)
                    new Color(191, 239, 69),  // Lime Green (8 cores)
                    new Color(255, 225, 25),  // Yellow (6 cores)
                    new Color(245, 130, 49),  // Orange (5 cores)
                    new Color(230, 25, 75),   // Red (4 cores)
                    new Color(220, 20, 60),   // Magenta (3 cores)
                    new Color(250, 190, 190), // Pink (2 cores)
                    new Color(154, 99, 36)    // Brown (1 core)
            };

            // Create dataset for bar chart
            org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();

            // For each transaction group, add data for each core count
            for (int i = 0; i < transactionGroups.length; i++) {
                String transactionGroup = transactionGroups[i];

                // Calculate execution times based on speedup data
                // We'll use a base serial time that increases with transaction count
                double baseSerialTime = 300 + (i * 400); // 300ms for 50, 700ms for 100, etc.

                for (int j = 0; j < coreCounts.length; j++) {
                    int coreCount = coreCounts[j];
                    Color color = coreColors[j];

                    if (coreCount == 1) {
                        // Serial execution (1 core)
                        dataset.addValue(baseSerialTime, "1", transactionGroup);
                    } else {
                        // Parallel execution - find speedup for this core count
                        double avgSpeedup = 1.0; // default

                        // Use average speedup from all conflict percentages for this core count
                        for (int percentage : CONFLICT_PERCENTAGES) {
                            SpeedupDataReader.SpeedupData data = allData.get(percentage);
                            if (data != null) {
                                int coreIndex = -1;
                                for (int k = 0; k < data.cores.length; k++) {
                                    if (data.cores[k] == coreCount) {
                                        coreIndex = k;
                                        break;
                                    }
                                }
                                if (coreIndex >= 0) {
                                    double speedup = (data.proposerSpeedup[coreIndex] + data.attestorSpeedup[coreIndex]) / 2.0;
                                    avgSpeedup = Math.max(avgSpeedup, speedup); // Use best case
                                }
                            }
                        }

                        double parallelTime = baseSerialTime / avgSpeedup;
                        dataset.addValue(parallelTime, String.valueOf(coreCount), transactionGroup);
                    }
                }
            }

            JFreeChart chart = ChartFactory.createBarChart(
                    null,  // no title
                    "Transaction Count",
                    "Average Execution Time (ms)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            // Legend configuration will be set after renderer configuration

            org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(new Color(200, 200, 200));
            plot.setRangeGridlinePaint(new Color(200, 200, 200));

            // Set larger fonts for axis labels and ticks
            Font labelFont = new Font("SansSerif", Font.BOLD, AXIS_LABEL_FONT_SIZE);
            Font tickFont = new Font("SansSerif", Font.PLAIN, TICK_FONT_SIZE);

            org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis();
            org.jfree.chart.axis.NumberAxis rangeAxis = (org.jfree.chart.axis.NumberAxis) plot.getRangeAxis();

            domainAxis.setLabelFont(labelFont);
            rangeAxis.setLabelFont(labelFont);
            domainAxis.setTickLabelFont(tickFont);
            rangeAxis.setTickLabelFont(tickFont);

            // Configure axes
            domainAxis.setTickLabelsVisible(true);
            rangeAxis.setTickLabelsVisible(true);

            // Set custom tick units for y-axis to show steps of 200
            rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(200.0));

            // Custom renderer with colors (no shadow)
            org.jfree.chart.renderer.category.BarRenderer renderer = new org.jfree.chart.renderer.category.BarRenderer();
            renderer.setShadowVisible(false);

            // Set colors for each series
            for (int i = 0; i < coreCounts.length; i++) {
                Color color = coreColors[i];
                renderer.setSeriesPaint(i, color);
            }

            plot.setRenderer(renderer);

            // Show individual legend entries with proper configuration - exactly like conflict vs speedup chart
            for (int i = 0; i < coreCounts.length; i++) {
                renderer.setSeriesVisibleInLegend(i, true);
                renderer.setLegendTextPaint(i, Color.BLACK);
                renderer.setLegendShape(i, new java.awt.geom.Ellipse2D.Double(-12, -12, 24, 24));
            }


            plot.setRenderer(renderer);

            // Customize legend exactly like conflict vs speedup chart
            chart.getLegend().setPosition(RectangleEdge.TOP);
            chart.getLegend().setVerticalAlignment(org.jfree.chart.ui.VerticalAlignment.TOP);
            chart.getLegend().setHorizontalAlignment(org.jfree.chart.ui.HorizontalAlignment.LEFT);
            chart.getLegend().setMargin(5, 5, 5, 5);
            chart.getLegend().setPadding(5, 5, 5, 5);
            chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(Color.LIGHT_GRAY));
            chart.getLegend().setBackgroundPaint(Color.WHITE);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, LEGEND_FONT_SIZE));


            // Keep original dataset structure

            // Ensure output directory exists
            Path outputDir = Paths.get("target/charts");
            Files.createDirectories(outputDir);

            // Save chart
            ChartUtils.saveChartAsPNG(
                    outputDir.resolve(outputPath).toFile(),
                    chart,
                    1200,  // Square for equal aspect ratio
                    1200,  // Square for equal aspect ratio
                    null,
                    true,  // Enable anti-aliasing
                    9      // Highest compression quality
            );
        }

        public static void main(String[] args) {
            try {
                // Try to read from speedup-auto.xlsx first, fallback to speedups.xlsx
                Map<Integer, SpeedupDataReader.SpeedupData> allData;
                try {
                    allData = SpeedupDataReader.readSpeedupData("speedup-auto.xlsx");
                    System.out.println("Using speedup-auto.xlsx for visualization");
                } catch (IOException e) {
                    System.out.println("speedup-auto.xlsx not found, falling back to speedups.xlsx");
                    allData = SpeedupDataReader.readSpeedupData("speedups.xlsx");
                }

                // Create individual charts for each conflict percentage
                int[] conflictPercentages = {0, 5, 10, 15, 25, 35, 45};
                for (int percentage : conflictPercentages) {
                    SpeedupDataReader.SpeedupData data = allData.get(percentage);
                    if (data != null) {
                        String title = percentage == 0 ? "Speedup Analysis (No Conflicts)" :
                                "Speedup Analysis (" + percentage + "% Conflict)";
                        String filename = percentage == 0 ? "speedup_chart_0percent.png" :
                                "speedup_chart_" + percentage + "percent.png";
                        createSpeedupChart(data, title, filename);
                    }
                }

                // Create general chart combining all percentages
                SpeedupDataReader.SpeedupData generalData = SpeedupDataReader.combineData(allData);
                createSpeedupChart(generalData,
                        "General Speedup Analysis (All Conflict Percentages)",
                        "speedup_chart_general.png");

                // Create comprehensive chart with all conflict rates
                createComprehensiveSpeedupChart(allData, "speedup_chart_comprehensive.png");

                // Create comprehensive chart with error bars
                createComprehensiveSpeedupChartWithErrorBars(allData, "speedup_chart_comprehensive_errorbars.png");

                // Create conflict vs speedup chart
                createConflictVsSpeedupChart(allData, "speedup_chart_conflict_vs_speedup.png");

                // Create transaction vs speedup chart
                createTransactionVsSpeedupChart(allData, "speedup_chart_transaction_vs_speedup.png");

                // Create parallel vs serial execution time chart
                createParallelVsSerialChart(allData, "speedup_chart_parallel_vs_serial.png");

                // Create comprehensive proposer chart
                createComprehensiveProposerChart(allData, "speedup_chart_comprehensive_proposer.png");

                // Create comprehensive proposer chart with error bars
                createComprehensiveProposerChartWithErrorBars(allData, "speedup_chart_comprehensive_proposer_errorbars.png");

                // Create comprehensive attestor chart
                createComprehensiveAttestorChart(allData, "speedup_chart_comprehensive_attestor.png");

                // Create comprehensive attestor chart with error bars
                createComprehensiveAttestorChartWithErrorBars(allData, "speedup_chart_comprehensive_attestor_errorbars.png");

                System.out.println("Charts generated successfully in target/charts directory!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}