package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.CloudCommon.Node;
import com.avevad.cloud9.core.util.Holder;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.avevad.cloud9.core.CloudCommon.FD_MODE_WRITE;
import static com.avevad.cloud9.core.CloudCommon.NodeType;
import static com.avevad.cloud9.desktop.DesktopCommon.*;

public class UploadTask extends SimpleTaskBase {
    private final File[] files;
    private final Node destination;
    private long sizeTotal = 0;
    private long sizeDone = 0;
    private int filesTotal = 0;
    private int filesSent = 0;
    byte[] buffer = new byte[BUFFER_SIZE];

    public UploadTask(CloudClient client, File[] files, Node destination) {
        super(client);
        this.files = files;
        this.destination = destination;
    }

    @Override
    public void cancel() {
        super.cancel();
        client.close();
    }

    public void run() throws IOException, CloudClient.RequestException {
        Holder<Integer> count = new Holder<>(0);
        for (File file : files) {
            sizeTotal += countFiles(file, count);
        }
        if (isCancelled()) return;
        filesTotal = count.value;
        updateStatus();
        try {
            for (File file : files) {
                uploadFiles(file, destination, true);
            }
        } catch (IOException e) {
            if (!isCancelled()) throw e;
        }
    }

    private long countFiles(File file, Holder<Integer> count) {
        waitResume();
        if (isCancelled()) return 0;
        if (file.isDirectory()) {
            long totalSize = 0;
            for (File child : file.listFiles()) totalSize += countFiles(child, count);
            return totalSize;
        } else if (file.isFile()) {
            count.value++;
            return file.length();
        } else return 0;
    }

    private void uploadFiles(File file, Node parent, boolean first) throws IOException, CloudClient.RequestException {
        waitResume();
        if (isCancelled()) return;
        String name = file.getName();
        if (first) {
            Set<String> names = new HashSet<>();
            client.listDirectory(parent, (childNode, childName) -> names.add(childName));
            name = renameCopy(name, names::contains);
        }
        if (file.isDirectory()) {
            Node dir = client.makeNode(parent, name, NodeType.DIRECTORY);
            for (File child : file.listFiles()) uploadFiles(child, dir, false);
        } else if (file.isFile()) {
            Node node = client.makeNode(parent, name, NodeType.FILE);
            byte fd = client.openFD(node, FD_MODE_WRITE);
            FileInputStream fis = new FileInputStream(file);
            client.longWriteFD(fd, file.length(), buffer, 0, () -> updateProgress(fis.read(buffer, 0, BUFFER_SIZE)));
            filesSent++;
            updateStatus();
            client.closeFD(fd);
        }
    }

    private int updateProgress(int sent) {
        waitResume();
        sizeDone += sent;
        SwingUtilities.invokeLater(() -> callback.setProgress((double) sizeDone / sizeTotal));
        return sent;
    }

    private void updateStatus() {
        SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_UPLOADING, filesSent, filesTotal)));
    }
}
