package com.messmgmt.ui;

import com.messmgmt.dao.MenuDAO;
import com.messmgmt.model.MenuItem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MenuPanel extends JPanel {

    private final MenuDAO menuDAO = new MenuDAO();
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private JComboBox<String> daySelector;
    private JTextArea breakfastArea;
    private JTextArea lunchArea;
    private JTextArea dinnerArea;

    public MenuPanel() {
        initUI();
        loadMenuForSelectedDay();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(15, 20, 15, 20));

        // Top Control Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));

        daySelector = new JComboBox<>(days);
        daySelector.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton editBreakfastBtn = new JButton("Edit Breakfast");
        JButton editLunchBtn = new JButton("Edit Lunch");
        JButton editDinnerBtn = new JButton("Edit Dinner");
        JButton viewAllBtn = new JButton("View Full Weekly Table");

        topBar.add(new JLabel("Select Day:"));
        topBar.add(daySelector);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(editBreakfastBtn);
        topBar.add(editLunchBtn);
        topBar.add(editDinnerBtn);
        topBar.add(Box.createHorizontalStrut(15));
        topBar.add(viewAllBtn);

        // Center: 3 Meal Cards (Breakfast, Lunch, Dinner)
        JPanel mealsPanel = new JPanel(new GridLayout(3, 1, 15, 15));

        breakfastArea = createMealTextArea();
        lunchArea = createMealTextArea();
        dinnerArea = createMealTextArea();

        mealsPanel.add(createMealCard("Breakfast (7:30 AM - 9:30 AM)", breakfastArea, new Color(241, 196, 15), editBreakfastBtn));
        mealsPanel.add(createMealCard("Lunch (12:30 PM - 2:30 PM)", lunchArea, new Color(46, 204, 113), editLunchBtn));
        mealsPanel.add(createMealCard("Dinner (7:30 PM - 9:30 PM)", dinnerArea, new Color(52, 152, 219), editDinnerBtn));

        // Listeners
        daySelector.addActionListener(e -> loadMenuForSelectedDay());
        editBreakfastBtn.addActionListener(e -> showEditMealDialog("Breakfast", breakfastArea.getText()));
        editLunchBtn.addActionListener(e -> showEditMealDialog("Lunch", lunchArea.getText()));
        editDinnerBtn.addActionListener(e -> showEditMealDialog("Dinner", dinnerArea.getText()));
        viewAllBtn.addActionListener(e -> showFullWeeklyMenuDialog());

        add(topBar, BorderLayout.NORTH);
        add(mealsPanel, BorderLayout.CENTER);
    }

    private JTextArea createMealTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBackground(new Color(252, 252, 253));
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    private JPanel createMealCard(String mealHeader, JTextArea textArea, Color bannerColor, JButton editBtn) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(mealHeader);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(new Color(40, 40, 40));

        JPanel strip = new JPanel();
        strip.setBackground(bannerColor);
        strip.setPreferredSize(new Dimension(8, 20));

        headerPanel.add(strip, BorderLayout.WEST);
        headerPanel.add(Box.createHorizontalStrut(8), BorderLayout.CENTER);
        headerPanel.add(titleLbl, BorderLayout.EAST);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(new JScrollPane(textArea), BorderLayout.CENTER);

        return card;
    }

    public void loadMenuForSelectedDay() {
        String day = (String) daySelector.getSelectedItem();
        if (day == null) return;

        List<MenuItem> items = menuDAO.getMenuByDay(day);
        String bf = "No menu configured.";
        String ln = "No menu configured.";
        String dn = "No menu configured.";

        for (MenuItem m : items) {
            if ("Breakfast".equalsIgnoreCase(m.getMealType())) bf = m.getItems();
            else if ("Lunch".equalsIgnoreCase(m.getMealType())) ln = m.getItems();
            else if ("Dinner".equalsIgnoreCase(m.getMealType())) dn = m.getItems();
        }

        breakfastArea.setText(bf);
        lunchArea.setText(ln);
        dinnerArea.setText(dn);
    }

    private void showEditMealDialog(String mealType, String currentItems) {
        String day = (String) daySelector.getSelectedItem();
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Set Menu - " + day + " [" + mealType + "]", true);
        dialog.setSize(480, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBorder(new EmptyBorder(15, 15, 10, 15));

        JLabel info = new JLabel("Enter menu items / dishes (comma separated):");
        info.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JTextArea editArea = new JTextArea(currentItems.startsWith("No menu") ? "" : currentItems, 5, 20);
        editArea.setLineWrap(true);
        editArea.setWrapStyleWord(true);
        editArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        form.add(info, BorderLayout.NORTH);
        form.add(new JScrollPane(editArea), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Menu");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String items = editArea.getText().trim();
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Menu items cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (menuDAO.saveOrUpdateMenu(day, mealType, items)) {
                JOptionPane.showMessageDialog(dialog, "Menu updated for " + day + " [" + mealType + "]!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadMenuForSelectedDay();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to save menu.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showFullWeeklyMenuDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Weekly Mess Menu Schedule", true);
        dialog.setSize(750, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        String[] cols = {"Menu ID", "Day", "Meal Type", "Dishes & Items"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(450);

        List<MenuItem> all = menuDAO.getAllMenuItems();
        for (MenuItem m : all) {
            model.addRow(new Object[]{m.getMenuId(), m.getDay(), m.getMealType(), m.getItems()});
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
