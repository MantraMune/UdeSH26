package ca.usherbrooke.ift630.tp2.coordination;

import ca.usherbrooke.ift630.tp2.modele.Evenement;
import ca.usherbrooke.ift630.tp2.modele.TypeEvenement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * File d'attente prioritaire thread-safe pour les evenements STS.
 * Utilise des files separees par type d'evenement pour eviter
 * le pattern inefficace "prendre-remettre".
 *
 * Architecture :
 * - File principale : reception de tous les evenements (producteurs)
 * - Files par type : EMBARQUEMENT, DEBARQUEMENT, PAIEMENT (consommateurs)
 * - Le dispatching est fait automatiquement lors de la production.
 *
 * Les evenements de passagers HAUTE priorite sont traites en premier
 * grace a l'implementation de Comparable dans Evenement.
 */
public class FileAttentePrioritaire {

    private static final Logger logger = LoggerFactory.getLogger(FileAttentePrioritaire.class);

    private final PriorityBlockingQueue<Evenement> fileEmbarquement;
    private final PriorityBlockingQueue<Evenement> fileDebarquement;
    private final PriorityBlockingQueue<Evenement> filePaiement;
    private final AtomicInteger totalProduits;
    private final AtomicInteger totalConsommes;

    public FileAttentePrioritaire() {
        this.fileEmbarquement = new PriorityBlockingQueue<>(500);
        this.fileDebarquement = new PriorityBlockingQueue<>(500);
        this.filePaiement = new PriorityBlockingQueue<>(500);
        this.totalProduits = new AtomicInteger(0);
        this.totalConsommes = new AtomicInteger(0);
    }

    /**
     * Depose un evenement et le dispatche automatiquement dans la file appropriee.
     */
    public void produire(Evenement evenement) {
        switch (evenement.getType()) {
            case EMBARQUEMENT -> fileEmbarquement.put(evenement);
            case DEBARQUEMENT -> fileDebarquement.put(evenement);
            case PAIEMENT -> filePaiement.put(evenement);
        }
        int count = totalProduits.incrementAndGet();
        logger.debug("Evenement produit [{}] : {} (type: {})", count, evenement, evenement.getType());
    }

    /**
     * Retire et retourne le prochain evenement du type specifie.
     * Methode bloquante : attend si la file du type est vide.
     */
    public Evenement consommerParType(TypeEvenement type) throws InterruptedException {
        Evenement evt = switch (type) {
            case EMBARQUEMENT -> fileEmbarquement.take();
            case DEBARQUEMENT -> fileDebarquement.take();
            case PAIEMENT -> filePaiement.take();
        };
        totalConsommes.incrementAndGet();
        return evt;
    }

    /**
     * Methode legacy : retire le prochain evenement de n'importe quel type.
     * Utilise un poll non-bloquant sur les 3 files dans l'ordre de priorite.
     */
    public Evenement consommer() throws InterruptedException {
        // Essayer dans l'ordre : paiement, embarquement, debarquement
        Evenement evt = filePaiement.poll();
        if (evt == null) evt = fileEmbarquement.poll();
        if (evt == null) evt = fileDebarquement.poll();
        if (evt != null) {
            totalConsommes.incrementAndGet();
            return evt;
        }
        // Si toutes les files sont vides, attendre sur la file d'embarquement
        evt = fileEmbarquement.take();
        totalConsommes.incrementAndGet();
        return evt;
    }

    /**
     * Remet un evenement dans la file appropriee.
     */
    public void remettre(Evenement evenement) {
        switch (evenement.getType()) {
            case EMBARQUEMENT -> fileEmbarquement.put(evenement);
            case DEBARQUEMENT -> fileDebarquement.put(evenement);
            case PAIEMENT -> filePaiement.put(evenement);
        }
    }

    public int getTaille() {
        return fileEmbarquement.size() + fileDebarquement.size() + filePaiement.size();
    }

    public int getTailleEmbarquement() {
        return fileEmbarquement.size();
    }

    public int getTailleDebarquement() {
        return fileDebarquement.size();
    }

    public int getTaillePaiement() {
        return filePaiement.size();
    }

    public int getTotalProduits() {
        return totalProduits.get();
    }

    public int getTotalConsommes() {
        return totalConsommes.get();
    }

    public boolean estVide() {
        return fileEmbarquement.isEmpty() && fileDebarquement.isEmpty() && filePaiement.isEmpty();
    }
}
