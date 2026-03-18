package ca.usherbrooke.ift630.tp2.modele;

/**
 * Represente un evenement dans la simulation STS.
 * Implemente Comparable pour la PriorityBlockingQueue :
 * les evenements de passagers HAUTE priorite sont traites avant les NORMALE.
 * A priorite egale, l'ordre est par timestamp croissant.
 */
public class Evenement implements Comparable<Evenement> {

    private final int timestamp;
    private final TypeEvenement type;
    private final String busId;
    private final String passagerId;
    private final String stationNom;
    private final Priorite prioritePassager;

    // References vers les objets reels (remplies lors de l'execution)
    private Bus bus;
    private Passager passager;

    public Evenement(int timestamp, TypeEvenement type, String busId,
                     String passagerId, String stationNom, Priorite prioritePassager) {
        this.timestamp = timestamp;
        this.type = type;
        this.busId = busId;
        this.passagerId = passagerId;
        this.stationNom = stationNom;
        this.prioritePassager = prioritePassager;
    }

    /**
     * Comparaison pour la file prioritaire :
     * 1. Priorite HAUTE avant NORMALE
     * 2. A priorite egale, timestamp plus petit en premier
     */
    @Override
    public int compareTo(Evenement autre) {
        // HAUTE (ordinal 0) avant NORMALE (ordinal 1)
        int cmpPriorite = this.prioritePassager.ordinal() - autre.prioritePassager.ordinal();
        if (cmpPriorite != 0) {
            return cmpPriorite;
        }
        return Integer.compare(this.timestamp, autre.timestamp);
    }

    // --- Accesseurs ---

    public int getTimestamp() {
        return timestamp;
    }

    public TypeEvenement getType() {
        return type;
    }

    public String getBusId() {
        return busId;
    }

    public String getPassagerId() {
        return passagerId;
    }

    public String getStationNom() {
        return stationNom;
    }

    public Priorite getPrioritePassager() {
        return prioritePassager;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public Passager getPassager() {
        return passager;
    }

    public void setPassager(Passager passager) {
        this.passager = passager;
    }

    /**
     * Format CSV pour la sauvegarde dans les fichiers de scenario.
     */
    public String toCSV() {
        return String.format("%d,%s,%s,%s,%s,%s",
                timestamp, type, busId, passagerId, stationNom, prioritePassager);
    }

    /**
     * Parse une ligne CSV pour recreer un evenement.
     */
    public static Evenement fromCSV(String ligne) {
        String[] parts = ligne.split(",");
        return new Evenement(
                Integer.parseInt(parts[0].trim()),
                TypeEvenement.valueOf(parts[1].trim()),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                Priorite.valueOf(parts[5].trim())
        );
    }

    @Override
    public String toString() {
        return String.format("Evenement[t=%d, %s, %s, %s, %s, %s]",
                timestamp, type, busId, passagerId, stationNom, prioritePassager);
    }
}
