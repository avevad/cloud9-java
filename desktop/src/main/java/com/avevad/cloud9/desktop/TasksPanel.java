package com.avevad.cloud9.desktop;

import com.avevad.cloud9.core.util.Holder;

import javax.swing.*;
import java.awt.*;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public final class TasksPanel {
    public final JPanel panel = new JPanel();
    private final JPanel taskPanel = new JPanel();
    private final JLabel header = new JLabel(string(STRING_TASKS));

    private int nextRow = 0;

    private final Font taskNameFont = header.getFont().deriveFont(16f).deriveFont(Font.BOLD);
    private final ImageIcon suspendIcon, resumeIcon, cancelIcon, removeIcon;

    public TasksPanel() {
        panel.setLayout(new BorderLayout());

        header.setFont(header.getFont().deriveFont(20f));
        header.setHorizontalAlignment(SwingConstants.CENTER);
        header.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(header, BorderLayout.NORTH);

        JScrollPane scroller = new JScrollPane();
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroller, BorderLayout.CENTER);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BorderLayout());
        scroller.setViewportView(wrapper);

        taskPanel.setLayout(new GridBagLayout());
        taskPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        wrapper.add(taskPanel, BorderLayout.NORTH);

        panel.setMinimumSize(new Dimension(panel.getFontMetrics(taskNameFont).getHeight() * 16, panel.getMinimumSize().height));

        suspendIcon = resizeHeight(icon(ICON_PAUSE), panel.getFontMetrics(taskNameFont).getHeight());
        resumeIcon = resizeHeight(icon(ICON_PLAY), panel.getFontMetrics(taskNameFont).getHeight());
        cancelIcon = resizeHeight(icon(ICON_CROSS), panel.getFontMetrics(taskNameFont).getHeight());
        removeIcon = resizeHeight(icon(ICON_MINUS), panel.getFontMetrics(taskNameFont).getHeight());
    }

    public TaskCallback addTask(String name, TaskController controller) {
        JPanel row = new JPanel();
        row.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTH;
        c.gridwidth = c.gridheight = 1;
        c.gridx = 0;
        c.gridy = nextRow++;
        taskPanel.add(row, c);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(taskNameFont);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = c.weighty = 1;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.gridx = c.gridy = 0;
        row.add(nameLabel, c);

        JLabel statusLabel = new JLabel(string(STRING_PENDING));
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 2;
        row.add(statusLabel, c);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        c.gridx = 0;
        c.gridy = 3;
        row.add(progressBar, c);

        Holder<Boolean> suspended = new Holder<>(false);
        JButton suspendResumeButton = new JButton(suspendIcon);
        makeBorderless(suspendResumeButton);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 0;
        row.add(suspendResumeButton, c);
        suspendResumeButton.addActionListener(e -> {
            if (suspended.value) controller.resume();
            else controller.suspend();
        });

        Holder<Boolean> finished = new Holder<>(false);
        JButton cancelRemoveButton = new JButton(cancelIcon);
        makeBorderless(cancelRemoveButton);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 0;
        row.add(cancelRemoveButton, c);
        cancelRemoveButton.addActionListener(e -> {
            if (finished.value) {
                taskPanel.remove(row);
                taskPanel.revalidate();
            } else {
                if (suspended.value) controller.resume();
                controller.cancel();
            }
        });


        taskPanel.revalidate();

        return new TaskCallback() {
            @Override
            public void setStatus(String status) {
                statusLabel.setText(status);
            }

            @Override
            public void setProgress(double progress) {
                progressBar.setValue((int) (progress * 100));
            }

            @Override
            public void setFinished() {
                finished.value = true;
                cancelRemoveButton.setIcon(removeIcon);
                suspendResumeButton.setEnabled(false);
            }

            @Override
            public void setSuspended(boolean isSuspended) {
                suspended.value = isSuspended;
                suspendResumeButton.setIcon(isSuspended ? resumeIcon : suspendIcon);
            }
        };
    }

    public interface TaskController {
        void suspend();

        void cancel();

        void resume();
    }

    public interface TaskCallback {
        void setStatus(String status);

        void setProgress(double progress);

        void setFinished();

        void setSuspended(boolean suspended);
    }
}
