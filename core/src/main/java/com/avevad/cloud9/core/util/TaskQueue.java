package com.avevad.cloud9.core.util;

import java.util.LinkedList;
import java.util.Queue;

public final class TaskQueue {
    private final Queue<Runnable> queue = new LinkedList<>();
    private boolean stop = false;
    private final String name;

    public TaskQueue(String name, int threadCount) {
        this.name = name;
        for (int i = 0; i < threadCount; i++)
            new Thread(() -> {
                while (true) {
                    Runnable task;
                    synchronized (queue) {
                        while (queue.isEmpty() && !stop) {
                            try {
                                queue.wait();
                            } catch (InterruptedException ignored) {
                            }
                        }
                        if (stop && queue.isEmpty()) return;
                        task = queue.peek();
                    }
                    task.run();
                    synchronized (queue) {
                        queue.poll();
                    }
                }
            }, this + "[" + i + "]").start();
    }

    public TaskQueue(String name) {
        this(name, 1);
    }

    public boolean isEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

    public void stop() {
        synchronized (queue) {
            if (stop) throw new IllegalStateException();
            stop = true;
            queue.notifyAll();
            queue.clear();
        }
    }

    public void submit(Runnable task) {
        synchronized (queue) {
            if (stop) throw new IllegalStateException();
            queue.add(task);
            queue.notifyAll();
        }
    }

    @Override
    public String toString() {
        return name + "TaskQueue";
    }
}
