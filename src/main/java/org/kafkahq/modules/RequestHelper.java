package org.kafkahq.modules;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Request;
import org.kafkahq.repositories.RecordRepository;

import java.time.Instant;

@Singleton
public class RequestHelper implements Jooby.Module {

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

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RequestHelper.class).toInstance(new RequestHelper());
    }
}
