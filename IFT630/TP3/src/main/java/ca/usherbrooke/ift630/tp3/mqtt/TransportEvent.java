package ca.usherbrooke.ift630.tp3.mqtt;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Représente un événement de transport reçu via MQTT.
 * Structure correspondant au producer.py
 */
public class TransportEvent {
    private String type;           // "embarquement", "debarquement", "trajet"
    private String busId;          // "BUS_001"
    private String ligne;          // "1", "2", etc.
    private String passagerId;     // "PASS_12345"
    private String arret;          // "Université", etc.
    private double montant;        // 2.5 - 4.0
    private String methode;        // "carte", "cash", "mobile"
    private String origine;        // Arrêt origine
    private String destination;    // Arrêt destination
    private String timestamp;      // ISO 8601 format

    // Constructeur vide pour Gson
    public TransportEvent() {}

    // Getters
    public String getType() { return type; }
    public String getBusId() { return busId; }
    public String getLigne() { return ligne; }
    public String getPassagerId() { return passagerId; }
    public String getArret() { return arret; }
    public double getMontant() { return montant; }
    public String getMethode() { return methode; }
    public String getOrigine() { return origine; }
    public String getDestination() { return destination; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setBusId(String busId) { this.busId = busId; }
    public void setLigne(String ligne) { this.ligne = ligne; }
    public void setPassagerId(String passagerId) { this.passagerId = passagerId; }
    public void setArret(String arret) { this.arret = arret; }
    public void setMontant(double montant) { this.montant = montant; }
    public void setMethode(String methode) { this.methode = methode; }
    public void setOrigine(String origine) { this.origine = origine; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    /**
     * Parse un message JSON en TransportEvent
     */
    public static TransportEvent fromJson(String json) {
        try {
            return new Gson().fromJson(json, TransportEvent.class);
        } catch (JsonSyntaxException e) {
            System.err.println("  Erreur de parsing JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertit l'événement en JSON
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return String.format("Event[%s, ligne=%s, bus=%s, montant=%.2f, arret=%s]",
                type, ligne, busId, montant, arret);
    }
}
