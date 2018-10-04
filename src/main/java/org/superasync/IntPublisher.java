package org.superasync;

import java.util.concurrent.atomic.AtomicInteger;

abstract class IntPublisher<S> extends Publisher<Integer, AtomicInteger, S> {

    IntPublisher(Integer initialValue) {
        super(initialValue);
    }

    @Override
    AtomicInteger initHolder() {
        return new AtomicInteger(initialValue);
    }

    @Override
    boolean compareAndSet(AtomicInteger holder, Integer expect, Integer newValue) {
        return holder.compareAndSet(expect, newValue);
    }

    @Override
    Integer getAndSet(AtomicInteger holder, Integer value) {
        return holder.getAndSet(value);
    }

    @Override
    Integer getValue(AtomicInteger holder) {
        return holder.get();
    }

}
