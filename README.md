# ConThereum: Declarative Implementation

## Project Overview
ConThereum’s Declarative implementation used a minimal declarative approach to find one _suboptimal_ (near-optimal 
solution) and in this implementation instead of finding the optimal solution, the focus is minimizing the _walltime_
(the time that it takes for the scheduler to find the solution), while the _makespan_ (the time that suboptimal 
schedule takes to execute in all the computers) will be short enough to make it practically beneficial to replace 
the parallel solution with. 

The final result is to use this scheduling solution to minimize execution time and manage process conflicts in EVM. 
This approach improves the scheduling of Ethereum transactions across multiple resources.

This project is part of the ConThereum initiative, which explores transaction optimization techniques for Ethereum Virtual Machine (EVM) environments. 
This project is a Maven-based Java it.unitn.emvscheduling.greedy.application. It contains a main class, `Main.java`, to execute the benchmarks.

## License
This project is licensed under the MIT License.

## Prerequisites

- **Java 17** or higher
- **Apache Maven** (latest recommended)

## Project Structure

- **Folder:** `concurrent-evm/Declarative`
- **Main Java Class Path:** `concurrent-evm/Declarative/src/main/java/it.unitn.emvscheduling.greedy.application/Main.java`

- (will be completed later)

## Setup Instructions

1. Clone this repository:
   (The link has been temporarily omitted from this initial submission to maintain the integrity of the blind review process.)

   ```bash
   git clone <repository-url> 
   cd concurrent-evm/Declarative
   ```

2. Ensure all dependencies are installed by running
   ```bash
   mvn clean install 
   ```

3. Running the Main Class and Passing Its Parameters

To run the Main class, you need to pass a list of nine numbers as parameters in the following order:

Note: The values that are marked as **NS** (Not Supported) are not used in the algorithm and are kept from the OR-Tool 
implementation to make the input and output structure unified.

### Execution Environment Settings:
- **randomSeed**: The random seed to reproduce the scheduling problem specification with determinism and absence of any randomization. It is used to generate the process execution time and produce the conflicting processes.
- **numberOfWorkers**(NS): The number of threads that execute this program. 
- **maxSolverExecutionTimeInSeconds**(NS): Execution time for the it.unitn.emvscheduling.greedy.solver to produce the best possible solution during that time.

### Scheduling Problem Facts:
- **processCount**: The number of processes.
- **processExecutionTimeMin**: The minimum time each process will take.
- **processExecutionTimeMax**: The maximum time that a process will take.
- **computerCount**: The number of computers available.
- **conflictPercentage**: The percentage of conflicts among processes.
- **timeWeight**: The weight assigned to time in the calculations.

**Note:** The execution time for each process will be a random value greater than or equal to `processExecutionTimeMin` and less than or equal to `processExecutionTimeMax`. If you want all processes to have the same execution time, set both `processExecutionTimeMin` and `processExecutionTimeMax` to that desired value.

There are two options to execute the program: `args` and `files`. Examples for executing the program using each option are provided below.

### args

Pattern:

   ```bash
   mvn exec:java -Dexec.args="args randomSeed numberOfWorkers maxSolverExecutionTimeInSeconds processCount processExecutionTimeMin processExecutionTimeMax computerCount conflictPercentage timeWeight"
   ```

Example:

   ```bash
   mvn exec:java -Dexec.args="args 0 8 -1 -1 5 5 3 15 100"
   ```
