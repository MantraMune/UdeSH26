package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.generation.GenerateurDeadlock;
import ca.usherbrooke.ift630.tp2.modele.*;
import ca.usherbrooke.ift630.tp2.threads.BusThreadDeadlock;
import ca.usherbrooke.ift630.tp2.threads.DeadlockDetector;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Timeout(90)
class Q6DeadlockTest {

    private static final long SEED = 12345;

    @Test
    @DisplayName("Tous les threads terminent en moins de 60 secondes sans deadlock")
    void pasDeDeadlock() throws InterruptedException {
        GenerateurDeadlock gen = new GenerateurDeadlock(SEED);
        List<Ligne> lignes = gen.creerLignesDeadlock();
        Map<String, Bus> busMap = gen.creerBusDeadlock(lignes);
        Map<String, Station> stationMap = gen.creerStationsDeadlock();
        List<Evenement> evts = gen.genererEvenements(busMap);
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));

        FileAttentePrioritaire file = new FileAttentePrioritaire();

        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        DeadlockDetector detector = new DeadlockDetector(500);
        detector.start();

        List<BusThreadDeadlock> threads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            threads.add(new BusThreadDeadlock(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        long debut = System.nanoTime();
        for (BusThreadDeadlock t : threads) t.start();
        for (BusThreadDeadlock t : threads) {
            t.join(60000);
            if (t.isAlive()) t.interrupt();
        }
        long dureeSec = (System.nanoTime() - debut) / 1_000_000_000;
        detector.arreter();

        boolean tousTermines = threads.stream().noneMatch(Thread::isAlive);
        assertTrue(tousTermines, "Tous les threads devraient etre termines");
        assertTrue(dureeSec < 60, "L'execution devrait prendre moins de 60 secondes (a pris " + dureeSec + "s)");

        long threadsBlocked = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("BusDL-"))
                .filter(t -> t.getState() == Thread.State.BLOCKED)
                .count();
        assertEquals(0, threadsBlocked, "Aucun thread ne devrait etre en etat BLOCKED");
    }
}
