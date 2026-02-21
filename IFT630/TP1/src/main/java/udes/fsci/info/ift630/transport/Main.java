package udes.fsci.info.ift630.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        SystemeTransport systeme = new SystemeTransport();
        
        try {
            String fichierArg = args.length > 0 ? args[0] : "scenario_simple.csv";
            // Extraire le nom du fichier sans le chemin
            String nomFichier = Paths.get(fichierArg).getFileName().toString();
            String nomTest = nomFichier.replace(".csv", "");
            
            // Configurer capacité pour scenario_capacite
            if (nomFichier.contains("capacite")) {
                systeme.setCapaciteBus("B011", 40);
                systeme.setCapaciteBus("B012", 40);
                systeme.setCapaciteBus("B021", 45);
                systeme.setCapaciteBus("B022", 45);
                systeme.setCapaciteBus("B023", 45);
                systeme.setCapaciteBus("B024", 45);
                systeme.setCapaciteBus("B025", 45);
                systeme.setCapaciteBus("B031", 50);
                systeme.setCapaciteBus("B041", 40);
                systeme.setCapaciteBus("B042", 40);
                systeme.setCapaciteBus("B051", 35);
                systeme.setCapaciteBus("B052", 35);
                systeme.setCapaciteBus("B061", 40);
                systeme.setCapaciteBus("B062", 40);
                systeme.setCapaciteBus("B063", 40);
                systeme.setCapaciteBus("B064", 40);
                systeme.setCapaciteBus("B065", 40);
                systeme.setCapaciteBus("B066", 40);
                systeme.setCapaciteBus("B067", 40);
                systeme.setCapaciteBus("B068", 40);
                systeme.setCapaciteBus("B069", 40);
                systeme.setCapaciteBus("B070", 40);
            }
            
            systeme.traiterFichier(nomFichier);
            systeme.attendreFinTraitement();
        
            //Thread.sleep(1000); // Attendre l'arrêt complet pour des besoins de logging et d'affichage

            // Afficher les résultats
            try {
                systeme.afficherResultats(nomTest);
            } catch (IOException e) {
                System.err.println("Erreur lors de l'écriture du fichier CSV: " + e.getMessage());
            }
            systeme.arreter();
            
        } catch (Exception e) {
            logger.error("Erreur", e);
        } 
    }
}