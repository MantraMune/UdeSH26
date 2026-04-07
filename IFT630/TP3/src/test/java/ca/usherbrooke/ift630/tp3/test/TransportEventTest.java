package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests TransportEvent et parsing JSON")
public class TransportEventTest {

    @Test
    @DisplayName("Parse JSON valide embarquement")
    public void testParseEmbarquement() {
        String json = "{\"type\":\"embarquement\",\"busId\":\"BUS_001\",\"ligne\":\"1\"," +
                      "\"passagerId\":\"PASS_12345\",\"arret\":\"Université\",\"montant\":3.50," +
                      "\"methode\":\"carte\",\"origine\":\"Université\",\"destination\":\"Centre-ville\"," +
                      "\"timestamp\":\"2025-01-15T10:30:00.000-05:00\"}";
        
        TransportEvent event = TransportEvent.fromJson(json);
        
        assertNotNull(event, "Event ne doit pas etre null");
        assertEquals("embarquement", event.getType());
        assertEquals("BUS_001", event.getBusId());
        assertEquals("1", event.getLigne());
        assertEquals("PASS_12345", event.getPassagerId());
        assertEquals("Université", event.getArret());
        assertEquals(3.50, event.getMontant(), 0.01);
        assertEquals("carte", event.getMethode());
    }

    @Test
    @DisplayName("Parse JSON valide debarquement")
    public void testParseDebarquement() {
        String json = "{\"type\":\"debarquement\",\"busId\":\"BUS_002\",\"ligne\":\"2\"," +
                      "\"passagerId\":\"PASS_67890\",\"arret\":\"Centre-ville\",\"montant\":0.0," +
                      "\"methode\":\"\",\"origine\":\"Université\",\"destination\":\"Centre-ville\"," +
                      "\"timestamp\":\"2025-01-15T10:35:00.000-05:00\"}";
        
        TransportEvent event = TransportEvent.fromJson(json);
        
        assertNotNull(event);
        assertEquals("debarquement", event.getType());
        assertEquals(0.0, event.getMontant(), 0.01);
    }

    @Test
    @DisplayName("Parse JSON invalide retourne null")
    public void testParseInvalidJson() {
        String invalidJson = "{invalid json}";
        
        TransportEvent event = TransportEvent.fromJson(invalidJson);
        
        assertNull(event, "JSON invalide doit retourner null");
    }

    @Test
    @DisplayName("Serialisation vers JSON")
    public void testToJson() {
        TransportEvent event = new TransportEvent();
        event.setType("embarquement");
        event.setBusId("BUS_001");
        event.setLigne("1");
        event.setMontant(3.50);
        
        String json = event.toJson();
        
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"embarquement\""));
        assertTrue(json.contains("\"busId\":\"BUS_001\""));
        assertTrue(json.contains("\"montant\":3.5"));
    }
}
