package ca.usherbrooke.ift630.tp2.modele;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represente une station du reseau STS.
 */
public class Station {

    private static final Logger logger = LoggerFactory.getLogger(Station.class);

    private final String nom;
    private final int capaciteAttente;
    private int passagersEnAttente;
    private final List<Bus> busPresents;

    private final ReentrantReadWriteLock rwLock;

    public Station(String nom, int capaciteAttente) {
        this.nom = nom;
        this.capaciteAttente = capaciteAttente;
        this.passagersEnAttente = 0;
        this.busPresents = new ArrayList<>();
        this.rwLock = new ReentrantReadWriteLock(true);
    }

    // --- Methodes de lecture ---.

    public int getNombrePassagersEnAttente() {
        rwLock.readLock().lock();
        try {
            return passagersEnAttente;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getCapaciteAttente() {
        rwLock.readLock().lock();
        try {
        return capaciteAttente;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean estPleine() {
        rwLock.readLock().lock();
        try {
        return passagersEnAttente >= capaciteAttente;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Bus> getBusPresents() {
        rwLock.readLock().lock();
        try {
        return Collections.unmodifiableList(new ArrayList<>(busPresents));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // --- Methodes d'ecriture ---

    public void ajouterPassagerEnAttente() {
        rwLock.writeLock().lock();
        try {
            passagersEnAttente++;
            logger.debug("Station {} : +1 passager en attente (total: {})", nom, passagersEnAttente); 
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void retirerPassagerEnAttente() {
        rwLock.writeLock().lock();
        try {
            if (passagersEnAttente > 0) {
                passagersEnAttente--;
                logger.debug("Station {} : -1 passager en attente (total: {})", nom, passagersEnAttente);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Enregistre l'arrivee d'un bus a cette station.
     * Doit notifier les passagers en attente.
     */
    public synchronized void arriverBus(Bus bus) {
        rwLock.writeLock().lock();
        try {
            busPresents.add(bus);
            logger.debug("Station {} : arrivee du bus {}", nom, bus.getId());
            notifyAll();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void departBus(Bus bus) {
        rwLock.writeLock().lock();
        try {
            busPresents.remove(bus);
            logger.debug("Station {} : depart du bus {}", nom, bus.getId());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String getNom() { return nom; }

    @Override
    public String toString() {
        return String.format("Station[%s, attente=%d/%d, bus=%d]",
                nom, getNombrePassagersEnAttente(), capaciteAttente, getBusPresents().size());
    }

    public ReentrantReadWriteLock getRwLock() 
    {
        return rwLock;
    }
}
