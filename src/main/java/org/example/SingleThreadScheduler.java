package org.example;

import org.example.Interfaces.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadScheduler implements Scheduler {
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable task) {
        pool.submit(task);
    }
}
