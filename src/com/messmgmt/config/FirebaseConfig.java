package com.messmgmt.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages Firebase configuration properties and provides runtime settings update.
 */
public class FirebaseConfig {

    private static String dbUrl ="https://mess-management-system-f3083-default-rtdb.firebaseio.com/";
    private static String authToken = "";
    private static int timeoutMs = 4000;
    private static double defaultMealRate = 50.00;

    private static boolean initialized = false;

    static {
        loadProperties();
    }

    public static synchronized void loadProperties() {
        if (initialized) return;

        Properties props = new Properties();
        boolean loaded = false;

        // Try 1: ClassLoader resource
        try (InputStream in = FirebaseConfig.class.getClassLoader().getResourceAsStream("firebase.properties")) {
            if (in != null) {
                props.load(in);
                loaded = true;
            }
        } catch (Exception ignored) {}

        // Try 2: Relative file path
        if (!loaded) {
            File propFile = new File("resources/firebase.properties");
            if (!propFile.exists()) {
                propFile = new File("../resources/firebase.properties");
            }
            if (propFile.exists()) {
                try (FileInputStream fis = new FileInputStream(propFile)) {
                    props.load(fis);
                    loaded = true;
                } catch (Exception ignored) {}
            }
        }

        if (loaded) {
            if (props.containsKey("firebase.db.url")) {
                dbUrl = props.getProperty("firebase.db.url").trim();
            }
            if (props.containsKey("firebase.auth.token")) {
                authToken = props.getProperty("firebase.auth.token").trim();
            }
            if (props.containsKey("firebase.timeout.ms")) {
                try {
                    timeoutMs = Integer.parseInt(props.getProperty("firebase.timeout.ms").trim());
                } catch (NumberFormatException ignored) {}
            }
            if (props.containsKey("mess.default.meal.rate")) {
                try {
                    defaultMealRate = Double.parseDouble(props.getProperty("mess.default.meal.rate").trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        normalizeDbUrl();
        initialized = true;
    }

    private static void normalizeDbUrl() {
        if (dbUrl != null && !dbUrl.isEmpty()) {
            if (!dbUrl.startsWith("http://") && !dbUrl.startsWith("https://")) {
                dbUrl = "https://" + dbUrl;
            }
            if (!dbUrl.endsWith("/")) {
                dbUrl = dbUrl + "/";
            }
        }
    }

    public static synchronized void updateConfig(String newUrl, String newToken, double newRate) {
        if (newUrl != null && !newUrl.trim().isEmpty()) {
            dbUrl = newUrl.trim();
            normalizeDbUrl();
        }
        if (newToken != null) {
            authToken = newToken.trim();
        }
        if (newRate > 0) {
            defaultMealRate = newRate;
        }

        // Persist to properties file
        Properties props = new Properties();
        props.setProperty("firebase.db.url", dbUrl);
        props.setProperty("firebase.auth.token", authToken);
        props.setProperty("firebase.timeout.ms", String.valueOf(timeoutMs));
        props.setProperty("mess.default.meal.rate", String.format("%.2f", defaultMealRate));

        saveToDisk(props, "resources/firebase.properties");
        saveToDisk(props, "bin/firebase.properties");
    }

    private static void saveToDisk(Properties props, String path) {
        try {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(f)) {
                props.store(fos, "Mess Management System - Firebase Configuration");
            }
        } catch (Exception e) {
            System.err.println("[FirebaseConfig] Warning: could not write to " + path + ": " + e.getMessage());
        }
    }

    public static String getDbUrl() {
        loadProperties();
        return dbUrl;
    }

    public static String getAuthToken() {
        loadProperties();
        return authToken;
    }

    public static int getTimeoutMs() {
        loadProperties();
        return timeoutMs;
    }

    public static double getDefaultMealRate() {
        loadProperties();
        return defaultMealRate;
    }

    public static void setDefaultMealRate(double rate) {
        if (rate > 0) {
            defaultMealRate = rate;
        }
    }
}
