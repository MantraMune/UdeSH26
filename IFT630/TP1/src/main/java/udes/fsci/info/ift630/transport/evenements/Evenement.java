package udes.fsci.info.ift630.transport.evenements;
public abstract class Evenement {
    public enum TypeEvenement { ENTREE_SYSTEME, PASSAGER_MONTE, PASSAGER_DESCEND, BUS_ARRIVEE, BUS_DEPART, PAIEMENT }
    private final TypeEvenement type;
    private final long timestamp;
    protected Evenement(TypeEvenement type, long timestamp) { this.type = type; this.timestamp = timestamp; }
    public TypeEvenement getType() { return type; }
    public long getTimestamp() { return timestamp; }
}
