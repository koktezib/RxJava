package org.example.test;

import org.example.ComputationScheduler;
import org.example.IOThreadScheduler;
import org.example.Interfaces.Observer;
import org.example.Observable;
import org.example.SingleThreadScheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    @Test
    void testSubscribeOnIO() throws InterruptedException {
        AtomicBoolean onNextThreadDifferent = new AtomicBoolean(false);
        String mainThread = Thread.currentThread().getName();

        Observable<Integer> source = Observable.create(obs -> {
            if (!Thread.currentThread().getName().equals(mainThread)) {
                onNextThreadDifferent.set(true);
            }
            obs.onNext(42);
            obs.onComplete();
        });

        CountDownLatch latch = new CountDownLatch(1);
        source
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { }
                    @Override public void onError(Throwable t) { fail(t); }
                    @Override public void onComplete() { latch.countDown(); }
                });

        latch.await();
        assertTrue(onNextThreadDifferent.get(), "Ожидалось, что эмиссия произойдёт не в основном потоке");
    }

    @Test
    void testObserveOnComputation() throws InterruptedException {
        AtomicBoolean onNextInComputation = new AtomicBoolean(false);

        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onComplete();
        });

        CountDownLatch latch = new CountDownLatch(1);
        source
                .observeOn(new ComputationScheduler())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onNext(Integer item) {
                        if (Thread.currentThread().getName().startsWith("pool-")) {
                            onNextInComputation.set(true);
                        }
                    }
                    @Override public void onError(Throwable t) { fail(t); }
                    @Override public void onComplete() { latch.countDown(); }
                });

        latch.await();
        assertTrue(onNextInComputation.get(), "Ожидалось, что onNext будет вызван в ComputationScheduler");
    }

    @Test
    void testSingleThreadSchedulerSerialExecution() throws InterruptedException {
        SingleThreadScheduler scheduler = new SingleThreadScheduler();
        String[] threads = new String[2];
        CountDownLatch latch = new CountDownLatch(2);

        scheduler.execute(() -> {
            threads[0] = Thread.currentThread().getName();
            latch.countDown();
        });
        scheduler.execute(() -> {
            threads[1] = Thread.currentThread().getName();
            latch.countDown();
        });
        latch.await();
        assertNotNull(threads[0]);
        assertEquals(threads[0], threads[1], "Должны выполняться на одном и том же потоке");
    }
}
