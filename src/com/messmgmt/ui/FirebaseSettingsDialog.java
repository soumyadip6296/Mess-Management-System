package com.messmgmt.ui;

import com.messmgmt.config.FirebaseClient;
import com.messmgmt.config.FirebaseConfig;
import com.messmgmt.config.FirebaseManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Interactive settings dialog for configuring Google Firebase Realtime Database
 * credentials, testing connectivity, and manual cloud synchronization.
 */
public class FirebaseSettingsDialog extends JDialog {

    private JTextField urlField;
    private JPasswordField tokenField;
    private JTextField mealRateField;
    private JLabel statusLabel;

    public FirebaseSettingsDialog(Frame parent) {
        super(parent, "Firebase Cloud Realtime Database Settings", true);
        initUI();
    }

    private void initUI() {
        setSize(560, 480);
        setResizable(false);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(25, 42, 57));
        headerPanel.setBorder(new EmptyBorder(14, 18, 14, 18));
        headerPanel.setLayout(new BorderLayout());

        JLabel titleLbl = new JLabel("🔥 Firebase Realtime Database Configuration");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Connect to Google Firebase Cloud for live synchronization");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(200, 220, 240));

        headerPanel.add(titleLbl, BorderLayout.NORTH);
        headerPanel.add(subLbl, BorderLayout.SOUTH);

        // Center Form Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        urlField = new JTextField(FirebaseConfig.getDbUrl(), 24);
        tokenField = new JPasswordField(FirebaseConfig.getAuthToken(), 24);
        mealRateField = new JTextField(String.format("%.2f", FirebaseConfig.getDefaultMealRate()), 10);

        String[] labels = {
                "Firebase Database URL:",
                "Auth Token / Secret (Optional):",
                "Default Meal Rate (INR):"
        };
        JComponent[] fields = {urlField, tokenField, mealRateField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.35;
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            formPanel.add(lbl, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.65;
            formPanel.add(fields[i], gbc);
        }

        // Info box
        JPanel infoBox = new JPanel(new BorderLayout(5, 5));
        infoBox.setBackground(new Color(248, 249, 250));
        infoBox.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel infoText = new JLabel("<html><b>💡 Firebase Setup Tips:</b><br/>" +
                "• Create a free project at <b>console.firebase.google.com</b>.<br/>" +
                "• Navigate to <b>Build &rarr; Realtime Database</b> &rarr; Create Database.<br/>" +
                "• Copy your database URL (e.g. <i>https://&lt;project&gt;-default-rtdb.firebaseio.com/</i>).<br/>" +
                "• For open read/write in testing, set <code>\".read\": true, \".write\": true</code> in Rules.</html>");
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoText.setForeground(new Color(73, 80, 87));
        infoBox.add(infoText, BorderLayout.CENTER);

        // Status Label
        statusLabel = new JLabel("Status: Ready to test Firebase connection", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(80, 80, 80));
        statusLabel.setBorder(new EmptyBorder(8, 5, 8, 5));

        // Cloud sync buttons panel
        JPanel syncPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton testBtn = new JButton("📡 Test Connection");
        JButton pushBtn = new JButton("⬆ Push All to Cloud");
        JButton pullBtn = new JButton("⬇ Pull All from Cloud");

        syncPanel.add(testBtn);
        syncPanel.add(pushBtn);
        syncPanel.add(pullBtn);

        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(infoBox);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(statusLabel);
        centerPanel.add(syncPanel);

        // Bottom Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBorder(new EmptyBorder(0, 15, 10, 15));

        JButton saveBtn = new JButton("💾 Save & Apply");
        saveBtn.setBackground(new Color(41, 128, 185));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setFocusPainted(false);

        JButton closeBtn = new JButton("Close");

        testBtn.addActionListener(e -> {
            applyConfigFromFields();
            FirebaseClient client = FirebaseManager.getInstance().getFirebaseClient();
            String res = client.testConnectionWithMessage();
            if (res.startsWith("SUCCESS")) {
                statusLabel.setText("● Connected to Firebase successfully!");
                statusLabel.setForeground(new Color(34, 139, 34));
                JOptionPane.showMessageDialog(this, res, "Firebase Connection Success", JOptionPane.INFORMATION_MESSAGE);
            } else if (res.startsWith("CONNECTED")) {
                statusLabel.setText("● Firebase host reachable (Auth restricted)");
                statusLabel.setForeground(new Color(211, 84, 0));
                JOptionPane.showMessageDialog(this, res, "Firebase Access Restricted", JOptionPane.WARNING_MESSAGE);
            } else {
                statusLabel.setText("● Offline / Unable to reach Firebase");
                statusLabel.setForeground(new Color(178, 34, 34));
                JOptionPane.showMessageDialog(this, res, "Connection Offline", JOptionPane.ERROR_MESSAGE);
            }
        });

        pushBtn.addActionListener(e -> {
            applyConfigFromFields();
            int conf = JOptionPane.showConfirmDialog(this,
                    "This will upload all local students, menus, attendance, bills, and complaints to your Firebase Realtime Database.\nContinue?",
                    "Push Data to Cloud", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                String res = FirebaseManager.getInstance().pushAllToFirebase();
                JOptionPane.showMessageDialog(this, res, "Cloud Push Result", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        pullBtn.addActionListener(e -> {
            applyConfigFromFields();
            int conf = JOptionPane.showConfirmDialog(this,
                    "This will replace local data with the current dataset in Firebase Realtime Database.\nContinue?",
                    "Pull Data from Cloud", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                String res = FirebaseManager.getInstance().pullAllFromFirebase();
                JOptionPane.showMessageDialog(this, res, "Cloud Pull Result", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        saveBtn.addActionListener(e -> {
            applyConfigFromFields();
            JOptionPane.showMessageDialog(this, "Firebase configuration saved and applied!", "Configuration Saved", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void applyConfigFromFields() {
        String url = urlField.getText().trim();
        String token = new String(tokenField.getPassword()).trim();
        double rate = 50.0;
        try {
            rate = Double.parseDouble(mealRateField.getText().trim());
        } catch (NumberFormatException ignored) {}

        FirebaseConfig.updateConfig(url, token, rate);
    }
}
