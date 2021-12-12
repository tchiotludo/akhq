package org.akhq.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.ApiException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import jakarta.inject.Singleton;

@Singleton
@Slf4j
public class Logger {
    public static <T> T call(Callable<T> task, String format, List<String> arguments) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            call = task.call();

            log.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (InterruptedException | ExecutionException exception) {
            if (exception.getCause() instanceof ApiException) {
                throw (ApiException) exception.getCause();
            }

            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }

    public static  <T> T call(KafkaFuture<T> future, String format) throws ApiException, ExecutionException{
        return Logger.call(future, format, null);
    }

    public static  <T> T call(KafkaFuture<T> future, String format, List<String> arguments) throws ExecutionException, ApiException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            call = future.get();

            log.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof ApiException) {
                throw (ApiException) exception.getCause();
            }

            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }
}
