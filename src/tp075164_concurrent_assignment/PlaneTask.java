package src.tp075164_concurrent_assignment;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

enum PlaneState {
    LANDING,
    DISEMBARK,
    CLEAN,
    REFILLSUPPLIES,
    REFUEL,
    BOARD,
    TAKEOFF,
    FINISHED;
}

public class PlaneTask implements Runnable {
    private int DisembarkPassengerAmount;
    private int BoardingPassengerAmount;

    private int planeNumber;

    private AirTrafficControl atc;

    public Boolean AllDisembarked = false;
    public Boolean AllBoarded = false;
    public final Lock Planelock = new ReentrantLock();
    public final Condition AllPassengersDisembarked = Planelock.newCondition();

    private PlaneState state;
    boolean EmergencyPlane;

    // Stats time var
    private long planeStartOperationTime = 0;
    private long planeEndOperationTime = 0;

    private final RefuelingTruck refuelTruck;
    private final CleaningCrew cleaningCrew;
    private final RefillSupplies supplyRefill;

    public PlaneTask(AirTrafficControl atc, int PlaneNumber, boolean Emergency, RefuelingTruck refuelTruck, CleaningCrew cleaningCrew, RefillSupplies supplyRefill) {
        this.planeNumber = PlaneNumber;
        this.atc = atc;
        this.EmergencyPlane = Emergency;

        this.refuelTruck = refuelTruck;
        this.cleaningCrew = cleaningCrew;
        this.supplyRefill = supplyRefill;
    }

    public int getPlaneNumber() {
        return planeNumber;
    }

    public void setPlaneState(PlaneState state) {
        Planelock.lock();
        try {
//            System.out.println("Set Passenger State: " + state);
            this.state = state;
            AllPassengersDisembarked.signalAll();
        }
        finally {
            Planelock.unlock();
        }
    }

    public PlaneState getPlaneState() {
        Planelock.lock();
        try {
            if (state == null) {
                state = PlaneState.LANDING;
            }
            return state;
        }
        finally {
            Planelock.unlock();
        }
    }

    private int getPassengerAmount() {
        int PassengerAmount = (int) (Math.random() * 20) + 30;
        return PassengerAmount;
    }


    @Override
    public void run() {
        Planelock.lock();
        try {
            setPlaneState(PlaneState.LANDING);
            ConcurrentAssignment.sleep(500);

            while (getPlaneState() == null) {
                AllPassengersDisembarked.await();
            }

            boolean finished = false;

            while (!finished) {
                switch (state) {
                    case LANDING:
                        planeStartOperationTime = System.currentTimeMillis();
                        if (EmergencyPlane) {
                            System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + ": EMERGENCY! We are low on fuel! Requesting emergency landing!");
                        }
                        else {
                            System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + ": Requesting permission to land.");
                        }

                        ConcurrentAssignment.sleep(500);
                        Boolean LandStatus = atc.RequestLanding(planeNumber, EmergencyPlane);


                        if (LandStatus) {
                            System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + ": Docking at gate now.");
                            setPlaneState(PlaneState.DISEMBARK);
                            ConcurrentAssignment.sleep(500);
                        }
                        break;

                    case DISEMBARK:
                        ConcurrentAssignment.sleep(500);
                        DisembarkPassengerAmount = getPassengerAmount();
                        // Notify the global PassengerTask that this plane is ready:
                        ConcurrentAssignment.globalPassengerTask.addReadyPlane(this, DisembarkPassengerAmount, getPlaneState());

                        while (!AllDisembarked) {
                            AllPassengersDisembarked.await();
                        }
                        ConcurrentAssignment.sleep(500);
                        break;

                    case CLEAN:
                        System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + " requesting cleaning.");
                        cleaningCrew.requestCleaning(planeNumber);
                        ConcurrentAssignment.sleep(500);
                        setPlaneState(PlaneState.REFUEL);
                        break;

                    case REFUEL:
                        System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + " requesting refuel.");
                        refuelTruck.requestRefuel(planeNumber); // Request refueling
                        ConcurrentAssignment.sleep(500);
                        setPlaneState(PlaneState.REFILLSUPPLIES);
                        break;

                    case REFILLSUPPLIES:
                        System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + " requesting supply refill.");
                        supplyRefill.requestSupplyRefill(planeNumber);
                        ConcurrentAssignment.sleep(500);
                        setPlaneState(PlaneState.BOARD);
                        break;

                    case BOARD:
                        ConcurrentAssignment.sleep(500);
                        BoardingPassengerAmount = getPassengerAmount();
                        // Notify the global PassengerTask that this plane is ready:
                        ConcurrentAssignment.globalPassengerTask.addReadyPlane(this, BoardingPassengerAmount, getPlaneState());

                        while (!AllBoarded) {
                            AllPassengersDisembarked.await();
                        }
                        ConcurrentAssignment.sleep(500);
                        break;

                    case TAKEOFF:
                        System.out.println(Thread.currentThread().getName() + ": Plane " + planeNumber + " requesting permission to Take off.");
                        ConcurrentAssignment.sleep(500);
                        atc.RequestTakeOff(planeNumber);
                        finished = true;
                        planeEndOperationTime = System.currentTimeMillis();
                        setPlaneState(PlaneState.FINISHED);
                        break;

                    case null, default:
                        break;
                }
            }

            long totalOperationTime = planeEndOperationTime - planeStartOperationTime;
            ConcurrentAssignment.globalStatistics.recordPlane(DisembarkPassengerAmount, BoardingPassengerAmount, totalOperationTime);

            ConcurrentAssignment.sleep(1000);

        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            Planelock.unlock();
        }
    }
}