package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.desktop.TasksPanel;

import javax.swing.*;
import java.io.IOException;

import static com.avevad.cloud9.desktop.DesktopCommon.*;

public abstract class SimpleTaskBase implements TasksPanel.TaskController {
    protected final CloudClient client;
    private boolean suspended = false;
    private boolean cancelled = false;
    private final Object lock = new Object();
    protected TasksPanel.TaskCallback callback = null;
    private final Runnable cancelHandler;

    public SimpleTaskBase(CloudClient client, Runnable cancelHandler) {
        this.cancelHandler = cancelHandler;
        CloudClient newClient = null;
        try {
            newClient = new CloudClient(client);
        } catch (IOException | CloudClient.InitException | CloudClient.ProtocolException | CloudClient.RequestException ignored) {
        }
        this.client = newClient;
    }

    public SimpleTaskBase(CloudClient client) {
        this(client, () -> {
        });
    }

    public abstract void run() throws IOException, CloudClient.RequestException;

    public Runnable start(TasksPanel.TaskCallback callback) {
        if (this.callback != null) throw new IllegalStateException("already started");
        this.callback = callback;
        return () -> {
            if (client == null) {
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_ERROR)));
            } else try {
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_PREPARING)));
                run();
                client.disconnect();
                SwingUtilities.invokeLater(() -> callback.setStatus(string(cancelled ? STRING_CANCELLED : STRING_COMPLETED)));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_CONNECTION_LOST, e.getLocalizedMessage())));
            } catch (CloudClient.RequestException e) {
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_REQUEST_ERROR, string(initStatusString(e.status)))));
            }
            SwingUtilities.invokeLater(callback::setFinished);
        };
    }

    public void waitResume() {
        synchronized (lock) {
            if (suspended) SwingUtilities.invokeLater(() -> callback.setSuspended(true));
            while (suspended) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
            SwingUtilities.invokeLater(() -> callback.setSuspended(false));
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void suspend() {
        synchronized (lock) {
            suspended = true;
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public void resume() {
        synchronized (lock) {
            suspended = false;
            lock.notifyAll();
        }
    }
}
