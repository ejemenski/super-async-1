package org.superasync;

import java.util.concurrent.atomic.AtomicReference;

abstract class TypedPublisher<C, S> extends Publisher<C, AtomicReference<C>, S> {

  TypedPublisher(C initialValue) {
    super(initialValue);
  }

  @Override
  AtomicReference<C> initHolder() {
    return new AtomicReference<C>(initialValue);
  }

  @Override
  boolean compareAndSet(AtomicReference<C> holder, C expect, C newValue) {
    return holder.compareAndSet(expect, newValue);
  }

  @Override
  C getAndSet(AtomicReference<C> holder, C value) {
    return holder.getAndSet(value);
  }

  @Override
  C getValue(AtomicReference<C> holder) {
    return holder.get();
  }
}
