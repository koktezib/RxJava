package org.example.Interfaces;

@FunctionalInterface
public interface OnSubscribe<T> {
    void subscribe(Observer<? super T> observer);
}