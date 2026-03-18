package ca.usherbrooke.ift630.tp2.threads;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.Dashboard;
import ca.usherbrooke.ift630.tp2.coordination.FileAttentePrioritaire;
import ca.usherbrooke.ift630.tp2.modele.Bus;
import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.Priorite;
import ca.usherbrooke.ift630.tp2.modele.TypeEvenement;

/**
 * Consommateur specialise pour les evenements de PAIEMENT.
 * Tarifs : 3.50$ regulier (NORMALE), 2.00$ senior (HAUTE).
 * Accumule les revenus de maniere thread-safe.
 */
public class ConsommateurPaiement extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ConsommateurPaiement.class);

    private static final long TARIF_REGULIER_CENTIMES = 350;
    private static final long TARIF_SENIOR_CENTIMES = 200;

    private final FileAttentePrioritaire fileEvenements;
    private final Map<String, Bus> busMap;
    private final Dashboard dashboard;
    private final AtomicInteger compteurTraites = new AtomicInteger(0);
    private final AtomicLong revenusAccumules = new AtomicLong(0);

    public ConsommateurPaiement(FileAttentePrioritaire fileEvenements,
                                 Map<String, Bus> busMap,
                                 Dashboard dashboard) {
        super("Conso-Paiement");
        this.fileEvenements = fileEvenements;
        this.busMap = busMap;
        this.dashboard = dashboard;
    }

    @Override
    public void run() {
        logger.info("Consommateur PAIEMENT demarre");

        while(!Thread.interrupted()){
            try {
                // Consommer les évènements de paiement dans la file paiement
                Evenement evt = fileEvenements.consommerParType(TypeEvenement.PAIEMENT);

                if (evt == null) // Si aucun évènement
                {
                    Thread.sleep(50);
                    continue;
                }

                // Récupérer le bus de l'évènement
                Bus bus = busMap.get(evt.getBusId());

                if (bus == null)
                {
                    logger.warn("Bus non trouvé pour l'évènement {}", evt);
                    continue;
                }

                Priorite priorite = evt.getPassager().getPriorite();
                long montant = 0;
                int numeroLigne = bus.getLigne().getNumero();

                // Cas 1 : Tarif pour priorité normale
                if (priorite == Priorite.NORMALE) {
                    montant = TARIF_REGULIER_CENTIMES;
                }
                // Cas 2 : Tarif pour priorité haute
                else if (priorite == Priorite.HAUTE) {
                    montant = TARIF_SENIOR_CENTIMES;
                }

                revenusAccumules.addAndGet(montant);
                compteurTraites.incrementAndGet();

                if(dashboard != null) {
                    dashboard.enregistrerPaiement(evt, montant, numeroLigne);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Erreur dans le consommateur de paiement", e);
            }
        }

        logger.info("Consommateur PAIEMENT arrete ({} evenements traites, revenus: {} $)",
                compteurTraites.get(), String.format("%.2f", getRevenusDollars()));
    }

    public int getCompteurTraites() {
        return compteurTraites.get();
    }

    public double getRevenusDollars() {
        return revenusAccumules.get() / 100.0;
    }
}
