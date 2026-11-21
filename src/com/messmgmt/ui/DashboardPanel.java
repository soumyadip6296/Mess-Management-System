package com.messmgmt.ui;

import com.messmgmt.dao.AttendanceDAO;
import com.messmgmt.dao.BillingDAO;
import com.messmgmt.dao.ComplaintDAO;
import com.messmgmt.dao.StudentDAO;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final BillingDAO billingDAO = new BillingDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();

    private JLabel activeStudentsLabel;
    private JLabel todayAttendanceLabel;
    private JLabel unpaidDuesLabel;
    private JLabel pendingComplaintsLabel;
    private MainFrame mainFrame;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));

        // Welcome / Date Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Mess Management System Overview");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(new Color(33, 37, 41));

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"));
        JLabel dateLabel = new JLabel("Today: " + dateStr);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setForeground(new Color(108, 117, 125));

        topPanel.add(welcomeLabel, BorderLayout.NORTH);
        topPanel.add(dateLabel, BorderLayout.SOUTH);

        // Stats Cards Grid
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(15, 0, 15, 0));

        activeStudentsLabel = new JLabel("...", SwingConstants.CENTER);
        todayAttendanceLabel = new JLabel("...", SwingConstants.CENTER);
        unpaidDuesLabel = new JLabel("...", SwingConstants.CENTER);
        pendingComplaintsLabel = new JLabel("...", SwingConstants.CENTER);

        cardsPanel.add(createCard("Active Registered Students", activeStudentsLabel, new Color(41, 128, 185), "Hostel residents currently enrolled"));
        cardsPanel.add(createCard("Today's Meals Logged", todayAttendanceLabel, new Color(39, 174, 96), "Attendance records marked today"));
        cardsPanel.add(createCard("Pending Mess Dues", unpaidDuesLabel, new Color(211, 84, 0), "Total outstanding unpaid bills"));
        cardsPanel.add(createCard("Open Feedback & Complaints", pendingComplaintsLabel, new Color(192, 57, 43), "Items awaiting resolution"));

        // Quick Actions & Information Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        JPanel actionsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        actionsCard.setBackground(Color.WHITE);
        actionsCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel actionHeader = new JLabel("Quick Actions: ");
        actionHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        actionsCard.add(actionHeader);

        JButton markAttBtn = new JButton("Mark Today's Attendance");
        JButton addStuBtn = new JButton("Register New Student");
        JButton genBillsBtn = new JButton("Generate Monthly Bills");
        JButton refreshBtn = new JButton("Refresh Dashboard");

        markAttBtn.addActionListener(e -> mainFrame.selectTab(3)); // Attendance tab
        addStuBtn.addActionListener(e -> mainFrame.selectTab(1));  // Students tab
        genBillsBtn.addActionListener(e -> mainFrame.selectTab(4)); // Billing tab
        refreshBtn.addActionListener(e -> refreshData());

        actionsCard.add(markAttBtn);
        actionsCard.add(addStuBtn);
        actionsCard.add(genBillsBtn);
        actionsCard.add(refreshBtn);

        bottomPanel.add(actionsCard, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshData();
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(new Color(73, 80, 87));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);

        JLabel subLbl = new JLabel(subtitle, SwingConstants.CENTER);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(108, 117, 125));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    public void refreshData() {
        SwingUtilities.invokeLater(() -> {
            try {
                int activeCount = studentDAO.getActiveStudentsCount();
                int totalStudents = studentDAO.getTotalStudentsCount();
                activeStudentsLabel.setText(activeCount + " / " + totalStudents);

                int todayMeals = attendanceDAO.getTodayPresentCount(LocalDate.now());
                todayAttendanceLabel.setText(String.valueOf(todayMeals));

                String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                double unpaid = billingDAO.getUnpaidTotal(null);
                unpaidDuesLabel.setText(String.format("INR %.2f", unpaid));

                int complaints = complaintDAO.getPendingCount();
                pendingComplaintsLabel.setText(String.valueOf(complaints));
            } catch (Exception ex) {
                System.err.println("[DashboardPanel] Error refreshing stats: " + ex.getMessage());
            }
        });
    }
}
