package src.tp075164_concurrent_assignment;

public class CleaningCrew implements Runnable {
    private volatile boolean isActive = true;

    // Instead of enqueuing, spawn a new thread for each cleaning request.
    public void requestCleaning(int planeNumber) {
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + ": Cleaning Plane " + planeNumber + "...");
            try {
                Thread.sleep(1500); // Simulate cleaning time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + ": Cleaning Complete for Plane " + planeNumber);
        }, "CleaningThread").start();
    }

    public void stopService() {
        isActive = false;
    }

    @Override
    public void run() {
        // Optionally, this thread can simply wait until shutdown.
        while (isActive) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(Thread.currentThread().getName() + ": Cleaning crew shutting down.");
    }
}
