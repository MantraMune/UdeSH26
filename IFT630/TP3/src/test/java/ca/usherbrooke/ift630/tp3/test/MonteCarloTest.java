package ca.usherbrooke.ift630.tp3.test;

import ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests simulateur Monte Carlo")
public class MonteCarloTest {

    @Test
    @DisplayName("Simulation avec petite iteration")
    public void testSmallSimulation() {
        MonteCarloSimulator simulator = new MonteCarloSimulator(100);
        List<TransportEvent> events = createTestEvents(50);
        
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(events);
        
        assertNotNull(result);
        assertTrue(result.getAvgWaitTime() > 0, "Temps attente doit etre positif");
        assertTrue(result.getSatisfactionRate() >= 0 && result.getSatisfactionRate() <= 100,
                   "Taux satisfaction doit etre entre 0 et 100");
        assertTrue(result.getComputationTime() > 0, "Temps calcul doit etre positif");
    }

    @Test
    @DisplayName("Simulation avec iterations par defaut")
    public void testDefaultSimulation() {
        MonteCarloSimulator simulator = new MonteCarloSimulator();
        List<TransportEvent> events = createTestEvents(100);
        
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(events);
        
        assertNotNull(result);
        assertTrue(result.getComputationTime() > 0);
    }

    @Test
    @DisplayName("Resultats coherents avec donnees vides")
    public void testEmptyData() {
        MonteCarloSimulator simulator = new MonteCarloSimulator(10);
        List<TransportEvent> events = new ArrayList<>();
        
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(events);
        
        assertNotNull(result);
        // Devrait quand meme produire des resultats (meme si peu significatifs)
    }

    @Test
    @DisplayName("Temps calcul augmente avec iterations")
    public void testComputationTimeScaling() {
        List<TransportEvent> events = createTestEvents(50);
        
        MonteCarloSimulator sim1 = new MonteCarloSimulator(100);
        MonteCarloSimulator.MonteCarloResult result1 = sim1.simulate(events);
        
        MonteCarloSimulator sim2 = new MonteCarloSimulator(1000);
        MonteCarloSimulator.MonteCarloResult result2 = sim2.simulate(events);
        
        assertTrue(result2.getComputationTime() > result1.getComputationTime(),
                   "Plus iterations = plus de temps calcul");
    }

    @Test
    @DisplayName("Calcul intensif teste parallelisme")
    public void testIntensiveComputation() {
        MonteCarloSimulator simulator = new MonteCarloSimulator(50000);
        List<TransportEvent> events = createTestEvents(2000);
        
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(events);
        
        assertNotNull(result);
        assertTrue(result.getComputationTime() >= 0.3, "Calcul intensif doit prendre au moins 0.3 seconde");
    }

    private List<TransportEvent> createTestEvents(int count) {
        List<TransportEvent> events = new ArrayList<>();
        String[] lines = {"1", "2", "3", "5"};
        String[] arrets = {"Université", "Centre-ville", "CEGEP"};
        
        for (int i = 0; i < count; i++) {
            TransportEvent event = new TransportEvent();
            event.setType(i % 3 == 0 ? "embarquement" : (i % 3 == 1 ? "debarquement" : "trajet"));
            event.setLigne(lines[i % lines.length]);
            event.setBusId("BUS_" + String.format("%03d", i % 10 + 1));
            event.setArret(arrets[i % arrets.length]);
            event.setMontant(i % 3 == 0 ? 3.50 : 0.0);
            event.setMethode(i % 3 == 0 ? "carte" : "");
            event.setPassagerId("PASS_" + (10000 + i));
            event.setOrigine(arrets[i % arrets.length]);
            event.setDestination(arrets[(i + 1) % arrets.length]);
            event.setTimestamp("2025-01-15T10:00:00.000-05:00");
            events.add(event);
        }
        
        return events;
    }
}
