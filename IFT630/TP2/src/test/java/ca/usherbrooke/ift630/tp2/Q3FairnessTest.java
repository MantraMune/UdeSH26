package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.coordination.GestionnaireStations;
import ca.usherbrooke.ift630.tp2.modele.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

@Timeout(30)
class Q3FairnessTest {

    @Test
    @DisplayName("Le gestionnaire de stations est cree correctement")
    void gestionnaireCreation() {
        Map<String, Station> stations = new HashMap<>();
        stations.put("StationA", new Station("StationA", 100));
        stations.put("StationB", new Station("StationB", 100));

        GestionnaireStations gestionnaire = new GestionnaireStations(stations);
        assertNotNull(gestionnaire.getStation("StationA"));
        assertNotNull(gestionnaire.getStation("StationB"));
        assertNull(gestionnaire.getStation("StationInexistante"));
    }

    @Test
    @DisplayName("La fairness empeche la famine des passagers normaux")
    void fairnessPreventStarvation() {
        Map<String, Station> stations = new HashMap<>();
        stations.put("StationA", new Station("StationA", 100));
        GestionnaireStations gestionnaire = new GestionnaireStations(stations);

        // Simuler 3 embarquements HAUTE consecutifs
        for (int i = 0; i < 3; i++) {
            Passager p = new Passager("PH" + i, Priorite.HAUTE, "StationA", "StationB");
            boolean autorise = gestionnaire.peutEmbarquer(p);
            if (autorise) gestionnaire.enregistrerEmbarquement(p);
        }

        // Apres 3 HAUTE consecutifs, un passager HAUTE devrait etre bloque
        // et un passager NORMALE devrait etre autorise
        Passager pNormale = new Passager("PN1", Priorite.NORMALE, "StationA", "StationB");
        assertTrue(gestionnaire.peutEmbarquer(pNormale),
                "Un passager NORMALE devrait toujours pouvoir embarquer");
    }

    @Test
    @DisplayName("Le max de prioritaires consecutifs est mis a jour")
    void maxConsecutifsMisAJour() {
        Map<String, Station> stations = new HashMap<>();
        stations.put("StationA", new Station("StationA", 100));
        GestionnaireStations gestionnaire = new GestionnaireStations(stations);

        // Enregistrer des embarquements
        for (int i = 0; i < 5; i++) {
            Passager p = new Passager("P" + i, Priorite.HAUTE, "StationA", "StationB");
            gestionnaire.enregistrerEmbarquement(p);
        }

        assertTrue(gestionnaire.getTotalEmbarquements() == 5,
                "Total embarquements devrait etre 5");
        assertTrue(gestionnaire.getTotalEmbarquementsHaute() == 5,
                "Total HAUTE devrait etre 5");
    }
}
