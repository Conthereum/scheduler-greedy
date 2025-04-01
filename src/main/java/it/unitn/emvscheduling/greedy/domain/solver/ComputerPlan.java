package it.unitn.emvscheduling.greedy.domain.solver;

import it.unitn.emvscheduling.greedy.domain.Computer;
import it.unitn.emvscheduling.greedy.domain.Process;

import java.util.ArrayList;
import java.util.List;

/**
 * initiated and ready to use timeSlots is maintained in construction and reset
 */
public class ComputerPlan {

    public Computer computer;
    public List<Process> processList = new ArrayList<>();
    public int busyTimeSum = 0;// save the total busy time for sake of access performance
    public int idleTimeSum = 0;
    public int firstFreeTime = 0; // the first free time on this computer to allocate

    public ComputerPlan(Computer computer) {
        this.computer = computer;
    }

//    public void addProcess(Process processToAdd, int timeToAdd) {
//        if(timeToAdd<firstFreeTime)
//            throw new RuntimeException("can not add process:"+processToAdd.processId+" at:" +timeToAdd +" because it " +
//                    "is before its first free time at:"+firstFreeTime);
//        processToAdd.startTime = timeToAdd;
//        processToAdd.endTime = timeToAdd + processToAdd.executionTime;
//        processToAdd.computer = this.computer;
//        processToAdd.idleDuration = timeToAdd - firstFreeTime;
//        processList.add(processToAdd);
//        this.busyTimeSum += processToAdd.executionTime;
//        this.idleTimeSum += processToAdd.idleDuration;
//        this.firstFreeTime = timeToAdd+processToAdd.executionTime;
//    }

    /**
     * remove the slot and update the start time of the slots after that one to be correct again
     *
     * @param slotToRemove
     */
    /*public void removeTimeSlot(TimeSlot slotToRemove) {
//        logger.trace("removeTimeSlot - slotToRemove: "+slotToRemove +", timeSlots: "+timeSlots);
        int index = timeSlots.indexOf(slotToRemove);
        if (index != -1 && index < timeSlots.size() - 1) {//if found an index and if it is not the last item of the list
            for (int i = index + 1; i < timeSlots.size(); i++) {
                *//*
                shift the start time of the slots after removed slot, backward
                 *//*
                if(slotToRemove.getBusyDuration() != timeSlots.get(index).getBusyDuration()){
                    System.out.println("Invalid status, the busy time is not synch. For event: "+slotToRemove.getBusyDuration() +
                            " and for plan:"+slotToRemove.getBusyDuration());
                }
                timeSlots.get(i).shiftSlotStart(-1 * slotToRemove.getBusyDuration());

                *//*todo slotToRemove.getBusyDuration() is not working and the idle time is not synch, why?*//*
//                timeSlots.get(i).shiftSlotStart(-1 * timeSlots.get(index).getBusyDuration());//temp

            }
        }
        if (index != -1) {
            Boolean result = timeSlots.remove(slotToRemove);
            if (!result) {
                throw new RuntimeException("Unexpectedly can't remove slot:" + slotToRemove + ".");
            }
            this.busyTime -= slotToRemove.getBusyDuration();
            this.idleTime -= slotToRemove.idleDuration;
        } else {
            System.out.println("can not find the removal requested slot as: " + slotToRemove);
        }
//        logger.trace("removeTimeSlot - done");
    }*/

    /**
     * return the planned process at the given time and in absence of any process return -1
     * Note: [start time, end time) starting included and ending not included
     * Note: this search algorithm is basic and can be improved by binary in case of need for better performance
     *
     * @param askedTime
     * @return
     */
    public int getProcessAt(int askedTime) {
            for (Process process : processList) {
                if (process.isActiveAt(askedTime)) {
                    return process.processId;
                }
            }
            return -1;
    }
}
