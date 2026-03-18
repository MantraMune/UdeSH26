package ca.usherbrooke.ift630.tp2.threads;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.usherbrooke.ift630.tp2.coordination.Dashboard;

/**
 * Thread daemon d'affichage temps reel du dashboard STS.
 * Rafraichit le dashboard a intervalle regulier (500ms par defaut).
 */
public class DashboardAfficheur extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DashboardAfficheur.class);

    private final Dashboard dashboard;
    private final long intervalleMs;
    private final AtomicInteger nbAffichages = new AtomicInteger(0);
    private final AtomicLong totalLecturesMs = new AtomicLong(0);
    private volatile boolean actif = true;

    public DashboardAfficheur(Dashboard dashboard, long intervalleMs) {
        super("DashboardAfficheur");
        this.dashboard = dashboard;
        this.intervalleMs = intervalleMs;
        setDaemon(true);
    }

    public DashboardAfficheur(Dashboard dashboard) {
        this(dashboard, 500);
    }

    @Override
    public void run() {
        logger.info("DashboardAfficheur demarre (rafraichissement toutes les {} ms)", intervalleMs);

        while (actif && !Thread.currentThread().isInterrupted()) {
            try {
                // Attendre l'intervalle
                Thread.sleep(intervalleMs);

                // Mesurer le temps de lecture
                long debut = System.nanoTime();

                // Lecture du dashboard
                dashboard.afficher();

                long fin = System.nanoTime();

                // Mise à jour des stats
                long dureeMs = (fin - debut) / 1_000_000;

                nbAffichages.incrementAndGet();
                totalLecturesMs.addAndGet(dureeMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("DashboardAfficheur arrete ({} affichages effectues)", nbAffichages.get());
    }

    public void arreter() {
        actif = false;
        interrupt();
    }

    public int getNbAffichages() { return nbAffichages.get(); }
    public long getTotalLecturesMs() { return totalLecturesMs.get(); }

    public double getMoyenneLectureMs() {
        int nb = nbAffichages.get();
        return nb > 0 ? (double) totalLecturesMs.get() / nb : 0;
    }
}
