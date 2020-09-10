package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.*;
import com.avevad.cloud9.core.util.Holder;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public final class HomeTabPanel {
    public final WindowController windowController;
    public final JPanel panel = new JPanel();

    public HomeTabPanel(WindowController windowController) {
        this.windowController = windowController;

        GridBagConstraints c = new GridBagConstraints();
        panel.setLayout(new GridBagLayout());

        JLabel quickHeader = new JLabel(string(STRING_QUICK_CONNECTION));
        quickHeader.setFont(quickHeader.getFont().deriveFont(23f));
        quickHeader.setHorizontalAlignment(SwingConstants.CENTER);
        quickHeader.setVerticalAlignment(SwingConstants.TOP);
        c.gridx = c.gridy = 0;
        c.gridwidth = 4;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(quickHeader, c);

        JLabel quickHostLabel = new JLabel(string(STRING_HOST));
        quickHostLabel.setHorizontalAlignment(SwingConstants.LEFT);
        quickHostLabel.setVerticalAlignment(SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickHostLabel, c);

        JTextField quickHostField = new JTextField();
        if(getConfig().lastQuickHost != null) quickHostField.setText(getConfig().lastQuickHost);
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickHostField, c);

        JLabel quickPortLabel = new JLabel(string(STRING_PORT));
        quickPortLabel.setHorizontalAlignment(SwingConstants.LEFT);
        quickPortLabel.setVerticalAlignment(SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickPortLabel, c);

        JTextField quickPortField = new JTextField("" + CloudCommon.CLOUD_DEFAULT_PORT);
        if(getConfig().lastQuickPort != null) quickPortField.setText(String.valueOf(getConfig().lastQuickPort));
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickPortField, c);

        JLabel quickLoginPasswordLabel = new JLabel(string(STRING_LOGIN_AND_PASSWORD));
        quickLoginPasswordLabel.setHorizontalAlignment(SwingConstants.LEFT);
        quickLoginPasswordLabel.setVerticalAlignment(SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickLoginPasswordLabel, c);

        JTextField quickLoginField = new JTextField(8);
        if(getConfig().lastQuickLogin != null) quickLoginField.setText(getConfig().lastQuickLogin);
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickLoginField, c);

        JPasswordField quickPasswordField = new JPasswordField(16);
        c.gridx = 3;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickPasswordField, c);

        JLabel quickSecureLabel = new JLabel(string(STRING_SECURE_CONNECTION));
        quickSecureLabel.setHorizontalAlignment(SwingConstants.LEFT);
        quickSecureLabel.setVerticalAlignment(SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickSecureLabel, c);

        JCheckBox quickSecureCheck = new JCheckBox();
        if(getConfig().lastQuickSecure != null) quickSecureCheck.setSelected(getConfig().lastQuickSecure);
        else quickSecureCheck.setSelected(true);
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(quickSecureCheck, c);

        JButton quickButton = new JButton(string(STRING_CONNECT));
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 4;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(quickButton, c);

        quickPortField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                check();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                check();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                check();
            }

            private void check() {
                try {
                    int port = Integer.parseInt(quickPortField.getText());
                    if (port < 0 || port > 65535) quickButton.setEnabled(false);
                    else quickButton.setEnabled(true);
                } catch (NumberFormatException e) {
                    quickButton.setEnabled(false);
                }
            }
        });

        ActionListener quickListener = e -> {
            if (quickButton.isEnabled()) {
                quickButton.setEnabled(false);
                quickButton.setText(string(STRING_CONNECTING));
                windowController.mainController.connectionQueue.submit(() -> {
                    getConfig().lastQuickHost = quickHostField.getText();
                    getConfig().lastQuickPort = Integer.parseInt(quickPortField.getText());
                    getConfig().lastQuickLogin = quickLoginField.getText();
                    getConfig().lastQuickSecure = quickSecureCheck.isSelected();
                    saveConfig();
                    Holder<String> error = new Holder<>();
                    try {
                        CloudConnection connection;
                        if(quickSecureCheck.isSelected()) connection = new SSLConnection(quickHostField.getText(), Integer.parseInt(quickPortField.getText()));
                        else connection = new TCPConnection(quickHostField.getText(), Integer.parseInt(quickPortField.getText()));
                        CloudClient client = new CloudClient(connection, quickLoginField.getText(), () -> new String(quickPasswordField.getPassword()));
                    } catch (UnknownHostException ex) {
                        error.value = string(STRING_UNKNOWN_HOST, ex.getMessage());
                    } catch (IOException ex) {
                        error.value = string(STRING_NET_CONNECTION_FAILED, ex.getMessage());
                    } catch (CloudClient.InitException ex) {
                        error.value = string(STRING_AUTH_FAILED, string(init_status_string(ex.status)));
                    } catch (CloudClient.ProtocolException ex) {
                        error.value = string(STRING_NEGOTIATION_ERROR);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (error.value != null)
                            JOptionPane.showMessageDialog(windowController.frame,
                                    string(STRING_CLOUD_CONNECTION_FAILED, error.value),
                                    string(STRING_ERROR), JOptionPane.ERROR_MESSAGE);
                        quickButton.setEnabled(true);
                        quickButton.setText(string(STRING_CONNECT));
                    });
                });
            }
        };

        quickHostField.addActionListener(quickListener);
        quickLoginField.addActionListener(quickListener);
        quickPasswordField.addActionListener(quickListener);
        quickPortField.addActionListener(quickListener);
        quickButton.addActionListener(quickListener);
    }
}
