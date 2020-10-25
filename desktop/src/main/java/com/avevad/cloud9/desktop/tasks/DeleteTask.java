package com.avevad.cloud9.desktop.tasks;

import com.avevad.cloud9.core.CloudClient;

import javax.swing.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.avevad.cloud9.core.CloudCommon.*;
import static com.avevad.cloud9.desktop.DesktopCommon.STRING_REMOVING;
import static com.avevad.cloud9.desktop.DesktopCommon.string;

public class DeleteTask extends SimpleTaskBase {
    private Node[] nodes;
    private List<Node> nodesToDelete = new LinkedList<>();

    public DeleteTask(CloudClient client, Node[] nodes) {
        super(client);
        this.nodes = nodes;
    }

    public void run() throws IOException, CloudClient.RequestException {
        for (Node node : nodes) countFiles(node);
        int removed = 0;
        for (Node node : nodesToDelete) {
            if (isCancelled()) return;
            waitResume();
            client.removeNode(node);
            removed++;
            int finalRemoved = removed;
            SwingUtilities.invokeLater(() -> {
                callback.setProgress((double) finalRemoved / nodesToDelete.size());
                callback.setStatus(string(STRING_REMOVING, finalRemoved, nodesToDelete.size()));
            });
        }
    }

    private void countFiles(Node node) throws IOException, CloudClient.RequestException {
        if (isCancelled()) return;
        waitResume();
        byte type = client.getNodeInfo(node).type;
        if (type == NODE_TYPE_DIRECTORY) {
            client.listDirectory(node, (child, name) -> countFiles(child));
            nodesToDelete.add(node);
        } else if (type == NODE_TYPE_FILE) nodesToDelete.add(node);
    }
}
