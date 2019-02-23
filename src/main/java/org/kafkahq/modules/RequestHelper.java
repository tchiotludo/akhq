package org.kafkahq.modules;

import lombok.extern.slf4j.Slf4j;
import org.kafkahq.controllers.AbstractController;

import javax.inject.Singleton;

@Singleton
@Slf4j
public class RequestHelper {
    public static AbstractController.Toast runnableToToast(ResultStatusResponseRunnable callable, String successMessage, String failedMessage) {
        AbstractController.Toast.ToastBuilder builder = AbstractController.Toast.builder();

        try {
            callable.run();
            builder
                .message(successMessage)
                .type(AbstractController.Toast.Type.success);
        } catch (Exception exception) {
            String cause = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();

            builder
                .title(failedMessage)
                .message(exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage())
                .type(AbstractController.Toast.Type.error);

            log.error(failedMessage != null ? failedMessage : cause, exception);
        }

        return builder.build();
    }

    public interface ResultStatusResponseRunnable {
        void run() throws Exception;
    }
}
