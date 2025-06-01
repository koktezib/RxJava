package org.example.test;

import org.example.Interfaces.Observer;
import org.example.Observable;
import org.example.Operators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class MapFilterTest {

    @Test
    void testMapOperator() throws InterruptedException {
        // Создадим Observable, отдающий 1, 2, 3
        Observable<Integer> source = Observable.create(observer -> {
            observer.onNext(1);
            observer.onNext(2);
            observer.onNext(3);
            observer.onComplete();
        });

        // Применим map: каждый x -> x * 10
        Observable<Integer> mapped = Operators.map(source, x -> x * 10);

        List<Integer> result = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mapped.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                result.add(item);
            }

            @Override
            public void onError(Throwable t) {
                fail("Ошибка не ожидалась");
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
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
}
