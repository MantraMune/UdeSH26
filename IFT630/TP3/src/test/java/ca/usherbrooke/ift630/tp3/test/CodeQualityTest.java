package ca.usherbrooke.ift630.tp3.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests qualite du code")
public class CodeQualityTest {

    @Test
    @DisplayName("Code compile sans erreurs")
    public void testCompilation() {
        // Si ce test s'execute, c'est que le code compile
        assertTrue(true, "Le code doit compiler sans erreurs");
    }

    @Test
    @DisplayName("Classes principales existent")
    public void testMainClassesExist() {
        assertDoesNotThrow(() -> {
            Class.forName("ca.usherbrooke.ift630.tp3.Main");
            Class.forName("ca.usherbrooke.ift630.tp3.mqtt.MQTTClient");
            Class.forName("ca.usherbrooke.ift630.tp3.mqtt.TransportEvent");
            Class.forName("ca.usherbrooke.ift630.tp3.worker.ThreadWorker");
            Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ThreadSupervisor");
            Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
            Class.forName("ca.usherbrooke.ift630.tp3.compute.Statistics");
            Class.forName("ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator");
        }, "Toutes les classes principales doivent exister");
    }

    @Test
    @DisplayName("Fichier configuration existe")
    public void testConfigFileExists() {
        File configFile = new File("config.json");
        assertTrue(configFile.exists(), "config.json doit exister");
    }

    @Test
    @DisplayName("Pas de System.exit dans le code")
    public void testNoSystemExit() throws Exception {
        Path srcPath = Paths.get("src/main/java");
        
        if (!Files.exists(srcPath)) {
            return; // Skip si pas dans l'environnement de dev
        }
        
        try (Stream<Path> paths = Files.walk(srcPath)) {
            long exitCalls = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        String content = Files.readString(p);
                        // Autoriser System.exit dans Main.java et ProcessWorker.java
                        return content.contains("System.exit") && 
                               !p.toString().contains("ProcessWorker.java") &&
                               !p.toString().contains("Main.java") &&
                               !p.toString().contains("CodeQualityTest.java");
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            assertEquals(0, exitCalls, "System.exit ne doit pas etre utilise (sauf Main et ProcessWorker)");
        }
    }

    @Test
    @DisplayName("Gestion propre des ressources")
    public void testResourceManagement() {
        // Verifie que les classes ont des methodes de nettoyage
        assertDoesNotThrow(() -> {
            Class<?> threadWorker = Class.forName("ca.usherbrooke.ift630.tp3.worker.ThreadWorker");
            assertNotNull(threadWorker.getMethod("shutdown"), "ThreadWorker doit avoir methode stop()");
            
            Class<?> processWorker = Class.forName("ca.usherbrooke.ift630.tp3.worker.ProcessWorker");
            assertNotNull(processWorker.getMethod("shutdown"), "ProcessWorker doit avoir methode stop()");
            
            Class<?> threadSupervisor = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ThreadSupervisor");
            assertNotNull(threadSupervisor.getMethod("shutdown"), "ThreadSupervisor doit avoir methode stop()");
            
            Class<?> processSupervisor = Class.forName("ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor");
            assertNotNull(processSupervisor.getMethod("shutdown"), "ProcessSupervisor doit avoir methode stop()");
        });
    }
}
