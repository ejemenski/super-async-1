package org.superasync;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract class Publisher<C, H, S> {
    final Collection<Wrapper> wrappers = new ConcurrentLinkedQueue<Wrapper>();
    private final H holder;
    final C initialValue;

    Publisher(C initialValue) {
        this.initialValue = initialValue;
        this.holder = initHolder();
    }

    abstract H initHolder();

    abstract boolean compareAndSet(H holder, C expect, C newValue);

    abstract C getAndSet(H holder, C value);

    abstract C getValue(H holder);

    C getValue() {
        return getValue(holder);
    }

    boolean publish(C value) {
        C old = getAndSet(holder, value);
        return onPublish(old, value);
    }

    boolean compareAndPublish(C expect, C revision) {
        if (!compareAndSet(holder, expect, revision)) {
            return false;
        }

        return onPublish(expect, revision);
    }

    private boolean onPublish(C old, C revision) {
        if (old != revision) {
            for (Wrapper w : wrappers) {
                w.update(revision);
            }
            return true;
        }
        return false;
    }

    Wrapper subscribe(S subscriber) {

        C currentValue = getValue(holder);
        Wrapper wrapper = new Wrapper(subscriber);
        wrapper.update(currentValue);

        wrappers.add(wrapper);

        wrapper.update(getValue(holder));

        return wrapper;
    }

    abstract void notifySubscriber(C value, Wrapper wrapper);

    class Wrapper implements Removable {

        private final S subscriber;
        private final H holder;

        Wrapper(S subscriber) {
            this.subscriber = subscriber;
            this.holder = initHolder();
        }

        void update(C value) {
            C old = getAndSet(holder, value);
            if (old != value) {
                notifySubscriber(value, this);
            }
        }

        S getObject() {
            return subscriber;
        }

        @Override
        public void remove() {
            wrappers.remove(this);
        }
    }
}
