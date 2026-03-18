package ca.usherbrooke.ift630.tp2.modele;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represente une ligne d'autobus du reseau STS.
 * Contient la liste ordonnee des stations desservies.
 */
public class Ligne {

    private final int numero;
    private final String nom;
    private final List<String> stations;

    public Ligne(int numero, String nom, List<String> stations) {
        this.numero = numero;
        this.nom = nom;
        this.stations = Collections.unmodifiableList(new ArrayList<>(stations));
    }

    public int getNumero() {
        return numero;
    }

    public String getNom() {
        return nom;
    }

    public List<String> getStations() {
        return stations;
    }

    /**
     * Cree toutes les lignes du reseau STS de Sherbrooke.
     */
    public static List<Ligne> creerLignesSTS() {
        List<Ligne> lignes = new ArrayList<>();

        lignes.add(new Ligne(1, "Carrefour de l'Estrie - Bowen Sud",
                Arrays.asList("Carrefour de l'Estrie", "King Ouest", "Universite de Sherbrooke", "Bowen Sud")));

        lignes.add(new Ligne(2, "Station du Cegep - Universite Bishop's",
                Arrays.asList("Station du Cegep", "King Ouest", "Universite de Sherbrooke", "Universite Bishop's")));

        lignes.add(new Ligne(3, "Carrefour de l'Estrie - 13e Avenue",
                Arrays.asList("Carrefour de l'Estrie", "King Ouest", "Station du Cegep", "13e Avenue")));

        lignes.add(new Ligne(4, "Carrefour de l'Estrie - Galvin",
                Arrays.asList("Carrefour de l'Estrie", "King Ouest", "Station du Cegep", "Galvin")));

        lignes.add(new Ligne(6, "Universite de Sherbrooke - De Lisieux",
                Arrays.asList("Universite de Sherbrooke", "CHUS-Fleurimont", "De Lisieux")));

        lignes.add(new Ligne(7, "Andre/Hallee - CHUS-Fleurimont",
                Arrays.asList("Andre/Hallee", "Plateau St-Joseph", "CHUS-Fleurimont")));

        lignes.add(new Ligne(8, "Universite de Sherbrooke - CHUS-Fleurimont",
                Arrays.asList("Universite de Sherbrooke", "Station du Cegep", "CHUS-Fleurimont")));

        lignes.add(new Ligne(11, "Universite Bishop's - Plateau St-Joseph",
                Arrays.asList("Universite Bishop's", "King Ouest", "Plateau St-Joseph")));

        return lignes;
    }

    @Override
    public String toString() {
        return String.format("Ligne[%d, %s, %d stations]", numero, nom, stations.size());
    }
}
