package udes.fsci.info.ift630.transport.evenements;
public class BusArrivee extends Evenement {
    private final String busId, ligne, arret;
    public BusArrivee(String busId, String ligne, String arret, long timestamp) {
        super(TypeEvenement.BUS_ARRIVEE, timestamp);
        this.busId = busId; this.ligne = ligne; this.arret = arret;
    }
    public String getBusId() { return busId; }
    public String getLigne() { return ligne; }
    public String getArret() { return arret; }
}
