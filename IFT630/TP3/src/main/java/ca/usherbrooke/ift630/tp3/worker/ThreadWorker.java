package ca.usherbrooke.ift630.tp3.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;

/**
 * Worker en mode threads - lit depuis une BlockingQueue partagée.
 * Pas d'IPC, communication via mémoire partagée (la queue).
 */
public class ThreadWorker extends Thread implements TransportWorker {
    private final int workerId;
    private final BlockingQueue<TransportEvent> eventQueue;
    private final Statistics statistics;
    private final List<TransportEvent> processedEvents;
    
    private volatile boolean running = true;
    private static final long POLL_TIMEOUT_MS = 100;

    public ThreadWorker(int workerId, BlockingQueue<TransportEvent> eventQueue) {
        super("ThreadWorker-" + workerId);
        this.workerId = workerId;
        this.eventQueue = eventQueue;
        this.statistics = new Statistics(workerId);
        this.processedEvents = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void run() {
        System.out.println(" Worker #" + workerId + " (thread) démarré");
        
        while(running || !eventQueue.isEmpty()) {
            try {
            TransportEvent event = eventQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (event != null) {
                processEvent(event, statistics);
                processedEvents.add(event);
            }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Tests à passer: ThreadWorkerTest (10 points)
        // - testStartStop: démarrage/arrêt propre
        // - testEventProcessing: traitement correct
        // - testMultipleWorkersSharedQueue: distribution équitable
        
        statistics.markEnd();
        System.out.println(" Worker #" + workerId + " (thread) arrêté - " + 
            statistics.getTotalEvents() + " événements traités");
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void shutdown() {
        running = false;
        try {
            join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public boolean isRunning() {
        return running && isAlive();
    }

    @Override
    public List<TransportEvent> getProcessedEvents() {
        return new ArrayList<>(processedEvents);
    }

    public int getWorkerId() {
        return workerId;
    }
}