- **Usage:** This option is the preferred approach for a quick test on a single sample.
- **Input:** The nine explained numbers is specified in the command
- **Output:** Results are displayed on the command prompt and appended to the end of the file
  `src/java/resources/output-accumulative.csv` too. The detailed output items are explained in the [Output Structure](#Output-structure)

### files
   ```bash
   mvn exec:java -Dexec.args="files"
   ```
- **Usage:** This option is preferred for batch execution of the it.unitn.emvscheduling.greedy.solver over a dataset.


- **Input:** The input file `src/java/resources/input.csv` includes a header row and it.unitn.emvscheduling.greedy.data rows, each containing the
  following items in order. The first two items (**no**, **groupNo**), are used solely for organizational purposes
  (item number and group number) and do not contribute to any calculations. The remaining items are explained earlier in
  this document.

  **no**, **groupNo**, randomSeed, numberOfWorkers, maxSolverExecutionTimeInSeconds, processCount,
  processExecutionTimeMin, processExecutionTimeMax, computerCount, conflictPercentage, timeWeight


- **Output:** The output file `src/java/resources/output.csv` is generated. For each row in the input, a
  corresponding row in the output contains the nine input items along with the output items. Additionally, the
  results are appended to `src/java/resources/output-accumulative.csv`, which accumulates the results of all
  executions (both `args` and `files`).  The detailed output items are explained in the [Output Structure](#Output-structure)


## Output Structure

The output produced by the 'args' option on the command line and the output in both output files `src/java/resources/output.csv` and `src/java/resources/output-accumulative.csv` have the same format and order as follows:

`no, groupNo, randomSeed, numberOfWorkers, maxSolverExecutionTimeInSeconds, processCount, processExecutionTimeMin,
processExecutionTimeMax, computerCount, conflictPercentage, timeWeight, SolverWallTime, OptimalScheduleTime,
parallelTimeSum, serialTimeHorizon, solverStatus`

The first non-bold items are repetitions of the input file to distinguish the input for the execution. The bold values are the results of the executions and are explained below:

- **SolverWallTime(ms)**: The time the it.unitn.emvscheduling.greedy.solver uses to find the result. In this current implementation walltime is in 
  Millisecond = 1/1000 s and in 6 digits of precision
- **Makespan**: The optimal schedule time found by the it.unitn.emvscheduling.greedy.solver for executing processes in parallel.
- **parallelTimeSum**: The summation of the last two numbers (SolverWallTime + OptimalScheduleTime), representing the practical time it will take for the processes to be executed using this scheduler in parallel.
- **serialTimeHorizon**: The time it will take if we execute all processes sequentially.
- **solverStatus**: The status of the it.unitn.emvscheduling.greedy.solver, which can be either "OPTIMAL" or "FEASIBLE." (in the current declarative 
  approach the status result is always feasible)


## Optimizing Hint and Experiments

## 1- Optimizing The Code Level Settings
* used `public` for all the variables instead of getter/setter or lombok annotations
* use primitive like int instead of objects like Integer
* no logger (like log4j) is used and there are a few System.out.print for tracing purpose that is commented for 
  benchmarking and maximizing the process speed, you can uncomment them for trace purpose.
* using the smallest possible variables for instance int instead of long or short instead of int
* not using generic classes in case of possibility
* lambda expressions and method references are avoided specially in comparator implementations
* use the minimum possible objects and less priority to fundamental object orientaion development principals to the 
  favor of performance improvement
* reuse of one object for many purposes and avoid cloning
* splitting the processes into assigned and non assigned and sorting on just the non assigned one was implemented 
  and it reduce the speedupFactor for processSortType=3 and  looseReviewRound=29 from 2.923 to 2.902 so I reverted 
  that code. 
* I have tried sorting the transactions in each round in loose approach but it decrease the performance, so I 
  reverted that.
* in the strategies that deal with the conflict duration, any time the conflict duration of unassigned processes are 
  summed

## 2- Optimizing Experimental Result
* Use the file mode of the execution and use a dummy it.unitn.emvscheduling.greedy.data as the first line, since it will take longer to 
  instantiations and setups and from line two on, it significantly reduces the wall time, like from millisecond to 
  nanosecond 


## Contact
For questions or contributions, please contact the project maintainer.

(The link has been temporarily omitted from this initial submission to maintain the integrity of the blind review process.)
