package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.utils.PerformanceMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests PerformanceMonitor")
public class PerformanceMonitorTest {

    @Test
    @DisplayName("Mesure temps total correctement")
    public void testTotalTimeMeasurement() throws InterruptedException {
        PerformanceMonitor monitor = new PerformanceMonitor("threads", 4);
        
        monitor.start();
        Thread.sleep(100);
        monitor.stop(1000);
        
        PerformanceMonitor.PerformanceResult result = monitor.getResult();
        
        assertTrue(result.getTotalSeconds() >= 0.1, "Temps doit etre >= 0.1s");
        assertEquals(1000, result.getTotalEvents());
        assertTrue(result.getThroughput() > 0);
    }

    @Test
    @DisplayName("Mesure phases correctement")
    public void testPhaseMeasurement() throws InterruptedException {
        PerformanceMonitor monitor = new PerformanceMonitor("threads", 4);
        
        monitor.startPhase("phase1");
        Thread.sleep(50);
        monitor.stopPhase("phase1");
        
        monitor.startPhase("phase2");
        Thread.sleep(100);
        monitor.stopPhase("phase2");
        
        double phase1Time = monitor.getPhaseTimeSeconds("phase1");
        double phase2Time = monitor.getPhaseTimeSeconds("phase2");
        
        assertTrue(phase1Time >= 0.05, "Phase 1 doit durer >= 50ms");
        assertTrue(phase2Time >= 0.1, "Phase 2 doit durer >= 100ms");
        assertTrue(phase2Time > phase1Time, "Phase 2 doit etre plus longue");
    }

    @Test
    @DisplayName("Calcule debit correctement")
    public void testThroughputCalculation() {
        PerformanceMonitor monitor = new PerformanceMonitor("threads", 4);
        
        monitor.start();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        monitor.stop(5000);
        
        PerformanceMonitor.PerformanceResult result = monitor.getResult();
        
        assertTrue(result.getThroughput() > 4000, 
                   "Debit doit etre environ 5000 evenements/s");
    }

    @Test
    @DisplayName("Modes threads et processes distincts")
    public void testDifferentModes() {
        PerformanceMonitor threadMonitor = new PerformanceMonitor("threads", 4);
        PerformanceMonitor processMonitor = new PerformanceMonitor("processes", 4);
        
        threadMonitor.start();
        threadMonitor.stop(1000);
        
        processMonitor.start();
        processMonitor.stop(1000);
        
        PerformanceMonitor.PerformanceResult threadResult = threadMonitor.getResult();
        PerformanceMonitor.PerformanceResult processResult = processMonitor.getResult();
        
        assertEquals("threads", threadResult.getMode());
        assertEquals("processes", processResult.getMode());
    }

    @Test
    @DisplayName("Reset efface resultats precedents")
    public void testReset() {
        PerformanceMonitor monitor1 = new PerformanceMonitor("threads", 4);
        monitor1.start();
        monitor1.stop(1000);
        monitor1.getResult();
        
        PerformanceMonitor.reset();
        
        // Apres reset, nouveau test doit fonctionner
        PerformanceMonitor monitor2 = new PerformanceMonitor("threads", 4);
        monitor2.start();
        monitor2.stop(1000);
        PerformanceMonitor.PerformanceResult result = monitor2.getResult();
        
        assertNotNull(result);
    }
}
