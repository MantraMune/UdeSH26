package ca.usherbrooke.ift630.tp3.supervisor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ca.usherbrooke.ift630.tp3.compute.MonteCarloSimulator;
import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.MQTTClient;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;
import ca.usherbrooke.ift630.tp3.utils.PerformanceMonitor;

/**
 * Superviseur en mode processus.
 * Spawne N processus Java workers et communique via sockets locales.
 */
public class ProcessSupervisor implements Supervisor {
    private final int numWorkers;
    private final String configFile;
    private final int basePort;
    
    private MQTTClient mqttClient;
    private final BlockingQueue<TransportEvent> eventQueue;
    private final List<Process> workerProcesses;
    private final List<WorkerConnection> workerConnections;
    private final PerformanceMonitor perfMonitor;
    
    private ServerSocket serverSocket;
    private ExecutorService distributorService;
    private volatile boolean running = true;
    
    private static final String WORKER_CLASS = "ca.usherbrooke.ift630.tp3.worker.ProcessWorker";

    public ProcessSupervisor(int numWorkers, String configFile) {
        this.numWorkers = numWorkers;
        this.configFile = configFile;
        this.basePort = 9000;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.workerProcesses = new ArrayList<>();
        this.workerConnections = new ArrayList<>();
        this.perfMonitor = new PerformanceMonitor("processes", numWorkers);
    }

