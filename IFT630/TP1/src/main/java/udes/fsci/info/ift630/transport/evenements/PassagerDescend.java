package udes.fsci.info.ift630.transport.evenements;
public class PassagerDescend extends Evenement {
    private final String passagerId, busId, ligne, arret;
    public PassagerDescend(String passagerId, String busId, String ligne, String arret, long timestamp) {
        super(TypeEvenement.PASSAGER_DESCEND, timestamp);
        this.passagerId = passagerId; this.busId = busId; this.ligne = ligne; this.arret = arret;
    }
    public String getPassagerId() { return passagerId; }
    public String getBusId() { return busId; }
    public String getLigne() { return ligne; }
    public String getArret() { return arret; }
}
