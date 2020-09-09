package com.avevad.cloud9.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public final class WindowController {
    public final MainController mainController;
    public final JFrame frame = new JFrame();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final HomeTabPanel home = new HomeTabPanel(this);


    public WindowController(MainController mainController, boolean first) {
        this.mainController = mainController;

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainController.closeWindow(WindowController.this);
            }
        });

        JMenuItem exitMenuItem = new JMenuItem(string(STRING_EXIT));
        exitMenuItem.addActionListener(e -> mainController.closeAllWindows());

        JMenu fileMenu = new JMenu(string(STRING_FILE));
        fileMenu.add(exitMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);

        frame.setJMenuBar(menuBar);

        tabbedPane.addTab("+", home.panel);
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setLocationByPlatform(true);

        if(first) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        else frame.pack();
    }

    public void dispose() {
        frame.setVisible(false);
        frame.dispose();
    }


    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
}
