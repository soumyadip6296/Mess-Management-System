package com.messmgmt.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class InvoiceDialog extends JDialog {

    private final String invoiceText;
    private final String studentId;
    private final String month;

    public InvoiceDialog(Frame parent, String invoiceText, String studentId, String month) {
        super(parent, "Student Mess Invoice Receipt - " + studentId, true);
        this.invoiceText = invoiceText;
        this.studentId = studentId;
        this.month = month;
        initUI();
    }

    private void initUI() {
        setSize(650, 560);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea(invoiceText);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        textArea.setEditable(false);
        textArea.setBackground(new Color(250, 250, 250));
        textArea.setBorder(new EmptyBorder(10, 15, 10, 15));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Official Mess Fee Receipt"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton copyBtn = new JButton("Copy to Clipboard");
        JButton exportBtn = new JButton("Export to .TXT File");
        JButton printBtn = new JButton("Print Receipt");
        JButton closeBtn = new JButton("Close");

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(invoiceText), null);
            JOptionPane.showMessageDialog(this, "Invoice text copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        exportBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("Invoice_" + studentId + "_" + month + ".txt"));
            int res = fileChooser.showSaveDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File target = fileChooser.getSelectedFile();
                try (FileWriter fw = new FileWriter(target)) {
                    fw.write(invoiceText);
                    JOptionPane.showMessageDialog(this, "Invoice exported to " + target.getName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        printBtn.addActionListener(e -> {
            try {
                boolean complete = textArea.print();
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Printing complete!", "Print", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(copyBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(closeBtn);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}
