package ca.usherbrooke.ift630.tp3.mqtt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client MQTT pour recevoir les événements de transport.
 * Ce code est FOURNI aux étudiants.
 * 
 * Le client se connecte au broker MQTT configuré dans config.json
 * et distribue les événements reçus aux workers.
 */
public class MQTTClient {
    private final String configFile;
    private final BlockingQueue<TransportEvent> eventQueue;
    private MqttClient mqttClient;
    private JsonObject config;
    
    // Statistiques de réception
    private final AtomicLong totalReceived = new AtomicLong(0);
    private final AtomicLong realtimeReceived = new AtomicLong(0);
    private final AtomicLong batchReceived = new AtomicLong(0);
    private final AtomicLong intervalReceived = new AtomicLong(0);

    /**
     * Constructeur
     * @param configFile Chemin vers config.json
     * @param eventQueue Queue pour distribuer les événements aux workers
     */
    public MQTTClient(String configFile, BlockingQueue<TransportEvent> eventQueue) {
        this.configFile = configFile;
        this.eventQueue = eventQueue;
    }

    /**
     * Charge la configuration depuis config.json
     */
    private void loadConfig() throws IOException {
        try (FileReader reader = new FileReader(configFile)) {
            config = new Gson().fromJson(reader, JsonObject.class);
        }
    }

    /**
     * Se connecte au broker MQTT et s'abonne aux topics
     */
    public void connect() throws MqttException, IOException {
        loadConfig();
        
        JsonObject brokerConfig = config.getAsJsonObject("broker");
        String host = brokerConfig.get("host").getAsString();
        int port = brokerConfig.get("port").getAsInt();
        String username = brokerConfig.get("username").getAsString();
        String password = brokerConfig.get("password").getAsString();
        
        String brokerUrl = "tcp://" + host + ":" + port;
        String clientId = "ift630-tp3-" + System.currentTimeMillis();
        
        System.out.println(" Connexion au broker MQTT...");
        System.out.println("   Broker: " + brokerUrl);
        System.out.println("   Client ID: " + clientId);
        
        mqttClient = new MqttClient(brokerUrl, clientId);
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        
        mqttClient.connect(options);
        
        // Callback pour les messages reçus
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("  Connexion MQTT perdue: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Non utilisé (on ne publie pas)
            }
        });
        
        // S'abonner aux 3 topics
        JsonObject topics = config.getAsJsonObject("topics");
        String realtimeTopic = topics.get("realtime").getAsString();
        String batchTopic = topics.get("batch").getAsString();
        String intervalTopic = topics.get("interval").getAsString();
        
        mqttClient.subscribe(realtimeTopic, 1);
        mqttClient.subscribe(batchTopic, 2);
        mqttClient.subscribe(intervalTopic, 1);
        
        System.out.println(" Connecté et abonné aux topics:");
        System.out.println("   " + realtimeTopic);
        System.out.println("   " + batchTopic);
        System.out.println("   " + intervalTopic);
    }

    /**
     * Traite un message MQTT reçu
     */
    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            TransportEvent event = TransportEvent.fromJson(payload);
            
            if (event != null) {
                // Mettre dans la queue pour les workers
                eventQueue.offer(event);
                
                // Statistiques
                totalReceived.incrementAndGet();
                if (topic.contains("realtime")) {
                    realtimeReceived.incrementAndGet();
                } else if (topic.contains("batch")) {
                    batchReceived.incrementAndGet();
                } else if (topic.contains("interval")) {
                    intervalReceived.incrementAndGet();
                }
            }
        } catch (Exception e) {
            System.err.println(" Erreur traitement message: " + e.getMessage());
        }
    }

    /**
     * Déconnexion du broker
     */
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println(" Déconnecté du broker MQTT");
            }
        } catch (MqttException e) {
            System.err.println(" Erreur déconnexion: " + e.getMessage());
        }
    }

    /**
     * Affiche les statistiques de réception
     */
    public void printStats() {
        System.out.println("\n Statistiques MQTT:");
        System.out.println("   Total reçu:    " + totalReceived.get());
        System.out.println("   Realtime:      " + realtimeReceived.get());
        System.out.println("   Batch:         " + batchReceived.get());
        System.out.println("   Interval:      " + intervalReceived.get());
    }

    public long getTotalReceived() {
        return totalReceived.get();
    }
}
