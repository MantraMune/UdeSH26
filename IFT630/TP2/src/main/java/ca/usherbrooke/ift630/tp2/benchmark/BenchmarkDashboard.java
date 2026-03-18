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
 * Benchmark comparatif Dashboard : synchronized vs RW Lock.
 *           - Configuration : 50 lecteurs + 1 ecrivain pendant 3 secondes
 */
public class BenchmarkDashboard {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkDashboard.class);

    private static final int NB_LECTEURS = 50;
    private static final int NB_ECRIVAINS = 1;
    private static final long DUREE_MS = 3000;
    private static final int NB_STATIONS = 100;
    private static final int NB_LIGNES = 20;
    
    private static class DashboardSynchronized {
        private final Map<Integer, Integer> stations = new HashMap<>();
        private final Map<Integer, Integer> lignes = new HashMap<>();
        
        public synchronized int lireTout() {
            Map<Integer, Integer> copieStations = new HashMap<>(stations);
            Map<Integer, Integer> copieLignes = new HashMap<>(lignes);

            int somme = 0;

            for (Integer v : copieStations.values()) {
                somme += v;
            }

            for (Integer v : copieLignes.values()) {
                somme += v;
            }

            return somme;
        }

        public synchronized void mettreAJour(int key, int value) {
            stations.put(key % NB_STATIONS, value);
            lignes.put(key % NB_LIGNES, value);
        }
    }

    private static class DashboardRWLock {
        private final Map<Integer, Integer> stations = new HashMap<>();
        private final Map<Integer, Integer> lignes = new HashMap<>();
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

        public int lireTout()
        {
            rwLock.readLock().lock();
            try {
                Map<Integer, Integer> copieStations = new HashMap<>(stations);
                Map<Integer, Integer> copieLignes = new HashMap<>(lignes);

                int somme = 0;

                for (Integer v : copieStations.values()) {
                    somme += v;
                }

                for (Integer v : copieLignes.values()) {
                    somme += v;
                }

                return somme;

            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void mettreAJour(int key, int value) {
            rwLock.writeLock().lock();
            try {
                stations.put(key % NB_STATIONS, value);
                lignes.put(key % NB_LIGNES, value);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
     
    private static void runBenchmarkDashboardSync(DashboardSynchronized dashSync, int nbLecteurs, int nbEcrivains,long dureeBenchMs, AtomicInteger lectures, AtomicInteger ecritures){
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        List<Thread> threads = new ArrayList<>();

        // Lecteurs
        for (int i = 0; i < nbLecteurs; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await();
                    while (running.get()) {
                        dashSync.lireTout();
                        lectures.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
        }

        // Écrivain
        for (int i = 0; i < nbEcrivains; i++){
            Thread t = new Thread(() -> {
                try {
                    startLatch.await();
                    int k = 0;
                    while (running.get()) {
                        dashSync.mettreAJour(k++, k);
                        ecritures.incrementAndGet();
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
        }

        threads.forEach(Thread::start);
        startLatch.countDown();

        sleep(dureeBenchMs);
        running.set(false);

        joinAll(threads);

    }

    private static void runBenchmarkDashboardRW(DashboardRWLock dashRW, int nbLecteurs, int nbEcrivains, long dureeBenchMs, AtomicInteger lectures, AtomicInteger ecritures)
    {
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        List<Thread> threads = new ArrayList<>();

        // Lecteurs
        for (int i = 0; i < nbLecteurs; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await();
                    while(running.get()) {
                        dashRW.lireTout();
                        lectures.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
        }

        // Écrivains
        for (int i = 0; i < nbEcrivains; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await();
                    int k = 0;
                    while (running.get()) {
                        dashRW.mettreAJour(k++, k);
                        ecritures.incrementAndGet();
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
        }

        threads.forEach(Thread::start);
        startLatch.countDown();

        sleep(dureeBenchMs);
        running.set(false);

        joinAll(threads);
    }

    /**
     * Execute le benchmark et retourne les resultats.
     */
    public static Map<String, Object> executer() {
        logger.info("=== Benchmark Dashboard : synchronized vs RW Lock ===");
        logger.info("Configuration : {} lecteurs + {} ecrivains, duree {} ms",
                NB_LECTEURS, NB_ECRIVAINS, DUREE_MS);

        // -- SYNCHRONIZED --
        DashboardSynchronized dashSync = new DashboardSynchronized();
        AtomicInteger lecturesSync = new AtomicInteger(0);
        AtomicInteger ecrituresSync = new AtomicInteger(0);

        runBenchmarkDashboardSync(dashSync, NB_LECTEURS, NB_ECRIVAINS, DUREE_MS, lecturesSync, ecrituresSync);

        // -- RW LOCK --
        DashboardRWLock dashRW = new DashboardRWLock();
        AtomicInteger lecturesRW = new AtomicInteger(0);
        AtomicInteger ecrituresRW = new AtomicInteger(0);

        runBenchmarkDashboardRW(dashRW, NB_LECTEURS, NB_ECRIVAINS, DUREE_MS, lecturesRW, ecrituresRW);

        // Résultats
        double secondes = DUREE_MS / 1000.0;

        double syncLecture = lecturesSync.get() / secondes;
        double syncEcriture = ecrituresSync.get() / secondes;

        double rwLecture = lecturesRW.get() / secondes;
        double rwEcriture = ecrituresRW.get() / secondes;

        double gainLecture = rwLecture / syncLecture;
        double gainEcriture = rwEcriture / syncEcriture;

        Map<String, Object> resultats = new HashMap<>();
        resultats.put("sync_throughput_lecture", syncLecture);
        resultats.put("sync_throughput_ecriture", syncEcriture);
        resultats.put("rw_throughput_lecture", rwLecture);
        resultats.put("rw_throughput_ecriture", rwEcriture);
        resultats.put("gain_lecture", gainLecture);
        resultats.put("gain_ecriture", gainEcriture);

        logger.info("Sync Lecture: {}", syncLecture);
        logger.info("Sync Écriture: {}", syncEcriture);
        logger.info("RW Lecture: {}", rwLecture);
        logger.info("Rw Écriture: {}", rwEcriture);
        logger.info("Gain Lecture: x{}", gainLecture);
        logger.info("Gain Écriture: x{}", gainEcriture);

        return resultats;
    }

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
