package org.example;

import org.example.Interfaces.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComputationScheduler implements Scheduler {
    private static final int N = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService pool = Executors.newFixedThreadPool(N);

    @Override
    public void execute(Runnable task) {
        pool.submit(task);
    }
}
