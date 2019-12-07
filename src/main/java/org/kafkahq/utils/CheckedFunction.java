package org.kafkahq.utils;

import java.util.concurrent.ExecutionException;

@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T t) throws ExecutionException, InterruptedException;
}