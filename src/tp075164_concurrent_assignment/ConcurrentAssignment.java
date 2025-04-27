package src.tp075164_concurrent_assignment;


public class ConcurrentAssignment {

    //Global Passenger Task instaance
    public static PassengerTask globalPassengerTask;
    public static volatile boolean ShutDown = false;

    public static StatisticsCollector globalStatistics = new StatisticsCollector();

    private static RefuelingTruck refuelTruck = new RefuelingTruck();
    private static CleaningCrew cleaningCrew = new CleaningCrew();
    private static RefillSupplies refillSupply = new RefillSupplies();

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public static void main(String[] args) {
        AirTrafficControl atc = new AirTrafficControl();

        globalStatistics.startSimulation();


        int maxPlanes = 6;
        Thread[] planes = new Thread[maxPlanes];

        Thread ATC = new Thread(atc, "ATC Thread");
        Thread refuelThread = new Thread(refuelTruck, "RefuelThread");

        ATC.start();
        ATC.start();

        ConcurrentAssignment.globalPassengerTask = new PassengerTask();

        refuelThread.start();

        for (int j = 0; j < planes.length; j++) {

            if (j == 4) {
                planes[j] = new Thread(new PlaneTask(atc, j+1, true, refuelTruck, cleaningCrew, refillSupply), "PlaneThread " + (j + 1));
            }
            else {
                planes[j] = new Thread(new PlaneTask(atc, j+1, false, refuelTruck, cleaningCrew, refillSupply), "PlaneThread " + (j + 1));
            }
            planes[j].start();
            sleep(1000);
        }

        // Wait for the ATC thread to finish meaning shutdown
        try {
            ATC.join();
            for (Thread plane : planes) {
                plane.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop all other threads
        refuelTruck.stopRefuelingTruck();
        cleaningCrew.stopService();
        refillSupply.stopService();

        try {
            refuelThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Airport is shutting down...");

        globalStatistics.endSimulation();




        System.out.println("\n\n=================================================");
        System.out.println("|              Simulation  Statistics           |");
        System.out.println("=================================================");
        System.out.printf("| %-34s %10d |\n", "Planes served:", ConcurrentAssignment.globalStatistics.getPlanesServed());
        System.out.printf("| %-34s %10d |\n", "Total passengers disembarked:", ConcurrentAssignment.globalStatistics.getPassengersDisembarked());
        System.out.printf("| %-34s %10d |\n", "Total passengers boarded:", ConcurrentAssignment.globalStatistics.getPassengersBoarded());
        System.out.printf("| %-31s %8d secs |\n", "Total Simulation time:", ConcurrentAssignment.globalStatistics.getSimTime());
        System.out.printf("| %-44s  |\n", "-Time from request landing to take off:- ");
        System.out.printf("| %-31s %8d secs |\n", "Total operation time:", ConcurrentAssignment.globalStatistics.getTotalWaitingTime());
        System.out.printf("| %-31s %8d secs |\n", "Maximum operation time:", ConcurrentAssignment.globalStatistics.getMaxWaitingTime());
        System.out.printf("| %-31s %8d secs |\n", "Minimum operation time:", ConcurrentAssignment.globalStatistics.getMinWaitingTime());
        System.out.printf("| %-31s %8.2f secs |\n", "Average operation time:", ConcurrentAssignment.globalStatistics.getAverageWaitingTime());
        System.out.println("=================================================");
    }
}