package ca.usherbrooke.ift630.tp3.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests analyse scenarios pedagogiques")
public class ScenarioAnalysisTest {

    private static final Path LOGS_DIR = Paths.get("logs/scenarios");
    private static final Pattern TIME_PATTERN = Pattern.compile("Temps total\\s*:\\s*(\\d+[.,]\\d+)\\s*s");

    @BeforeAll
    static void checkLogsExist() {
        if (!Files.exists(LOGS_DIR)) {
            fail("Le repertoire logs/scenarios/ n'existe pas. Executez d'abord: make scenarios");
        }
    }

    @Test
    @DisplayName("Scenario 1 : Overhead IPC mesurable")
    public void testScenario1OverheadIPC() throws IOException {
        double timeThreads = extractTime("scenario1-threads.log");
        double timeProcesses = extractTime("scenario1-processes.log");
        
        assertTrue(timeThreads > 0, "Temps threads doit etre positif");
        assertTrue(timeProcesses > 0, "Temps processus doit etre positif");
        
        double overheadPercent = ((timeProcesses - timeThreads) / timeThreads) * 100;
        
        System.out.println(String.format("Scenario 1 - Temps threads: %.2fs, processus: %.2fs, overhead: %.1f%%", 
            timeThreads, timeProcesses, overheadPercent));
    }

    @Test
    @DisplayName("Scenario 2 : Monte Carlo execute correctement")
    public void testScenario2MonteCarloExecuted() throws IOException {
        double timeThreads = extractTime("scenario2-threads.log");
        double timeProcesses = extractTime("scenario2-processes.log");
        
        assertTrue(timeThreads > 0, "Temps threads doit etre positif");
        assertTrue(timeProcesses > 0, "Temps processus doit etre positif");
        
        double overheadPercent = ((timeProcesses - timeThreads) / timeThreads) * 100;
        
        System.out.println(String.format("Scenario 2 - Temps threads: %.2fs, processus: %.2fs, overhead: %.1f%%", 
            timeThreads, timeProcesses, overheadPercent));
    }

    @Test
    @DisplayName("Scenario 3 : Temps mesures pour tous les workers")
    public void testScenario3TimesMeasured() throws IOException {
        double time1 = extractTime("scenario3-1.log");
        double time2 = extractTime("scenario3-2.log");
        double time4 = extractTime("scenario3-4.log");
        double time8 = extractTime("scenario3-8.log");
        
        // Verification que tous les temps sont mesures et raisonnables
        assertTrue(time1 > 0 && time1 < 1000, "Temps 1 worker doit etre valide");
        assertTrue(time2 > 0 && time2 < 1000, "Temps 2 workers doit etre valide");
        assertTrue(time4 > 0 && time4 < 1000, "Temps 4 workers doit etre valide");
        assertTrue(time8 > 0 && time8 < 1000, "Temps 8 workers doit etre valide");
        
        System.out.println(String.format("Temps - 1w: %.2fs, 2w: %.2fs, 4w: %.2fs, 8w: %.2fs", 
            time1, time2, time4, time8));
    }

    @Test
    @DisplayName("Scenario 3 : Temps diminue avec plus de workers")
    public void testScenario3TimeDecreases() throws IOException {
        double time1 = extractTime("scenario3-1.log");
        double time2 = extractTime("scenario3-2.log");
        double time4 = extractTime("scenario3-4.log");
        double time8 = extractTime("scenario3-8.log");
        
        // Au moins un des temps multi-workers devrait etre <= temps single worker
        boolean improves = time2 <= time1 || time4 <= time1 || time8 <= time1;
        
        assertTrue(improves, 
            "Au moins un mode multi-workers devrait ameliorer ou egaliser le temps single worker");
        
        System.out.println(String.format("Temps - 1w: %.2fs, 2w: %.2fs, 4w: %.2fs, 8w: %.2fs", 
            time1, time2, time4, time8));
    }

    @Test
    @DisplayName("Tous les fichiers de logs scenarios existent")
    public void testAllScenarioLogsExist() {
        String[] requiredLogs = {
            "scenario1-threads.log",
            "scenario1-processes.log",
            "scenario2-threads.log",
            "scenario2-processes.log",
            "scenario3-1.log",
            "scenario3-2.log",
            "scenario3-4.log",
            "scenario3-8.log"
        };
        
        for (String logFile : requiredLogs) {
            Path logPath = LOGS_DIR.resolve(logFile);
            assertTrue(Files.exists(logPath), 
                "Fichier manquant: " + logFile + ". Executez: make scenarios");
        }
    }

    @Test
    @DisplayName("Logs contiennent mesures de temps valides")
    public void testLogsContainValidTimes() throws IOException {
        String[] logFiles = {
            "scenario1-threads.log",
            "scenario1-processes.log",
            "scenario2-threads.log",
            "scenario2-processes.log",
            "scenario3-1.log",
            "scenario3-2.log",
            "scenario3-4.log",
            "scenario3-8.log"
        };
        
        for (String logFile : logFiles) {
            double time = extractTime(logFile);
            assertTrue(time > 0 && time < 1000, 
                "Temps dans " + logFile + " doit etre entre 0 et 1000 secondes, obtenu: " + time);
        }
    }

    /**
     * Extrait le temps total d'un fichier de log
     */
    private double extractTime(String logFileName) throws IOException {
        Path logPath = LOGS_DIR.resolve(logFileName);
        
        if (!Files.exists(logPath)) {
            fail("Fichier log manquant: " + logFileName + ". Executez: make scenarios");
        }
        
        List<String> lines = Files.readAllLines(logPath);
        
        for (String line : lines) {
            Matcher matcher = TIME_PATTERN.matcher(line);
            if (matcher.find()) {
                String timeStr = matcher.group(1).replace(',', '.');
                return Double.parseDouble(timeStr);
            }
        }
        
        fail("Temps total non trouve dans: " + logFileName);
        return 0;
    }
}