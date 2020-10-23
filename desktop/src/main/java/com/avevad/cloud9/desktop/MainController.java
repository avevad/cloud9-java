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

    public boolean closeWindow(WindowController windowController) {
        if (!windowController.dispose()) return false;
        windows.remove(windowController);
        if (windows.isEmpty()) SwingUtilities.invokeLater(() -> System.exit(0));
        return true;
    }

    public boolean closeAllWindows() {
        int pos = windows.size();
        boolean ok = true;
        while (pos > 0) ok &= closeWindow(windows.get(--pos));
        return ok;
    }

    public void updateLaf(String name) {
        for (WindowController windowController : windows) windowController.updateLaf(name);
    }
}
