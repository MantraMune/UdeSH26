package ca.usherbrooke.ift630.tp3.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ca.usherbrooke.ift630.tp3.compute.Statistics;
import ca.usherbrooke.ift630.tp3.mqtt.TransportEvent;

/**
 * Worker en mode processus - communique via socket avec le superviseur.
 * Ce programme tourne dans un processus Java séparé.
 * 
 * Usage: java ProcessWorker <workerId> <supervisorHost> <supervisorPort>
 */
public class ProcessWorker implements TransportWorker {
    private final int workerId;
    private final String supervisorHost;
    private final int supervisorPort;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    private final Statistics statistics;
    private final List<TransportEvent> processedEvents;
    private volatile boolean running = true;

    public ProcessWorker(int workerId, String supervisorHost, int supervisorPort) {
        this.workerId = workerId;
        this.supervisorHost = supervisorHost;
        this.supervisorPort = supervisorPort;
        this.statistics = new Statistics(workerId);
        this.processedEvents = new ArrayList<>();
    }

    @Override
    public void start() {
        try {
            System.out.println(" Worker #" + workerId + " (processus) connexion à " + 
                supervisorHost + ":" + supervisorPort);
            
            socket = new Socket(supervisorHost, supervisorPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            writer.println("WORKER_READY:" + workerId);
            
            System.out.println(" Worker #" + workerId + " (processus) connecté");
            
            run();
            
        } catch (IOException e) {
            System.err.println(" Erreur connexion worker #" + workerId + ": " + e.getMessage());
        }
    }

    /**
     * Boucle principale de traitement
     */
    private void run() {

        while (running) {
            try {
                String line = reader.readLine();

                if (line == null) {
                    System.out.println("Connexion fermée.");
                    break;
                }

                if (line.equals("STOP")) {
                    running = false;
                    break;
                }

                if(line.startsWith("EVENT:")) {
                    String eventJson = line.substring(6);
                    TransportEvent event = TransportEvent.fromJson(eventJson);

                    if (event != null) {
                        processEvent(event, statistics);
                        processedEvents.add(event);
                        writer.println("ACK");
                    }
                }

            } catch (IOException e) {
                System.out.println("Erreur donne par IOException:" + e.getMessage());
                break;
            }
        }
        
        // Tests à passer: ProcessWorkerTest + IPCTest (15 points)
        
        statistics.markEnd();
        sendStatistics();
        cleanup();
        
        System.out.println(" Worker #" + workerId + " (processus) arrêté - " + 
            statistics.getTotalEvents() + " événements traités");
    }

    private void sendStatistics() {
        try {
            writer.println("STATS:" + serializeStatistics());
        } catch (Exception e) {
            System.err.println(" Erreur envoi statistiques: " + e.getMessage());
        }
    }

    private String serializeStatistics() {
        return String.format("%d,%d,%.2f,%d,%.2f",
            statistics.getWorkerId(),
            statistics.getTotalEvents(),
            statistics.getTotalRevenue(),
            statistics.getTotalPassengers(),
            statistics.getTotalKilometers()
        );
    }

    private void cleanup() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignorer
        }
    }

    @Override
    public void shutdown() {
        running = false;
    }

    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public boolean isRunning() {
        return running && socket != null && socket.isConnected();
    }

    @Override
    public List<TransportEvent> getProcessedEvents() {
        return new ArrayList<>(processedEvents);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java ProcessWorker <workerId> <host> <port>");
            System.exit(1);
        }
        
        try {
            int workerId = Integer.parseInt(args[0]);
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            
            ProcessWorker worker = new ProcessWorker(workerId, host, port);
            worker.start();
            
        } catch (NumberFormatException e) {
            System.err.println(" Arguments invalides: " + e.getMessage());
            System.exit(1);
        }
    }
}