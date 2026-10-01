package com.rambabu.pumpcontroller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publishes pump commands to the MQTT broker.
 *
 * The ESP32 at each well subscribes to {@code farm/pump/{pumpId}/command}
 * and toggles its relay when a command arrives. Requires the Eclipse Paho
 * MQTT client dependency (org.eclipse.paho:org.eclipse.paho.client.mqttv3).
 */
@Service
public class MqttService {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    @Value("${mqtt.broker-url:tcp://mosquitto:1883}")
    private String brokerUrl;

    private MqttClient client;

    @PostConstruct
    public void connect() {
        try {
            client = new MqttClient(brokerUrl,
                    "pump-backend-" + System.currentTimeMillis(),
                    new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client.connect(options);
            log.info("Connected to MQTT broker at {}", brokerUrl);
        } catch (Exception e) {
            log.error("Could not connect to MQTT broker at {}", brokerUrl, e);
        }
    }

    /**
     * Publishes a command (ON / OFF / SCHEDULE:...) to the pump's topic.
     *
     * @param pumpId  identifier of the pump, e.g. "well-1"
     * @param command command payload
     */
    public void publishCommand(String pumpId, String command) {
        String topic = "farm/pump/" + pumpId + "/command";
        try {
            MqttMessage message = new MqttMessage(command.getBytes());
            message.setQos(1);
            message.setRetained(false);
            client.publish(topic, message);
            log.info("Published '{}' to {}", command, topic);
        } catch (Exception e) {
            log.error("Failed to publish '{}' to {}", command, topic, e);
            throw new IllegalStateException("MQTT publish failed", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception e) {
            log.warn("Error while disconnecting MQTT client", e);
        }
    }
}
