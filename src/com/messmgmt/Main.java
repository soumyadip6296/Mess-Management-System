package com.messmgmt;

import com.messmgmt.model.Admin;
import com.messmgmt.ui.LoginDialog;
import com.messmgmt.ui.MainFrame;
import java.awt.*;
import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Check for CLI argument
        for (String arg : args) {
            if ("--console".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg) || "--cli".equalsIgnoreCase(arg)) {
                MainConsole.main(args);
                return;
            }
        }

        // Check if graphics environment is available
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("[i] Headless environment detected. Launching CLI mode...");
            MainConsole.main(args);
            return;
        }

        // Configure modern Look and Feel for Swing
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog(null);
            loginDialog.setVisible(true);

            Admin admin = loginDialog.getAuthenticatedAdmin();
            if (admin != null) {
                MainFrame frame = new MainFrame(admin);
                frame.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
