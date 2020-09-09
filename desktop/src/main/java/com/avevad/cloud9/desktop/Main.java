package com.avevad.cloud9.desktop;

import javax.swing.*;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            SwingUtilities.invokeLater(() -> {
                System.err.print("Unhandled exception in thread '" + t.getName() + "': ");
                e.printStackTrace(System.err);
                JOptionPane.showMessageDialog(null, string(STRING_FATAL_ERROR_ALERT), string(STRING_FATAL_ERROR), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        });
        loadConfig();
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        MainController controller = new MainController();
        controller.newWindow();
    }
}
