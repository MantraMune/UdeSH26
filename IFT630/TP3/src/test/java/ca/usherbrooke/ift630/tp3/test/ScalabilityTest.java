package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import ca.usherbrooke.ift630.tp3.worker.ThreadWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests scalabilite")
public class ScalabilityTest {

    @Test
    @DisplayName("Scalabilite avec 1 worker")
    public void testSingleWorker() throws InterruptedException {
        int numEvents = 100;
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker = new ThreadWorker(0, queue);
        
        worker.start();
        
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(500);
        worker.shutdown();
        worker.join(2000);
        
        assertEquals(numEvents, worker.getStatistics().getTotalEvents());
    }

    @Test
    @DisplayName("Scalabilite avec 2 workers")
    public void testTwoWorkers() throws InterruptedException {
        int numEvents = 100;
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker1 = new ThreadWorker(0, queue);
        ThreadWorker worker2 = new ThreadWorker(1, queue);
        
        worker1.start();
        worker2.start();
        
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(500);
        
        worker1.shutdown();
        worker2.shutdown();
        worker1.join(2000);
        worker2.join(2000);
        
        long total = worker1.getStatistics().getTotalEvents() + 
                    worker2.getStatistics().getTotalEvents();
        
        assertEquals(numEvents, total);
    }

    @Test
    @DisplayName("Scalabilite avec 4 workers")
    public void testFourWorkers() throws InterruptedException {
        int numEvents = 200;
        int numWorkers = 4;
        
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        List<ThreadWorker> workers = new ArrayList<>();
        
        for (int i = 0; i < numWorkers; i++) {
            ThreadWorker worker = new ThreadWorker(i, queue);
            worker.start();
            workers.add(worker);
        }
        
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(1000);
        
        for (ThreadWorker worker : workers) {
            worker.shutdown();
        }
        
        for (ThreadWorker worker : workers) {
            worker.join(2000);
        }
        
        long total = workers.stream()
            .mapToLong(w -> w.getStatistics().getTotalEvents())
            .sum();
        
        assertEquals(numEvents, total);
    }

    @Test
    @DisplayName("Distribution equilibree entre workers")
    public void testBalancedDistribution() throws InterruptedException {
        int numEvents = 1000;
        int numWorkers = 4;
        
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        List<ThreadWorker> workers = new ArrayList<>();
        
        for (int i = 0; i < numWorkers; i++) {
            ThreadWorker worker = new ThreadWorker(i, queue);
            worker.start();
            workers.add(worker);
        }
        
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(2000); // Laisser les workers traiter
        
        // Attendre que tous les evenements soient traites
        while (queue.size() > 0) {
            Thread.sleep(10);
        }
        
        for (ThreadWorker worker : workers) {
            worker.shutdown();
            worker.join(2000);
        }
        
        // Vérifier que tous les événements ont été traités
        long totalProcessed = workers.stream()
            .mapToLong(w -> w.getStatistics().getTotalEvents())
            .sum();
        assertEquals(numEvents, totalProcessed, "Tous les événements doivent être traités");
        
        // Verifier que chaque worker a traite au moins quelques evenements
        for (ThreadWorker worker : workers) {
            long processed = worker.getStatistics().getTotalEvents();
            assertTrue(processed > 0, "Worker " + worker.getWorkerId() + " doit avoir traite des evenements");
        }
        
        // Verifier que la distribution n'est pas trop desequilibree
        long min = workers.stream()
            .mapToLong(w -> w.getStatistics().getTotalEvents())
            .min()
            .orElse(0);
        
        long max = workers.stream()
            .mapToLong(w -> w.getStatistics().getTotalEvents())
            .max()
            .orElse(0);
        
        // Le ratio ne doit pas depasser 5:1
        assertTrue(max <= min * 5, "Distribution trop desequilibree");
    }

    @Test
    @DisplayName("Performance augmente avec plus workers")
    public void testPerformanceScaling() throws InterruptedException {
        int numEvents = 500;
        
        // Test avec 1 worker
        long time1 = measureProcessingTime(1, numEvents);
        
        // Test avec 4 workers
        long time4 = measureProcessingTime(4, numEvents);
        
        // Avec 4 workers devrait etre plus rapide (ou au moins pas beaucoup plus lent)
        // On accepte jusqu'a 150% du temps (overhead de coordination)
        assertTrue(time4 <= time1 * 1.5, 
                   "4 workers ne devrait pas etre beaucoup plus lent que 1 worker");
    }

    private long measureProcessingTime(int numWorkers, int numEvents) throws InterruptedException {
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        List<ThreadWorker> workers = new ArrayList<>();
        
        for (int i = 0; i < numWorkers; i++) {
            ThreadWorker worker = new ThreadWorker(i, queue);
            worker.start();
            workers.add(worker);
        }
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createEvent("embarquement", "1", 3.50));
        }
        
        // Attendre que tous les evenements soient traites
        while (queue.size() > 0) {
            Thread.sleep(10);
        }
        
        long endTime = System.currentTimeMillis();
        
        for (ThreadWorker worker : workers) {
            worker.shutdown();
            worker.join(2000);
        }
        
        return endTime - startTime;
    }

    private TransportEvent createEvent(String type, String ligne, double montant) {
        TransportEvent event = new TransportEvent();
        event.setType(type);
        event.setLigne(ligne);
        event.setBusId("BUS_001");
        event.setMontant(montant);
        event.setMethode("carte");
        event.setArret("Test");
        event.setPassagerId("PASS_TEST");
        event.setOrigine("A");
        event.setDestination("B");
        event.setTimestamp("2025-01-15T10:00:00.000-05:00");
        return event;
    }
}
