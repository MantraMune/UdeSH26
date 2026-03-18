package ca.usherbrooke.ift630.tp2.threads;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread daemon de detection de deadlocks en temps reel.
 */
public class DeadlockDetector extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DeadlockDetector.class);

    private final ThreadMXBean threadMXBean;
    private final long intervalleMs;
    private final AtomicInteger deadlocksDetectes = new AtomicInteger(0);
    private final AtomicInteger sondes = new AtomicInteger(0);
    private volatile boolean actif = true;

    public DeadlockDetector(long intervalleMs) {
        super("DeadlockDetector");
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.intervalleMs = intervalleMs;
        setDaemon(true);
    }

    public DeadlockDetector() {
        this(500);
    }

    @Override
    public void run() {
        logger.info("DeadlockDetector demarre (intervalle: {} ms)", intervalleMs);

        while(actif && !Thread.currentThread().isInterrupted()) {
            try {
                verifierDeadlocks();
                sondes.incrementAndGet(); // Compter les sondes lors de la vérification

                Thread.sleep(intervalleMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Erreur lors de la vérification des deadlocks", e);
            }
        }

        logger.info("DeadlockDetector arrete ({} sondes effectuees, {} deadlocks detectes)",
                sondes.get(), deadlocksDetectes.get());
    }

    private void verifierDeadlocks() {
            // Liste des ID des threads impliquant un deadlock
            long[] threadIDs = threadMXBean.findDeadlockedThreads();

            if (threadIDs != null && threadIDs.length > 0) {
                deadlocksDetectes.incrementAndGet();

                logger.warn("Deadlock détecté impliquant {} threads", threadIDs.length);

                // Informations des threads stockés dans une liste
                ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIDs, true, true);

                for (ThreadInfo info : threadInfos) {
                    logger.warn("Thread: {}", info.getThreadName());
                    logger.warn("Lock: {}", info.getLockName());
                    logger.warn("Stack traces:");

                    for (StackTraceElement ste : info.getStackTrace()) {
                        logger.warn("    at {}", ste);
                    }

                    // Résolution par interruption
                    Thread t = trouverThread(info.getThreadId());
                     if (t != null) {
                        logger.warn("Interruption du thread {}", t.getName());
                        t.interrupt();
                    }
                }
            }
        
        }

        /**
         * Recherche un thread par son ID parmi les threads actifs.
         */
        private Thread trouverThread(long threadId) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.threadId() == threadId) {
                    return t;
                }
            }
            return null;
        }

        public void arreter() {
            actif = false;
            interrupt();
        }

        public int getDeadlocksDetectes() { return deadlocksDetectes.get(); }
        public int getSondes() { return sondes.get(); }
        public boolean isActif() { return actif; }
}
