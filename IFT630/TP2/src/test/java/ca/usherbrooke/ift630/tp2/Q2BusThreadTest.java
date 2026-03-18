package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.generation.GenerateurEvenements;
import ca.usherbrooke.ift630.tp2.modele.*;
import ca.usherbrooke.ift630.tp2.threads.BusThread;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Timeout(30)
class Q2BusThreadTest {

    private static final long SEED = 12345;

    @Test
    @DisplayName("Tous les threads bus terminent sans deadlock")
    void tousLesThreadsTerminent() throws InterruptedException {
        GenerateurEvenements gen = new GenerateurEvenements(SEED);
        List<Evenement> evts = gen.genererScenario(1000);
        Map<String, Bus> busMap = gen.creerBus();
        Map<String, Station> stationMap = gen.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));
        FileAttentePrioritaire file = new FileAttentePrioritaire();

        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        List<BusThread> threads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            threads.add(new BusThread(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        for (BusThread t : threads) t.start();
        for (BusThread t : threads) t.join(20000);

        for (BusThread t : threads) {
            assertFalse(t.isAlive(), "Thread " + t.getName() + " ne devrait plus etre vivant");
        }
    }

    @Test
    @DisplayName("Aucun bus ne depasse sa capacite")
    void capaciteRespectee() throws InterruptedException {
        GenerateurEvenements gen = new GenerateurEvenements(SEED);
        List<Evenement> evts = gen.genererScenario(1000);
        Map<String, Bus> busMap = gen.creerBus();
        Map<String, Station> stationMap = gen.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));
        FileAttentePrioritaire file = new FileAttentePrioritaire();

        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        List<BusThread> threads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            threads.add(new BusThread(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        for (BusThread t : threads) t.start();
        for (BusThread t : threads) t.join(20000);

        for (Bus bus : busMap.values()) {
            assertTrue(bus.getNombrePassagers() <= bus.getCapaciteMax(),
                    "Bus " + bus.getId() + " depasse sa capacite: " + bus.getNombrePassagers() + "/" + bus.getCapaciteMax());
        }
    }

    @Test
    @DisplayName("Des evenements sont produits dans la file")
    void evenementsProduits() throws InterruptedException {
        GenerateurEvenements gen = new GenerateurEvenements(SEED);
        List<Evenement> evts = gen.genererScenario(1000);
        Map<String, Bus> busMap = gen.creerBus();
        Map<String, Station> stationMap = gen.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));
        FileAttentePrioritaire file = new FileAttentePrioritaire();

        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        List<BusThread> threads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            threads.add(new BusThread(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        for (BusThread t : threads) t.start();
        for (BusThread t : threads) t.join(20000);

        assertTrue(file.getTotalProduits() > 0, "La file devrait contenir des evenements produits");
    }
}
