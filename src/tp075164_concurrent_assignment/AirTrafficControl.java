package src.tp075164_concurrent_assignment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class AirTrafficControl implements Runnable{
    // arrays for waiting, finished planes and for the 3 gates in the airport
    private final int[] AirportGates;
    private final int[] WaitingToLand;
    private final int[] FinishedPlanes;

    private int WaitingQueue = 0;

    //Reentrant lock and conditions to prevent race conditions and ensure thread safe operatins
    public final Lock atcLock = new ReentrantLock();
    public final Condition atcCondition = atcLock.newCondition();
    private final Condition TakeOffLock = atcLock.newCondition();
    private final Condition EmergencyPlaneLock = atcLock.newCondition();

    // volatile flag to check if the airport is still in operation or has finished all the tasks
    private volatile boolean isActive = true;

    // list to store the planes that request for take off
    private final ArrayList<Integer> TakeOffReq = new ArrayList<>();

    //falg to check if plane has taken off
    private boolean PlaneTakeOff = false;

    // check if emergency plane has landed and print the ATC permits emergency landing outptu
    int EmergencyRequest = 0;
    Boolean EmergencyPlaneLanded = false;

    // flag to check if the runway is empty or a plane is currently using it
    Boolean RunwayFree = true;


    public AirTrafficControl() {
        //initialise all the arrays
        AirportGates = new int[3];
        WaitingToLand = new int[6];
        FinishedPlanes = new int[6];

        //fill them with null (-1)
        for (int i = 0; i < AirportGates.length; i++) {
            AirportGates[i] = -1;
        }
        for (int i = 0; i < WaitingToLand.length; i++) {
            WaitingToLand[i] = -1;
            FinishedPlanes[i] = -1;
        }
    }

    //Signal the main class that ATC tasks are finished
    public void StopAirport() {
        atcLock.lock();
        try {
            isActive = false;
            ConcurrentAssignment.ShutDown = true;
            atcCondition.signalAll();
            TakeOffLock.signalAll();
        }
        finally {
            atcLock.unlock();
        }
    }

    //Method to check if all planes have landed and disembarked, meaning all atc tasks are finished
    public Boolean AirportIdle() {
        atcLock.lock();
        try {
            boolean emptyGates = true;
            for (int gates : AirportGates) {
                if (gates != -1) { // if all the airport gates are not empty, return false
                    emptyGates = false;
                    break;
                }
            }
            boolean emptyQueue = (WaitingQueue == 0); // emptyQueue flag depends on if the waiting queue amount is 0, meaning no planes left
            boolean planesFinished = true;
            for (int planes : FinishedPlanes) {
                if (planes == -1) { //checks if there are any planes that are not in the finished planes list yet, if not return false
                    planesFinished = false;
                    break;
                }
            }
            return emptyGates && emptyQueue && planesFinished; // returns the collective bool value of all 3 flags
        }
        finally {
            atcLock.unlock();
        }
    }

    //checks if a plane already exists, returns true if they are
    private boolean planeExist(int planeID) {
        for (int id : WaitingToLand) {
            if (id == planeID) return true; // Plane is already in the queue
        }
        for (int id : AirportGates) {
            if (id == planeID) return true; // Plane is already at a gate
        }
        for (int id : FinishedPlanes) {
            if (id == planeID) return true; // Plane has already finished
        }

        return false;
    }

    // Planes request atc for landing here.
    public Boolean RequestLanding(int planeID, boolean emergency) {
        atcLock.lock();
        try {
            // Check if the planeID is already in any queue (array)
            if (planeExist(planeID)) {
                return false;
            }

            // Add to the waiting queue if space is available and plane has not requested permission before
            else if (WaitingQueue < WaitingToLand.length) {
                WaitQueue(planeID, emergency);
                // check in AirportGates if 2 gates are occupied, then wait and dont assignGates.
                //Check through Request for Emergenncy Landing Request and push Emergency Plane to start of Waiting Queue
            }
            else {
                return false;
            }

            while (!isPlaneAssigned(planeID)) { // wait until plane has confirmed to be assigned a gate
                atcCondition.await();
            }
            return true;
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            atcLock.unlock();
        }
    }

    // used in requestLanding method to check if plane has approved for landing and has landed.
    private boolean isPlaneAssigned(int planeID) {
        for (int gate : AirportGates) {
            if (gate == planeID) { //checks if the planeID matches any in the gates
                return true;
            }
        }
        return false;
    }

    // Wait queue function where plane gets added when request landing
    private void WaitQueue(int planeID, boolean emergency) {
        if (WaitingQueue < WaitingToLand.length) {
            if (emergency) { //checks if plane has emergency or not
                for (int i = WaitingQueue; i > 0; i--) {
                    WaitingToLand[i] = WaitingToLand[i - 1]; // if emergency, push existing planes back
                }
                WaitingToLand[0] = planeID; // emergency plane added to front of the queue
                WaitingQueue++;
                EmergencyPlaneLanded = true; // set emergency plane landed to true as already requested)
                EmergencyPlaneLock.signalAll(); // signal to await condition in AssignPlaneGate() to break out
            }
            else {
                WaitingToLand[WaitingQueue] = planeID; //add planes to the waiting queue normally
                WaitingQueue++;
            }
            atcCondition.signalAll();
        }
    }

    //Used to check how many gates are being used
    private int countFullGates() {
        int count = 0;
        for (int gate : AirportGates) {
            if (gate != -1) {
                count++; // increments count if gate is not empty
            }
        }
        return count;
    }

    // Assigns plane to a gate if available
    public void AssignPlaneGate() {
        atcLock.lock();
        try {
            while (!RunwayFree) { //checks if runway is free, if not then waits
                atcCondition.await();
            }
            boolean assigned = false;
            for (int i = 0; i < AirportGates.length; i++) {
                int planeID;
                if (AirportGates[i] == -1 && WaitingQueue > 0) { // checks if a gate is empty, and there is a plane waiting to land
                    if (countFullGates() == 2 && EmergencyRequest == 0){
                        while(!EmergencyPlaneLanded) {// if 2 planes already landed, then waits and checks for emergency plane
                            EmergencyPlaneLock.await();
                        }
                        planeID = LeaveQueue();  //if emergency has requested, leave wait queue and ATC outputs, acknowledging emergency plane and allowing to land
                        System.out.println(Thread.currentThread().getName() + ": ATC: A Gate is available. Emergency Plane " + planeID + " is given priority. Checking if Runway is free");
                        EmergencyRequest++;
                    }
                    else {
                        planeID = LeaveQueue(); //if not emergency plane, output regular message
                        System.out.println(Thread.currentThread().getName() + ": ATC: Plane " + planeID + ", A Gate is available. Checking if Runway is free");
                    }


                    Runway(planeID, "Landing");  // goes through runway method, with landing messages prompted. Runway method exclusive, so only 1 plane can us at a time
                    AirportGates[i] = planeID; // assign plane to gate

                    System.out.println(Thread.currentThread().getName() + ": ATC: Plane " + AirportGates[i] + " is assigned to gate " + (i + 1));

                    RunwayFree = true; //sets runway to true after plane docks

                    atcCondition.signalAll();

                    assigned = true;  // Mark that a plane was assigned
                    break;
                }

            }
            if (!assigned && WaitingQueue > 0) {
                for (int i = 0; i < WaitingQueue; i++) {
                    System.out.println("ATC: Gates are full. Plane " + WaitingToLand[i] + " has been requested to wait.");
                }
            }
            
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            atcLock.unlock();
        }
    }

    // Runway method. reentrant lock ensure only 1 action can happen at a time, eitheer landing or take off
    public void Runway(int planeID, String action) {
        atcLock.lock();
        try {
            if (RunwayFree) { // goes through landing, time taken for landing and for assigning a gate to plane
                if (Objects.equals(action, "Landing")) {
                    System.out.println(Thread.currentThread().getName() + ": ATC: Runway is free. Plane " + planeID + " has permission to land.");
                    ConcurrentAssignment.sleep(500);
                    System.out.println(Thread.currentThread().getName() + ": ATC: Plane " + planeID + " has landed on the runway. Assigning gate now.");
                    ConcurrentAssignment.sleep(500);
                }
                else if (Objects.equals(action, "Leaving")) { //goes through taking off runway
                    System.out.println(Thread.currentThread().getName() + ": ATC: Runway is free. Plane " + planeID + " is permitted to take off.");
                    ConcurrentAssignment.sleep(500);
                    System.out.println(Thread.currentThread().getName() + ": ATC: Plane " + planeID + " is taking off from Runway.");
                    ConcurrentAssignment.sleep(500);
                }
                RunwayFree = false;
            }
        }
        finally {
            atcLock.unlock();
        }
    }

    // method to remove first plane from the waiting queue
    private int LeaveQueue() {
        int planeID = WaitingToLand[0]; // takes first plane in queue

        if (WaitingQueue == 0) { // check if queue is empty
            return -1;
        }

        for (int i = 1; i < WaitingQueue; i++) {
            WaitingToLand[i-1] = WaitingToLand[i]; // move all planes 1 space front of the queue
        }
        WaitingToLand[WaitingQueue -1] = -1;
        WaitingQueue--;

        return planeID; // returns the first plane in queue
    }

    // method for plane to request take off once all tasks have finished
    public void RequestTakeOff(int PlaneID) {
        atcLock.lock();
        try {
            for (int p : AirportGates) {
                if (p == PlaneID && !TakeOffReq.contains(PlaneID)) { //checks if plane exists in a gate and if plane does not have a take off request
                    TakeOffReq.add(PlaneID); // adds to take off req list
                    PlaneTakeOff = true;
                }
            }
            TakeOffLock.signalAll();
        }
        finally {
            atcLock.unlock();
        }
    }

    // ATC checks for take off requests
    private int TakeOffRequestCheck() {
        atcLock.lock();
        try {

            if (TakeOffReq.isEmpty()) {
                TakeOffLock.await(); // if take off request list is empty, wait
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            atcLock.unlock();
        }
        return TakeOffReq.removeFirst(); // once a plane submits take off request, remove plane from the take off request list.
    }                                   // plane id is taken in the TakeOff method

    //ATC Take Off method
    public void TakeOff(int planeID) {
        atcLock.lock();
        try {
            for (int i = 0; i < AirportGates.length; i++) {
                if (AirportGates[i] == planeID && planeID != -1) { //checks if plane exists in a gate and if the plane is an actual plane
                    if (RunwayFree) { // checks if runway is free
                        AirportGates[i] = -1; // Free the gate
                         System.out.println(Thread.currentThread().getName() + ": ATC: TakeOff Request has been approved: " + planeID);
                        Runway(planeID, "Leaving"); // runs the Runway method
                        addToFinishedPlanes(planeID); // adds the plane to finished queue

                        atcCondition.signalAll();
                        ConcurrentAssignment.sleep(500);
                        return;
                    }
                    else { // if runway isn't free, prints out a method
                        System.out.println(Thread.currentThread().getName() + ": ATC: Runway is currently being used. Plane " + planeID + ", please wait for runway to free.");
                        ConcurrentAssignment.sleep(500);
                    }
                }
            }
        }
        finally {
            atcLock.unlock();
        }
    }

    // function to track planes who have finished all their tasks and have taken off
    private void addToFinishedPlanes(int planeID) {
        atcLock.lock();
        try {
            for (int i = 0; i < FinishedPlanes.length; i++) {
                if (FinishedPlanes[i] == -1) { // if space in the array is empty, add the plane to it.
                    FinishedPlanes[i] = planeID;
                    System.out.println(Thread.currentThread().getName() + ": ATC: Plane " + planeID + " has left the airport."); // output confirmation messsage
                    RunwayFree = true; // set runway back to true
                    ConcurrentAssignment.sleep(500);
                    return;
                }
            }
        }
        finally {
            atcLock.unlock();
        }
    }

    @Override
    public void run() {
        // Start the ATC with the message
        System.out.println(Thread.currentThread().getName() + ": Air Traffic Control is now active to manage Airport");
        ConcurrentAssignment.sleep(500);
        // atc continuously runs while active
        while (isActive) {
            atcLock.lock();
            try {
                if (AirportIdle()) { // if atc is finished output message
                    System.out.println(Thread.currentThread().getName() + ": ATC: All planes have left the Airport. ATC shutting down.");
                    StopAirport(); // run StopAirport() method
                    break;
                }

                if (WaitingQueue > 0) { // checks if plane is in waiting queue
                    AssignPlaneGate(); // runs AssignPlaneGate()
                }

                // checks if there is a plane that requested take off
                if (PlaneTakeOff) {
                    int planeID = TakeOffRequestCheck(); // removes first plane to request take off
                    TakeOff(planeID); // runs TakeOff method for that plane
                }


            } finally {
                atcLock.unlock();
            }
            ConcurrentAssignment.sleep(1000);
        }
    }
}
