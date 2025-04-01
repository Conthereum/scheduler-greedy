package it.unitn.emvscheduling.greedy.domain;

public class Computer {

    public int computerId;

    public Computer(int computerId) {
        this.computerId = computerId;
    }

//    public int costPerOperation;
//    public int costPerIdleTime;

    //    Not involved parameters in this phase
    /*is initiated with zero,  will increase by adding further processes to be calculated by this
     computer, can be idle between, in millisecond*/
    /*private Integer busyTime;
    private String name;
    private float clockSpeed; //GHz
    private int icp; // Instructions per Cycle (IPC) - can vary based on workload
    private float instructionSpeed; //Giga per second = clockSpeed * ICP*/

    /*private int cpuPower; // in gigahertz
    private int memory; // in gigabyte RAM
    private int networkBandwidth; // in gigabyte per hour
    private int cost; // in euro per month*/
}
