package com.messmgmt.ui;

import com.messmgmt.dao.ComplaintDAO;
import com.messmgmt.dao.StudentDAO;
import com.messmgmt.model.Complaint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ComplaintPanel extends JPanel {

    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private JComboBox<String> filterCombo;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    public ComplaintPanel() {
        initUI();
        loadComplaints();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        filterCombo = new JComboBox<>(new String[]{"All Complaints", "Pending", "In Progress", "Resolved"});
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("+ Submit Feedback");
        addBtn.setBackground(new Color(41, 128, 185));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);

        topBar.add(new JLabel("Status Filter:"));
        topBar.add(filterCombo);
        topBar.add(refreshBtn);
        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(addBtn);

        // Table
        String[] cols = {"ID", "Student ID", "Student Name", "Category", "Status", "Date", "Description", "Resolution Note"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(250);
        table.getColumnModel().getColumn(7).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        countLabel = new JLabel("Total Feedback: 0");
        countLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton updateBtn = new JButton("Resolve / Update Status");
        JButton deleteBtn = new JButton("Delete");

        actionBtns.add(updateBtn);
        actionBtns.add(deleteBtn);

        bottomBar.add(countLabel, BorderLayout.WEST);
        bottomBar.add(actionBtns, BorderLayout.EAST);

        // Listeners
        refreshBtn.addActionListener(e -> loadComplaints());
        filterCombo.addActionListener(e -> loadComplaints());
        addBtn.addActionListener(e -> showSubmitComplaintDialog());
        updateBtn.addActionListener(e -> showUpdateStatusDialog());
        deleteBtn.addActionListener(e -> deleteSelectedComplaint());

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadComplaints() {
        String filter = (String) filterCombo.getSelectedItem();
        List<Complaint> list;
        if ("All Complaints".equalsIgnoreCase(filter) || filter == null) {
            list = complaintDAO.getAllComplaints();
        } else {
            list = complaintDAO.getComplaintsByStatus(filter);
        }

        tableModel.setRowCount(0);
        for (Complaint c : list) {
            tableModel.addRow(new Object[]{
                    c.getComplaintId(),
                    c.getStudentId(),
                    c.getStudentName(),
                    c.getCategory(),
                    c.getStatus(),
                    c.getCreatedAt() != null ? c.getCreatedAt().toString() : "-",
                    c.getMessage(),
                    c.getResolutionNote() != null ? c.getResolutionNote() : "-"
            });
        }
        countLabel.setText("Showing: " + list.size() + " feedback/complaint item(s)");
    }

    private void showSubmitComplaintDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Submit Mess Complaint / Feedback", true);
        dialog.setSize(440, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(new EmptyBorder(15, 20, 5, 20));

        JTextField idF = new JTextField();
        JComboBox<String> catC = new JComboBox<>(new String[]{"Food Quality", "Cleanliness", "Service", "Portion Size", "Other"});

        form.add(new JLabel("Student ID:")); form.add(idF);
        form.add(new JLabel("Category:")); form.add(catC);

        JPanel msgPanel = new JPanel(new BorderLayout(5, 5));
        msgPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
        JLabel msgLbl = new JLabel("Description / Complaint Details:");
        JTextArea msgArea = new JTextArea(4, 20);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgPanel.add(msgLbl, BorderLayout.NORTH);
        msgPanel.add(new JScrollPane(msgArea), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Submit");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String stuId = idF.getText().trim().toUpperCase();
            String cat = (String) catC.getSelectedItem();
            String msg = msgArea.getText().trim();

            if (!studentDAO.exists(stuId)) {
                JOptionPane.showMessageDialog(dialog, "Student ID not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (msg.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter complaint description.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Complaint c = new Complaint(stuId, cat, msg);
            if (complaintDAO.addComplaint(c)) {
                JOptionPane.showMessageDialog(dialog, "Complaint submitted successfully!", "Submitted", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadComplaints();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to submit complaint.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(form, BorderLayout.NORTH);
        centerPanel.add(msgPanel, BorderLayout.CENTER);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showUpdateStatusDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 4);
        String currentNote = (String) tableModel.getValueAt(row, 7);
        if ("-".equals(currentNote)) currentNote = "";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Resolve Complaint #" + id, true);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBorder(new EmptyBorder(15, 15, 10, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Pending", "In Progress", "Resolved"});
        statusCombo.setSelectedItem(currentStatus);
        top.add(new JLabel("Update Status:"));
        top.add(statusCombo);

        JTextArea noteArea = new JTextArea(currentNote, 4, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JPanel mid = new JPanel(new BorderLayout(4, 4));
        mid.add(new JLabel("Resolution Note / Action Taken:"), BorderLayout.NORTH);
        mid.add(new JScrollPane(noteArea), BorderLayout.CENTER);

        form.add(top, BorderLayout.NORTH);
        form.add(mid, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Status");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String st = (String) statusCombo.getSelectedItem();
            String note = noteArea.getText().trim();
            if (complaintDAO.updateStatusAndNote(id, st, note)) {
                JOptionPane.showMessageDialog(dialog, "Complaint updated successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadComplaints();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to update complaint.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteSelectedComplaint() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        int conf = JOptionPane.showConfirmDialog(this, "Delete Complaint #" + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            if (complaintDAO.deleteComplaint(id)) {
                JOptionPane.showMessageDialog(this, "Complaint deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                loadComplaints();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
