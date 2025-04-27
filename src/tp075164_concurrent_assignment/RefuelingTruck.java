package src.tp075164_concurrent_assignment;

import java.util.concurrent.locks.ReentrantLock;
import java.util.Queue;
import java.util.LinkedList;

public class RefuelingTruck implements Runnable {
    private static final ReentrantLock refuelLock = new ReentrantLock();
    private final Queue<Integer> refuelQueue = new LinkedList<>();
    private volatile boolean isActive = true;

    public synchronized void requestRefuel(int planeNumber) {
        refuelQueue.add(planeNumber);
        notifyAll(); // Notify the refueling truck thread
    }

    private synchronized Integer getNextPlane() throws InterruptedException {
        while (refuelQueue.isEmpty() && isActive) {
            wait();
        }
        return refuelQueue.poll();
    }

    public void stopRefuelingTruck() {
        isActive = false;
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void run() {
        while (isActive) {
            try {
                Integer planeNumber = getNextPlane();
                if (planeNumber == null) continue;


                // Check if the refuel lock is busy
                if (!refuelLock.tryLock()) {
                    refuelLock.lock(); // Block until the lock is acquired
                }


                try {
                    System.out.println(Thread.currentThread().getName() + ": Refueling Plane " + planeNumber + "...");
                    Thread.sleep(2000);

                    synchronized (this) {
                        if (refuelQueue.size() > 0) {
                            // This message is printed by the refuel thread, indicating that additional planes are waiting.
                            System.out.println(Thread.currentThread().getName() + ": Refuel Truck is busy. Plane " + refuelQueue.peek() + " requested to wait.");
                        }
                    }

                    System.out.println(Thread.currentThread().getName() + ": Refueling Complete for Plane " + planeNumber);
                } finally {
                    refuelLock.unlock();
                }


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(Thread.currentThread().getName() + ": Refueling truck shutting down.");
    }
}