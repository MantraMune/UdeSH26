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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generateur deterministe d'evenements pour la simulation STS.
 * Utilise un seed fixe pour garantir la reproductibilite :
 * chaque execution avec le meme seed produit exactement les memes evenements.
 */
public class GenerateurEvenements {

    private static final Logger logger = LoggerFactory.getLogger(GenerateurEvenements.class);

    private final long seed;
    private final Random random;
    private final List<Ligne> lignes;
    private final List<String> toutesLesStations;
    private final Map<Integer, List<String>> busParLigne; // numero ligne -> liste d'ids bus

    public GenerateurEvenements(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        this.lignes = Ligne.creerLignesSTS();
        this.toutesLesStations = extraireToutesStations();
        this.busParLigne = creerBusParLigne();
    }

    /**
     * Extrait la liste unique de toutes les stations du reseau.
     */
    private List<String> extraireToutesStations() {
        List<String> stations = new ArrayList<>();
        stations.add("Universite de Sherbrooke");
        stations.add("Station du Cegep");
        stations.add("Carrefour de l'Estrie");
        stations.add("CHUS-Fleurimont");
        stations.add("Universite Bishop's");
        stations.add("Plateau St-Joseph");
        stations.add("Andre/Hallee");
        stations.add("Bowen Sud");
        stations.add("De Lisieux");
        stations.add("13e Avenue");
        stations.add("Galvin");
        stations.add("King Ouest");
        return stations;
    }

    /**
     * Cree 2 bus par ligne (16 bus au total).
     */
    private Map<Integer, List<String>> creerBusParLigne() {
        Map<Integer, List<String>> map = new HashMap<>();
        for (Ligne ligne : lignes) {
            List<String> busIds = new ArrayList<>();
            busIds.add(String.format("BUS-L%d-01", ligne.getNumero()));
            busIds.add(String.format("BUS-L%d-02", ligne.getNumero()));
            map.put(ligne.getNumero(), busIds);
        }
        return map;
    }

    /**
     * Genere un scenario deterministe avec le nombre d'evenements specifie.
     * Par defaut : 600 embarquements + 400 debarquements = 1000 evenements.
     *
     * @param nbEvenements nombre total d'evenements a generer
     * @return liste ordonnee d'evenements
     */
    public List<Evenement> genererScenario(int nbEvenements) {
        // Reinitialiser le random pour garantir le determinisme
        Random rng = new Random(seed);

        int nbEmbarquements = (int) (nbEvenements * 0.6); // 60% embarquements
        int nbDebarquements = nbEvenements - nbEmbarquements; // 40% debarquements

        logger.info("Generation de {} evenements ({} embarquements, {} debarquements) avec seed {}",
                nbEvenements, nbEmbarquements, nbDebarquements, seed);

        List<Evenement> evenements = new ArrayList<>();
        int passagerId = 1;

        // Compteur pour suivre les passagers dans chaque bus (pour les debarquements)
        Map<String, List<String>> passagersDansBus = new HashMap<>();
        for (List<String> busIds : busParLigne.values()) {
            for (String busId : busIds) {
                passagersDansBus.put(busId, new ArrayList<>());
            }
        }

        int embarquementsGeneres = 0;
        int debarquementsGeneres = 0;

        for (int t = 0; t < nbEvenements; t++) {
            // Choisir une ligne aleatoirement
            Ligne ligne = lignes.get(rng.nextInt(lignes.size()));
            List<String> busIds = busParLigne.get(ligne.getNumero());
            String busId = busIds.get(rng.nextInt(busIds.size()));

            // Choisir une station de la ligne
            List<String> stationsLigne = ligne.getStations();
            String station = stationsLigne.get(rng.nextInt(stationsLigne.size()));

            // Determiner le type d'evenement
            TypeEvenement type;
            List<String> passagersBus = passagersDansBus.get(busId);

            if (embarquementsGeneres < nbEmbarquements && debarquementsGeneres < nbDebarquements) {
                // Les deux types sont possibles : favoriser l'embarquement si le bus n'est pas trop plein
                if (passagersBus.size() > 0 && rng.nextDouble() < 0.4) {
                    type = TypeEvenement.DEBARQUEMENT;
                } else {
                    type = TypeEvenement.EMBARQUEMENT;
                }
            } else if (embarquementsGeneres < nbEmbarquements) {
                type = TypeEvenement.EMBARQUEMENT;
            } else {
                type = TypeEvenement.DEBARQUEMENT;
            }

            // Determiner la priorite du passager (30% HAUTE, 70% NORMALE)
            Priorite priorite = rng.nextDouble() < 0.3 ? Priorite.HAUTE : Priorite.NORMALE;

            String pId;

            if (type == TypeEvenement.EMBARQUEMENT) {
                pId = String.format("P%03d", passagerId);
                passagerId++;
                passagersBus.add(pId);
                embarquementsGeneres++;
            } else {
                // Debarquement : choisir un passager deja dans le bus
                if (passagersBus.isEmpty()) {
                    // Pas de passager a debarquer, forcer un embarquement
                    type = TypeEvenement.EMBARQUEMENT;
                    pId = String.format("P%03d", passagerId);
                    passagerId++;
                    passagersBus.add(pId);
                    embarquementsGeneres++;
                    // Ajuster les compteurs si on a depasse les embarquements
                    if (embarquementsGeneres > nbEmbarquements) {
                        nbEmbarquements = embarquementsGeneres;
                        nbDebarquements = nbEvenements - nbEmbarquements;
                    }
                } else {
                    int idx = rng.nextInt(passagersBus.size());
                    pId = passagersBus.remove(idx);
                    debarquementsGeneres++;
                }
            }

            Evenement evt = new Evenement(t, type, busId, pId, station, priorite);
            evenements.add(evt);
        }

        logger.info("Generation terminee : {} embarquements, {} debarquements, {} passagers uniques",
                embarquementsGeneres, debarquementsGeneres, passagerId - 1);

        return evenements;
    }

