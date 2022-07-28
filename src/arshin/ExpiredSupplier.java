package arshin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class ExpiredSupplier<T> implements Supplier<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredSupplier.class);

    private final Supplier<T> original;
    private final long msTimeout;
    private final Object lock = new Object();

    private T cached;
    private long cachedTimestamp;

    ExpiredSupplier(long amount, TimeUnit unit, Supplier<T> original) {
        this.original = original;
        this.msTimeout = unit.toMillis(amount);
    }

    @Override
    public T get() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            if (cached == null || now - cachedTimestamp >= msTimeout) {
                if (cached instanceof AutoCloseable) {
                    AutoCloseable closeable = (AutoCloseable) cached;
                    try {
                        closeable.close();
                    } catch (Exception ex) {
                        LOGGER.warn("Error during close", ex);
                    }
                }
                cached = original.get();
                cachedTimestamp = now;
            }
            return cached;
        }
    }
}
