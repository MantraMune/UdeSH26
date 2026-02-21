package udes.fsci.info.ift630.transport.evenements;
public class Paiement extends Evenement {
    private final String passagerId, methodePaiement;
    private final double montant;
    public Paiement(String passagerId, double montant, String methodePaiement, long timestamp) {
        super(TypeEvenement.PAIEMENT, timestamp);
        this.passagerId = passagerId; this.montant = montant; this.methodePaiement = methodePaiement;
    }
    public String getPassagerId() { return passagerId; }
    public double getMontant() { return montant; }
    public String getMethodePaiement() { return methodePaiement; }
}