    /**
     * Sauvegarde les evenements dans un fichier CSV.
     */
    public void sauvegarderCSV(List<Evenement> evenements, String cheminFichier) {
        Path path = Paths.get(cheminFichier);
        try {
            // Creer le repertoire parent si necessaire
            Files.createDirectories(path.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                // En-tete
                writer.write("timestamp,type,bus_id,passager_id,station,priorite");
                writer.newLine();

                for (Evenement evt : evenements) {
                    writer.write(evt.toCSV());
                    writer.newLine();
                }
            }

            logger.info("Scenario sauvegarde dans {} ({} evenements)", cheminFichier, evenements.size());
        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde du scenario dans {}", cheminFichier, e);
        }
    }

    /**
     * Charge les evenements depuis un fichier CSV.
     */
    public static List<Evenement> chargerCSV(String cheminFichier) {
        List<Evenement> evenements = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(cheminFichier));
            // Ignorer l'en-tete
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    evenements.add(Evenement.fromCSV(line));
                }
            }
            logger.info("Scenario charge depuis {} ({} evenements)", cheminFichier, evenements.size());
        } catch (IOException e) {
            logger.error("Erreur lors du chargement du scenario depuis {}", cheminFichier, e);
        }
        return evenements;
    }

    /**
     * Cree les objets Bus a partir des lignes STS.
     * Retourne une map busId -> Bus.
     */
    public Map<String, Bus> creerBus() {
        Map<String, Bus> busMap = new HashMap<>();
        for (Ligne ligne : lignes) {
            List<String> busIds = busParLigne.get(ligne.getNumero());
            for (String busId : busIds) {
                busMap.put(busId, new Bus(busId, 50, ligne));
            }
        }
        return busMap;
    }

    /**
     * Cree les objets Station a partir du reseau STS.
     * Retourne une map nomStation -> Station.
     */
    public Map<String, Station> creerStations() {
        Map<String, Station> stationMap = new HashMap<>();
        for (String nom : toutesLesStations) {
            stationMap.put(nom, new Station(nom, 100));
        }
        return stationMap;
    }

    /**
     * Cree les objets Passager a partir des evenements d'embarquement.
     * Retourne une map passagerId -> Passager.
     */
    public Map<String, Passager> creerPassagers(List<Evenement> evenements) {
        Map<String, Passager> passagerMap = new HashMap<>();
        for (Evenement evt : evenements) {
            if (evt.getType() == TypeEvenement.EMBARQUEMENT) {
                String pId = evt.getPassagerId();
                if (!passagerMap.containsKey(pId)) {
                    // Trouver la station de destination (prochain debarquement de ce passager)
                    String destination = trouverDestination(evenements, pId, evt.getStationNom());
                    passagerMap.put(pId, new Passager(pId, evt.getPrioritePassager(),
                            evt.getStationNom(), destination));
                }
            }
        }
        return passagerMap;
    }

    /**
     * Trouve la station de destination d'un passager (son prochain debarquement).
     */
    private String trouverDestination(List<Evenement> evenements, String passagerId, String stationDepart) {
        for (Evenement evt : evenements) {
            if (evt.getType() == TypeEvenement.DEBARQUEMENT && evt.getPassagerId().equals(passagerId)) {
                return evt.getStationNom();
            }
        }
        // Si pas de debarquement trouve, utiliser la derniere station de la ligne
        return stationDepart;
    }

    public List<Ligne> getLignes() {
        return lignes;
    }

    public long getSeed() {
        return seed;
    }
}
