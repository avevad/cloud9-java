package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.CloudCommon.Node;
import com.avevad.cloud9.core.util.Holder;
import com.avevad.cloud9.desktop.TasksPanel;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.avevad.cloud9.core.CloudCommon.FD_MODE_WRITE;
import static com.avevad.cloud9.core.CloudCommon.NodeType;
import static com.avevad.cloud9.desktop.DesktopCommon.*;

public class UploadTask implements TasksPanel.TaskController {
    private final CloudClient dataClient;
    private boolean cancelled = false, suspended = false;
    private final Object resumeNotifier = new Object();
    private File[] files;
    private Node destination;
    private long sizeTotal = 0;
    private long sizeDone = 0;
    private TasksPanel.TaskCallback callback = null;
    private int filesTotal = 0;
    private int filesSent = 0;
    byte[] buffer = null;

    public UploadTask(CloudClient controlClient, File[] files, Node destination) {
        this.files = files;
        this.destination = destination;
        CloudClient client = null;
        try {
            client = new CloudClient(controlClient);
        } catch (IOException | CloudClient.ProtocolException | CloudClient.RequestException | CloudClient.InitException e) {
        }
        dataClient = client;
    }

    public Runnable start(TasksPanel.TaskCallback callback) {
        if (buffer != null) throw new IllegalStateException("already started");
        buffer = new byte[BUFFER_SIZE];
        this.callback = callback;
        return () -> {
            if (dataClient == null) {
                SwingUtilities.invokeLater(() -> {
                    callback.setStatus(string(STRING_ERROR));
                });
            } else {
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_PREPARING)));
                try {
                    Holder<Integer> count = new Holder<>(0);
                    for (File file : files) {
                        sizeTotal += countFiles(file, count);
                    }
                    filesTotal = count.value;
                    updateStatus();
                    for (File file : files) {
                        uploadFiles(file, destination);
                    }
                    SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_COMPLETED)));
                } catch (FileNotFoundException e) {
                    SwingUtilities.invokeLater(() -> {
                        callback.setStatus(string(STRING_ERROR));
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        callback.setStatus(cancelled ? string(STRING_CANCELLED) : string(STRING_CONNECTION_LOST, e.getLocalizedMessage()));
                    });
                } catch (CloudClient.RequestException e) {
                    SwingUtilities.invokeLater(() -> {
                        callback.setStatus(string(STRING_REQUEST_ERROR, string(requestStatusString(e.status))));
                    });
                }
            }
            SwingUtilities.invokeLater(callback::setFinished);
        };
    }

    private long countFiles(File file, Holder<Integer> count) {
        waitSuspend();
        if (file.isDirectory()) {
            long totalSize = 0;
            for (File child : file.listFiles()) totalSize += countFiles(child, count);
            return totalSize;
        } else if (file.isFile()) {
            count.value++;
            return file.length();
        } else return 0;
    }

    private void uploadFiles(File file, Node parent) throws IOException, CloudClient.RequestException {
        if (file.isDirectory()) {
            Node dir = dataClient.makeNode(parent, file.getName(), NodeType.DIRECTORY);
            for (File child : file.listFiles()) uploadFiles(child, dir);
        } else if (file.isFile()) {
            Node node = dataClient.makeNode(parent, file.getName(), NodeType.FILE);
            byte fd = dataClient.openFD(node, FD_MODE_WRITE);
            FileInputStream fis = new FileInputStream(file);
            dataClient.longWriteFD(fd, file.length(), buffer, 0, BUFFER_SIZE, () -> updateProgress(fis.read(buffer, 0, BUFFER_SIZE)));
            filesSent++;
            updateStatus();
            dataClient.closeFD(fd);
        }
    }

    private int updateProgress(int sent) {
        waitSuspend();
        sizeDone += sent;
        SwingUtilities.invokeLater(() -> callback.setProgress((double) sizeDone / sizeTotal));
        return sent;
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_UPLOADING, filesSent, filesTotal)));
    }

    private void waitSuspend() {
        synchronized (resumeNotifier) {
            if (suspended) SwingUtilities.invokeLater(() -> callback.setSuspended(true));
            while (suspended) {
                try {
                    resumeNotifier.wait();
                } catch (InterruptedException ignored) {
                }
            }
            SwingUtilities.invokeLater(() -> callback.setSuspended(false));
        }
    }

    @Override
    public void suspend() {
        synchronized (resumeNotifier) {
            suspended = true;
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
        dataClient.close();
    }

    @Override
    public void resume() {
        synchronized (resumeNotifier) {
            suspended = false;
            resumeNotifier.notifyAll();
        }
    }
}
