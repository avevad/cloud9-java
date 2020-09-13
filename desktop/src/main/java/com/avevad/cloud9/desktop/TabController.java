package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.util.TaskQueue;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.avevad.cloud9.core.CloudCommon.*;
import static com.avevad.cloud9.desktop.DesktopCommon.*;

public final class TabController {
    private static final String NAVIGATE = "navigate";
    public final WindowController windowController;
    private final CloudClient controlClient;
    public final JPanel panel = new JPanel();
    private final TaskQueue networkQueue = new TaskQueue("Network");
    private final CardLayout cardLayout = new CardLayout();
    private static final String CARD_TABLE = "card_table";
    private static final String CARD_LOADING = "card_loading";
    private static final String CARD_ERROR = "card_error";
    private final JTable table = new JTable();
    private final CloudTableModel tableModel = new CloudTableModel();
    private final JLabel errorLabel = new JLabel(icon(ICON_ERROR));
    private String path = String.valueOf(CLOUD_PATH_HOME);
    private Node node;
    private final JTextField pathField = new JTextField();
    private final JButton parentButton = new JButton();

    private final List<DirectoryEntry> content = new ArrayList<>();

    public TabController(WindowController windowController, CloudClient cloud) {
        this.windowController = windowController;
        controlClient = cloud;
        panel.setLayout(cardLayout);

        JLabel loadingLabel = new JLabel(string(STRING_LOADING));
        loadingLabel.setVerticalAlignment(SwingConstants.CENTER);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(loadingLabel, CARD_LOADING);

        errorLabel.setVerticalAlignment(SwingConstants.CENTER);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        errorLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        errorLabel.setFont(errorLabel.getFont().deriveFont(18f));
        panel.add(errorLabel, CARD_ERROR);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new GridBagLayout());
        tablePanel.add(navPanel, BorderLayout.NORTH);
        panel.add(tablePanel, CARD_TABLE);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.gridwidth = c.gridheight = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        navPanel.add(pathField, c);

        parentButton.addActionListener(e -> {
            networkQueue.submit(() -> {
                try {
                    Node parent = controlClient.getNodeParent(node);
                    String newPath = path.substring(0, path.lastIndexOf(CLOUD_PATH_SEP));
                    navigate(parent, newPath);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        errorLabel.setText(string(STRING_CONNECTION_LOST, ex.getLocalizedMessage()));
                        cardLayout.show(panel, CARD_ERROR);
                    });
                } catch (CloudClient.RequestException ex) {
                    // TODO: add appropriate handler
                    throw new RuntimeException(ex);
                }
            });
        });
        parentButton.setToolTipText(string(STRING_GO_UP));
        c.gridx = 1;
        c.weightx = 0;
        navPanel.add(parentButton, c);
        parentButton.setIcon(resizeHeight(icon(ICON_OUTWARDS), pathField.getFontMetrics(pathField.getFont()).getHeight()));

        table.setFillsViewportHeight(true);
        table.setModel(tableModel);
        table.getColumn(string(STRING_FILE_TYPE)).setMaxWidth(tableModel.fileIcon.getIconWidth());
        table.getColumn(string(STRING_FILE_TYPE)).setMinWidth(tableModel.fileIcon.getIconWidth());
        Runnable rowSelectTask = () -> {
            int row = table.getSelectionModel().getAnchorSelectionIndex();
            if (row == -1) return;
            if (content.get(row).type == NODE_TYPE_DIRECTORY)
                navigate(content.get(row).node, path + CLOUD_PATH_SEP + content.get(row).name);
        };
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) rowSelectTask.run();
            }
        });
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, NAVIGATE);
        table.getActionMap().put(NAVIGATE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rowSelectTask.run();
            }
        });

        networkQueue.submit(() -> {
            try {
                navigate(cloud.getHome(), path);
            } catch (IOException e) {
                errorLabel.setText(string(STRING_CONNECTION_LOST, e.getLocalizedMessage()));
                cardLayout.show(panel, CARD_ERROR);
            } catch (CloudClient.RequestException e) { // should never happen in normal conditions
                throw new RuntimeException();
            }
        });
    }

    public void destroy() {
        controlClient.disconnect();
    }

    private final class CloudTableModel extends AbstractTableModel {

        private final ImageIcon fileIcon = icon(ICON_FILE);
        private final ImageIcon folderIcon = icon(ICON_FOLDER);

        public CloudTableModel() {
            table.setRowHeight(fileIcon.getIconHeight());
        }


        @Override
        public int getRowCount() {
            return content.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return content.get(rowIndex).type == NODE_TYPE_DIRECTORY ? folderIcon : fileIcon;
            else return content.get(rowIndex).name;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return ImageIcon.class;
            else return String.class;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) return string(STRING_FILE_TYPE);
            else return string(STRING_FILE_NAME);
        }
    }

    private void navigate(Node node, String path) {
        networkQueue.submit(() -> {
            content.clear();
            try {
                SwingUtilities.invokeLater(() -> cardLayout.show(panel, CARD_LOADING));
                controlClient.listDirectory(node, (child, name) -> {
                    DirectoryEntry entry = new DirectoryEntry();
                    entry.node = child;
                    entry.name = name;
                    CloudClient.NodeInfo nodeInfo = controlClient.getNodeInfo(child);
                    entry.type = nodeInfo.type;
                    content.add(entry);
                });
                SwingUtilities.invokeLater(() -> {
                    this.path = path;
                    this.node = node;
                    pathField.setText(path);
                    parentButton.setEnabled(path.contains(String.valueOf(CLOUD_PATH_SEP)));
                    tableModel.fireTableDataChanged();
                    cardLayout.show(panel, CARD_TABLE);
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    errorLabel.setText(string(STRING_CONNECTION_LOST, e.getLocalizedMessage()));
                    cardLayout.show(panel, CARD_ERROR);
                });
            } catch (CloudClient.RequestException e) {
                // TODO: replace with appropriate handler
                throw new RuntimeException(e);
            }
        });
    }

    private static final class DirectoryEntry {
        public Node node;
        public byte type;
        public String name;
    }
}
