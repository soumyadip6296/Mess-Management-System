package com.messmgmt.ui;

import com.messmgmt.dao.AttendanceDAO;
import com.messmgmt.dao.BillingDAO;
import com.messmgmt.dao.StudentDAO;
import com.messmgmt.model.Attendance;
import com.messmgmt.model.AttendanceSummary;
import com.messmgmt.model.Bill;
import com.messmgmt.model.Student;
import com.messmgmt.util.ReportGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportPanel extends JPanel {

    private final BillingDAO billingDAO = new BillingDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private JComboBox<String> reportTypeCombo;
    private JTextField monthField;
    private JTextField studentIdField;
    private JTextArea reportArea;

    public ReportPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        reportTypeCombo = new JComboBox<>(new String[]{
                "Monthly Revenue & Billing Summary",
                "Monthly Attendance Audit Sheet",
                "Student Mess Bill & Invoice"
        });

        String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        monthField = new JTextField(curMonth, 7);
        studentIdField = new JTextField(8);
        studentIdField.setEnabled(false);

        JButton genBtn = new JButton("Generate Report");
        genBtn.setBackground(new Color(41, 128, 185));
        genBtn.setForeground(Color.WHITE);
        genBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        genBtn.setFocusPainted(false);

        JButton copyBtn = new JButton("Copy");
        JButton exportBtn = new JButton("Export to Disk");
        JButton printBtn = new JButton("Print");

        topBar.add(new JLabel("Report Type:"));
        topBar.add(reportTypeCombo);
        topBar.add(new JLabel("Month:"));
        topBar.add(monthField);
        topBar.add(new JLabel("Student ID:"));
        topBar.add(studentIdField);
        topBar.add(genBtn);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(copyBtn);
        topBar.add(exportBtn);
        topBar.add(printBtn);

        // Center Text Area
        reportArea = new JTextArea();
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        reportArea.setEditable(false);
        reportArea.setBackground(new Color(250, 250, 252));
        reportArea.setBorder(new EmptyBorder(10, 15, 10, 15));
        reportArea.setText("Select a report type and month, then click 'Generate Report'.\n");

        JScrollPane scrollPane = new JScrollPane(reportArea);

        // Listeners
        reportTypeCombo.addActionListener(e -> {
            boolean isStudentReport = reportTypeCombo.getSelectedIndex() == 2;
            studentIdField.setEnabled(isStudentReport);
        });

        genBtn.addActionListener(e -> generateReport());

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(reportArea.getText()), null);
            JOptionPane.showMessageDialog(this, "Report copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        exportBtn.addActionListener(e -> exportToFile());

        printBtn.addActionListener(e -> {
            try {
                boolean done = reportArea.print();
                if (done) JOptionPane.showMessageDialog(this, "Print sent!", "Print", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void generateReport() {
        int typeIdx = reportTypeCombo.getSelectedIndex();
        String month = monthField.getText().trim();

        if (month.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid month (YYYY-MM).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (typeIdx == 0) {
            // Revenue & Billing
            List<Bill> bills = billingDAO.getBillsByMonth(month);
            if (bills.isEmpty()) {
                reportArea.setText("No billing records found for month " + month + ".\nPlease generate bills first from the Billing tab.");
            } else {
                reportArea.setText(ReportGenerator.generateMonthlyBillingReport(month, bills));
            }
        } else if (typeIdx == 1) {
            // Attendance Summary
            List<AttendanceSummary> list = attendanceDAO.getMonthlyAttendanceSummary(month);
            if (list.isEmpty()) {
                reportArea.setText("No attendance records found for month " + month + ".");
            } else {
                reportArea.setText(ReportGenerator.generateAttendanceSummaryReport(month, list));
            }
        } else if (typeIdx == 2) {
            // Single Student Invoice
            String stuId = studentIdField.getText().trim().toUpperCase();
            if (stuId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter Student ID for individual invoice.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Bill b = billingDAO.getBillByStudentAndMonth(stuId, month);
            if (b == null) {
                reportArea.setText("No bill generated for student '" + stuId + "' in month '" + month + "'.\nPlease generate the bill first.");
                return;
            }
            Student s = studentDAO.getStudentById(stuId);
            List<Attendance> atts = attendanceDAO.getAttendanceByStudentAndMonth(stuId, month);
            reportArea.setText(ReportGenerator.generateStudentInvoice(b, s, atts));
        }
    }

    private void exportToFile() {
        String content = reportArea.getText().trim();
        if (content.isEmpty() || content.startsWith("Select a report")) {
            JOptionPane.showMessageDialog(this, "No generated report to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Mess_Report_" + monthField.getText().trim() + ".txt"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(content);
                JOptionPane.showMessageDialog(this, "Report saved to " + f.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
