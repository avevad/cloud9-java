package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public final class WindowController {
    public final MainController mainController;
    public final JFrame frame = new JFrame();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final HomeTabPanel home = new HomeTabPanel(this);
    private final java.util.List<TabController> tabs = new LinkedList<>();
    private final Map<String, JRadioButtonMenuItem> lafButtons = new HashMap<>();

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

        JMenuItem newWindowMenuItem = new JMenuItem(string(STRING_NEW_WINDOW));
        newWindowMenuItem.addActionListener(e -> mainController.newWindow());

        JMenu fileMenu = new JMenu(string(STRING_FILE));
        fileMenu.add(newWindowMenuItem);
        fileMenu.add(exitMenuItem);

        JMenu lafSubmenu = new JMenu(string(STRING_LOOK_AND_FEEL));
        ButtonGroup lafGroup = new ButtonGroup();
        for (Pair<String, String> laf : iterateLafs()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(laf.b);
            lafButtons.put(laf.a, item);
            item.setSelected(UIManager.getLookAndFeel().getClass().getCanonicalName().equals(laf.a));
            item.addActionListener(e -> {
                try {
                    UIManager.setLookAndFeel(laf.a);
                    mainController.updateLaf(laf.a);
                    getConfig().lookAndFeel = laf.a;
                    saveConfig();
                } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
                    throw new RuntimeException(ex);
                }
            });
            lafSubmenu.add(item);
            lafGroup.add(item);
        }

        JMenu settingsMenu = new JMenu(string(STRING_SETTINGS));
        settingsMenu.add(lafSubmenu);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);

        frame.setJMenuBar(menuBar);

        tabbedPane.addTab("+", home.panel);
        KeyStroke ctrlTab = KeyStroke.getKeyStroke("ctrl TAB");
        KeyStroke ctrlShiftTab = KeyStroke.getKeyStroke("ctrl shift TAB");
        Set<AWTKeyStroke> forwardKeys = new HashSet<>(tabbedPane.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardKeys.remove(ctrlTab);
        tabbedPane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);
        Set<AWTKeyStroke> backwardKeys = new HashSet<>(tabbedPane.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backwardKeys.remove(ctrlShiftTab);
        tabbedPane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys);
        InputMap inputMap = tabbedPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(ctrlTab, "navigateNext");
        inputMap.put(ctrlShiftTab, "navigatePrevious");
        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setLocationByPlatform(true);

        frame.pack();
        frame.setMinimumSize(new Dimension(frame.getWidth() + 5, frame.getHeight() + 5));
        if (first) frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    public void dispose() {
        closeAllTabs();
        frame.setVisible(false);
        frame.dispose();
    }

    public void newTab(TabController tabController, String title, boolean display) {
        tabbedPane.insertTab(null, null, tabController.root, null, tabs.size());
        JPanel tabPanel = new JPanel();
        tabPanel.setOpaque(false);
        tabPanel.setLayout(new BorderLayout());
        JLabel tabHeader = new JLabel(title + " ");
        tabPanel.add(tabHeader, BorderLayout.CENTER);
        JButton tabCloseButton = new JButton(resizeHeight(icon(ICON_CROSS), tabHeader.getFontMetrics(tabHeader.getFont()).getHeight()));
        tabCloseButton.addActionListener(e -> closeTab(tabController));
        tabPanel.add(tabCloseButton, BorderLayout.LINE_END);
        tabbedPane.setTabComponentAt(tabs.size(), tabPanel);
        if (display) tabbedPane.setSelectedIndex(tabs.size());
        tabs.add(tabController);
        tabController.init();
    }

    public void closeTab(TabController tabController) {
        int pos = tabs.indexOf(tabController);
        tabController.destroy();
        tabbedPane.removeTabAt(pos);
        tabs.remove(pos);
    }

    private void closeAllTabs() {
        for (int i = tabs.size() - 1; i >= 0; i--) closeTab(tabs.get(i));
    }


    public void updateLaf(String name) {
        SwingUtilities.updateComponentTreeUI(frame);
        lafButtons.get(name).setSelected(true);
    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }
}
