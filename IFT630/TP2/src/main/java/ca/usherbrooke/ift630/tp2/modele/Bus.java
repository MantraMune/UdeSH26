package ca.usherbrooke.ift630.tp2.modele;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represente un autobus du reseau STS.
 */
public class Bus {

    private static final Logger logger = LoggerFactory.getLogger(Bus.class);

    private final String id;
    private final int capaciteMax;
    private final List<Passager> passagersActuels;
    private final Ligne ligne;

    private final ReentrantReadWriteLock rwLock;

    public Bus(String id, int capaciteMax, Ligne ligne) {
        this.id = id;
        this.capaciteMax = capaciteMax;
        this.passagersActuels = new ArrayList<>();
        this.ligne = ligne;
        this.rwLock = new ReentrantReadWriteLock(true); // Lock équitable
    }

    // --- Methodes de lecture ---

    public int getNombrePassagers() {
        rwLock.readLock().lock(); 
        try{
            return passagersActuels.size();
        } finally {
            rwLock.readLock().unlock();
        }
        
    }

    public double getPourcentageOccupation() {
        rwLock.readLock().lock();
        try {
        return (double) passagersActuels.size() / capaciteMax * 100.0;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Passager> getPassagers() {
        rwLock.readLock().lock();
        try {
        return Collections.unmodifiableList(new ArrayList<>(passagersActuels));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean estPlein() {
        rwLock.readLock().lock();
        try{
        return passagersActuels.size() >= capaciteMax;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // --- Methodes d'ecriture ---.

    public boolean embarquer(Passager passager) {
        
        rwLock.writeLock().lock();
        try {
            if (passagersActuels.size() >= capaciteMax) {
                logger.debug("Bus {} plein ({}/{}), impossible d'embarquer {}",
                    id, passagersActuels.size(), capaciteMax, passager.getId());
                return false;
            } 

            passagersActuels.add(passager);

            logger.debug("Bus {} : embarquement de {} ({}/{})",
                id, passager.getId(), passagersActuels.size(), capaciteMax);

            return true;

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean debarquer(Passager passager) {

        rwLock.writeLock().lock();
        try {
            boolean retire = passagersActuels.remove(passager);
            if (retire) {
                logger.debug("Bus {} : debarquement de {} ({}/{})",
                    id, passager.getId(), passagersActuels.size(), capaciteMax);
            } else {
                logger.warn("Bus {} : passager {} non trouve pour debarquement",
                    id, passager.getId());
            }
            return retire;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // --- Accesseurs simples (pas besoin de lock) ---

    public String getId() { return id; }
    public int getCapaciteMax() { return capaciteMax; }
    public Ligne getLigne() { return ligne; }

    @Override
    public String toString() {
        return String.format("Bus[%s, ligne=%s, passagers=%d/%d]",
                id, ligne.getNumero(), getNombrePassagers(), capaciteMax);
    }
    
    public ReentrantReadWriteLock getRwLock()
    {
        return rwLock;
    }
}
