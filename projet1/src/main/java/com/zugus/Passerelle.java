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

    public static class DataReceiverServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String temperature = request.getParameter("temperature");
            receivedTemperatures.add(temperature);
            response.sendRedirect("/display");
        }
    }

    public static class DisplayServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Temperatures recues:</h1>");
            response.getWriter().println("<ul>");
            for (String temp : receivedTemperatures) {
                response.getWriter().println("<li>" + temp + "</li>");
            }
            response.getWriter().println("</ul>");
            response.getWriter().println("<form method='post' action='/display'>");
            response.getWriter().println("<input type='submit' value='Envoyer la derniere temperature a MQTT'/>");
            response.getWriter().println("</form>");
            response.getWriter().println("</body></html>");
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (!receivedTemperatures.isEmpty()) {
                String lastTemperature = receivedTemperatures.get(receivedTemperatures.size() - 1);
                publishToMqtt(lastTemperature);
            }
            response.sendRedirect("/display");
        }
    }

    public static void publishToMqtt(String temperature) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(temperature.getBytes());
            
            client.publish(TOPIC, message);
            System.out.println("Message publié !");
            
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
