package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import ca.usherbrooke.ift630.tp3.worker.ThreadWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests ThreadWorker")
public class ThreadWorkerTest {

    @Test
    @DisplayName("ThreadWorker demarre et arrete correctement")
    public void testStartStop() throws InterruptedException {
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker = new ThreadWorker(0, queue);
        
        worker.start();
        assertTrue(worker.isRunning());
        assertTrue(worker.isAlive());
        
        Thread.sleep(100);
        
        worker.shutdown();
        assertFalse(worker.isRunning());
        
        worker.join(2000);
        assertFalse(worker.isAlive());
    }

    @Test
    @DisplayName("ThreadWorker traite evenements de la queue")
    public void testEventProcessing() throws InterruptedException {
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker = new ThreadWorker(0, queue);
        
        worker.start();
        
        // Ajouter des evenements
        for (int i = 0; i < 10; i++) {
            TransportEvent event = createTestEvent("embarquement", "1", 3.50);
            queue.offer(event);
        }
        
        // Attendre traitement
        Thread.sleep(500);
        
        worker.shutdown();
        worker.join(2000);
        
        assertEquals(10, worker.getStatistics().getTotalEvents());
        assertEquals(10, worker.getStatistics().getTotalPassengers());
        assertEquals(35.0, worker.getStatistics().getTotalRevenue(), 0.01);
    }

    @Test
    @DisplayName("ThreadWorker accumule evenements traites")
    public void testProcessedEventsAccumulation() throws InterruptedException {
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker = new ThreadWorker(0, queue);
        
        worker.start();
        
        int numEvents = 20;
        for (int i = 0; i < numEvents; i++) {
            queue.offer(createTestEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(500);
        
        worker.shutdown();
        worker.join(2000);
        
        assertEquals(numEvents, worker.getProcessedEvents().size());
    }

    @Test
    @DisplayName("Plusieurs ThreadWorkers partagent queue")
    public void testMultipleWorkersSharedQueue() throws InterruptedException {
        BlockingQueue<TransportEvent> sharedQueue = new LinkedBlockingQueue<>();
        ThreadWorker worker1 = new ThreadWorker(0, sharedQueue);
        ThreadWorker worker2 = new ThreadWorker(1, sharedQueue);
        
        worker1.start();
        worker2.start();
        
        int totalEvents = 100;
        for (int i = 0; i < totalEvents; i++) {
            sharedQueue.offer(createTestEvent("embarquement", "1", 3.50));
        }
        
        Thread.sleep(1000);
        
        worker1.shutdown();
        worker2.shutdown();
        
        worker1.join(2000);
        worker2.join(2000);
        
        long totalProcessed = worker1.getStatistics().getTotalEvents() + 
                             worker2.getStatistics().getTotalEvents();
        
        assertEquals(totalEvents, totalProcessed, 
                     "Tous evenements doivent etre traites par les 2 workers");
    }

    @Test
    @DisplayName("ThreadWorker gere queue vide correctement")
    public void testEmptyQueue() throws InterruptedException {
        BlockingQueue<TransportEvent> queue = new LinkedBlockingQueue<>();
        ThreadWorker worker = new ThreadWorker(0, queue);
        
        worker.start();
        Thread.sleep(500);
        
        worker.shutdown();
        worker.join(2000);
        
        assertEquals(0, worker.getStatistics().getTotalEvents());
    }

    private TransportEvent createTestEvent(String type, String ligne, double montant) {
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
