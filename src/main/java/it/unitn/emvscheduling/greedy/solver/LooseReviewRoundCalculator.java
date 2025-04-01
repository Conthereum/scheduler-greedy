package it.unitn.emvscheduling.greedy.solver;

public class LooseReviewRoundCalculator {

    /**
     * Enum representing different heuristic approaches for computing looseReviewRound.
     */
    public enum HeuristicType {
        LINEAR_SCALING(1),
        LOGARITHMIC(2),
        EXPONENTIAL_DECAY(3),
        ADAPTIVE_THRESHOLD(4),
        WEIGHTED_COMBINATION(5);

        private final int value;

        HeuristicType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static HeuristicType getByValue(int value) {
            for (HeuristicType type : HeuristicType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No enum constant with value: " + value);
        }
    }

    /**
     * Selects the appropriate heuristic method based on the provided enum value
     * and returns the computed looseReviewRound.
     *
     * @param processesCount     The number of processes.
     * @param conflictPercentage The percentage of conflicting processes (0-100).
     * @param computersCount     The number of available computers.
     * @param heuristicType      The heuristic type to use for calculation.
     * @return The computed looseReviewRound value.
     */
    public static int getValue(int processesCount, int conflictPercentage, int computersCount, HeuristicType heuristicType) {
        switch (heuristicType) {
            case LINEAR_SCALING:
                return linearScalingHeuristic(processesCount, conflictPercentage, computersCount);
            case LOGARITHMIC:
                return logarithmicHeuristic(processesCount, conflictPercentage, computersCount);
            case EXPONENTIAL_DECAY:
                return exponentialDecayHeuristic(processesCount, conflictPercentage, computersCount);
            case ADAPTIVE_THRESHOLD:
                return adaptiveThresholdHeuristic(processesCount, conflictPercentage, computersCount);
            case WEIGHTED_COMBINATION:
                return weightedCombinationHeuristic(processesCount, conflictPercentage, computersCount);
            default:
                throw new IllegalArgumentException("Unknown heuristic type: " + heuristicType);
        }
    }

    // Assume these methods already exist in the class
    public static int linearScalingHeuristic(int processesCount, int conflictPercentage, int computersCount) {
        return Math.max(1, (processesCount * conflictPercentage) / (100 * computersCount));
    }

    public static int logarithmicHeuristic(int processesCount, int conflictPercentage, int computersCount) {
        return Math.max(1, (int) (Math.log(processesCount * conflictPercentage + 1) / Math.log(computersCount + 1)));
    }

    public static int exponentialDecayHeuristic(int processesCount, int conflictPercentage, int computersCount) {
        return Math.max(1, (int) (processesCount * Math.exp(-computersCount / (double) (conflictPercentage + 1))));
    }

    public static int adaptiveThresholdHeuristic(int processesCount, int conflictPercentage, int computersCount) {
        return Math.max(1, (conflictPercentage > 50) ? (processesCount / (computersCount / 2)) : (processesCount / computersCount));
    }

    public static int weightedCombinationHeuristic(int processesCount, int conflictPercentage, int computersCount) {
        return Math.max(1, (processesCount * conflictPercentage) / (computersCount * 10) + (int) Math.log(processesCount + 1));
    }
}
