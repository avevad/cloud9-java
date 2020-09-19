package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.util.TaskQueue;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
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
    private static final String CARD_CONTENT = "card_content";
    private static final String CARD_NET_ERROR = "card_net_error";
    private final JPanel tablePanel = new JPanel();
    private final CardLayout tableLayout = new CardLayout();
    private static final String CARD_TABLE = "card_table";
    private static final String CARD_REQ_ERROR = "card_req_error";
    private final JTable table = new JTable();
    private final CloudTableModel tableModel = new CloudTableModel();
    private final JLabel netErrorLabel = new JLabel(icon(ICON_ERROR));
    private final JLabel reqErrorLabel = new JLabel(icon(ICON_ERROR));
    private String path = String.valueOf(CLOUD_PATH_HOME);
    private Node node;
    private final JTextField pathField = new JTextField();
    private final JButton goButton = new JButton();
    private final JButton parentButton = new JButton();

    private final List<DirectoryEntry> content = new ArrayList<>();

    public TabController(WindowController windowController, CloudClient cloud) {
        this.windowController = windowController;
        controlClient = cloud;
        panel.setLayout(cardLayout);

        netErrorLabel.setVerticalAlignment(SwingConstants.CENTER);
        netErrorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        netErrorLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        netErrorLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        netErrorLabel.setFont(netErrorLabel.getFont().deriveFont(18f));
        panel.add(netErrorLabel, CARD_NET_ERROR);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        tablePanel.setLayout(tableLayout);
        tablePanel.add(scrollPane, CARD_TABLE);

        reqErrorLabel.setVerticalAlignment(SwingConstants.CENTER);
        reqErrorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        reqErrorLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        reqErrorLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        reqErrorLabel.setFont(reqErrorLabel.getFont().deriveFont(18f));
        tablePanel.add(reqErrorLabel, CARD_REQ_ERROR);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(tablePanel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new GridBagLayout());
        contentPanel.add(navPanel, BorderLayout.NORTH);
        panel.add(contentPanel, CARD_CONTENT);

        ActionListener goListener = e -> {
            String path = pathField.getText();
            Node start;
            int splitPos = path.indexOf(CLOUD_PATH_SEP);
            splitPos = splitPos == -1 ? path.length() : splitPos;
            try {
                if (path.startsWith(String.valueOf(CLOUD_PATH_HOME))) {
                    String user = path.substring(1, splitPos);
                    start = controlClient.getHome(user);
                } else if (path.startsWith(String.valueOf(CLOUD_PATH_NODE))) {
                    String node = path.substring(1, splitPos);
                    try {
                        start = Node.fromString(node);
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(windowController.frame, string(STRING_INVALID_NODE_ID, node), string(STRING_ERROR), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(windowController.frame, string(STRING_INVALID_PATH, string(STRING_PATH_FORMAT_ALERT)), string(STRING_ERROR), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    navigate(parsePath(controlClient, start, path.substring(splitPos)), path);
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(windowController.frame, string(STRING_FILE_NOT_FOUND, ex.getMessage()), string(STRING_ERROR), JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                netErrorLabel.setText(string(STRING_CONNECTION_LOST, ex.getLocalizedMessage()));
                cardLayout.show(panel, CARD_NET_ERROR);
            } catch (CloudClient.RequestException ex) {
                JOptionPane.showMessageDialog(windowController.frame, string(STRING_REQUEST_ERROR, string(request_status_string(ex.status))), string(STRING_ERROR), JOptionPane.ERROR_MESSAGE);
            }

        };

        pathField.addActionListener(goListener);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = c.gridy = 0;
        c.gridwidth = c.gridheight = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        navPanel.add(pathField, c);

        goButton.addActionListener(goListener);
        goButton.setIcon(resizeHeight(icon(ICON_RIGHT), pathField.getFontMetrics(pathField.getFont()).getHeight()));
        goButton.setToolTipText(string(STRING_GO));
        c.weightx = 0;
        c.gridx++;
        navPanel.add(goButton, c);

        parentButton.setIcon(resizeHeight(icon(ICON_OUTWARDS), pathField.getFontMetrics(pathField.getFont()).getHeight()));
        parentButton.addActionListener(e -> {
            networkQueue.submit(() -> {
                try {
                    Node parent = controlClient.getNodeParent(node);
                    String newPath = path;
                    while (newPath.endsWith(String.valueOf(CLOUD_PATH_SEP)))
                        newPath = newPath.substring(0, newPath.length() - 1);
                    newPath = newPath.substring(0, path.lastIndexOf(CLOUD_PATH_SEP));
                    navigate(parent, newPath);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        netErrorLabel.setText(string(STRING_CONNECTION_LOST, ex.getLocalizedMessage()));
                        cardLayout.show(panel, CARD_NET_ERROR);
                    });
                } catch (CloudClient.RequestException ex) {
                    SwingUtilities.invokeLater(() -> {
                        reqErrorLabel.setText(string(STRING_REQUEST_ERROR, string(init_status_string(ex.status))));
                        tableLayout.show(tablePanel, CARD_REQ_ERROR);
                    });
                }
            });
        });
        parentButton.setToolTipText(string(STRING_GO_UP));
        c.gridx++;
        navPanel.add(parentButton, c);


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
        table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pathField.setText(path);
            }
        });

        cardLayout.show(panel, CARD_CONTENT);

        networkQueue.submit(() -> {
            try {
                navigate(cloud.getHome(), path);
            } catch (IOException e) {
                netErrorLabel.setText(string(STRING_CONNECTION_LOST, e.getLocalizedMessage()));
                cardLayout.show(panel, CARD_NET_ERROR);
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
                controlClient.listDirectory(node, (child, name) -> {
                    DirectoryEntry entry = new DirectoryEntry();
                    entry.node = child;
                    entry.name = name;
                    CloudClient.NodeInfo nodeInfo = controlClient.getNodeInfo(child);
                    entry.type = nodeInfo.type;
                    content.add(entry);
                });
                SwingUtilities.invokeLater(() -> tableLayout.show(tablePanel, CARD_TABLE));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    netErrorLabel.setText(string(STRING_CONNECTION_LOST, e.getLocalizedMessage()));
                    cardLayout.show(panel, CARD_NET_ERROR);
                });
            } catch (CloudClient.RequestException e) {
                SwingUtilities.invokeLater(() -> {
                    reqErrorLabel.setText(string(STRING_REQUEST_ERROR, string(request_status_string(e.status))));
                    tableLayout.show(tablePanel, CARD_REQ_ERROR);
                });
            }
            SwingUtilities.invokeLater(() -> {
                this.path = path;
                this.node = node;
                pathField.setText(path);
                parentButton.setEnabled(path.matches(".*[^/]/+[^/].*"));
                tableModel.fireTableDataChanged();
            });
        });
    }

    private static final class DirectoryEntry {
        public Node node;
        public byte type;
        public String name;
    }
}
