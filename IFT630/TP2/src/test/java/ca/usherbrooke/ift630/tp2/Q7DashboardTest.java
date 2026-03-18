package ca.usherbrooke.ift630.tp2;

import ca.usherbrooke.ift630.tp2.coordination.Dashboard;
import ca.usherbrooke.ift630.tp2.modele.*;
import ca.usherbrooke.ift630.tp2.threads.DashboardAfficheur;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

@Timeout(30)
class Q7DashboardTest {

    @Test
    @DisplayName("DashboardAfficheur fait au moins un affichage")
    void afficheurFonctionne() throws InterruptedException {
        Map<String, Station> stations = new HashMap<>();
        stations.put("StationA", new Station("StationA", 100));
        List<Ligne> lignes = List.of(new Ligne(1, "Test", List.of("StationA")));

        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stations, lignes);

        DashboardAfficheur afficheur = new DashboardAfficheur(dashboard, 100);
        afficheur.start();

        Thread.sleep(500);
        afficheur.arreter();
        afficheur.join(2000);

        assertTrue(afficheur.getNbAffichages() > 0,
                "L'afficheur devrait avoir fait au moins un affichage, mais en a fait " + afficheur.getNbAffichages());
    }

    @Test
    @DisplayName("DashboardAfficheur est un daemon thread")
    void afficheurEstDaemon() {
        Dashboard dashboard = new Dashboard();
        DashboardAfficheur afficheur = new DashboardAfficheur(dashboard);
        assertTrue(afficheur.isDaemon(), "DashboardAfficheur devrait etre un daemon thread");
    }

    @Test
    @DisplayName("Dashboard supporte des lectures concurrentes pendant les ecritures")
    void dashboardConcurrence() throws InterruptedException {
        Map<String, Station> stations = new HashMap<>();
        stations.put("StationA", new Station("StationA", 100));
        List<Ligne> lignes = List.of(new Ligne(1, "Test", List.of("StationA")));

        Dashboard dashboard = new Dashboard();
        dashboard.initialiser(stations, lignes);

        // Thread ecrivain
        Thread ecrivain = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                Evenement evt = new Evenement(i, TypeEvenement.EMBARQUEMENT,
                        "BUS-1", "P" + i, "StationA", Priorite.NORMALE);
                dashboard.enregistrerEmbarquement(evt);
                try { Thread.sleep(1); } catch (InterruptedException e) { break; }
            }
        });

        // DashboardAfficheur comme lecteur
        DashboardAfficheur afficheur = new DashboardAfficheur(dashboard, 50);

        afficheur.start();
        ecrivain.start();
        ecrivain.join(10000);
        Thread.sleep(200);
        afficheur.arreter();
        afficheur.join(2000);

        assertTrue(dashboard.getTotalEmbarquements() == 100,
                "Le dashboard devrait avoir 100 embarquements");
        assertTrue(afficheur.getNbAffichages() > 0,
                "L'afficheur devrait avoir fonctionne en parallele avec l'ecrivain");
    }
}
