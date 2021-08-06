package org.akhq.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class Debug {
    private static final Logger log = LoggerFactory.getLogger(Thread.currentThread().getStackTrace()[2].getClassName());

    private static String caller() {
        return Thread.currentThread().getStackTrace()[3].getClassName() + " -> " +
            Thread.currentThread().getStackTrace()[3].getMethodName() + " # " +
            Thread.currentThread().getStackTrace()[3].getLineNumber();
    }

    public static <T> String toJson(T arg) {
        String output;

        if (arg instanceof String) {
            output = (String) arg;
        } else if (arg instanceof byte[]) {
            output = new String((byte[]) arg);
        } else {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
            output = gson.toJson(arg);
        }

        return output;
    }

    public static <T> void time(String message, Runnable runnable, Object... arguments) {
        long start = System.currentTimeMillis();

        runnable.run();

        log.trace("[" + (System.currentTimeMillis() - start ) + " ms] " + message, arguments);
    }

    @SafeVarargs
    public static <T> void print(T... args) {
        System.out.println("\033[44;30m " + caller() + " \033[0m");

        for (Object arg : args) {
            System.out.println("\033[46;30m " + arg.getClass().getName() + " \033[0m \n" + toJson(arg));
        }
    }

    @SafeVarargs
    public static <T> void log(T... args) {
        log.trace("\033[44;30m " + caller() + " \033[0m");

        for (Object arg : args) {
            log.trace("\033[46;30m " + arg.getClass().getName() + " \033[0m " + toJson(arg));
        }
    }

    public static <T> T call(Callable<T> task, String format, List<String> arguments) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        T call;

        try {
            call = task.call();

            log.debug("{} ms -> " + format, (System.currentTimeMillis() - startTime), arguments);
            return call;
        } catch (InterruptedException | ExecutionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Error for " + format, exception);
        }
    }
}
