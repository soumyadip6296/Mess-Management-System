package com.messmgmt.ui;

import com.messmgmt.dao.StudentDAO;
import com.messmgmt.model.Student;
import com.messmgmt.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel countLabel;

    public StudentPanel() {
        initUI();
        loadStudents();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        searchField = new JTextField(15);
        JButton searchBtn = new JButton("Search");
        JButton clearBtn = new JButton("Clear");

        statusFilter = new JComboBox<>(new String[]{"All", "Active", "Inactive"});
        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("+ Register Student");
        addBtn.setBackground(new Color(41, 128, 185));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);

        topBar.add(new JLabel("Search:"));
        topBar.add(searchField);
        topBar.add(searchBtn);
        topBar.add(clearBtn);
        topBar.add(new JLabel("Status:"));
        topBar.add(statusFilter);
        topBar.add(refreshBtn);
        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(addBtn);

        // Table
        String[] columns = {"Student ID", "Full Name", "Room No", "Contact No", "Mess Status", "Registered Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        countLabel = new JLabel("Total Students: 0");
        countLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton editBtn = new JButton("Edit Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.setForeground(new Color(178, 34, 34));

        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);

        bottomBar.add(countLabel, BorderLayout.WEST);
        bottomBar.add(actionButtons, BorderLayout.EAST);

        // Action Listeners
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadStudents();
        });

        statusFilter.addActionListener(e -> performSearch());
        refreshBtn.addActionListener(e -> loadStudents());
        addBtn.addActionListener(e -> showAddStudentDialog());
        editBtn.addActionListener(e -> showEditStudentDialog());
        deleteBtn.addActionListener(e -> deleteSelectedStudent());

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadStudents() {
        tableModel.setRowCount(0);
        List<Student> list = studentDAO.getAllStudents();
        populateTable(list);
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String filter = (String) statusFilter.getSelectedItem();

        List<Student> list;
        if (!query.isEmpty()) {
            list = studentDAO.searchStudents(query);
        } else {
            list = studentDAO.getAllStudents();
        }

        if (!"All".equalsIgnoreCase(filter)) {
            list = list.stream().filter(s -> s.getStatus().equalsIgnoreCase(filter)).toList();
        }

        tableModel.setRowCount(0);
        populateTable(list);
    }

    private void populateTable(List<Student> list) {
        for (Student s : list) {
            tableModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getName(),
                    s.getRoomNo(),
                    s.getContact(),
                    s.getStatus(),
                    s.getRegisteredAt() != null ? (s.getRegisteredAt().length() >= 19 ? s.getRegisteredAt().substring(0, 19) : s.getRegisteredAt()) : "-"
            });
        }
        countLabel.setText("Showing: " + list.size() + " student(s)");
    }

    private void showAddStudentDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Register New Student", true);
        dialog.setSize(400, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setBorder(new EmptyBorder(15, 20, 10, 20));

        JTextField idF = new JTextField();
        JTextField nameF = new JTextField();
        JTextField roomF = new JTextField();
        JTextField contactF = new JTextField();
        JComboBox<String> statusC = new JComboBox<>(new String[]{"Active", "Inactive"});

        form.add(new JLabel("Student ID (e.g. STU107):")); form.add(idF);
        form.add(new JLabel("Full Name:")); form.add(nameF);
        form.add(new JLabel("Room Number:")); form.add(roomF);
        form.add(new JLabel("Contact Number:")); form.add(contactF);
        form.add(new JLabel("Mess Status:")); form.add(statusC);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Register");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String id = idF.getText().trim();
            String name = nameF.getText().trim();
            String room = roomF.getText().trim();
            String contact = contactF.getText().trim();
            String status = (String) statusC.getSelectedItem();

            if (!ValidationUtil.isValidStudentId(id)) {
                JOptionPane.showMessageDialog(dialog, "Invalid Student ID (3-20 alphanumeric characters).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (studentDAO.exists(id)) {
                JOptionPane.showMessageDialog(dialog, "Student ID already exists in database.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtil.isValidName(name)) {
                JOptionPane.showMessageDialog(dialog, "Name must be between 2 and 100 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtil.isValidRoomNo(room)) {
                JOptionPane.showMessageDialog(dialog, "Room number cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtil.isValidContact(contact)) {
                JOptionPane.showMessageDialog(dialog, "Invalid contact number format (10-15 digits).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Student s = new Student(id, name, room, contact, status);
            if (studentDAO.addStudent(s)) {
                JOptionPane.showMessageDialog(dialog, "Student registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadStudents();
            } else {
                JOptionPane.showMessageDialog(dialog, "Database error adding student.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showEditStudentDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String studentId = (String) tableModel.getValueAt(row, 0);
        Student s = studentDAO.getStudentById(studentId);
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Could not fetch student details from database.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Student Details - " + studentId, true);
        dialog.setSize(400, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setBorder(new EmptyBorder(15, 20, 10, 20));

        JTextField idF = new JTextField(s.getStudentId());
        idF.setEditable(false);
        JTextField nameF = new JTextField(s.getName());
        JTextField roomF = new JTextField(s.getRoomNo());
        JTextField contactF = new JTextField(s.getContact());
        JComboBox<String> statusC = new JComboBox<>(new String[]{"Active", "Inactive"});
        statusC.setSelectedItem(s.getStatus());

        form.add(new JLabel("Student ID (fixed):")); form.add(idF);
        form.add(new JLabel("Full Name:")); form.add(nameF);
        form.add(new JLabel("Room Number:")); form.add(roomF);
        form.add(new JLabel("Contact Number:")); form.add(contactF);
        form.add(new JLabel("Mess Status:")); form.add(statusC);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String name = nameF.getText().trim();
            String room = roomF.getText().trim();
            String contact = contactF.getText().trim();
            String status = (String) statusC.getSelectedItem();

            if (!ValidationUtil.isValidName(name)) {
                JOptionPane.showMessageDialog(dialog, "Name must be between 2 and 100 characters.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtil.isValidRoomNo(room)) {
                JOptionPane.showMessageDialog(dialog, "Room number cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!ValidationUtil.isValidContact(contact)) {
                JOptionPane.showMessageDialog(dialog, "Invalid contact number format (10-15 digits).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            s.setName(name);
            s.setRoomNo(room);
            s.setContact(contact);
            s.setStatus(status);

            if (studentDAO.updateStudent(s)) {
                JOptionPane.showMessageDialog(dialog, "Student updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadStudents();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to update student in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String studentId = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete student '" + name + "' (" + studentId + ")?\n" +
                "WARNING: This will permanently remove all associated attendance and billing records!",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (studentDAO.deleteStudent(studentId)) {
                JOptionPane.showMessageDialog(this, "Student deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                loadStudents();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete student.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
