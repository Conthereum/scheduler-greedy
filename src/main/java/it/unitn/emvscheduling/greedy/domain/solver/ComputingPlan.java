package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Computer;
import it.unitn.emvscheduling.greedy.domain.ExecutionFacts;
import it.unitn.emvscheduling.greedy.domain.Process;

import java.util.ArrayList;
import java.util.List;

/**
 * in construction and reset, it is always maintained to initiate the planMap and having the ready to use
 * ComputerPlan for all the computers
 */
public class ComputingPlan {
    public List<ComputerPlan> computerPlanList;// in each index, there is the computerPlan of that computer id

//    private ExecutionFacts facts;

    public ComputingPlan(ExecutionFacts facts) {
        computerPlanList = new ArrayList<>(facts.computers.size());
        for (Computer computer : facts.computers) {
            ComputerPlan plan = new ComputerPlan(computer);
            computerPlanList.add(plan);
        }
    }

//    /**
//     * check if the processes are assigned
//     *
//     * @return
//     */
//    public boolean isComplete() {
//        Boolean completed = true;
//        for (Process process : facts.processes) {
//            if (process.computer == null) {
//                completed = false;
//                break;
//            }
//        }
//        return completed;
//    }

    /**
     * assign the process to the first free computer at the first non-conflicting time, make enough dalay to ensure
     * the lack of conflict in that computer but make sure to add that process to the first available computer
     *
     * @param process
     */
    public void assignStrictly(Process process) {
        //choosing computer
        int firstFreeTime = Integer.MAX_VALUE;
        ComputerPlan selectedComputerPlan = null;
        for (ComputerPlan computerPlan : computerPlanList) {
            if (computerPlan.firstFreeTime < firstFreeTime) {
                selectedComputerPlan = computerPlan;
                firstFreeTime = computerPlan.firstFreeTime;
            }
        }
        if (selectedComputerPlan != null) {
            process.computer = selectedComputerPlan.computer;
        } else {
            throw new RuntimeException("could not find computer for process " + process.processId);
        }

        //choosing start time
        int startTime = firstFreeTime;
        for (Process cProcess : process.conflictingProcesses) {
            if (cProcess.computer != null && !cProcess.computer.equals(selectedComputerPlan.computer)) {
                // Check for all possible overlap scenarios
                if (startTime < cProcess.endTime && (startTime + process.executionTime) > cProcess.startTime) {
                    startTime = cProcess.endTime;
                }
            }
        }
        process.startTime = startTime;
        process.endTime = startTime + process.executionTime;
        process.idleDuration = startTime - firstFreeTime;
        selectedComputerPlan.processList.add(process);
        selectedComputerPlan.firstFreeTime = process.endTime;
        selectedComputerPlan.busyTimeSum += process.executionTime;
        selectedComputerPlan.idleTimeSum += process.idleDuration;
    }

    /**
     * assign to the first free computer just if does not make any conflict with any other existing concurrent
     * processes in other computers, otherwise, just ignore it
     *
     * @param process
     */
    public boolean assignLoosely(Process process) {
        //choosing computer
        int firstFreeTime = Integer.MAX_VALUE;
        ComputerPlan selectedComputerPlan = null;
        for (ComputerPlan computerPlan : computerPlanList) {
            if (computerPlan.firstFreeTime < firstFreeTime) {
                selectedComputerPlan = computerPlan;
                firstFreeTime = computerPlan.firstFreeTime;
            }
        }

        //check if not conflicting with other processes
        int startTime = firstFreeTime;
        for (Process cProcess : process.conflictingProcesses) {
            if (cProcess.computer != null && !cProcess.computer.equals(selectedComputerPlan.computer)) {// is already assigned
                if (startTime >= cProcess.startTime && startTime <= cProcess.endTime) {
                    //conflict with another existing concurrent process
//                    process.attempts++;
                    return false;
                }
            }
        }
        //if is not returned, there is no conflict then:
        if (selectedComputerPlan != null) {
            process.computer = selectedComputerPlan.computer;
        } else {
            throw new RuntimeException("could not find computer for process " + process.processId);
        }

        process.startTime = startTime;
        process.endTime = startTime + process.executionTime;

//        process.idleDuration = startTime - firstFreeTime;
        selectedComputerPlan.processList.add(process);
        selectedComputerPlan.firstFreeTime = process.endTime;
        selectedComputerPlan.busyTimeSum += process.executionTime;
        selectedComputerPlan.idleTimeSum += process.idleDuration;
        return true;
    }

