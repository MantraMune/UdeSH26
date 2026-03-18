package ca.usherbrooke.ift630.tp2.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Benchmark comparatif entre synchronized et ReentrantReadWriteLock.
 *
 * Scenario : une ressource partagee lue frequemment (50 threads lecteurs)
 * et ecrite rarement (1 thread ecrivain, 1 ecriture toutes les 10ms).
 */
public class BenchmarkRWLock {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkRWLock.class);

    private static class RessourceSynchronized { // Lectures simultanés mais un écrivain
        private final Map<Integer, Integer> data = new HashMap<>(); // HashMap donc data est partagée entre les threads

        public synchronized int lire() {
            // Copie défensive
            Map<Integer, Integer> copie = new HashMap<>(data);

            // Simulation lecture lourde
            int somme = 0;
            for (int v : copie.values()) {
                somme += v;
            }
            return somme;
        }

        public synchronized void ecrire(int key, int value){
            data.put(key, value);
        }
    }

    private static class RessourceRWLock { // Lectures fréquentes ou écritures rares
        private final Map<Integer, Integer> data = new HashMap<>();
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); // Verrou pour lecture et écriture

        public int lire() {
            lock.readLock().lock(); // Verrouillé
            try {
                Map<Integer, Integer> copie = new HashMap<>(data);

                int somme = 0;
                for (int v : copie.values()) {
                    somme += v;
                }
                return somme;
            } finally {
                lock.readLock().unlock(); // Dévérouillé
            }
        }

        public void ecrire(int key, int value) {
            lock.writeLock().lock();
            try {
                data.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }
    } // TRY - FINALLY servent à libérer le verrou même si on a une exception (évite un deadlock, mais risqué)

    // Test de benchmark avec synchronized
    private static void runBenchmarkSync(RessourceSynchronized res, 
                                        int nbLecteurs, 
                                        int nbEcrivains, 
                                        long dureeBenchMs, 
                                        AtomicInteger lectures, 
                                        AtomicInteger ecritures){

            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicBoolean running = new AtomicBoolean(true);
            List<Thread> threads = new ArrayList<>();

            // Lecteurs
            for (int i = 0; i < nbLecteurs; i++){
                Thread t = new Thread(() -> {
                    try {
                        startLatch.await();
                        while (running.get()) {
                            res.lire();
                            lectures.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {}
                });
                threads.add(t);
            } // Chaque lecteur est un thread qui sont ajoutés à une liste après avoir lu l'information

            // Écrivain
            for (int i = 0; i < nbEcrivains; i++) {
                Thread t = new Thread(() -> {
                    try {
                        startLatch.await();
                        int k = 0;
                        while (running.get()) {
                            res.ecrire(k++, k);
                            ecritures.incrementAndGet();
                            Thread.sleep(10); // On fait attendre le thread (lecture avant d'écrire)
                        }
                    } catch (InterruptedException ignored) {}
                });
                threads.add(t);
            }

            threads.forEach(Thread::start); // Départ simultané des threads
            startLatch.countDown();

            sleep(dureeBenchMs);
            running.set(false); // Repos

            joinAll(threads); // Joindre tous les threads dans la liste
        }

        // Test de benchmark avec RessourceReadWriteLock
        private static void runBenchmarkRW(RessourceRWLock res,
                                   int nbLecteurs,
                                   int nbEcrivains,
                                   long dureeMs,
                                   AtomicInteger lectures,
                                   AtomicInteger ecritures) {

            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicBoolean running = new AtomicBoolean(true);
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < nbLecteurs; i++) {
                Thread t = new Thread(() -> {
                    try {
                        startLatch.await();
                        while (running.get()) {
                            res.lire();
                            lectures.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {}
                });
                threads.add(t);
            }

            for (int i = 0; i < nbEcrivains; i++) {
                Thread t = new Thread(() -> {
                     try {
                        startLatch.await();
                        int k = 0;
                        while (running.get()) {
                            res.ecrire(k++, k);
                            ecritures.incrementAndGet();
                            Thread.sleep(10); 
                        }
                    } catch (InterruptedException ignored) {}
                });
                threads.add(t);
            }

            threads.forEach(Thread::start);
            startLatch.countDown();

            sleep(dureeMs);
            running.set(false);

            joinAll(threads);
        }
    /**
     * Execute le benchmark comparatif.
     * Configuration : 50 lecteurs, 1 ecrivain, 3 secondes.
     */
    public static Map<String, Object> executer() {
        logger.info("=== Benchmark Read-Write Lock vs Synchronized ===");

        int nbLecteurs = 50;
        int nbEcrivains = 1;
        long dureeBenchMs = 3000;

        Map<String, Object> resultats = new HashMap<>();

        // -- SYNCHRONIZED -- 
        RessourceSynchronized resSync = new RessourceSynchronized();
        AtomicInteger lecturesSync = new AtomicInteger(0);
        AtomicInteger ecrituresSync = new AtomicInteger(0);

        runBenchmarkSync(resSync, nbLecteurs, nbEcrivains, dureeBenchMs, lecturesSync, ecrituresSync);

        // -- RW LOCK --
        RessourceRWLock resRW = new RessourceRWLock();
        AtomicInteger lecturesRW = new AtomicInteger(0);
        AtomicInteger ecrituresRW = new AtomicInteger(0);

        runBenchmarkRW(resRW, nbLecteurs, nbEcrivains, dureeBenchMs, lecturesRW, ecrituresRW);

        // Résultats
        double secondes = dureeBenchMs / 1000.0;

        double syncLecture = lecturesSync.get() / secondes;
        double syncEcriture = ecrituresSync.get() / secondes;

        double rwLecture = lecturesRW.get() / secondes;
        double rwEcriture = ecrituresRW.get() / secondes;

        double gainLecture = rwLecture / syncLecture;

        resultats.put("sync_throughput_lecture", syncLecture);
        resultats.put("sync_throughput_ecriture", syncEcriture);
        resultats.put("rw_throughput_lecture", rwLecture);
        resultats.put("rw_throughput_ecriture", rwEcriture);
        resultats.put("gain_lecture", gainLecture);

        logger.info("Sync lecture: {}", syncLecture);
        logger.info("RW lecture: {}", rwLecture);
        logger.info("Gain: x{}", gainLecture);
        
        return resultats;
    }

    // Utilitaires
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private static void joinAll (List<Thread> threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignored) {}
        }
    }
}
