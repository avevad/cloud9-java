package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.util.TaskQueue;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class MainController {
    public final TaskQueue connectionQueue = new TaskQueue("Connection");
    private final List<WindowController> windows = new ArrayList<>();

    public MainController() {

    }

    public void newWindow() {
        WindowController windowController = new WindowController(this, windows.isEmpty());
        windows.add(windowController);
        SwingUtilities.invokeLater(() -> windowController.setVisible(true));
    }

    public void closeWindow(WindowController windowController) {
        if (!windows.contains(windowController)) return;
        windows.remove(windowController);
        windowController.dispose();
        if (windows.isEmpty()) SwingUtilities.invokeLater(() -> System.exit(0));
    }

    public void closeAllWindows() {
        while (!windows.isEmpty()) closeWindow(windows.get(windows.size() - 1));
    }

    public void updateLaf() {
        for (WindowController windowController : windows) SwingUtilities.updateComponentTreeUI(windowController.frame);
    }
}
