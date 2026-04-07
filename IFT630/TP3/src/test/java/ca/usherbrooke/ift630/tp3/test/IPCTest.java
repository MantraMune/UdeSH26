package ca.usherbrooke.ift630.tp3.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests communication IPC")
public class IPCTest {

    @Test
    @DisplayName("ProcessSupervisor classe existe")
    public void testProcessSupervisorExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
            assertNotNull(clazz);
        });
    }

    @Test
    @DisplayName("ProcessSupervisor implemente Supervisor")
    public void testImplementsSupervisor() throws ClassNotFoundException {
        Class<?> supervisorClass = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
        Class<?> interfaceClass = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.Supervisor");
        
        assertTrue(interfaceClass.isAssignableFrom(supervisorClass),
                   "ProcessSupervisor doit implementer Supervisor");
    }

    @Test
    @DisplayName("ProcessSupervisor a constructeur approprie")
    public void testProcessSupervisorConstructor() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
            assertNotNull(clazz.getConstructor(int.class, String.class),
                         "ProcessSupervisor doit avoir constructeur(int, String)");
        });
    }

    @Test
    @DisplayName("ProcessSupervisor a methodes IPC")
    public void testProcessSupervisorMethods() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
            assertNotNull(clazz.getMethod("start"));
            assertNotNull(clazz.getMethod("shutdown"));
            assertNotNull(clazz.getMethod("awaitCompletion", int.class));
        });
    }

    @Test
    @DisplayName("TransportEvent serialisable JSON")
    public void testEventSerialization() throws Exception {
        Class<?> eventClass = Class.forName("ca.usherbrooke.ift630.tp3.mqtt.TransportEvent");
        Object event = eventClass.getDeclaredConstructor().newInstance();
        
        // Verifier presence methodes serialisation
        assertNotNull(eventClass.getMethod("toJson"),
                     "TransportEvent doit avoir methode toJson()");
        assertNotNull(eventClass.getMethod("fromJson", String.class),
                     "TransportEvent doit avoir methode fromJson(String)");
    }
}
