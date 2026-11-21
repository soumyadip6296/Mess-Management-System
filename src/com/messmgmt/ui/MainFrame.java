package com.messmgmt.ui;

import com.messmgmt.config.FirebaseConfig;
import com.messmgmt.config.FirebaseManager;
import com.messmgmt.model.Admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private final Admin loggedInAdmin;
    private JTabbedPane tabbedPane;
    private DashboardPanel dashboardPanel;
    private StudentPanel studentPanel;
    private MenuPanel menuPanel;
    private AttendancePanel attendancePanel;
    private BillingPanel billingPanel;
    private ComplaintPanel complaintPanel;
    private ReportPanel reportPanel;
    private JLabel statusMsgLabel;

    public MainFrame(Admin admin) {
        super("Mess Management System - Hostel / College Mess Portal (Firebase Cloud)");
        this.loggedInAdmin = admin;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header Bar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(33, 58, 80));
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);

        JLabel logoLabel = new JLabel("🍽️");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel titleLabel = new JLabel("MESS MANAGEMENT SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        titleBox.add(logoLabel);
        titleBox.add(titleLabel);

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userBox.setOpaque(false);

        JLabel adminLabel = new JLabel("👤 " + (loggedInAdmin != null ? loggedInAdmin.getFullName() : "Admin"));
        adminLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        adminLabel.setForeground(new Color(220, 235, 252));

        JButton firebaseSettingsBtn = new JButton("🔥 Firebase Settings");
        firebaseSettingsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        firebaseSettingsBtn.setFocusPainted(false);
        firebaseSettingsBtn.addActionListener(e -> {
            FirebaseSettingsDialog dlg = new FirebaseSettingsDialog(this);
            dlg.setVisible(true);
            updateStatusBar();
        });

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());

        userBox.add(adminLabel);
        userBox.add(firebaseSettingsBtn);
        userBox.add(logoutBtn);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(userBox, BorderLayout.EAST);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        dashboardPanel = new DashboardPanel(this);
        studentPanel = new StudentPanel();
        menuPanel = new MenuPanel();
        attendancePanel = new AttendancePanel();
        billingPanel = new BillingPanel();
        complaintPanel = new ComplaintPanel();
        reportPanel = new ReportPanel();

        tabbedPane.addTab("  Dashboard  ", dashboardPanel);
        tabbedPane.addTab("  Students  ", studentPanel);
        tabbedPane.addTab("  Weekly Menu  ", menuPanel);
        tabbedPane.addTab("  Attendance  ", attendancePanel);
        tabbedPane.addTab("  Fee & Billing  ", billingPanel);
        tabbedPane.addTab("  Complaints  ", complaintPanel);
        tabbedPane.addTab("  Reports  ", reportPanel);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            switch (idx) {
                case 0 -> dashboardPanel.refreshData();
                case 1 -> studentPanel.loadStudents();
                case 2 -> menuPanel.loadMenuForSelectedDay();
                case 3 -> attendancePanel.loadAttendanceForCurrentSelection();
                case 4 -> billingPanel.loadBills();
                case 5 -> complaintPanel.loadComplaints();
            }
        });

        // Bottom Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(6, 15, 6, 15));
        statusBar.setBackground(new Color(238, 240, 244));

        statusMsgLabel = new JLabel("System Ready | 🔥 Firebase Realtime DB: " + FirebaseConfig.getDbUrl());
        statusMsgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusMsgLabel.setForeground(new Color(90, 95, 100));

        JLabel verLabel = new JLabel("Mess Management System v2.0 (Firebase Edition)");
        verLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        verLabel.setForeground(new Color(120, 125, 130));

        statusBar.add(statusMsgLabel, BorderLayout.WEST);
        statusBar.add(verLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        updateStatusBar();
    }

    public void updateStatusBar() {
        if (statusMsgLabel != null) {
            boolean online = FirebaseManager.getInstance().getFirebaseClient().isOnline();
            String mode = online ? "Online Cloud Sync" : "Local Resilient Cache";
            statusMsgLabel.setText("System Ready | 🔥 Firebase: " + FirebaseConfig.getDbUrl() + " [" + mode + "]");
            statusMsgLabel.setForeground(online ? new Color(34, 139, 34) : new Color(110, 110, 110));
        }
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    private void logout() {
        int conf = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginDialog loginDialog = new LoginDialog(null);
                loginDialog.setVisible(true);
                Admin admin = loginDialog.getAuthenticatedAdmin();
                if (admin != null) {
                    new MainFrame(admin).setVisible(true);
                } else {
                    System.exit(0);
                }
            });
        }
    }
}
