package com.avevad.cloud9.desktop;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            SwingUtilities.invokeLater(() -> {
                System.err.print("Unhandled exception in thread '" + t.getName() + "': ");
                e.printStackTrace(System.err);
                JOptionPane.showMessageDialog(null, string(STRING_FATAL_ERROR_ALERT), string(STRING_FATAL_ERROR), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        });
        loadConfig();
        if (getConfig().lookAndFeel == null) FlatIntelliJLaf.install();
        else {
            try {
                UIManager.setLookAndFeel(getConfig().lookAndFeel);
            } catch (IllegalAccessException | UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException e) {
                getConfig().lookAndFeel = null;
                saveConfig();
                throw new RuntimeException(e);
            }
        }
        MainController controller = new MainController();
        controller.newWindow();
    }
}
