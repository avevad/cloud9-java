package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;
import com.avevad.cloud9.core.util.Holder;

import javax.swing.*;
import java.io.IOException;

import static com.avevad.cloud9.core.CloudCommon.*;
import static com.avevad.cloud9.desktop.DesktopCommon.STRING_REMOVING;
import static com.avevad.cloud9.desktop.DesktopCommon.string;

public class DeleteTask extends SimpleTaskBase {
    private Node[] nodes;
    private int filesCount = 0;
    private int filesRemoved = 0;

    public DeleteTask(CloudClient client, Node[] nodes) {
        super(client);
        this.nodes = nodes;
    }

    public void run() throws IOException, CloudClient.RequestException {
        for (Node node : nodes) filesCount += countFiles(node);
        if (isCancelled()) return;
        for (Node node : nodes) deleteFiles(node);
    }

    private int countFiles(Node node) throws IOException, CloudClient.RequestException {
        waitResume();
        if (isCancelled()) return 0;
        byte type = client.getNodeInfo(node).type;
        if (type == NODE_TYPE_DIRECTORY) {
            Holder<Integer> filesCount = new Holder<>(1);
            client.listDirectory(node, (child, name) -> filesCount.value += countFiles(child));
            return filesCount.value;
        } else if (type == NODE_TYPE_FILE) return 1;
        else return 0;
    }

    private void deleteFiles(Node node) throws IOException, CloudClient.RequestException {
        waitResume();
        if (isCancelled()) return;
        byte type = client.getNodeInfo(node).type;
        if (type == NODE_TYPE_DIRECTORY) {
            client.listDirectory(node, (child, name) -> deleteFiles(child));
            client.removeNode(node);
        } else if (type == NODE_TYPE_FILE) client.removeNode(node);
        else return;
        SwingUtilities.invokeLater(() -> {
            filesRemoved++;
            callback.setProgress((double) filesRemoved / filesCount);
            callback.setStatus(string(STRING_REMOVING, filesRemoved, filesCount));
        });
    }
}
