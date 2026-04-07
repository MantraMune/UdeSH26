package ca.usherbrooke.ift630.tp3.test;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * Systeme d'evaluation automatique du TP3.
 * Calcule la note sur 100 points selon le bareme defini.
 */
public class EvaluationRunner {
    
    // Bareme d'evaluation (total 100 points)
    private static final Map<String, Integer> BAREME = new LinkedHashMap<>();
    
    static {
        // Tests fonctionnels de base (30 points)
        BAREME.put("TransportEventTest", 5);
        BAREME.put("ThreadWorkerTest", 10);
        BAREME.put("ProcessWorkerTest", 10);  // Sera cree
        BAREME.put("IPCTest", 5);             // Sera cree
        
        // Calculs corrects (30 points)
        BAREME.put("StatisticsTest", 10);
        BAREME.put("MonteCarloTest", 10);
        BAREME.put("ScenarioAnalysisTest", 10);

        // Fusion et agregation (10 points)
        BAREME.put("MergeTest", 10);
        
        // Performance et parallelisme (20 points)
        BAREME.put("PerformanceMonitorTest", 10);
        BAREME.put("ScalabilityTest", 10);    // Sera cree
        
        // Qualite du code (10 points)
        BAREME.put("CodeQualityTest", 10);    // Sera cree
    }

    private int totalTests = 0;
    private int passedTests = 0;
    private Map<String, TestClassResult> resultsByClass = new HashMap<>();

    public static void main(String[] args) {
        EvaluationRunner runner = new EvaluationRunner();
        runner.runEvaluation();
    }

    public void runEvaluation() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EVALUATION AUTOMATIQUE TP3 - IFT630");
        System.out.println("=".repeat(80));
        System.out.println();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectPackage("ca.usherbrooke.ift630.tp3.test"))
            .build();

        Launcher launcher = LauncherFactory.create();
        TestExecutionListener listener = new TestExecutionListener() {
            private String currentClass = "";
            
            @Override
            public void executionStarted(TestIdentifier testIdentifier) {
                if (testIdentifier.isContainer()) {
                    String className = extractClassName(testIdentifier);
                    resultsByClass.putIfAbsent(className, new TestClassResult(className));
                }
                if (testIdentifier.isTest()) {
                    String className = extractClassName(testIdentifier);
                    if (!className.equals(currentClass)) {
                        currentClass = className;
                        resultsByClass.putIfAbsent(className, new TestClassResult(className));
                    }
                }
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                if (testIdentifier.isTest()) {
                    totalTests++;
                    String className = extractClassName(testIdentifier);
                    TestClassResult classResult = resultsByClass.get(className);
                    
                    if (testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                        passedTests++;
                        classResult.passed++;
                    } else {
                        classResult.failed++;
                        if (testExecutionResult.getThrowable().isPresent()) {
                            classResult.errors.add(testExecutionResult.getThrowable().get().getMessage());
                        }
                    }
                    classResult.total++;
                }
            }
        };

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        printResults();
    }

    private String extractClassName(TestIdentifier testIdentifier) {
        String sourceStr = testIdentifier.getSource()
            .map(source -> source.toString())
            .orElse("");
        
        int classNameStart = sourceStr.indexOf("className");
        if (classNameStart != -1) {
            int quoteStart = sourceStr.indexOf("'", classNameStart);
            if (quoteStart != -1) {
                quoteStart++;
                int quoteEnd = sourceStr.indexOf("'", quoteStart);
                if (quoteEnd != -1) {
                    String fullClassName = sourceStr.substring(quoteStart, quoteEnd);
                    int lastDot = fullClassName.lastIndexOf('.');
                    return lastDot != -1 ? fullClassName.substring(lastDot + 1) : fullClassName;
                }
            }
        }
        return "Unknown";
    }

    private void printResults() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("                 RESULTATS DES TESTS UNITAIRES                 ");
        System.out.println("=".repeat(80));
        System.out.println();

        int totalPoints = 0;
        int maxPoints = 0;

        for (Map.Entry<String, Integer> entry : BAREME.entrySet()) {
            String className = entry.getKey();
            int classPoints = entry.getValue();
            TestClassResult result = resultsByClass.get(className);
            
            if (result == null) {
                result = new TestClassResult(className);
            }
            
            maxPoints += classPoints;

            double successRate = result.total > 0 ? (double) result.passed / result.total : 0.0;
            int earnedPoints = (int) Math.round(classPoints * successRate);
            totalPoints += earnedPoints;

            System.out.printf("%-30s : %2d/%2d tests reussis (%3.0f%%) -> %2d/%2d points%n",
                className,
                result.passed,
                result.total,
                successRate * 100,
                earnedPoints,
                classPoints);
        }
        
        double finalGrade = maxPoints > 0 ? (totalPoints * 100.0 / maxPoints) : 0;
        System.out.println("=".repeat(80));
        System.out.printf("   NOTE FINALE                                              : %.1f / 100%n", finalGrade);
        System.out.println("=".repeat(80));
        
        printGradeInterpretation(finalGrade);


        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("RESUME");
        System.out.println("=".repeat(80));
        System.out.printf("Tests totaux        : %d%n", totalTests);
        System.out.printf("Tests reussis       : %d%n", passedTests);
        System.out.printf("Tests echoues       : %d%n", totalTests - passedTests);
        System.out.printf("Taux de reussite    : %.1f%%%n", totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0);
        System.out.println();
        System.out.printf("Points obtenus      : %d%n", totalPoints);
        System.out.printf("Points maximum      : %d%n", maxPoints);
        System.out.println();
    }

    private void printGradeInterpretation(double grade) {
        System.out.println();
        System.out.println("INTERPRETATION:");
        
        if (grade >= 90) {
            System.out.println("Excellent! Tous les objectifs sont atteints.");
        } else if (grade >= 80) {
            System.out.println("Tres bon travail! Quelques ameliorations mineures possibles.");
        } else if (grade >= 70) {
            System.out.println("Bon travail! Verifier les tests echoues.");
        } else if (grade >= 60) {
            System.out.println("Travail acceptable mais incomplet. Revoir les concepts de base.");
        } else {
            System.out.println("Travail insuffisant. Consulter l'enseignant pour aide.");
        }
        
        System.out.println();
    }

    private static class TestClassResult {
        String className;
        int total = 0;
        int passed = 0;
        int failed = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        TestClassResult(String className) {
            this.className = className;
        }
    }
}
