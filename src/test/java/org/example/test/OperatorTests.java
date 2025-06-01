package org.example.test;

import org.example.Interfaces.Observer;
import org.example.Observable;
import org.example.Operators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class OperatorTests {

    @Test
    void simpleObservableTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Observable<Integer> src = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });
        src.subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) { System.out.println("item = " + item); }
            @Override public void onError(Throwable t) { t.printStackTrace(); }
            @Override public void onComplete() { System.out.println("COMPLETE!"); latch.countDown(); }
        });
        latch.await();
    }


    @Test
    void testMapOperator() throws InterruptedException {
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        Observable<Integer> mapped = Operators.map(source, x -> x * 10);

        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) { result.add(item); }
            @Override public void onError(Throwable t) { fail("Ошибка не ожидалась"); }
            @Override public void onComplete() { latch.countDown(); }
        });

        latch.await();
        assertEquals(List.of(10, 20, 30), result);
    }

    @Test
    void testFilterOperator() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            for (int i = 1; i <= 5; i++) obs.onNext(i);
            obs.onComplete();
        });

        Observable<Integer> evenOnly = Operators.filter(source, x -> x % 2 == 0);

        List<Integer> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        evenOnly.subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) { received.add(item); }
            @Override public void onError(Throwable t) { fail("Неожиданная ошибка"); }
            @Override public void onComplete() { latch.countDown(); }
        });

        latch.await();
        assertEquals(List.of(2, 4), received);
    }

    @Test
    void testFlatMapSimple() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            obs.onNext(1);
            obs.onNext(2);
            obs.onComplete();
        });

        Observable<Integer> flatMapped = Operators.flatMap(source, n -> Observable.create(o -> {
            o.onNext(n * 10);
            o.onNext(n * 100);
            o.onComplete();
        }));

        List<Integer> collected = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        flatMapped.subscribe(new Observer<Integer>() {
            @Override public void onNext(Integer item) { collected.add(item); }
            @Override public void onError(Throwable t) { fail(t); }
            @Override public void onComplete() { latch.countDown(); }
        });

        latch.await();

        assertTrue(collected.contains(10));
        assertTrue(collected.contains(100));
        assertTrue(collected.contains(20));
        assertTrue(collected.contains(200));
        assertEquals(4, collected.size());
    }
}
