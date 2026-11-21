package com.messmgmt.ui;

import com.messmgmt.dao.AttendanceDAO;
import com.messmgmt.dao.StudentDAO;
import com.messmgmt.model.Attendance;
import com.messmgmt.model.Student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendancePanel extends JPanel {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private JTextField dateField;
    private JComboBox<String> mealSelector;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statsLabel;

    public AttendancePanel() {
        initUI();
        loadAttendanceForCurrentSelection();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), 10);
        mealSelector = new JComboBox<>(new String[]{"Breakfast", "Lunch", "Dinner"});
        JButton loadBtn = new JButton("Load Records");

        JButton allPresentBtn = new JButton("Mark All Present");
        JButton allAbsentBtn = new JButton("Mark All Absent");
        JButton saveBtn = new JButton("💾 Save Attendance");
        saveBtn.setBackground(new Color(39, 174, 96));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setFocusPainted(false);

        topBar.add(new JLabel("Date (YYYY-MM-DD):"));
        topBar.add(dateField);
        topBar.add(new JLabel("Meal:"));
        topBar.add(mealSelector);
        topBar.add(loadBtn);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(allPresentBtn);
        topBar.add(allAbsentBtn);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(saveBtn);

        // Table
        String[] cols = {"Student ID", "Student Name", "Room No", "Present?"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return (col == 3) ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3; // Only the Present checkbox is editable
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getColumnModel().getColumn(3).setPreferredWidth(90);

        tableModel.addTableModelListener(e -> updateStats());

        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        statsLabel = new JLabel("Total: 0 | Present: 0 | Absent: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton historyBtn = new JButton("View Student Attendance History");
        historyBtn.addActionListener(e -> showStudentAttendanceHistoryDialog());

        bottomBar.add(statsLabel, BorderLayout.WEST);
        bottomBar.add(historyBtn, BorderLayout.EAST);

        // Listeners
        loadBtn.addActionListener(e -> loadAttendanceForCurrentSelection());
        mealSelector.addActionListener(e -> loadAttendanceForCurrentSelection());
        allPresentBtn.addActionListener(e -> setAllAttendance(true));
        allAbsentBtn.addActionListener(e -> setAllAttendance(false));
        saveBtn.addActionListener(e -> saveAttendance());

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadAttendanceForCurrentSelection() {
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String meal = (String) mealSelector.getSelectedItem();
        List<Student> students = studentDAO.getActiveStudents();
        List<Attendance> existingAtt = attendanceDAO.getAttendanceByDateAndMeal(date, meal);

        Map<String, Boolean> statusMap = new HashMap<>();
        for (Attendance a : existingAtt) {
            statusMap.put(a.getStudentId(), a.isPresent());
        }

        tableModel.setRowCount(0);
        for (Student s : students) {
            // Default to Present if not already marked, or use existing status
            boolean isPresent = statusMap.getOrDefault(s.getStudentId(), true);
            tableModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getName(),
                    s.getRoomNo(),
                    isPresent
            });
        }

        updateStats();
    }

    private void setAllAttendance(boolean present) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(present, i, 3);
        }
        updateStats();
    }

    private void updateStats() {
        int total = tableModel.getRowCount();
        int present = 0;
        for (int i = 0; i < total; i++) {
            Boolean val = (Boolean) tableModel.getValueAt(i, 3);
            if (val != null && val) present++;
        }
        int absent = total - present;
        statsLabel.setText(String.format("Total: %d  |  Present: %d  |  Absent: %d", total, present, absent));
    }

    private void saveAttendance() {
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String meal = (String) mealSelector.getSelectedItem();
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            JOptionPane.showMessageDialog(this, "No student records to save.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int successCount = 0;
        for (int i = 0; i < rowCount; i++) {
            String stuId = (String) tableModel.getValueAt(i, 0);
            Boolean isPres = (Boolean) tableModel.getValueAt(i, 3);
            String status = (isPres != null && isPres) ? "Present" : "Absent";

            if (attendanceDAO.markAttendance(stuId, date, meal, status)) {
                successCount++;
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "Successfully saved " + successCount + " attendance records for " + date + " [" + meal + "]!",
                "Attendance Saved",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showStudentAttendanceHistoryDialog() {
        int selectedRow = table.getSelectedRow();
        String defaultId = selectedRow >= 0 ? (String) tableModel.getValueAt(selectedRow, 0) : "";

        String inputId = JOptionPane.showInputDialog(this, "Enter Student ID to view monthly history:", defaultId);
        if (inputId == null || inputId.trim().isEmpty()) return;
        inputId = inputId.trim().toUpperCase();

        String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String month = JOptionPane.showInputDialog(this, "Enter Month (YYYY-MM):", curMonth);
        if (month == null || month.trim().isEmpty()) return;

        List<Attendance> history = attendanceDAO.getAttendanceByStudentAndMonth(inputId, month);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Attendance History - " + inputId + " (" + month + ")", true);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        String[] cols = {"Date", "Meal Type", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        int present = 0;
        for (Attendance a : history) {
            model.addRow(new Object[]{a.getDate(), a.getMealType(), a.getStatus()});
            if (a.isPresent()) present++;
        }

        JTable hTable = new JTable(model);
        hTable.setRowHeight(24);

        JLabel sumLabel = new JLabel(" Total Meals Present: " + present + " / " + history.size(), SwingConstants.CENTER);
        sumLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sumLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

        dialog.add(new JScrollPane(hTable), BorderLayout.CENTER);
        dialog.add(sumLabel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
