package com.rambabu.pumpcontroller;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

/**
 * Main screen of the Smart Farm Pump Controller app.
 *
 * Lets the farmer switch an irrigation pump ON/OFF and set a watering
 * schedule. Commands are sent to the Spring Boot backend over REST,
 * which forwards them to the ESP32 in the field via MQTT.
 */
public class MainActivity extends AppCompatActivity {

    // TODO: point this at your backend (use your machine's LAN IP on a real device)
    private static final String BASE_URL = "http://192.168.1.100:8080/api/pumps";
    private static final String PUMP_ID = "well-1";

    private TextView statusText;
    private EditText scheduleTimeInput;
    private EditText scheduleDurationInput;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        scheduleTimeInput = findViewById(R.id.scheduleTimeInput);
        scheduleDurationInput = findViewById(R.id.scheduleDurationInput);

        Button btnOn = findViewById(R.id.btnPumpOn);
        Button btnOff = findViewById(R.id.btnPumpOff);
        Button btnSchedule = findViewById(R.id.btnSchedule);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        btnOn.setOnClickListener(v -> sendCommand("on", null));
        btnOff.setOnClickListener(v -> sendCommand("off", null));
        btnRefresh.setOnClickListener(v -> fetchStatus());
        btnSchedule.setOnClickListener(v -> {
            String startTime = scheduleTimeInput.getText().toString().trim();
            String duration = scheduleDurationInput.getText().toString().trim();
            if (startTime.isEmpty() || duration.isEmpty()) {
                toast("Enter a start time (HH:mm) and duration (minutes)");
                return;
            }
            String body = "{\"startTime\":\"" + startTime
                    + "\",\"durationMinutes\":" + duration + "}";
            sendCommand("schedule", body);
        });

        fetchStatus();
    }

    /** Sends ON/OFF/schedule commands to the backend REST API. */
    private void sendCommand(String action, String jsonBody) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(BASE_URL + "/" + PUMP_ID + "/" + action);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(jsonBody != null);
                if (jsonBody != null) {
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonBody.getBytes());
                    }
                }
                int code = conn.getResponseCode();
                uiHandler.post(() -> {
                    toast(code == 200 ? "Command sent: " + action.toUpperCase()
                            : "Failed (HTTP " + code + ")");
                    fetchStatus();
                });
            } catch (Exception e) {
                uiHandler.post(() -> toast("Error: " + e.getMessage()));
            }
        });
    }

    /** Fetches the current pump status and shows it on screen. */
    private void fetchStatus() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(BASE_URL + "/" + PUMP_ID + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String body = new String(conn.getInputStream().readAllBytes());
                uiHandler.post(() -> statusText.setText("Pump status: " + body));
            } catch (Exception e) {
                uiHandler.post(() -> statusText.setText("Pump status: unknown"));
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
