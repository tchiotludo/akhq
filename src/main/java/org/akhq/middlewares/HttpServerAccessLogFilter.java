package org.akhq.middlewares;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.security.utils.SecurityService;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

@Singleton
@Requires(property = "akhq.server.access-log.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@Filter("/**")
public class HttpServerAccessLogFilter extends OncePerRequestHttpServerFilter {
    private static final Integer ORDER = ServerFilterPhase.SECURITY.order() + 1;

    private final String logFormat;
    private final List<String> filters;
    private final Logger accessLogger;
    private final ApplicationContext applicationContext;

    public HttpServerAccessLogFilter(
        @Value("${akhq.server.access-log.name:access-log}") String name,
        @Value("${akhq.server.access-log.format}") String logFormat,
        @Value("${akhq.server.access-log.filters}") List<String> filters,
        ApplicationContext applicationContext
    ) {
        this.accessLogger = LoggerFactory.getLogger(name);
        this.logFormat = logFormat;
        this.filters = filters;
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilterOnce(HttpRequest<?> request, ServerFilterChain chain) {
        long start = System.nanoTime();
        Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed(request);

        return new HttpServerPublisher<>(
            responsePublisher,
            this.accessLogger,
            this.logFormat,
            this.filters,
            start,
            request.getMethod().toString(),
            request.getPath(),
            request.getRemoteAddress().getAddress().toString(),
            resolveUser(request)
        );
    }

    private String resolveUser(HttpRequest<?> request) {
        if (applicationContext.containsBean(SecurityService.class)) {
            return request.getUserPrincipal()
                .map(Principal::getName)
                .orElse("Anonymous");
        }

        return "-";
    }

    @SuppressWarnings({"ReactiveStreamsPublisherImplementation"})
    public static class HttpServerPublisher<T extends HttpResponse<?>> implements Publisher<T> {
        private final Publisher<T> publisher;
        private final Logger accessLogger;

        private final String logFormat;
        private final List<String> filters;

        private final long start;
        private final ZonedDateTime datetime;

        private final String method;
        private final String uri;
        private final String inetAddress;
        private final String user;

        HttpServerPublisher(
            Publisher<T> publisher,
            Logger accessLogger,
            String logFormat,
            List<String> filters,
            long start,
            String method,
            String uri,
            String inetAddress,
            String user
        ) {
            this.publisher = publisher;
            this.accessLogger = accessLogger;
            this.logFormat = logFormat;
            this.filters = filters;
            this.start = start;
            this.datetime = ZonedDateTime.now();
            this.method = method;
            this.uri = uri;
            this.inetAddress = inetAddress;
            this.user = user;
        }

        @Override
        @SuppressWarnings({"ReactiveStreamsSubscriberImplementation"})
        public void subscribe(Subscriber<? super T> actual) {

            publisher.subscribe(new Subscriber<T>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    actual.onSubscribe(subscription);
                }

                @Override
                public void onNext(T httpResponse) {
                    log(httpResponse);
                    actual.onNext(httpResponse);
                }

                @Override
                public void onError(Throwable throwable) {
                    log(null);
                    actual.onError(throwable);
                }

                @Override
                public void onComplete() {
                    actual.onComplete();
                }
            });
        }

        private void log(HttpResponse<?> response) {
            if (accessLogger.isInfoEnabled()) {
                final long timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                final int status = status(response);

                String message = MessageFormatter.arrayFormat(
                    logFormat,
                    new Object[]{
                        datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        timeElapsed,
                        method,
                        uri,
                        status,
                        inetAddress,
                        user,
                    }
                ).getMessage();

                boolean filtered = filters
                    .stream()
                    .anyMatch(message::matches);

                if (!filtered) {
                    return;
                }

                if (status >= 400) {
                    accessLogger.warn(message);
                } else {
                    accessLogger.info(message);
                }
            }
        }

        private static int status(HttpResponse<?> httpResponse) {
            if (httpResponse == null) {
                return 500;
            }

            HttpStatus status = httpResponse.status();
            if (status == null) {
                status = HttpStatus.OK;
            }

            return status.getCode();
        }

    }
}
