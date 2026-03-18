package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.coordination.Dashboard;
import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.generation.GenerateurEvenements;
import ca.usherbrooke.ift630.tp2.modele.*;
import ca.usherbrooke.ift630.tp2.threads.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Timeout(60)
class Q4ConsommateursTest {

    private static final long SEED = 12345;

    @Test
    @DisplayName("Les consommateurs traitent les evenements produits par les bus")
    void consommateursTraitentEvenements() throws InterruptedException {
        GenerateurEvenements gen = new GenerateurEvenements(SEED);
        List<Evenement> evts = gen.genererScenario(1000);
        Map<String, Bus> busMap = gen.creerBus();
        Map<String, Station> stationMap = gen.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));

        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stationMap, gen.getLignes());
        FileAttentePrioritaire file = new FileAttentePrioritaire();

        // Creer consommateurs daemon
        ConsommateurEmbarquement ce = new ConsommateurEmbarquement(file, busMap, passagerMap, dashboard);
        ConsommateurDebarquement cd = new ConsommateurDebarquement(file, busMap, passagerMap, dashboard);
        ConsommateurPaiement cp = new ConsommateurPaiement(file, busMap, dashboard);
        ce.setDaemon(true); cd.setDaemon(true); cp.setDaemon(true);

        // Repartir evenements par bus
        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            busThreads.add(new BusThread(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        // Demarrer consommateurs puis bus
        ce.start(); cd.start(); cp.start();
        for (BusThread bt : busThreads) bt.start();
        for (BusThread bt : busThreads) bt.join(20000);

        // Attendre que la file se vide
        int attente = 0;
        while (!file.estVide() && attente < 50) {
            Thread.sleep(100);
            attente++;
        }
        Thread.sleep(500);

        ce.interrupt(); cd.interrupt(); cp.interrupt();

        int totalTraites = ce.getCompteurTraites() + cd.getCompteurTraites() + cp.getCompteurTraites();
        assertTrue(totalTraites > 0, "Au moins un evenement devrait etre traite, mais total = " + totalTraites);
    }

    @Test
    @DisplayName("Le dashboard est mis a jour par les consommateurs")
    void dashboardMisAJour() throws InterruptedException {
        GenerateurEvenements gen = new GenerateurEvenements(SEED);
        List<Evenement> evts = gen.genererScenario(1000);
        Map<String, Bus> busMap = gen.creerBus();
        Map<String, Station> stationMap = gen.creerStations();
        Map<String, Passager> passagerMap = new ConcurrentHashMap<>(gen.creerPassagers(evts));

        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stationMap, gen.getLignes());
        FileAttentePrioritaire file = new FileAttentePrioritaire();

        ConsommateurEmbarquement ce = new ConsommateurEmbarquement(file, busMap, passagerMap, dashboard);
        ConsommateurDebarquement cd = new ConsommateurDebarquement(file, busMap, passagerMap, dashboard);
        ConsommateurPaiement cp = new ConsommateurPaiement(file, busMap, dashboard);
        ce.setDaemon(true); cd.setDaemon(true); cp.setDaemon(true);

        Map<String, List<Evenement>> evtsParBus = new HashMap<>();
        for (Bus bus : busMap.values()) evtsParBus.put(bus.getId(), new ArrayList<>());
        for (Evenement evt : evts) {
            List<Evenement> l = evtsParBus.get(evt.getBusId());
            if (l != null) l.add(evt);
        }

        List<BusThread> busThreads = new ArrayList<>();
        for (Bus bus : busMap.values()) {
            busThreads.add(new BusThread(bus, file, evtsParBus.get(bus.getId()), stationMap, passagerMap));
        }

        ce.start(); cd.start(); cp.start();
        for (BusThread bt : busThreads) bt.start();
        for (BusThread bt : busThreads) bt.join(20000);

        int attente = 0;
        while (!file.estVide() && attente < 50) {
            Thread.sleep(100);
            attente++;
        }
        Thread.sleep(500);

        ce.interrupt(); cd.interrupt(); cp.interrupt();

        assertTrue(dashboard.getTotalEvenementsTraites() > 0,
                "Le dashboard devrait avoir traite des evenements");
    }
}
