package com.locate.dangerousanimaltracker;

import java.util.concurrent.atomic.AtomicBoolean;

public class Once {
    private final AtomicBoolean done = new AtomicBoolean();

    public void run(Runnable task) {
        if (done.get()) return;
        if (done.compareAndSet(false, true)) {
            task.run();
        }
    }
}
