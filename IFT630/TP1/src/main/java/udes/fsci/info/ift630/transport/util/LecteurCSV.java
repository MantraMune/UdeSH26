package udes.fsci.info.ift630.transport.util;

import udes.fsci.info.ift630.transport.evenements.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;

/**
 * Lecteur de fichiers CSV pour charger les événements de test.
 * FOURNI - NE PAS MODIFIER
 */
public class LecteurCSV {
    
    private static final Logger logger = LoggerFactory.getLogger(LecteurCSV.class);
    
    public static List<Evenement> lireFichier(String nomFichier) throws IOException {
        List<Evenement> evenements = new ArrayList<>();
        
        InputStream is = LecteurCSV.class.getClassLoader().getResourceAsStream(nomFichier);
        if (is == null) {
            throw new IOException("Fichier non trouvé: " + nomFichier);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            reader.readLine(); // Skip header
            
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;
                
                try {
                    Evenement evt = parseLigne(ligne);
                    if (evt != null) evenements.add(evt);
                } catch (Exception e) {
                    logger.warn("Erreur parsing: {}", ligne, e);
                }
            }
        }
        
        logger.info("Fichier {} chargé: {} événements", nomFichier, evenements.size());
        return evenements;
    }
    
    private static Evenement parseLigne(String ligne) {
        String[] p = ligne.split(",", -1);
        long ts = Long.parseLong(p[0]);
        
        return switch (p[1]) {
            case "ENTREE_SYSTEME" -> new EntreeSysteme(p[2], p[3], p[4], ts);
            case "PASSAGER_MONTE" -> new PassagerMonte(p[2], p[3], p[4], p[5], ts);
            case "PASSAGER_DESCEND" -> new PassagerDescend(p[2], p[3], p[4], p[5], ts);
            case "PAIEMENT" -> new Paiement(p[2], Double.parseDouble(p[6]), p[7], ts);
            case "BUS_ARRIVEE" -> new BusArrivee(p[3], p[4], p[5], ts);
            case "BUS_DEPART" -> new BusDepart(p[3], p[4], p[5], ts);
            default -> null;
        };
    }
}
