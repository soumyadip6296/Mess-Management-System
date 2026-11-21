package com.messmgmt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

/**
 * REST Client for Google Firebase Realtime Database using Java 11+ HttpClient and Gson.
 */
public class FirebaseClient {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.toString());
                    }
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    String val = in.nextString();
                    if (val == null || val.isEmpty() || "null".equalsIgnoreCase(val)) {
                        return null;
                    }
                    return LocalDate.parse(val);
                }
            })
            .setPrettyPrinting()
            .create();

    private final HttpClient httpClient;

    public FirebaseClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                .build();
    }

    public static Gson getGson() {
        return GSON;
    }

    private String buildUrl(String path) {
        String base = FirebaseConfig.getDbUrl();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String cleanPath = path.endsWith(".json") ? path : path + ".json";
        String url = base + cleanPath;

        String token = FirebaseConfig.getAuthToken();
        if (token != null && !token.trim().isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "auth=" + URLEncoder.encode(token.trim(), StandardCharsets.UTF_8);
        }
        return url;
    }

    public String get(String path) throws IOException, InterruptedException {
        String url = buildUrl(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            String body = response.body();
            if (body == null || body.trim().isEmpty() || "null".equals(body.trim())) {
                return null;
            }
            return body;
        } else {
            throw new IOException("HTTP " + code + ": " + response.body());
        }
    }

    public String put(String path, String jsonBody) throws IOException, InterruptedException {
        String url = buildUrl(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            return response.body();
        } else {
            throw new IOException("HTTP " + code + ": " + response.body());
        }
    }

    public String patch(String path, String jsonBody) throws IOException, InterruptedException {
        String url = buildUrl(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            return response.body();
        } else {
            throw new IOException("HTTP " + code + ": " + response.body());
        }
    }

    public boolean delete(String path) throws IOException, InterruptedException {
        String url = buildUrl(path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                .header("Accept", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public String testConnectionWithMessage() {
        String base = FirebaseConfig.getDbUrl();
        if (base == null || base.trim().isEmpty() || !base.contains("http")) {
            return "ERROR: Firebase Database URL is not configured.";
        }

        try {
            long start = System.currentTimeMillis();
            String url = base + ".json?shallow=true";
            String token = FirebaseConfig.getAuthToken();
            if (token != null && !token.trim().isEmpty()) {
                url += "&auth=" + URLEncoder.encode(token.trim(), StandardCharsets.UTF_8);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(FirebaseConfig.getTimeoutMs()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long latency = System.currentTimeMillis() - start;

            int code = response.statusCode();
            if (code == 200) {
                return "SUCCESS: Connected to Firebase Realtime Database (" + latency + "ms)\n" + base;
            } else if (code == 401 || code == 403) {
                return "CONNECTED (" + code + " Access Restricted): Firebase host reachable (" + latency + "ms), but Database Security Rules require authentication.\nAdd your Firebase Secret/Token in settings or update Firebase Rules.";
            } else {
                return "HTTP " + code + ": " + response.body();
            }
        } catch (Exception e) {
            return "OFFLINE / UNREACHABLE: " + e.getMessage() + "\n(System is safely operating in local offline cache mode)";
        }
    }

    public boolean isOnline() {
        try {
            String test = testConnectionWithMessage();
            return test.startsWith("SUCCESS") || test.startsWith("CONNECTED");
        } catch (Exception e) {
            return false;
        }
    }
}
