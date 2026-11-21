package com.messmgmt.ui;

import com.messmgmt.dao.AdminDAO;
import com.messmgmt.model.Admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginDialog extends JDialog {

    private final AdminDAO adminDAO = new AdminDAO();
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private Admin authenticatedAdmin = null;

    public LoginDialog(Frame parent) {
        super(parent, "Mess Management System - Admin Login", true);
        initUI();
    }

    private void initUI() {
        setSize(450, 370);
        setResizable(false);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 70, 91));
        headerPanel.setBorder(new EmptyBorder(18, 15, 18, 15));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("HOSTEL MESS SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Secure Admin Portal • Powered by Firebase");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(200, 220, 240));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subLabel);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 30, 10, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usernameField = new JTextField("admin", 15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        passwordField = new JPasswordField("admin123", 15);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(passwordField, gbc);

        // Status & Hint label
        statusLabel = new JLabel("Default login: admin / admin123", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(new Color(100, 100, 100));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(statusLabel, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBorder(new EmptyBorder(0, 20, 15, 20));

        JButton firebaseBtn = new JButton("🔥 Firebase Settings");
        JButton loginBtn = new JButton("Login");
        JButton exitBtn = new JButton("Exit");

        loginBtn.setBackground(new Color(41, 128, 185));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginBtn.setFocusPainted(false);

        loginBtn.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        firebaseBtn.addActionListener(e -> {
            FirebaseSettingsDialog dlg = new FirebaseSettingsDialog((Frame) getParent());
            dlg.setVisible(true);
        });

        exitBtn.addActionListener(e -> {
            authenticatedAdmin = null;
            dispose();
        });

        buttonPanel.add(firebaseBtn);
        buttonPanel.add(exitBtn);
        buttonPanel.add(loginBtn);

        getRootPane().setDefaultButton(loginBtn);

        add(headerPanel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void attemptLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Admin admin = adminDAO.authenticate(user, pass);
        if (admin != null) {
            this.authenticatedAdmin = admin;
            dispose();
        } else {
            statusLabel.setText("Login failed: invalid username/password.");
            statusLabel.setForeground(new Color(178, 34, 34));
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Invalid login credentials.\n\n" +
                    "Would you like to configure Firebase connection settings now?",
                    "Authentication Failed",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE
            );
            if (opt == JOptionPane.YES_OPTION) {
                FirebaseSettingsDialog dlg = new FirebaseSettingsDialog((Frame) getParent());
                dlg.setVisible(true);
            }
        }
    }

    public Admin getAuthenticatedAdmin() {
        return authenticatedAdmin;
    }
}
