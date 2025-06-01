package org.example;

import org.example.Interfaces.Disposable;
import org.example.Interfaces.Observer;
import org.example.Interfaces.OnSubscribe;
import org.example.Interfaces.Scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class Observable<T> {

    private final OnSubscribe<T> onSubscribe;

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> Observable<T> create(OnSubscribe<T> onSubscribe) {
        return new Observable<>(onSubscribe);
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return Observable.create(downstream -> {
            Observer<T> threadedObserver = new Observer<T>() {
                @Override
                public void onNext(T item) {
                    scheduler.execute(() -> downstream.onNext(item));
                }

                @Override
                public void onError(Throwable t) {
                    scheduler.execute(() -> downstream.onError(t));
                }

                @Override
                public void onComplete() {
                    scheduler.execute(downstream::onComplete);
                }
            };

            Observable.this.subscribe(threadedObserver);
        });
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return Observable.create(downstream -> {
            scheduler.execute(() -> {
                try {
                    onSubscribe.subscribe(downstream);
                } catch (Throwable ex) {
                    downstream.onError(ex);
                }
            });
        });
    }

    public Disposable subscribe(final Observer<? super T> downstream) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observer<T> safeObserver = new Observer<T>() {
            @Override
            public void onNext(T item) {
                if (!disposed.get()) {
                    downstream.onNext(item);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!disposed.get()) {
                    downstream.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!disposed.get()) {
                    downstream.onComplete();
                }
            }
        };

        try {
            onSubscribe.subscribe(safeObserver);
        } catch (Throwable ex) {
            safeObserver.onError(ex);
        }

        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }
}
