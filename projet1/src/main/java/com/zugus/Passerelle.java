package com.zugus;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.servlet.ServletHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Passerelle {

    private static final String BROKER_URL = "tcp://test.mosquitto.org:1883";
    private static final String CLIENT_ID = "JavaPasserellePublisher";
    private static final String TOPIC = "maison/temperature";
    private static IMqttClient client;
    private static List<String> receivedTemperatures = new ArrayList<>();  // Stocker les températures reçues

    public static void main(String[] args) throws Exception {
        initializeMqttClient();

        Server server = new Server(8080);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        
        // Créez des instances de ServletHolder pour chaque servlet
        ServletHolder dataReceiverHolder = new ServletHolder(new DataReceiverServlet());
        ServletHolder displayHolder = new ServletHolder(new DisplayServlet());

        // Ajoutez ces ServletHolders au ServletHandler en utilisant addServletWithMapping()
        handler.addServletWithMapping(dataReceiverHolder, "/data");
        handler.addServletWithMapping(displayHolder, "/display");

        server.start();
        
        // Restez à l'écoute des demandes HTTP
        server.join();
    }

    private static void initializeMqttClient() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            
            client.connect(options);
            System.out.println("Connecté au broker MQTT");
            
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    