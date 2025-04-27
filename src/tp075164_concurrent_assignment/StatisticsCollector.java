package src.tp075164_concurrent_assignment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StatisticsCollector {
    private final AtomicInteger planesServed = new AtomicInteger(0);
    private final AtomicInteger passengersBoarded = new AtomicInteger(0);
    private final AtomicInteger passengersDisembarked = new AtomicInteger(0);
    private final AtomicLong totalWaitingTime = new AtomicLong(0);
    private final AtomicLong maxWaitingTime = new AtomicLong(0);
    private final AtomicLong minWaitingTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong simStartTime = new AtomicLong(0);
    private final AtomicLong simEndTime = new AtomicLong(0);


    public void recordPlane(int passengerDisembarkCount, int passengerBoardingCount, long waitingTime) {
        planesServed.incrementAndGet();
        passengersBoarded.addAndGet(passengerBoardingCount);
        passengersDisembarked.addAndGet(passengerDisembarkCount);
        totalWaitingTime.addAndGet(waitingTime);

        // Update maximum waiting time
        maxWaitingTime.updateAndGet(prev -> Math.max(prev, waitingTime));
        // Update minimum waiting time
        minWaitingTime.updateAndGet(prev -> Math.min(prev, waitingTime));
    }

    // Call this when the simulation starts
    public void startSimulation() {
        simStartTime.set(System.currentTimeMillis());
    }

    // Call this when the simulation ends
    public void endSimulation() {
        simEndTime.set(System.currentTimeMillis());
    }

    // Return total simulation time in seconds
    public long getSimTime() {
        return (simEndTime.get() - simStartTime.get()) / 1000;
    }

    public int getPlanesServed() {
        return planesServed.get();
    }

    public int getPassengersBoarded() {
        return passengersBoarded.get();
    }

    public int getPassengersDisembarked() {
        return passengersDisembarked.get();
    }

    public long getTotalWaitingTime() {
        return totalWaitingTime.get()/1000;
    }

    public long getMaxWaitingTime() {
        return maxWaitingTime.get()/1000;
    }

    public long getMinWaitingTime() {
        // If no plane was served, return 0
        return (minWaitingTime.get() == Long.MAX_VALUE) ? 0 : minWaitingTime.get()/1000;
    }

    public double getAverageWaitingTime() {
        int served = planesServed.get();
        if (served == 0) return 0.0;
        return (double) (totalWaitingTime.get()/1000) / served;
    }
}