    @Override
    public void start() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" DÉMARRAGE MODE PROCESSUS");
        System.out.println("=".repeat(80));
        System.out.println("   Workers: " + numWorkers);
        System.out.println("   IPC:     Sockets TCP localhost");
        System.out.println("   Port:    " + basePort);
        System.out.println("=".repeat(80));
        
        perfMonitor.start();
        
        perfMonitor.startPhase("socket_server");
        System.out.println("\n Démarrage serveur socket...");
        serverSocket = new ServerSocket(basePort);
        System.out.println(" Serveur socket écoute sur port " + basePort);
        perfMonitor.stopPhase("socket_server");
        
        perfMonitor.startPhase("spawn_processes");
        System.out.println("\n Lancement des processus workers...");
        spawnWorkerProcesses();
        perfMonitor.stopPhase("spawn_processes");
        
        perfMonitor.startPhase("accept_connections");
        System.out.println("\n Attente des connexions workers...");
        acceptWorkerConnections();
        perfMonitor.stopPhase("accept_connections");
        
        perfMonitor.startPhase("mqtt_connection");
        System.out.println("\n Connexion au broker MQTT...");
        mqttClient = new MQTTClient(configFile, eventQueue);
        mqttClient.connect();
        perfMonitor.stopPhase("mqtt_connection");
        
        perfMonitor.startPhase("distribution_startup");
        System.out.println("\n Démarrage distribution événements...");
        startEventDistribution();
        perfMonitor.stopPhase("distribution_startup");
        
        System.out.println("\n Système processus prêt - réception en cours...\n");
    }

    /**
     * Spawne les processus workers
     */
    private void spawnWorkerProcesses() throws IOException {

        // Récupérer les chemins Java
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        // Boucle de N connexions (workers)
        for (int i = 0; i < numWorkers; i++) {
            // Construire la commande
            List<String> command = Arrays.asList(
                javaBin,
                "-cp", classpath,
                WORKER_CLASS,       // "ca.usherbrooke.ift630.tp3.worker.ProcessWorker"
                String.valueOf(i),  // workerId
                "localhost",        // host
                String.valueOf(basePort) // port
            );

            // Créer ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Rediriger stdout/stderr vers console

            // Lancer le processus
            Process process = pb.start();
            workerProcesses.add(process);

            // Afficher la confirmation
            System.out.println("   Processus worker #" + i + " lancé (PID: " + process.pid() + ")");
        }

        
        // Tests à passer: IPCTest (5 points)
        
        // Afficher le nombre de processus lancés selon numWorkers
        System.out.println(" " + workerProcesses.size() + " processus lancés");
    }

    /**
     * Accepte les connexions des workers
     */
    private void acceptWorkerConnections() throws IOException {

        // Configurer le timeout
        serverSocket.setSoTimeout(30000);

        // Boucle connexion-streams
        for (int i = 0; i < numWorkers; i++) {

            // Accepter la connexion
            Socket clientSocket = serverSocket.accept();

            // Créer streams
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true);
            
            // Lire le message de connexion
            String connectMsg = reader.readLine();

            // Parser workerId
            if (connectMsg != null && connectMsg.startsWith("WORKER_READY:")) {
                int workerId = Integer.parseInt(connectMsg.split(":")[1]);
                WorkerConnection conn = new WorkerConnection(
                    workerId, clientSocket, reader, writer);
                workerConnections.add(conn);
                System.out.println("   Worker #" + workerId + " connecté");
            }
        }
        
        // Tests à passer: IPCTest (5 points)
        
        System.out.println(" " + workerConnections.size() + " workers connectés");
    }

    private void startEventDistribution() {
        distributorService = Executors.newSingleThreadExecutor();
        
        distributorService.submit(() -> {
            int eventCounter = 0;
            
            while (running) {
                try {
                    TransportEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                    
                    if (event != null) {
                        int workerIndex = eventCounter % numWorkers;
                        WorkerConnection conn = workerConnections.get(workerIndex);
                        
                        conn.sendEvent(event);
                        
                        eventCounter++;
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println(" Erreur distribution: " + e.getMessage());
                }
            }
        });
        
        System.out.println(" Distribution événements démarrée");
    }

    @Override
    public void awaitCompletion(int durationSeconds) throws InterruptedException {
        System.out.println(" Traitement pendant " + durationSeconds + " secondes...\n");
        perfMonitor.startPhase("processing");
        Thread.sleep(durationSeconds * 1000L);
        perfMonitor.stopPhase("processing");
    }

    @Override
    public void shutdown() {
        System.out.println("\n Arrêt du système processus...");
        
        perfMonitor.startPhase("shutdown");
        running = false;
        
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        
        if (distributorService != null) {
            distributorService.shutdown();
        }
        
        System.out.println("   Envoi signal arrêt aux workers...");
        for (WorkerConnection conn : workerConnections) {
            conn.sendStop();
        }
        
        System.out.println("   Attente terminaison processus...");
        for (Process process : workerProcesses) {
            try {
                boolean exited = process.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
            }
        }
        
        for (WorkerConnection conn : workerConnections) {
            conn.close();
        }
        
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignorer
        }
        
        perfMonitor.stopPhase("shutdown");
        
        long totalEvents = workerConnections.stream()
            .mapToLong(WorkerConnection::getEventsProcessed)
            .sum();
        
        perfMonitor.stop(totalEvents);
        
        System.out.println(" Arrêt terminé");
    }

    @Override
    public Statistics getAggregatedStatistics() {
        Statistics aggregated = new Statistics(-1);
        
        for (WorkerConnection conn : workerConnections) {
            Statistics workerStats = new Statistics(conn.workerId);
            aggregated.merge(workerStats);
        }
        
        aggregated.markEnd();
        return aggregated;
    }

    @Override
    public MonteCarloSimulator.MonteCarloResult runMonteCarloSimulation(int iterations) {
        perfMonitor.startPhase("monte_carlo");
        
        List<TransportEvent> allEvents = new ArrayList<>();
        
        System.out.println("\n Simulation Monte Carlo (mode simplifié)...");
        
        MonteCarloSimulator simulator = new MonteCarloSimulator(iterations);
        MonteCarloSimulator.MonteCarloResult result = simulator.simulate(allEvents);
        
        perfMonitor.stopPhase("monte_carlo");
        
        return result;
    }

    @Override
    public void printReports() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" RAPPORTS FINAUX - MODE PROCESSUS");
        System.out.println("=".repeat(80));
        
        if (mqttClient != null) {
            mqttClient.printStats();
        }
        
        perfMonitor.printReport();
        
        System.out.println("\n Statistiques par worker:");
        for (WorkerConnection conn : workerConnections) {
            System.out.println("   Worker #" + conn.workerId + ": " + 
                conn.getEventsProcessed() + " événements");
        }
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return perfMonitor;
    }

    private static class WorkerConnection {
        final int workerId;
        final Socket socket;
        final BufferedReader reader;
        final PrintWriter writer;
        long eventsProcessed = 0;

        WorkerConnection(int workerId, Socket socket, BufferedReader reader, PrintWriter writer) {
            this.workerId = workerId;
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
        }

        void sendEvent(TransportEvent event) {
            writer.println("EVENT:" + event.toJson());
            eventsProcessed++;
        }

        void sendStop() {
            writer.println("STOP");
        }

        long getEventsProcessed() {
            return eventsProcessed;
        }

        void close() {
            try {
                writer.close();
                reader.close();
                socket.close();
            } catch (IOException e) {
                // Ignorer
            }
        }
    }
}