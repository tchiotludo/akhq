package org.kafkahq.utils;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Singleton
@Slf4j
public class Lock {
    public static <T> T call(Callable<T> task, String format, List<String> arguments) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            String lockKey = format + String.join("-", arguments == null ? Collections.emptyList() : arguments);
            synchronized(lockKey.intern()) {
                call = task.call();
            }

            log.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (InterruptedException | ExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }
}
