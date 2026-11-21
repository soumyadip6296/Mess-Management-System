package com.messmgmt.ui;

import com.messmgmt.config.FirebaseConfig;
import com.messmgmt.dao.AttendanceDAO;
import com.messmgmt.dao.BillingDAO;
import com.messmgmt.dao.StudentDAO;
import com.messmgmt.model.Attendance;
import com.messmgmt.model.Bill;
import com.messmgmt.model.Student;
import com.messmgmt.util.ReportGenerator;
import com.messmgmt.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BillingPanel extends JPanel {

    private final BillingDAO billingDAO = new BillingDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    private JTextField monthField;
    private JTextField rateField;
    private JComboBox<String> statusFilter;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel summaryLabel;

    public BillingPanel() {
        initUI();
        loadBills();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        String curMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        monthField = new JTextField(curMonth, 7);
        rateField = new JTextField(String.valueOf(FirebaseConfig.getDefaultMealRate()), 5);

        JButton calcAllBtn = new JButton("⚡ Generate Month Bills");
        calcAllBtn.setBackground(new Color(41, 128, 185));
        calcAllBtn.setForeground(Color.WHITE);
        calcAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        calcAllBtn.setFocusPainted(false);

        JButton calcSingleBtn = new JButton("Generate for Student");

        statusFilter = new JComboBox<>(new String[]{"All Bills", "Paid", "Unpaid"});
        JButton refreshBtn = new JButton("Refresh");

        topBar.add(new JLabel("Month (YYYY-MM):"));
        topBar.add(monthField);
        topBar.add(new JLabel("Rate/Meal (INR):"));
        topBar.add(rateField);
        topBar.add(calcAllBtn);
        topBar.add(calcSingleBtn);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(new JLabel("Filter:"));
        topBar.add(statusFilter);
        topBar.add(refreshBtn);

        // Table
        String[] cols = {"Bill ID", "Student ID", "Student Name", "Room", "Month", "Meals", "Rate (INR)", "Amount (INR)", "Status", "Paid Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        summaryLabel = new JLabel("Billed: INR 0.00 | Collected: INR 0.00 | Pending: INR 0.00");
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton invoiceBtn = new JButton("📄 View / Print Invoice");
        invoiceBtn.setBackground(new Color(46, 204, 113));
        invoiceBtn.setForeground(Color.WHITE);
        invoiceBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        invoiceBtn.setFocusPainted(false);

        JButton markPaidBtn = new JButton("Mark as Paid");
        JButton markUnpaidBtn = new JButton("Mark as Unpaid");
        JButton deleteBtn = new JButton("Delete Bill");

        actionBtns.add(invoiceBtn);
        actionBtns.add(markPaidBtn);
        actionBtns.add(markUnpaidBtn);
        actionBtns.add(deleteBtn);

        bottomBar.add(summaryLabel, BorderLayout.WEST);
        bottomBar.add(actionBtns, BorderLayout.EAST);

        // Listeners
        calcAllBtn.addActionListener(e -> generateBillsForAll());
        calcSingleBtn.addActionListener(e -> generateBillForSingle());
        refreshBtn.addActionListener(e -> loadBills());
        statusFilter.addActionListener(e -> filterBills());

        markPaidBtn.addActionListener(e -> updateStatus("Paid"));
        markUnpaidBtn.addActionListener(e -> updateStatus("Unpaid"));
        invoiceBtn.addActionListener(e -> openInvoiceDialog());
        deleteBtn.addActionListener(e -> deleteSelectedBill());

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadBills() {
        String filter = (String) statusFilter.getSelectedItem();
        List<Bill> list = billingDAO.getAllBills();
        populateTable(list, filter);
    }

    private void filterBills() {
        String filter = (String) statusFilter.getSelectedItem();
        List<Bill> list = billingDAO.getAllBills();
        populateTable(list, filter);
    }

    private void populateTable(List<Bill> list, String filter) {
        tableModel.setRowCount(0);
        double totalBilled = 0;
        double totalPaid = 0;
        double totalUnpaid = 0;

        for (Bill b : list) {
            if ("Paid".equalsIgnoreCase(filter) && !b.isPaid()) continue;
            if ("Unpaid".equalsIgnoreCase(filter) && b.isPaid()) continue;

            tableModel.addRow(new Object[]{
                    b.getBillId(),
                    b.getStudentId(),
                    b.getStudentName(),
                    b.getRoomNo(),
                    b.getMonth(),
                    b.getTotalMeals(),
                    String.format("%.2f", b.getPerMealRate()),
                    String.format("%.2f", b.getAmount()),
                    b.getPaidStatus(),
                    b.getPaidDate() != null ? b.getPaidDate().toString() : "-"
            });

            totalBilled += b.getAmount();
            if (b.isPaid()) totalPaid += b.getAmount();
            else totalUnpaid += b.getAmount();
        }

        summaryLabel.setText(String.format("Showing %d bills | Total Billed: INR %.2f | Collected: INR %.2f | Due: INR %.2f",
                tableModel.getRowCount(), totalBilled, totalPaid, totalUnpaid));
    }

    private void generateBillsForAll() {
        String month = monthField.getText().trim();
        if (!ValidationUtil.isValidMonth(month)) {
            JOptionPane.showMessageDialog(this, "Invalid month format. Please use YYYY-MM (e.g. 2026-09).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateField.getText().trim());
            if (!ValidationUtil.isValidMealRate(rate)) throw new Exception();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive meal rate.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int count = billingDAO.generateBillsForMonth(month, rate);
        JOptionPane.showMessageDialog(this, "Successfully generated " + count + " bills for month " + month + " @ INR " + rate + "/meal!", "Calculation Complete", JOptionPane.INFORMATION_MESSAGE);
        loadBills();
    }

    private void generateBillForSingle() {
        String stuId = JOptionPane.showInputDialog(this, "Enter Student ID (e.g. STU101):");
        if (stuId == null || stuId.trim().isEmpty()) return;
        stuId = stuId.trim().toUpperCase();

        if (!studentDAO.exists(stuId)) {
            JOptionPane.showMessageDialog(this, "Student ID does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String month = monthField.getText().trim();
        double rate;
        try {
            rate = Double.parseDouble(rateField.getText().trim());
        } catch (Exception ex) {
            rate = FirebaseConfig.getDefaultMealRate();
        }

        if (billingDAO.generateBillForStudent(stuId, month, rate)) {
            Bill b = billingDAO.getBillByStudentAndMonth(stuId, month);
            JOptionPane.showMessageDialog(this,
                    "Bill generated for " + stuId + " (" + month + "):\n" +
                    "Meals Count: " + b.getTotalMeals() + "\n" +
                    "Rate: INR " + b.getPerMealRate() + "\n" +
                    "Total Amount: INR " + b.getAmount(),
                    "Bill Generated", JOptionPane.INFORMATION_MESSAGE);
            loadBills();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to generate bill.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int billId = (int) tableModel.getValueAt(row, 0);
        LocalDate paidDate = "Paid".equalsIgnoreCase(newStatus) ? LocalDate.now() : null;

        if (billingDAO.updatePaymentStatus(billId, newStatus, paidDate)) {
            JOptionPane.showMessageDialog(this, "Bill #" + billId + " updated to " + newStatus + "!", "Updated", JOptionPane.INFORMATION_MESSAGE);
            loadBills();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update bill payment status.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInvoiceDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table to view invoice.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int billId = (int) tableModel.getValueAt(row, 0);
        Bill b = billingDAO.getBillById(billId);
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Bill record not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Student s = studentDAO.getStudentById(b.getStudentId());
        List<Attendance> atts = attendanceDAO.getAttendanceByStudentAndMonth(b.getStudentId(), b.getMonth());
        String invoiceText = ReportGenerator.generateStudentInvoice(b, s, atts);

        InvoiceDialog dlg = new InvoiceDialog((Frame) SwingUtilities.getWindowAncestor(this), invoiceText, b.getStudentId(), b.getMonth());
        dlg.setVisible(true);
    }

    private void deleteSelectedBill() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int billId = (int) tableModel.getValueAt(row, 0);
        int conf = JOptionPane.showConfirmDialog(this, "Delete Bill #" + billId + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            if (billingDAO.deleteBill(billId)) {
                JOptionPane.showMessageDialog(this, "Bill deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                loadBills();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete bill.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
