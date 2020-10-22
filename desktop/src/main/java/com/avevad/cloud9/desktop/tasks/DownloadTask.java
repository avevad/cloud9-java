package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.util.Holder;
import com.avevad.cloud9.core.util.Pair;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.avevad.cloud9.core.CloudCommon.*;
import static com.avevad.cloud9.desktop.DesktopCommon.*;

public class DownloadTask extends SimpleTaskBase {
    private final List<Pair<Node, String>> nodes;
    private final File destination;
    private final List<Pair<Node, File>> files = new ArrayList<>();
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private long doneSize = 0;

    public DownloadTask(CloudClient client, List<Pair<Node, String>> nodes, File destination) {
        super(client);
        this.nodes = nodes;
        this.destination = destination;
    }

    @Override
    public void run() throws IOException, CloudClient.RequestException {
        long totalSize = 0;
        for (Pair<Node, String> p : nodes) totalSize += countFiles(p.a, p.b, destination);
        long finalTotalSize = totalSize;
        if (isCancelled()) return;
        try {
            int done = 0;
            SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_DOWNLOADING, 0, files.size())));
            for (Pair<Node, File> nodeFilePair : files) {
                if (isCancelled()) return;
                Node node = nodeFilePair.a;
                File file = nodeFilePair.b;
                byte fd = client.openFD(node, FD_MODE_READ);
                FileOutputStream out = new FileOutputStream(file);
                client.longReadFD(fd, client.getNodeInfo(node).size, buffer, 0, BUFFER_SIZE, read -> {
                    waitResume();
                    out.write(buffer, 0, read);
                    doneSize += read;
                    SwingUtilities.invokeLater(() -> callback.setProgress((double) doneSize / finalTotalSize));
                });
                int finalDone = ++done;
                SwingUtilities.invokeLater(() -> callback.setStatus(string(STRING_DOWNLOADING, finalDone, files.size())));
                client.closeFD(fd);
            }
        } catch (IOException e) {
            if (!isCancelled()) throw e;
        }
    }

    private long countFiles(Node node, String name, File dir) throws IOException, CloudClient.RequestException {
        waitResume();
        if (isCancelled()) return 0;
        CloudClient.NodeInfo info = client.getNodeInfo(node);
        name = renameCopy(name, newName -> new File(dir, newName).exists());
        File file = new File(dir, name);
        if (info.type == NODE_TYPE_DIRECTORY) {
            file.mkdir();
            Holder<Long> size = new Holder<>(0L);
            client.listDirectory(node, (childNode, childName) -> size.value += countFiles(childNode, childName, file));
            return size.value;
        } else if (info.type == NODE_TYPE_FILE) {
            files.add(new Pair<>(node, file));
            return info.size;
        } else return 0;
    }

    @Override
    public void cancel() {
        super.cancel();
        client.close();
    }
}
