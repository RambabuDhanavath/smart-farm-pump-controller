# Smart Farm Pump Controller 🚜💧

> ⚠️ **Personal project — work in progress.** An IoT starter application; hardware integration is under active development.

## Problem

In many farming regions, irrigation water pumps are located at wells far from the farmer's home. To switch a pump ON or OFF, farmers must walk to the well — often at night, in the rain, or across muddy fields. This is inconvenient, unsafe, and wastes time and water when pumps run longer than needed.

## Solution

**Smart Farm Pump Controller** lets farmers switch their irrigation water-pump motors **ON/OFF from home using a mobile app**, with **scheduling/timers** so pumps run exactly when needed.

### Architecture

```
┌──────────────┐      REST       ┌──────────────────┐      MQTT       ┌─────────────────┐      GPIO      ┌─────────┐
│  Android App │ ──────────────▶ │  Spring Boot     │ ──────────────▶ │  MQTT Broker    │ ────────────▶ │  ESP32  │ ───▶ │ Relay   │ ───▶ Pump
│ (farmer UI)  │                 │  Backend         │  publish        │  (Mosquitto)    │  subscribe     │ + Relay │      │ Motor   │
└──────────────┘                 └──────────────────┘               └─────────────────┘                └─────────┘
       │                                  │                                 ▲
       │  pump status (polling/MQTT)       │  schedules & state              │
       └──────────────────────────────────┘                                 │
                                      command topic: farm/pump/{pumpId}/command
                                      status topic:  farm/pump/{pumpId}/status
```

## ✨ Features

- 🔌 **Remote ON/OFF** — switch any registered pump from the mobile app
- ⏰ **Scheduling / timers** — auto-start and auto-stop at set times (e.g., run 6:00–8:00 AM daily)
- 📊 **Pump status** — see whether each pump is currently ON or OFF
- 🚜 **Multiple pumps** — manage several pumps/wells from one app
- 📶 **Works over the internet** — MQTT broker bridges the phone and the field

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Mobile app | Android (Java) |
| Backend | Spring Boot, REST APIs, Eclipse Paho MQTT client |
| Messaging | MQTT (Eclipse Mosquitto broker) |
| Firmware | ESP32 (Arduino), PubSubClient |
| Infra | Docker & Docker Compose |

## 🧰 Hardware List

- **ESP32** development board (Wi-Fi + Bluetooth)
- **Relay module** (5V, rated for the pump motor's voltage/current — use a contactor for high-power motors)
- Water pump motor + power supply
- Jumper wires, breadboard / enclosure
- Stable Wi-Fi coverage at the pump site

> ⚡ **Safety:** mains-voltage wiring must be done by a qualified electrician. Never work on live circuits.

## 📁 Project Structure

```
smart-farm-pump-controller/
├── android/                 # Android mobile app (Java)
│   └── app/src/main/java/com/rambabu/pumpcontroller/
│       └── MainActivity.java
├── backend/                 # Spring Boot REST + MQTT service
│   ├── src/main/java/com/rambabu/pumpcontroller/
│   │   ├── PumpController.java   # REST endpoints
│   │   └── MqttService.java      # Publishes commands to MQTT
│   └── Dockerfile
├── firmware/                # ESP32 firmware
│   └── pump_controller/
│       └── pump_controller.ino   # Subscribes to MQTT, toggles relay
├── docker-compose.yml       # Mosquitto broker + backend
└── README.md
```

## 🚀 Getting Started

### 1. Start the MQTT broker and backend

```bash
docker-compose up -d
```

- Mosquitto broker → `mqtt://localhost:1883`
- Backend API → `http://localhost:8080/api/pumps`

### 2. Flash the ESP32

1. Install the [Arduino IDE](https://www.arduino.cc/en/software) and the ESP32 board package.
2. Install the **PubSubClient** library.
3. Open `firmware/pump_controller/pump_controller.ino`.
4. Set `WIFI_SSID`, `WIFI_PASSWORD`, `MQTT_SERVER`, and `PUMP_ID`.
5. Wire the relay module to GPIO 26 (see sketch comments).
6. Upload to the ESP32.

### 3. Run the Android app

1. Open the `android/` project in Android Studio.
2. Set the backend base URL in `MainActivity.java` to your machine's IP.
3. Build and run on a device/emulator.
4. Tap **Pump ON / Pump OFF**, or set a schedule time and tap **Schedule**.

### REST API quick reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/pumps/{id}/on` | Switch pump ON |
| `POST` | `/api/pumps/{id}/off` | Switch pump OFF |
| `POST` | `/api/pumps/{id}/schedule` | Schedule a run (`{"startTime":"06:00","durationMinutes":120}`) |
| `GET` | `/api/pumps/{id}/status` | Get pump status |

## 🗺️ Roadmap

- [ ] Pump runtime history and water-usage estimates
- [ ] Soil-moisture sensor integration for auto-irrigation
- [ ] Push notifications on pump state changes
- [ ] User authentication and multi-farm support
- [ ] Offline queueing of commands

## 📄 License

MIT
