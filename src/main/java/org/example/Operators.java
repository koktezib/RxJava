package org.example;

import org.example.Interfaces.Disposable;
import org.example.Interfaces.Observer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

public class Operators {

    public static <T, R> Observable<R> map(Observable<T> source, Function<? super T, ? extends R> mapper) {
        return Observable.create(downstreamObserver -> {
            source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    R mapped;
                    try {
                        mapped = mapper.apply(item);
                    } catch (Throwable ex) {
                        downstreamObserver.onError(ex);
                        return;
                    }
                    downstreamObserver.onNext(mapped);
                }

                @Override
                public void onError(Throwable t) {
                    downstreamObserver.onError(t);
                }

                @Override
                public void onComplete() {
                    downstreamObserver.onComplete();
                }
            });
        });
    }

    public static <T> Observable<T> filter(Observable<T> source, Predicate<? super T> predicate) {
        return Observable.create(downstream -> {
            source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    boolean test;
                    try {
                        test = predicate.test(item);
                    } catch (Throwable ex) {
                        downstream.onError(ex);
                        return;
                    }
                    if (test) {
                        downstream.onNext(item);
                    }
                    // Если не проходит predicate, просто игнорируем элемент.
                }

                @Override
                public void onError(Throwable t) {
                    downstream.onError(t);
                }

                @Override
                public void onComplete() {
                    downstream.onComplete();
                }
            });
        });
    }

    public static <T, R> Observable<R> flatMap(
            Observable<T> source,
            Function<? super T, Observable<? extends R>> mapper
    ) {
        return Observable.create(downstream -> {
            Set<Disposable> innerDisposables = Collections.newSetFromMap(new ConcurrentHashMap<>());
            AtomicBoolean sourceCompleted = new AtomicBoolean(false);

            source.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    Observable<? extends R> inner;
                    try {
                        inner = mapper.apply(item);
                    } catch (Throwable ex) {
                        downstream.onError(ex);
                        return;
                    }

                    final boolean[] innerDone = { false };
                    final Disposable[] dispRef = new Disposable[1];

                    Observer<R> innerObserver = new Observer<R>() {
                        @Override
                        public void onNext(R r) {
                            downstream.onNext(r);
                        }
                        @Override
                        public void onError(Throwable t) {
                            downstream.onError(t);
                            if (dispRef[0] != null) {
                                innerDisposables.remove(dispRef[0]);
                            }
                        }
                        @Override
                        public void onComplete() {
                            innerDone[0] = true;
                            if (dispRef[0] != null) {
                                innerDisposables.remove(dispRef[0]);
                                if (sourceCompleted.get() && innerDisposables.isEmpty()) {
                                    downstream.onComplete();
                                }
                            }
                        }
                    };

                    dispRef[0] = inner.subscribe(innerObserver);
                    innerDisposables.add(dispRef[0]);

                    if (innerDone[0]) {
                        innerDisposables.remove(dispRef[0]);
                        if (sourceCompleted.get() && innerDisposables.isEmpty()) {
                            downstream.onComplete();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    downstream.onError(t);
                }

                @Override
                public void onComplete() {
                    sourceCompleted.set(true);
                    if (innerDisposables.isEmpty()) {
                        downstream.onComplete();
                    }
                }
            });
        });
    }




}
