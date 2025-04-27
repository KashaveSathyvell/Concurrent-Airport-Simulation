package src.tp075164_concurrent_assignment;

public class PassengerTask implements Runnable {

    // Instead of enqueuing the plane for processing, immediately spawn a new thread.
    public void addReadyPlane(PlaneTask plane, int passengerAmount, PlaneState state) {
        new Thread(() -> processPassengers(plane, passengerAmount, state), "PassengerThread").start();
    }

    private void processPassengers(PlaneTask plane, int passengerAmount, PlaneState state) {
        plane.Planelock.lock();
        try {
            ConcurrentAssignment.sleep(500);
            if (state == PlaneState.DISEMBARK) {

                System.out.println(Thread.currentThread().getName() + ": PassengerTask: Passengers in Plane " + plane.getPlaneNumber() + " are disembarking now...");
                ConcurrentAssignment.sleep(15 * passengerAmount); // Time proportional to passenger count
                System.out.println(Thread.currentThread().getName() + ": All " + passengerAmount + " Passengers have Disembarked from Plane " + plane.getPlaneNumber());

                plane.setPlaneState(PlaneState.CLEAN);
                plane.AllDisembarked = true;
                plane.AllPassengersDisembarked.signalAll();

            } else if (state == PlaneState.BOARD) {

                System.out.println(Thread.currentThread().getName() + ": PassengerTask: Passengers in Plane " + plane.getPlaneNumber() + " are boarding now...");
                ConcurrentAssignment.sleep(15 * passengerAmount);
                System.out.println(Thread.currentThread().getName() + ": All " + passengerAmount + " Passengers have Boarded Plane " + plane.getPlaneNumber());

                plane.setPlaneState(PlaneState.TAKEOFF);
                plane.AllBoarded = true;
                plane.AllPassengersDisembarked.signalAll();
            }
        } finally {
            plane.Planelock.unlock();
        }
    }

    @Override
    public void run() {
        // This thread can simply wait until shutdown.
        while (!ConcurrentAssignment.ShutDown) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(Thread.currentThread().getName() + ": Passenger service shutting down.");
    }
}
