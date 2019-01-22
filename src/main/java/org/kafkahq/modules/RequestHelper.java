package org.kafkahq.modules;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import lombok.Getter;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Request;
import org.kafkahq.controllers.AbstractController;
import org.kafkahq.controllers.TopicController;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@Singleton
public class RequestHelper implements Jooby.Module {
    private transient static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    public static RecordRepository.Options buildRecordRepositoryOptions(Request request) {
        RecordRepository.Options options = new RecordRepository.Options(
            request.param("cluster").value(),
            request.param("topic").value()
        );

        request.param("after").toOptional().ifPresent(options::setAfter);
        request.param("partition").toOptional(Integer.class).ifPresent(options::setPartition);
        request.param("sort").toOptional(RecordRepository.Options.Sort.class).ifPresent(options::setSort);
        request.param("timestamp").toOptional(String.class).ifPresent(s -> options.setTimestamp(Instant.parse(s).toEpochMilli()));
        request.param("search").toOptional(String.class).ifPresent(options::setSearch);

        return options;
    }

    public static AbstractController.Toast runnableToToast(ResultStatusResponseRunnable callable, String successMessage, String failedMessage) {
        AbstractController.Toast.ToastBuilder builder = AbstractController.Toast.builder();

        try {
            callable.run();
            builder
                .message(successMessage)
                .type(AbstractController.Toast.Type.success);
        } catch (Exception exception) {

            String cause = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
            String message = failedMessage != null ? failedMessage + ": \n" + cause : cause;

            builder
                .message(message)
                .type(AbstractController.Toast.Type.error);

            logger.error(message, exception);
        }

        return builder.build();
    }

    public interface ResultStatusResponseRunnable {
        void run() throws Exception;
    }

    @Getter
    public static class RunnableResult {
        private Boolean result;
        private String message;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RequestHelper.class).toInstance(new RequestHelper());
    }
}
