package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.CloudClient;

import javax.swing.*;

public final class TabController {
    public final WindowController windowController;
    private final CloudClient controlClient;
    public final JPanel panel = new JPanel();

    public TabController(WindowController windowController, CloudClient cloud) {
        this.windowController = windowController;
        controlClient = cloud;
    }

    public void destroy() {
        controlClient.close();
    }
}
