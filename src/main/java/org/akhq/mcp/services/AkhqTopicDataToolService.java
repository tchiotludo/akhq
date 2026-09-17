package org.akhq.mcp.services;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import org.akhq.mcp.model.FindMessageInTopicArguments;
import org.akhq.mcp.model.FindMessageInTopicResult;
import org.akhq.mcp.model.GetMessageDetailArguments;
import org.akhq.mcp.model.GetMessageDetailResult;
import org.akhq.mcp.model.MessageHeader;
import org.akhq.mcp.model.MessageOverview;
import org.akhq.mcp.model.SearchMatchType;
import org.akhq.mcp.model.TimeWindowSuggestion;
import org.akhq.models.KeyValue;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqTopicDataToolService {
    private static final int DEFAULT_MAX_MATCHES = 1;
    private static final int MAX_ALLOWED_MATCHES = 25;

    private final TopicRepository topicRepository;
    private final RecordRepository recordRepository;
    private final ApplicationContext applicationContext;

    public AkhqTopicDataToolService(TopicRepository topicRepository, RecordRepository recordRepository, ApplicationContext applicationContext) {
        this.topicRepository = topicRepository;
        this.recordRepository = recordRepository;
        this.applicationContext = applicationContext;
    }

    public FindMessageInTopicResult findMessageInTopic(FindMessageInTopicArguments arguments) throws ExecutionException, InterruptedException {
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = asRequiredString(arguments.cluster(), "`arguments.cluster` is required");
        String topicName = asRequiredString(arguments.topic(), "`arguments.topic` is required");

        if (isEmpty(arguments.searchByKey())
            && isEmpty(arguments.searchByValue())
            && isEmpty(arguments.searchByHeaderKey())
            && isEmpty(arguments.searchByHeaderValue())) {
            throw new IllegalArgumentException("At least one of searchByKey/searchByValue/searchByHeaderKey/searchByHeaderValue is required");
        }

        int maxMatches = clampMaxMatches(arguments.maxMatches() == null ? DEFAULT_MAX_MATCHES : arguments.maxMatches());

        Environment environment = applicationContext.getEnvironment();
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setSize(maxMatches);

        asInteger(arguments.partition()).ifPresent(options::setPartition);
        asString(arguments.searchByKey()).map(value -> toSearchFilter(value, arguments.searchByKeyMatchType())).ifPresent(options::setSearchByKey);
        asString(arguments.searchByValue()).map(value -> toSearchFilter(value, arguments.searchByValueMatchType())).ifPresent(options::setSearchByValue);
        asString(arguments.searchByHeaderKey()).map(value -> toSearchFilter(value, arguments.searchByHeaderKeyMatchType())).ifPresent(options::setSearchByHeaderKey);
        asString(arguments.searchByHeaderValue()).map(value -> toSearchFilter(value, arguments.searchByHeaderValueMatchType())).ifPresent(options::setSearchByHeaderValue);
        asEpochMillis(arguments.timestamp()).ifPresent(options::setTimestamp);
        asEpochMillis(arguments.endTimestamp()).ifPresent(options::setEndTimestamp);

        Topic topic = topicRepository.findByName(cluster, topicName);
        List<Record> matches = recordRepository.search(topic, options)
            .flatMapIterable(event -> event.getData().getRecords())
            .take(maxMatches)
            .collectList()
            .blockOptional()
            .orElse(List.of());

        if (matches.isEmpty()) {
            boolean hasTimeWindow = asString(arguments.timestamp()).isPresent() || asString(arguments.endTimestamp()).isPresent();
            String message = hasTimeWindow
                ? "No matching message found in topic '" + topicName + "' in the provided time window."
                : "No matching message found in topic '" + topicName + "'. Provide `timestamp` and `endTimestamp` to narrow the search window.";

            TimeWindowSuggestion suggestion = hasTimeWindow
                ? null
                : new TimeWindowSuggestion(
                    "Try the last 15 minutes",
                    Instant.now().minusSeconds(15 * 60).toString(),
                    Instant.now().toString()
                );

            return new FindMessageInTopicResult(false, topicName, 0, List.of(), message, suggestion);
        }

        List<MessageOverview> overviews = matches.stream()
            .map(this::toMessageOverview)
            .toList();

        return new FindMessageInTopicResult(
            true,
            topicName,
            matches.size(),
            overviews,
            "Found " + matches.size() + " matching message(s) in topic '" + topicName + "'.",
            null
        );
    }

    public GetMessageDetailResult getMessageDetail(GetMessageDetailArguments arguments) throws ExecutionException, InterruptedException {
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = asRequiredString(arguments.cluster(), "`arguments.cluster` is required");
        String topicName = asRequiredString(arguments.topic(), "`arguments.topic` is required");
        int partition = asRequiredInteger(arguments.partition(), "`arguments.partition` is required");
        long offset = asRequiredLong(arguments.offset(), "`arguments.offset` is required");

        if (partition < 0) {
            throw new IllegalArgumentException("`arguments.partition` must be >= 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("`arguments.offset` must be >= 0");
        }

        Topic topic = topicRepository.findByName(cluster, topicName);
        RecordRepository.Options options = singleRecordOptions(cluster, topicName, partition, offset);
        Optional<Record> maybeRecord = recordRepository.consumeSingleRecord(cluster, topic, options);

        if (maybeRecord.isEmpty()) {
            return notFoundDetail(topicName, partition, offset);
        }

        Record record = maybeRecord.get();
        if (record.getPartition() != partition || record.getOffset() != offset) {
            return notFoundDetail(topicName, partition, offset);
        }

        List<MessageHeader> headers = toHeaders(record.getHeaders());
        return new GetMessageDetailResult(
            true,
            topicName,
            record.getPartition(),
            record.getOffset(),
            record.getTimestamp() == null ? null : record.getTimestamp().toInstant().toString(),
            record.getKey(),
            record.getValue(),
            headers,
            "Message found."
        );
    }

    private RecordRepository.Options singleRecordOptions(String cluster, String topicName, int partition, long offset) {
        Environment environment = applicationContext.getEnvironment();
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        options.setSize(1);
        options.setPartition(partition);
        if (offset > 0) {
            options.setAfter(partition + "-" + (offset - 1));
        }
        return options;
    }

    private GetMessageDetailResult notFoundDetail(String topicName, int partition, long offset) {
        return new GetMessageDetailResult(
            false,
            topicName,
            partition,
            offset,
            null,
            null,
            null,
            List.of(),
            "No message found at partition " + partition + " and offset " + offset + "."
        );
    }

    private MessageOverview toMessageOverview(Record record) {
        return new MessageOverview(
            record.getPartition(),
            record.getOffset(),
            record.getTimestamp() == null ? null : record.getTimestamp().toInstant().toString(),
            record.getKey(),
            toValueOverview(record.getValue())
        );
    }

    private List<MessageHeader> toHeaders(List<KeyValue<String, String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return List.of();
        }
        return headers.stream().map(header -> new MessageHeader(header.getKey(), header.getValue())).toList();
    }

    private String toValueOverview(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        int maxLength = 200;
        return normalized.length() <= maxLength
            ? normalized
            : normalized.substring(0, maxLength) + "...";
    }

    private int clampMaxMatches(int maxMatches) {
        if (maxMatches < 1) {
            return DEFAULT_MAX_MATCHES;
        }
        return Math.min(maxMatches, MAX_ALLOWED_MATCHES);
    }

    private Optional<String> asString(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        String converted = String.valueOf(value).trim();
        return converted.isEmpty() ? Optional.empty() : Optional.of(converted);
    }

    private String asRequiredString(Object value, String errorMessage) {
        return asString(value).orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private int asRequiredInteger(Object value, String errorMessage) {
        return asInteger(value).orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private long asRequiredLong(Object value, String errorMessage) {
        return asLong(value).orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    private Optional<Integer> asInteger(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }

        try {
            return Optional.of(Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected an integer value but got: " + value);
        }
    }

    private Optional<Long> asLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }

        try {
            return Optional.of(Long.parseLong(String.valueOf(value).trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a long value but got: " + value);
        }
    }

    private Optional<Long> asEpochMillis(Object value) {
        if (value == null) {
            return Optional.empty();
        }

        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }

        String converted = String.valueOf(value).trim();
        if (converted.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(converted));
        } catch (NumberFormatException ignored) {
            // Not epoch millis as string, try ISO-8601 next.
        }

        try {
            return Optional.of(Instant.parse(converted).toEpochMilli());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Expected ISO-8601 timestamp or epoch milliseconds but got: " + value);
        }
    }

    private boolean isEmpty(Object value) {
        return asString(value).isEmpty();
    }

    private String toSearchFilter(String value, SearchMatchType matchType) {
        SearchMatchType effectiveMatchType = matchType == null ? SearchMatchType.CONTAINS : matchType;
        return value + "_" + effectiveMatchType.repositorySuffix();
    }
}