    public int getScheduleMakespan() {
        Integer maxMakespan = 0;
        for (ComputerPlan computerPlan : computerPlanList) {
            if (computerPlan.firstFreeTime > maxMakespan) {
                maxMakespan = computerPlan.firstFreeTime;
            }
        }
        return maxMakespan;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ComputerPlan computerPlan : computerPlanList) {
            sb.append("C-" + computerPlan.computer.computerId + ": ");
            for (Process process : computerPlan.processList) {
                sb.append("P-" + process.processId + "(" + process.startTime + ", " + process.executionTime + "), ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

//    public void addSlot(Computer computer, Process process) {
//        TimeSlot timeSlot = new TimeSlot(process);
////        if (existProcessInPlans(timeSlot)) {
////            System.out.println("duplicate process is requested to add:" + process);
////        }
//        computerPlanList.get(computer.computerId).addTimeSlot(timeSlot);
//    }

//    private boolean existProcessInPlans(TimeSlot timeSlot) {
//        for (ComputerPlan computerPlan : computerPlanList) {
//            if (computerPlan.timeSlots.indexOf(timeSlot) >= 0) {
//                return true;
//            }
//        }
//        return false;
//    }

//    public void removeSlot(Computer computer, Process process) {
//        TimeSlot timeSlot = new TimeSlot(process);
//        computerPlanList.get(computer.computerId).removeTimeSlot(timeSlot);
//    }

//    public void removeSlot(Process process) {//temporary method
//        TimeSlot timeSlot = new TimeSlot(process);
//        for (ComputerPlan computerPlan : computerPlanList) {
//            if (computerPlan.timeSlots.indexOf(timeSlot) != -1) {
//                removeSlot(computerPlan.computer, process);
//            }
//        }
//    }


    /**
     * @param processId
     * @return the timeSlot of givven processId by searching all the computers and return null if not find it.
     * Note that the computer id is accessible through TimeSlot.Process.Computer.id
     */
//    public TimeSlot getTimeSlot(int processId) {
//        for (ComputerPlan computerPlan : computerPlanList) {
//            for (TimeSlot slot : computerPlan.timeSlots) {
//                if (slot.process.processId == processId)
//                    return slot;
//            }
//        }
//        return null;
//    }

    /**
     * get the maximum of all busy time for all computers
     *
     * @return
     */
    public int getBusyTime() {
        if (computerPlanList == null || computerPlanList.size() == 0) {
            return 0;
        }
        int maxBusyTime = 0; // between all computer plans
        for (ComputerPlan computerPlan : computerPlanList) {
            if (computerPlan.busyTimeSum > maxBusyTime) {
                maxBusyTime = computerPlan.busyTimeSum;
            }
        }
        return maxBusyTime;
    }

    /**
     * @return sum of all idle time of all computers
     */
    private int getIdleTime() {
        if (computerPlanList == null || computerPlanList.size() == 0) {
            return 0;
        }
        int idleTimeSum = 0; // between all computer plans
        for (ComputerPlan computerPlan : computerPlanList) {
            idleTimeSum += computerPlan.idleTimeSum;
        }
        return idleTimeSum;
    }

    private int getUsedComputersCount() {
        int counter = 0;
        for (ComputerPlan computerPlan : computerPlanList) {
            if (computerPlan.busyTimeSum > 0) {
                counter++;
            }
        }
        return counter;
    }

   /* public int getScore() {
        //recalculating the scores
        hardScore = 0;
        mediumScore = 0;
        softScore = 0;


        //if a plan is possible with minimal computers, it is more favorable
        int usedComputers = getUsedComputersCount();
        softScore += -1 * usedComputers;

        //give hard penalty for unassigned processes to any computer
        // I specify nullable = false for computer in process class and so it is not needed now

        //give soft penalty for busy time
        int busyTime = (int) getBusyTime();

        //give soft penalty for any idle time before any processes in any computer
        int idleTime = (int) getIdleTime();

        //give penalty for computer cost model
        *//*int computerCosts = (int) costs;
        mediumScore += -1 * ((idleTime + busyTime) * timeWeigh) + (computerCosts * costWeigh);*//*
        mediumScore += -1 * (idleTime + busyTime);

        //avoid all conflicts so there is no need to penalty for it
        //give a VERY hard penalty for any conflicting transaction
//        int conflictingTime = getConflictingTime();
//        hardScore += (int) conflictingTime * Integer.MAX_VALUE;

        //give a VERY hard penalty for any commutativity deviation count
//        int commutativityDeviationCount = getCommutativityDeviationCount();
//        hardScore += (int) commutativityDeviationCount * Integer.MAX_VALUE;


        System.out.println("Soft: usedComputers: " + usedComputers + ", busyTime: " + busyTime + ", idleTime: " +
                +idleTime
//                + ", Hard: conflictingTime: " + conflictingTime + ", commutativityDeviationCount: " + commutativityDeviationCount
        );
        return 1000000*hardScore + 100*mediumScore + softScore;
    }*/

    /*private int getCosts() {
        if (computerPlanMap == null || computerPlanMap.keySet().isEmpty()) {
            return 0;
        }
        int costs = 0; // in all computer plans
        for (ComputerPlan computerPlan : computerPlanMap.values()) {
            Integer costPerOperation = computerPlan.computer.costPerOperation;
            Integer costPerIdleTime = computerPlan.computer.costPerIdleTime;

            for (TimeSlot slot: computerPlan.timeSlots) {
                costs += (slot.process.operationCount*costPerOperation);
                costs += (slot.process.idleTimeBeforeProcess*costPerIdleTime);
            }
        }
        return costs;
    }*/


    /**
     * sum up all the time that any two transactions have conflicts, if at any time more than tow transactions may
     * conflict, it is counted as twice.
     *
     * @return
     */
    /*private int getConflictingTime() {
        int emvBusyTime = (int) getBusyTime();
        int conflictingCount = 0;
        List<Integer> concurrentProcessList = new ArrayList<>();
        for (int time = 0; time <= emvBusyTime; time++) {//in each time
            concurrentProcessList.clear();
            for (ComputerPlan computerPlan : computerPlanMap.values()) {//iterate the computers
                int processId = computerPlan.getProcessAt(time);//find the active process
                if (processId != -1) {
                    concurrentProcessList.add(processId);
                }
            }
            conflictingCount += getConflictingCount(concurrentProcessList);
        }
        return conflictingCount;
    }*/

    /*private Integer getConflictingCount(List<Integer> concurrentProcessList) {
        Integer count = 0;
        UnorderedPair unorderedPair = new UnorderedPair();//instantiate once for better performance
        for (int i = 0; i < concurrentProcessList.size() - 1; i++) {
            for (int j = i + 1; j < concurrentProcessList.size(); j++) {
                unorderedPair.setValues(concurrentProcessList.get(i), concurrentProcessList.get(j));
                if (conflictsMap.get(unorderedPair) != null) {
                    count++;
                }
            }
        }
        return count;
    }
*/
    /**
     * counts how many times the commutativity constraint is deviated in all the computer plans and return the sum of
     * the counts.
     * <p>
     * Every non-commutative methods are conflicting, because conflict times are counted in another method so here we do
     * not consider the conflicting anymore, and so we just compare the actual execution start times (not slot start
     * time). so if two conflicting and non-commutative transactions starts with correct order but have overlap
     * execution time, this method does not count them because they are correct in terms of commutativity and in
     * terms of conflict will be counted in a separate method.
     *
     * @return
     */
/*    private Integer getCommutativityDeviationCount() {
        Integer commutativityDeviationCount = 0;
        for (OrderedPair pair : nonCommutativeList) {
            int firstProcessId = pair.first;
            int secondProcessId = pair.second;
            TimeSlot firstSlot = getTimeSlot(firstProcessId);
            TimeSlot secondSlot = getTimeSlot(secondProcessId);
            if(firstSlot != null && secondSlot != null) {
                if (!(firstSlot.getExeStart() < secondSlot.getExeStart())) {// if the second one start before the first
                    // one finish, then
                    commutativityDeviationCount++;
                }
            }
        }
        return commutativityDeviationCount;
    }*/


   /* public void resetWorkingSolution() {
        if (computerPlanMap != null) {
            for (Integer computerId : computerPlanMap.keySet()) {
                computerPlanMap.get(computerId).reset();
            }
        }
        hardScore = 0;
        softScore = 0;
    }*/
}
