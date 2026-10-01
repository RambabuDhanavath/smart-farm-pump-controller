package com.rambabu.pumpcontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API for the Smart Farm Pump Controller.
 *
 * The Android app calls these endpoints; each command is forwarded to the
 * ESP32 in the field by publishing to an MQTT topic via {@link MqttService}.
 *
 *   POST /api/pumps/{id}/on        -> switch the pump ON
 *   POST /api/pumps/{id}/off       -> switch the pump OFF
 *   POST /api/pumps/{id}/schedule  -> schedule a timed run
 *   GET  /api/pumps/{id}/status    -> current pump status
 */
@RestController
@RequestMapping("/api/pumps")
public class PumpController {

    private final MqttService mqttService;

    // In-memory pump state (replace with a database for production)
    private final Map<String, String> pumpStatus = new ConcurrentHashMap<>();

    public PumpController(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostMapping("/{id}/on")
    public ResponseEntity<Map<String, String>> switchOn(@PathVariable String id) {
        mqttService.publishCommand(id, "ON");
        pumpStatus.put(id, "ON");
        return ResponseEntity.ok(Map.of("pumpId", id, "status", "ON"));
    }

    @PostMapping("/{id}/off")
    public ResponseEntity<Map<String, String>> switchOff(@PathVariable String id) {
        mqttService.publishCommand(id, "OFF");
        pumpStatus.put(id, "OFF");
        return ResponseEntity.ok(Map.of("pumpId", id, "status", "OFF"));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<Map<String, Object>> schedule(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        String startTime = String.valueOf(request.getOrDefault("startTime", ""));
        Object duration = request.getOrDefault("durationMinutes", 0);
        // Forward the schedule to the ESP32 as a compact command payload
        mqttService.publishCommand(id, "SCHEDULE:" + startTime + ":" + duration);
        pumpStatus.put(id, "SCHEDULED");
        return ResponseEntity.ok(Map.of(
                "pumpId", id,
                "scheduledStart", startTime,
                "durationMinutes", duration));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> status(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "pumpId", id,
                "status", pumpStatus.getOrDefault(id, "UNKNOWN")));
    }
}
