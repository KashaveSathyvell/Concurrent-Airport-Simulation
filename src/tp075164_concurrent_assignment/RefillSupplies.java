package src.tp075164_concurrent_assignment;

public class RefillSupplies implements Runnable {
    private volatile boolean isActive = true;

    // Immediately spawn a new thread for each supply refill request.
    public void requestSupplyRefill(int planeNumber) {
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + ": Refilling Supplies for Plane " + planeNumber + "...");
            try {
                Thread.sleep(1200); // Simulate supply refilling time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + ": Supplies Refilled for Plane " + planeNumber);
        }, "RefillSupplyThread").start();
    }


    public void stopService() {
        isActive = false;
    }

    @Override
    public void run() {
        // Just loop until shutdown.
        while (isActive) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(Thread.currentThread().getName() + ": Supply Refill Service shutting down.");
    }
}
