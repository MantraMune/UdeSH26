package ca.usherbrooke.ift630.tp2.modele;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represente un passager du reseau STS.
 * Chaque passager a une priorite (HAUTE pour mobilite reduite/seniors, NORMALE sinon),
 * une station de depart et une station de destination.
 */
public class Passager {

    private static final Logger logger = LoggerFactory.getLogger(Passager.class);

    private final String id;
    private final Priorite priorite;
    private final String stationDepart;
    private final String stationDestination;

    public Passager(String id, Priorite priorite, String stationDepart, String stationDestination) {
        this.id = id;
        this.priorite = priorite;
        this.stationDepart = stationDepart;
        this.stationDestination = stationDestination;
    }

    public String getId() {
        return id;
    }

    public Priorite getPriorite() {
        return priorite;
    }

    public String getStationDepart() {
        return stationDepart;
    }

    public String getStationDestination() {
        return stationDestination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passager passager = (Passager) o;
        return id.equals(passager.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Passager[%s, %s, %s -> %s]",
                id, priorite, stationDepart, stationDestination);
    }
}
