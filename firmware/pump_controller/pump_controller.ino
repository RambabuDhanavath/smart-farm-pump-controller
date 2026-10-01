/*
 * Smart Farm Pump Controller — ESP32 firmware
 *
 * Subscribes to the MQTT command topic for one pump and toggles a relay
 * module that switches the irrigation water-pump motor.
 *
 * Wiring (example):
 *   ESP32 GPIO 26  -> relay module IN pin
 *   ESP32 5V/GND   -> relay module VCC/GND
 *   Relay COM/NO   -> in series with the pump motor supply
 *
 * Libraries required (Arduino IDE Library Manager):
 *   - PubSubClient by Nick O'Leary
 *
 * SAFETY: mains-voltage wiring must be done by a qualified electrician.
 */

#include <WiFi.h>
#include <PubSubClient.h>

// ---------- User configuration ----------
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
const char* MQTT_SERVER   = "192.168.1.100"; // Mosquitto broker IP
const int   MQTT_PORT     = 1883;
const char* PUMP_ID       = "well-1";         // must match the app/backend
// ----------------------------------------

const int RELAY_PIN = 26; // GPIO driving the relay module

WiFiClient espClient;
PubSubClient mqtt(espClient);

String commandTopic;
String statusTopic;
bool pumpOn = false;

void setPump(bool on) {
  pumpOn = on;
  digitalWrite(RELAY_PIN, on ? HIGH : LOW);
  // Publish retained status so late subscribers see the current state
  mqtt.publish(statusTopic.c_str(), on ? "ON" : "OFF", true);
  Serial.println(on ? "Pump switched ON" : "Pump switched OFF");
}

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String msg;
  for (unsigned int i = 0; i < length; i++) msg += (char)payload[i];
  msg.trim();
  Serial.print("Command received: ");
  Serial.println(msg);

  if (msg == "ON") {
    setPump(true);
  } else if (msg == "OFF") {
    setPump(false);
  } else if (msg.startsWith("SCHEDULE:")) {
    // Format: SCHEDULE:HH:mm:durationMinutes — scheduling handled on-device
    // (simple version: parse and act; a full version would use NTP time)
    Serial.println("Schedule command received (see README for details)");
    setPump(true); // placeholder: start the scheduled run
  }
}

void reconnectMqtt() {
  while (!mqtt.connected()) {
    Serial.print("Connecting to MQTT...");
    String clientId = "esp32-pump-" + String(PUMP_ID) + "-" + String(random(0xffff), HEX);
    if (mqtt.connect(clientId.c_str())) {
      Serial.println("connected");
      mqtt.subscribe(commandTopic.c_str());
      mqtt.publish(statusTopic.c_str(), pumpOn ? "ON" : "OFF", true);
    } else {
      Serial.print("failed, rc=");
      Serial.println(mqtt.state());
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW); // pump OFF at boot

  commandTopic = String("farm/pump/") + PUMP_ID + "/command";
  statusTopic  = String("farm/pump/") + PUMP_ID + "/status";

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  mqtt.setServer(MQTT_SERVER, MQTT_PORT);
  mqtt.setCallback(onMqttMessage);
}

void loop() {
  if (!mqtt.connected()) reconnectMqtt();
  mqtt.loop();
}
