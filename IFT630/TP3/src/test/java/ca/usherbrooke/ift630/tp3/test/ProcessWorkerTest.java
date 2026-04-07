package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.worker.ProcessWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests ProcessWorker et IPC")
public class ProcessWorkerTest {

    @Test
    @DisplayName("ProcessWorker classe existe")
    public void testProcessWorkerExists() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            assertNotNull(clazz);
        });
    }

    @Test
    @DisplayName("ProcessWorker implemente TransportWorker")
    public void testImplementsInterface() throws ClassNotFoundException {
        Class<?> workerClass = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
        Class<?> interfaceClass = Class.forName("ca.usherbrooke.ift630.tp3.worker.TransportWorker");
        
        assertTrue(interfaceClass.isAssignableFrom(workerClass),
                   "ProcessWorker doit implementer TransportWorker");
    }

    @Test
    @DisplayName("ProcessWorker a methode main")
    public void testHasMainMethod() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            assertNotNull(clazz.getMethod("main", String[].class),
                         "ProcessWorker doit avoir methode main pour lancement processus");
        });
    }

    @Test
    @DisplayName("ProcessWorker a constructeur approprie")
    public void testHasConstructor() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            assertNotNull(clazz.getConstructor(int.class, String.class, int.class),
                         "ProcessWorker doit avoir constructeur(int, String, int)");
        });
    }

    @Test
    @DisplayName("ProcessWorker a methodes requises")
    public void testHasRequiredMethods() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            assertNotNull(clazz.getMethod("start"));
            assertNotNull(clazz.getMethod("shutdown"));
            assertNotNull(clazz.getMethod("getStatistics"));
            assertNotNull(clazz.getMethod("isRunning"));
        });
    }
}
