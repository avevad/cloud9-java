package com.avevad.cloud9.desktop;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

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
        FlatLightLaf.install();
        FlatDarculaLaf.install();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
        }
        MainController controller = new MainController();
        controller.newWindow();
    }
}
