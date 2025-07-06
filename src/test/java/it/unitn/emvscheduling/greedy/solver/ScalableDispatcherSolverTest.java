package it.unitn.emvscheduling.greedy.solver;

import it.unitn.emvscheduling.greedy.data.DataGenerator;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.ExecutionOutput;
import it.unitn.emvscheduling.greedy.domain.ExecutionSettings;
import it.unitn.emvscheduling.greedy.domain.Process;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class ScalableDispatcherSolverTest {
    
    private ScalableDispatcherSolver solver;
    private ExecutionSettings settings;
    private Strategy strategy;
    
    @BeforeEach
    void setUp() {
        solver = new ScalableDispatcherSolver();
        settings = new ExecutionSettings(-1, -1, 42); // Fixed seed for reproducibility
        strategy = new Strategy(Strategy.ProcessSortType.MCDF, 29);
    }
    
    @Test
    void testNoConflictsScenario() {
        // Test with no conflicts - should achieve perfect speedup
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 10, 5, 10, 4, 0, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicts (since conflict percentage is 0)
        assertNoConflicts(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
        
        // Verify makespan is reasonable
        assertTrue(output.scheduleMakespan > 0, "Makespan should be positive");
        assertTrue(output.scheduleMakespan <= facts.processes.stream().mapToInt(p -> p.executionTime).sum(), 
                  "Makespan should not exceed total execution time");
    }
    
    @Test
    void testHighConflictScenario() {
        // Test with high conflicts - should still maintain correctness
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 20, 5, 10, 8, 80, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
        
        // Verify makespan is reasonable
        assertTrue(output.scheduleMakespan > 0, "Makespan should be positive");
    }
    
    @Test
    void testProposerMode() {
        // Test proposer mode specifically
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 15, 5, 10, 6, 30, 100);
        facts.isProposerMode = true;
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
    }
    
    @Test
    void testAttestorMode() {
        // Test attestor mode specifically
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 15, 5, 10, 6, 30, 100);
        facts.isProposerMode = false;
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
    }
    
    @Test
    void testScalabilityWithManyCores() {
        // Test with many cores to ensure scalability
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 50, 5, 10, 32, 25, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
        
        // Verify makespan is reasonable
        assertTrue(output.scheduleMakespan > 0, "Makespan should be positive");
    }
    
    @Test
    void testLoadBalancing() {
        // Test that load is reasonably balanced across cores
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 40, 5, 10, 8, 20, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Check load balancing
        Map<Integer, Integer> computerBusyTime = new HashMap<>();
        for (Process process : facts.processes) {
            computerBusyTime.merge(process.computer.computerId, process.executionTime, Integer::sum);
        }
        
        // Calculate load balance metric
        int totalBusyTime = computerBusyTime.values().stream().mapToInt(Integer::intValue).sum();
        double avgBusyTime = (double) totalBusyTime / facts.computers.size();
        double maxDeviation = computerBusyTime.values().stream()
                .mapToDouble(bt -> Math.abs(bt - avgBusyTime))
                .max()
                .orElse(0.0);
        
        // Load should be reasonably balanced (deviation should not be too high)
        assertTrue(maxDeviation <= avgBusyTime * 0.5, 
                  "Load should be reasonably balanced across computers");
    }
    
    @Test
    void testEdgeCaseSingleCore() {
        // Test edge case with single core
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 10, 5, 10, 1, 30, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
        
        // With single core, makespan should equal total execution time
        int totalExecutionTime = facts.processes.stream().mapToInt(p -> p.executionTime).sum();
        assertEquals(totalExecutionTime, output.scheduleMakespan, 
                    "With single core, makespan should equal total execution time");
    }
    
    @Test
    void testEdgeCaseManyProcesses() {
        // Test edge case with many processes
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 200, 5, 10, 16, 15, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Verify all processes are assigned
        assertAllProcessesAssigned(facts.processes);
        
        // Verify no conflicting processes overlap
        assertNoConflictingOverlaps(facts.processes);
        
        // Verify execution times are respected
        assertExecutionTimesRespected(facts.processes);
        
        // Verify makespan is reasonable
        assertTrue(output.scheduleMakespan > 0, "Makespan should be positive");
    }
    
    // Helper assertion methods
    
    private void assertAllProcessesAssigned(List<Process> processes) {
        for (Process process : processes) {
            assertNotNull(process.computer, "Process " + process.processId + " should be assigned to a computer");
            assertTrue(process.startTime >= 0, "Process " + process.processId + " should have valid start time");
            assertTrue(process.endTime > process.startTime, "Process " + process.processId + " should have valid end time");
        }
    }
    
    private void assertNoConflicts(List<Process> processes) {
        // Since conflict percentage is 0, no processes should have conflicts
        for (Process process : processes) {
            assertTrue(process.conflictingProcesses.isEmpty(), 
                      "Process " + process.processId + " should have no conflicts when conflict percentage is 0");
        }
    }
    
    private void assertNoConflictingOverlaps(List<Process> processes) {
        for (Process process1 : processes) {
            for (Process process2 : process1.conflictingProcesses) {
                // Check if these conflicting processes overlap in time
                boolean overlap = process1.startTime < process2.endTime && 
                                process1.endTime > process2.startTime;
                
                assertFalse(overlap, 
                           "Conflicting processes " + process1.processId + " and " + process2.processId + 
                           " should not overlap in time. Process1: [" + process1.startTime + ", " + process1.endTime + 
                           "], Process2: [" + process2.startTime + ", " + process2.endTime + "]");
            }
        }
    }
    
    private void assertExecutionTimesRespected(List<Process> processes) {
        for (Process process : processes) {
            int actualDuration = process.endTime - process.startTime;
            assertEquals(process.executionTime, actualDuration, 
                        "Process " + process.processId + " execution time should be respected");
        }
    }
    
    @Test
    void testDeterministicResults() {
        // Test that results are deterministic with same seed
        ExecutionFacts facts1 = DataGenerator.getBenchmark(42, 20, 5, 10, 8, 30, 100);
        ExecutionFacts facts2 = DataGenerator.getBenchmark(42, 20, 5, 10, 8, 30, 100);
        
        ExecutionOutput output1 = solver.solve(facts1, settings, strategy);
        ExecutionOutput output2 = solver.solve(facts2, settings, strategy);
        
        // Results should be identical with same seed
        assertEquals(output1.scheduleMakespan, output2.scheduleMakespan, 
                    "Results should be deterministic with same seed");
    }
    
    @Test
    void testPerformanceImprovement() {
        // Test that the scalable solver performs at least as well as expected
        ExecutionFacts facts = DataGenerator.getBenchmark(42, 30, 5, 10, 16, 25, 100);
        
        ExecutionOutput output = solver.solve(facts, settings, strategy);
        
        // Calculate speedup
        int totalExecutionTime = facts.processes.stream().mapToInt(p -> p.executionTime).sum();
        double speedup = (double) totalExecutionTime / output.scheduleMakespan;
        
        // Speedup should be reasonable (not negative, and ideally > 1 for multiple cores)
        assertTrue(speedup > 0, "Speedup should be positive");
        if (facts.computers.size() > 1) {
            assertTrue(speedup >= 1.0, "Speedup should be at least 1.0 with multiple cores");
        }
    }
} 