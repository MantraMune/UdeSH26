package udes.fsci.info.ift630.transport.evenements;
public class EntreeSysteme extends Evenement {
    private final String passagerId, origine, destination;
    public EntreeSysteme(String passagerId, String origine, String destination, long timestamp) {
        super(TypeEvenement.ENTREE_SYSTEME, timestamp);
        this.passagerId = passagerId; this.origine = origine; this.destination = destination;
    }
    public String getPassagerId() { return passagerId; }
    public String getOrigine() { return origine; }
    public String getDestination() { return destination; }
}
