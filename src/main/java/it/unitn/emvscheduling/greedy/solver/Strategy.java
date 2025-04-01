package it.unitn.emvscheduling.greedy.solver;

/**
 * All fields have the optimal default values
 */
public class Strategy {
    public AssignmentType assignmentType = AssignmentType.LOOSE;
    public ProcessSortType processSortType = ProcessSortType.MCDF;
    /**
     * default value of 9 (will be ignored in AssignmentType.STRICT)
     */
    public int looseReviewRound = 9;

    public Strategy() {
    }

    public Strategy(AssignmentType assignmentType, ProcessSortType processSortType, int looseReviewRound) {
        this.assignmentType = assignmentType;
        this.processSortType = processSortType;
        this.looseReviewRound = looseReviewRound;
    }

    public Strategy(ProcessSortType processSortType, int looseReviewRound) {
        this.processSortType = processSortType;
        this.looseReviewRound = looseReviewRound;
        if (looseReviewRound > 0) {
            this.assignmentType = AssignmentType.LOOSE;
        } else {
            this.assignmentType = AssignmentType.STRICT;
        }
    }

    /**
     * can implicitly be concluded by looseReviewRound, if looseReviewRound is equal to zero then STRICT otherwise LOOSE
     */
    public enum AssignmentType {
        /**
         * try to assign each process in the sorted order in the first available processor and in case of any
         * conflict, just skip this process and go easily for the next one of the sorted list. after trying for all
         * the transactions go again review from the beginning and do it as looseReviewRound times and after that
         * switch automatically to the STRICT strategy and assign the remaining.
         */
        LOOSE,

        /**
         * accommodating each process in the sorted order in the first available processor and in case of conflict,
         * just make some delais for this current process to solve any conflicts
         */
        STRICT
    }

    public enum ProcessSortType {
        FIFO(1),  // First In First Out
        MCCF(2),  // Most Conflicting Count First
        MCDF(3),  // Most Conflicting Duration First
        LCCF(4),  // Least Conflicting Count First
        LCDF(5);  // Least Conflicting Duration First

        private final int value;

        ProcessSortType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ProcessSortType getByValue(int value) {
            for (ProcessSortType type : ProcessSortType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No enum constant with value: " + value);
        }
    }
}
