package ca.usherbrooke.ift630.tp2.generation;

import ca.usherbrooke.ift630.tp2.modele.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generateur specialise pour le scenario de test de deadlock.
 *
 * Configuration specifique :
 * - 5 stations partagees entre toutes les lignes
 * - 20 bus repartis sur 5 lignes (4 bus par ligne)
 * - Chaque ligne passe par au moins 3 stations
 * - Les stations sont ordonnees differemment selon les lignes
 *   pour maximiser les chances de deadlock (ordres inverses)
 *
 * Scenario de deadlock typique :
 *   Ligne A : Universite -> Cegep -> Carrefour
 *   Ligne B : Carrefour -> Cegep -> Universite
 *   => Les bus des deux lignes veulent les stations dans l'ordre inverse
 */
public class GenerateurDeadlock {

    private static final Logger logger = LoggerFactory.getLogger(GenerateurDeadlock.class);

    /** Les 5 stations du scenario de deadlock */
    private static final List<String> STATIONS_DEADLOCK = Arrays.asList(
            "Universite de Sherbrooke",
            "Station du Cegep",
            "Carrefour de l'Estrie",
            "CHUS-Fleurimont",
            "King Ouest"
    );

    private final long seed;

    public GenerateurDeadlock(long seed) {
        this.seed = seed;
    }

    /**
     * Cree les 5 lignes specialisees pour le test de deadlock.
     * Les lignes ont des ordres de stations inverses pour maximiser
     * les chances de deadlock.
     */
    public List<Ligne> creerLignesDeadlock() {
        List<Ligne> lignes = new ArrayList<>();

        // Ligne 1 : Universite -> Cegep -> Carrefour -> CHUS -> King
        lignes.add(new Ligne(101, "Deadlock-Ligne-A",
                Arrays.asList("Universite de Sherbrooke", "Station du Cegep",
                        "Carrefour de l'Estrie", "CHUS-Fleurimont", "King Ouest")));

        // Ligne 2 : King -> CHUS -> Carrefour -> Cegep -> Universite (INVERSE de Ligne 1)
        lignes.add(new Ligne(102, "Deadlock-Ligne-B",
                Arrays.asList("King Ouest", "CHUS-Fleurimont",
                        "Carrefour de l'Estrie", "Station du Cegep", "Universite de Sherbrooke")));

        // Ligne 3 : Carrefour -> Universite -> King -> Cegep -> CHUS
        lignes.add(new Ligne(103, "Deadlock-Ligne-C",
                Arrays.asList("Carrefour de l'Estrie", "Universite de Sherbrooke",
                        "King Ouest", "Station du Cegep", "CHUS-Fleurimont")));

        // Ligne 4 : CHUS -> King -> Cegep -> Universite -> Carrefour (INVERSE de Ligne 3)
        lignes.add(new Ligne(104, "Deadlock-Ligne-D",
                Arrays.asList("CHUS-Fleurimont", "King Ouest",
                        "Station du Cegep", "Universite de Sherbrooke", "Carrefour de l'Estrie")));

        // Ligne 5 : Cegep -> Carrefour -> CHUS -> King -> Universite
        lignes.add(new Ligne(105, "Deadlock-Ligne-E",
                Arrays.asList("Station du Cegep", "Carrefour de l'Estrie",
                        "CHUS-Fleurimont", "King Ouest", "Universite de Sherbrooke")));

        return lignes;
    }

    /**
     * Cree les 20 bus (4 par ligne) pour le test de deadlock.
     */
    public Map<String, Bus> creerBusDeadlock(List<Ligne> lignes) {
        Map<String, Bus> busMap = new HashMap<>();
        for (Ligne ligne : lignes) {
            for (int i = 1; i <= 4; i++) {
                String busId = String.format("BUS-DL%d-%02d", ligne.getNumero(), i);
                busMap.put(busId, new Bus(busId, 50, ligne));
            }
        }
        logger.info("Crees {} bus pour le test de deadlock", busMap.size());
        return busMap;
    }

    /**
     * Cree les 5 stations du scenario de deadlock.
     */
    public Map<String, Station> creerStationsDeadlock() {
        Map<String, Station> stationMap = new HashMap<>();
        for (String nom : STATIONS_DEADLOCK) {
            stationMap.put(nom, new Station(nom, 100));
        }
        return stationMap;
    }

    /**
     * Genere les evenements de test de deadlock.
     * Chaque bus traite des evenements a chaque station de sa ligne,
     * ce qui force les acquisitions de lock dans des ordres differents.
     *
     * @param busMap les bus du scenario
     * @return liste des evenements generes
     */
    public List<Evenement> genererEvenements(Map<String, Bus> busMap) {
        Random rng = new Random(seed);
        List<Evenement> evenements = new ArrayList<>();
        int timestamp = 0;
        int passagerId = 1;

        for (Bus bus : busMap.values()) {
            List<String> stationsLigne = bus.getLigne().getStations();

            for (String station : stationsLigne) {
                // Generer 2-4 embarquements par station par bus
                int nbEmb = 2 + rng.nextInt(3);
                for (int j = 0; j < nbEmb; j++) {
                    String pId = String.format("PDL%04d", passagerId++);
                    Priorite priorite = rng.nextDouble() < 0.3 ? Priorite.HAUTE : Priorite.NORMALE;
                    evenements.add(new Evenement(timestamp++, TypeEvenement.EMBARQUEMENT,
                            bus.getId(), pId, station, priorite));
                }

                // Generer 1-2 debarquements par station
                int nbDeb = 1 + rng.nextInt(2);
                for (int j = 0; j < nbDeb; j++) {
                    // Reutiliser un passager deja cree
                    int pIdx = 1 + rng.nextInt(Math.max(1, passagerId - 1));
                    String pId = String.format("PDL%04d", pIdx);
                    Priorite priorite = rng.nextDouble() < 0.3 ? Priorite.HAUTE : Priorite.NORMALE;
                    evenements.add(new Evenement(timestamp++, TypeEvenement.DEBARQUEMENT,
                            bus.getId(), pId, station, priorite));
                }
            }
        }

        logger.info("Generes {} evenements de deadlock test pour {} bus", evenements.size(), busMap.size());
        return evenements;
    }

    /**
     * Cree les passagers a partir des evenements.
     */
    public Map<String, Passager> creerPassagers(List<Evenement> evenements) {
        Map<String, Passager> passagerMap = new HashMap<>();
        for (Evenement evt : evenements) {
            if (evt.getType() == TypeEvenement.EMBARQUEMENT) {
                String pId = evt.getPassagerId();
                if (!passagerMap.containsKey(pId)) {
                    passagerMap.put(pId, new Passager(pId, evt.getPrioritePassager(),
                            evt.getStationNom(), evt.getStationNom()));
                }
            }
        }
        return passagerMap;
    }

    /**
     * Sauvegarde les evenements dans un fichier CSV.
     */
    public void sauvegarderCSV(List<Evenement> evenements, String cheminFichier) {
        Path path = Paths.get(cheminFichier);
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write("timestamp,type,bus_id,passager_id,station,priorite");
                writer.newLine();
                for (Evenement evt : evenements) {
                    writer.write(evt.toCSV());
                    writer.newLine();
                }
            }
            logger.info("Scenario deadlock sauvegarde dans {} ({} evenements)",
                    cheminFichier, evenements.size());
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde du scenario deadlock", e);
        }
    }
}
