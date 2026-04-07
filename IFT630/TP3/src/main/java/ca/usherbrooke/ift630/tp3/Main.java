package ca.usherbrooke.ift630.tp3;

import ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator;
import ca.usherbrooke.ift630.tp3.supervisor.ProcessSupervisor;
import ca.usherbrooke.ift630.tp3.supervisor.Supervisor;
import ca.usherbrooke.ift630.tp3.supervisor.ThreadSupervisor;

/**
 * Point d'entrée principal de l'application.
 * Parse les arguments, crée le superviseur (threads ou processus),
 * et gère le cycle de vie de l'application.
 * Supporte une exécution en mode threads ou processus, avec configuration
 * du nombre de workers, durée d'exécution, et options Monte Carlo.
 * 
 * @author VotreNom - CIP
 */
public class Main {
    private static final String DEFAULT_MODE = "threads";
    private static final int DEFAULT_WORKERS = 4;
    private static final int DEFAULT_DURATION = 30;
    private static final String DEFAULT_CONFIG = "config.json";
    private static final int DEFAULT_MONTE_CARLO = 10000;

    public static void main(String[] args) {
        try {
            // Parser les arguments
            Config config = parseArguments(args);
            
            // Afficher le banner
            printBanner(config);
            
            // Créer le superviseur approprié
            Supervisor supervisor = createSupervisor(config);
            
            // Configurer l'arrêt propre
            setupShutdownHook(supervisor);
            
            // Démarrer le système
            supervisor.start();
            
            // Attendre la durée spécifiée
            supervisor.awaitCompletion(config.duration);
            
            // Arrêter le système
            supervisor.shutdown();
            
            // Exécuter Monte Carlo si demandé
            if (config.monteCarloIterations > 0) {
                MonteCarloSimulator.MonteCarloResult mcResult = 
                    supervisor.runMonteCarloSimulation(config.monteCarloIterations);
                mcResult.printReport();
            }
            
            // Afficher les rapports
            supervisor.printReports();
            
            System.out.println("\nExécution terminée avec succès!");
            
        } catch (Exception e) {
            System.err.println("\nErreur fatale: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Parse les arguments de ligne de commande
     */
    private static Config parseArguments(String[] args) {
        String mode = DEFAULT_MODE;
        int workers = DEFAULT_WORKERS;
        int duration = DEFAULT_DURATION;
        String configFile = DEFAULT_CONFIG;
        int monteCarloIterations = DEFAULT_MONTE_CARLO;

        for (String arg : args) {
            if (arg.startsWith("--mode=")) {
                mode = arg.substring(7);
                if (!mode.equals("threads") && !mode.equals("processes")) {
                    throw new IllegalArgumentException(
                        "Mode invalide: " + mode + " (attendu: threads ou processes)");
                }
            } else if (arg.startsWith("--workers=")) {
                workers = Integer.parseInt(arg.substring(10));
                if (workers < 1 || workers > 32) {
                    throw new IllegalArgumentException(
                        "Nombre de workers invalide: " + workers + " (1-32)");
                }
            } else if (arg.startsWith("--duration=")) {
                duration = Integer.parseInt(arg.substring(11));
                if (duration < 1) {
                    throw new IllegalArgumentException(
                        "Durée invalide: " + duration + " (minimum 1)");
                }
            } else if (arg.startsWith("--config=")) {
                configFile = arg.substring(9);
            } else if (arg.startsWith("--monte-carlo=")) {
                monteCarloIterations = Integer.parseInt(arg.substring(14));
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            } else {
                System.err.println("Argument inconnu ignoré: " + arg);
            }
        }

        return new Config(mode, workers, duration, configFile, monteCarloIterations);
    }

    /**
     * Crée le superviseur approprié selon le mode
     */
    private static Supervisor createSupervisor(Config config) {
        return switch (config.mode) {
            case "threads" -> new ThreadSupervisor(config.workers, config.configFile);
            case "processes" -> new ProcessSupervisor(config.workers, config.configFile);
            default -> throw new IllegalStateException("Mode inconnu: " + config.mode);
        };
    }

    /**
     * Configure l'arrêt propre en cas de Ctrl+C
     */
    private static void setupShutdownHook(Supervisor supervisor) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nSignal d'interruption reçu...");
            supervisor.shutdown();
        }));
    }

    /**
     * Affiche le banner de démarrage
     */
    private static void printBanner(Config config) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("IFT630 - TP3 - SYSTÈME DE TRANSPORT DISTRIBUÉ");
        System.out.println("=".repeat(80));
        System.out.println("   Mode:             " + config.mode.toUpperCase());
        System.out.println("   Workers:          " + config.workers);
        System.out.println("   Durée:            " + config.duration + " secondes");
        System.out.println("   Config:           " + config.configFile);
        if (config.monteCarloIterations > 0) {
            System.out.println("   Monte Carlo:      " + config.monteCarloIterations + " itérations");
        }
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Affiche l'usage du programme
     */
    private static void printUsage() {
        System.out.println("\nUsage: java -jar tp3.jar [options]\n");
        System.out.println("Options:");
        System.out.println("  --mode=<mode>          Mode d'exécution: threads ou processes");
        System.out.println("                         (défaut: threads)");
        System.out.println("  --workers=<n>          Nombre de workers (1-32)");
        System.out.println("                         (défaut: 4)");
        System.out.println("  --duration=<s>         Durée d'exécution en secondes");
        System.out.println("                         (défaut: 30)");
        System.out.println("  --config=<file>        Fichier de configuration MQTT");
        System.out.println("                         (défaut: config.json)");
        System.out.println("  --monte-carlo=<n>      Itérations Monte Carlo (0 = désactivé)");
        System.out.println("                         (défaut: 10000)");
        System.out.println("  --help, -h             Affiche cette aide\n");
        System.out.println("Exemples:");
        System.out.println("  java -jar tp3.jar --mode=threads --workers=4 --duration=30");
        System.out.println("  java -jar tp3.jar --mode=processes --workers=8 --duration=60");
        System.out.println("  java -jar tp3.jar --mode=threads --workers=2 --monte-carlo=5000\n");
    }

    /**
     * Configuration de l'exécution
     */
    private static record Config(
        String mode,
        int workers,
        int duration,
        String configFile,
        int monteCarloIterations
    ) {}
}
