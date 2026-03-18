package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.modele.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Timeout(30)
class Q5RWLockTest {

    @Test
    @DisplayName("Bus supporte des lectures concurrentes sans corruption")
    void busLecturesConcurrentes() throws InterruptedException {
        Ligne ligne = new Ligne(1, "Test", Arrays.asList("A", "B"));
        Bus bus = new Bus("BUS-T1", 50, ligne);

        // Embarquer quelques passagers
        for (int i = 0; i < 10; i++) {
            bus.embarquer(new Passager("P" + i, Priorite.NORMALE, "A", "B"));
        }

        AtomicBoolean erreurDetectee = new AtomicBoolean(false);
        AtomicInteger lectures = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        int nbThreads = 20;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < nbThreads; i++) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < 1000; j++) {
                        int nb = bus.getNombrePassagers();
                        if (nb < 0 || nb > 50) {
                            erreurDetectee.set(true);
                        }
                        bus.estPlein();
                        bus.getPourcentageOccupation();
                        lectures.incrementAndGet();
                    }
                } catch (Exception e) {
                    erreurDetectee.set(true);
                }
            });
            threads.add(t);
            t.start();
        }

        latch.countDown();
        for (Thread t : threads) t.join(10000);

        assertFalse(erreurDetectee.get(), "Aucune erreur ne devrait etre detectee lors de lectures concurrentes");
        assertTrue(lectures.get() > 0, "Des lectures devraient avoir ete effectuees");
    }

    @Test
    @DisplayName("Station supporte des lectures et ecritures concurrentes")
    void stationConcurrence() throws InterruptedException {
        Station station = new Station("TestStation", 200);
        AtomicBoolean erreur = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Threads ecrivains
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < 100; j++) {
                        station.ajouterPassagerEnAttente();
                        station.retirerPassagerEnAttente();
                    }
                } catch (Exception e) {
                    erreur.set(true);
                }
            });
            threads.add(t);
            t.start();
        }

        // Threads lecteurs
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < 100; j++) {
                        station.getNombrePassagersEnAttente();
                        station.estPleine();
                        station.getBusPresents();
                    }
                } catch (Exception e) {
                    erreur.set(true);
                }
            });
            threads.add(t);
            t.start();
        }

        latch.countDown();
        for (Thread t : threads) t.join(10000);

        assertFalse(erreur.get(), "Aucune erreur lors d'acces concurrents a la station");
    }
}
