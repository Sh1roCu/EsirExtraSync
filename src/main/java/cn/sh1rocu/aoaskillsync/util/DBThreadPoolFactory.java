package cn.sh1rocu.aoaskillsync.util;


import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DBThreadPoolFactory implements ThreadFactory {
    private final AtomicInteger threadIdx = new AtomicInteger(0);

    private final String threadNamePrefix;

    public DBThreadPoolFactory(String Prefix) {
        threadNamePrefix = Prefix;
    }

    @Override
    public Thread newThread(@Nonnull Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadNamePrefix + "-thread-" + threadIdx.getAndIncrement());
        return thread;
    }


}