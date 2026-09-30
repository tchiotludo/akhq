package org.akhq.mcp.services;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import org.akhq.configs.Mcp;
import org.akhq.mcp.model.FindMessageInTopicArguments;
import org.akhq.mcp.model.FindMessageInTopicResult;
import org.akhq.mcp.model.GetMessageDetailArguments;
import org.akhq.mcp.model.GetMessageDetailResult;
import org.akhq.mcp.model.GetTopicLastRecordTimestampArguments;
import org.akhq.mcp.model.GetTopicLastRecordTimestampResult;
import org.akhq.mcp.model.MessageHeader;
import org.akhq.mcp.model.MessageOverview;
import org.akhq.mcp.model.SearchMatchType;
import org.akhq.mcp.model.TimeWindowSuggestion;
import org.akhq.models.KeyValue;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.repositories.RecordRepository;
import org.akhq.repositories.TopicRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqTopicDataToolService {
    private static final int DEFAULT_MAX_MATCHES = 1;
    private static final int MAX_ALLOWED_MATCHES = 25;
    private static final int MAX_ALLOWED_PROJECTED_MATCHES = 500;

    private final TopicRepository topicRepository;
    private final RecordRepository recordRepository;
    private final ApplicationContext applicationContext;
    private final Mcp mcp;

    public AkhqTopicDataToolService(
        TopicRepository topicRepository,
        RecordRepository recordRepository,
        ApplicationContext applicationContext,
        Mcp mcp
    ) {
        this.topicRepository = topicRepository;
        this.recordRepository = recordRepository;
        this.applicationContext = applicationContext;
        this.mcp = mcp;
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

        List<String> fields = fields(arguments.fields());
        int maxMatches = clampMaxMatches(
            arguments.maxMatches() == null ? DEFAULT_MAX_MATCHES : arguments.maxMatches(),
            fields.isEmpty() ? MAX_ALLOWED_MATCHES : MAX_ALLOWED_PROJECTED_MATCHES
        );

        Environment environment = applicationContext.getEnvironment();
        RecordRepository.Options options = new RecordRepository.Options(environment, cluster, topicName);
        options.setSort(RecordRepository.Options.Sort.OLDEST);
        // One more match than requested tells whether more matches remain.
        options.setSize(maxMatches + 1);
        asString(arguments.after()).ifPresent(after -> setCursor(options, after));

        asInteger(arguments.partition()).ifPresent(options::setPartition);
        asString(arguments.searchByKey()).map(value -> toSearchFilter(value, arguments.searchByKeyMatchType())).ifPresent(options::setSearchByKey);
        asString(arguments.searchByValue()).map(value -> toSearchFilter(value, arguments.searchByValueMatchType())).ifPresent(options::setSearchByValue);
        asString(arguments.searchByHeaderKey()).map(value -> toSearchFilter(value, arguments.searchByHeaderKeyMatchType())).ifPresent(options::setSearchByHeaderKey);
        asString(arguments.searchByHeaderValue()).map(value -> toSearchFilter(value, arguments.searchByHeaderValueMatchType())).ifPresent(options::setSearchByHeaderValue);
        asEpochMillis(arguments.timestamp()).ifPresent(options::setTimestamp);
        asEpochMillis(arguments.endTimestamp()).ifPresent(options::setEndTimestamp);

        Topic topic = topicRepository.findByName(cluster, topicName);
        // Without a time window a search scans the whole topic, so bound it: the scan is cancelled once the budget
        // is spent, which closes its consumer, and the matches found so far are returned.
        Duration searchTimeout = mcp.getSearchTimeout();
        long startedAt = System.nanoTime();
        List<Record> matches = recordRepository.search(topic, options)
            .take(searchTimeout)
            .flatMapIterable(event -> event.getData().getRecords())
            .take(maxMatches + 1)
            .collectList()
            .blockOptional()
            .orElse(List.of());
        boolean timedOut = matches.size() <= maxMatches
            && Duration.ofNanos(System.nanoTime() - startedAt).compareTo(searchTimeout) >= 0;
        String timeoutNotice = timedOut
            ? " The search stopped after " + searchTimeout.toSeconds() + "s before scanning the whole topic, so "
                + "results may be incomplete: narrow it with `timestamp`/`endTimestamp` or `partition`."
            : "";

        if (matches.isEmpty()) {
            boolean hasTimeWindow = asString(arguments.timestamp()).isPresent() || asString(arguments.endTimestamp()).isPresent();
            String message = hasTimeWindow
                ? "No matching message found in topic '" + topicName + "' in the provided time window."
                : "No matching message found in topic '" + topicName + "'. Provide `timestamp` and `endTimestamp` to narrow the search window.";
            message += timeoutNotice;

            TimeWindowSuggestion suggestion = hasTimeWindow
                ? null
                : new TimeWindowSuggestion(
                    "Try the last 15 minutes",
                    Instant.now().minusSeconds(15 * 60).toString(),
                    Instant.now().toString()
                );

            return new FindMessageInTopicResult(false, topicName, 0, false, null, List.of(), message, suggestion);
        }

        boolean moreMatches = matches.size() > maxMatches;
        List<Record> page = moreMatches ? matches.subList(0, maxMatches) : matches;
        Page overviews = fields.isEmpty() ? valuePage(page) : projectedPage(page, fields);
        List<Record> returned = page.subList(0, overviews.messages().size());
        boolean hasMore = moreMatches || returned.size() < page.size();
        String nextCursor = hasMore ? options.pagination(returned) : null;

        StringBuilder message = new StringBuilder("Found ")
            .append(returned.size())
            .append(" matching message(s) in topic '")
            .append(topicName)
            .append("'.");
        if (hasMore) {
            message.append(returned.size() < page.size() ? " The result size budget is reached." : "")
                .append(" More matches remain: call again with `after` set to `nextCursor` to get them.");
        }
        if (overviews.truncatedValues() > 0) {
            message.append(" ")
                .append(overviews.truncatedValues())
                .append(" value(s) were truncated to fit the result: use `fields` to extract what you need, or ")
                .append("`akhq.get_message_detail` for a full message.");
        }
        if (overviews.notJson() > 0) {
            message.append(" ")
                .append(overviews.notJson())
                .append(" value(s) are not JSON, so `fields` could not be extracted and the value is returned instead.");
        }
        message.append(timeoutNotice);

        return new FindMessageInTopicResult(
            true,
            topicName,
            returned.size(),
            hasMore,
            nextCursor,
            overviews.messages(),
            message.toString(),
            null
        );
    }

    /**
     * Returns the whole values when they fit the result budget, and truncates the longest ones evenly otherwise.
     */
    private Page valuePage(List<Record> records) {
        int maxValueLength = MessageValues.maxValueLength(
            records.stream().map(record -> record.getValue() == null ? 0 : record.getValue().length()).toList(),
            mcp.getMaxResultLength()
        );

        int truncated = 0;
        List<MessageOverview> messages = new ArrayList<>();
        for (Record record : records) {
            String value = MessageValues.truncate(record.getValue(), maxValueLength);
            boolean valueTruncated = value != null && !value.equals(record.getValue());
            truncated += valueTruncated ? 1 : 0;
            messages.add(toMessageOverview(record, value, valueTruncated ? true : null, null));
        }

        return new Page(messages, truncated, 0);
    }

    /**
     * Returns the requested fields of each value, and stops once the result budget is reached so the remaining
     * matches can be fetched with the next cursor.
     */
    private Page projectedPage(List<Record> records, List<String> fields) {
        int budget = mcp.getMaxResultLength();
        int notJson = 0;
        List<MessageOverview> messages = new ArrayList<>();

        for (Record record : records) {
            Optional<Map<String, Object>> projection = MessageValues.project(record.getValue(), fields);
            String value = projection.isPresent() ? null : MessageValues.truncate(record.getValue(), MessageValues.MIN_VALUE_LENGTH);
            MessageOverview overview = toMessageOverview(
                record,
                value,
                value != null && !value.equals(record.getValue()) ? true : null,
                projection.orElse(null)
            );

            budget -= MessageValues.length(overview);
            if (budget < 0 && !messages.isEmpty()) {
                break;
            }
            notJson += projection.isPresent() ? 0 : 1;
            messages.add(overview);
        }

        return new Page(messages, 0, notJson);
    }

    private record Page(List<MessageOverview> messages, int truncatedValues, int notJson) {
    }

    private static List<String> fields(List<String> fields) {
        if (fields == null) {
            return List.of();
        }

        return fields.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(field -> !field.isEmpty())
            .distinct()
            .toList();
    }

    private static void setCursor(RecordRepository.Options options, String after) {
        try {
            options.setAfter(after);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("`arguments.after` must be a `nextCursor` returned by a previous search");
        }
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

    public GetTopicLastRecordTimestampResult getTopicLastRecordTimestamp(GetTopicLastRecordTimestampArguments arguments)
        throws ExecutionException, InterruptedException {
        if (arguments == null) {
            throw new IllegalArgumentException("`arguments` is required");
        }

        String cluster = asRequiredString(arguments.cluster(), "`arguments.cluster` is required");
        String topicName = asRequiredString(arguments.topic(), "`arguments.topic` is required");
        Record record = recordRepository.getLastRecord(cluster, List.of(topicName)).get(topicName);

        if (record == null) {
            return new GetTopicLastRecordTimestampResult(
                false,
                topicName,
                null,
                "No records found in topic '" + topicName + "'."
            );
        }

        return new GetTopicLastRecordTimestampResult(
            true,
            topicName,
            record.getTimestamp().toInstant().toString(),
            "Last record timestamp found."
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

    private static MessageOverview toMessageOverview(
        Record record,
        String value,
        Boolean valueTruncated,
        Map<String, Object> fields
    ) {
        return new MessageOverview(
            record.getPartition(),
            record.getOffset(),
            record.getTimestamp() == null ? null : record.getTimestamp().toInstant().toString(),
            record.getKey(),
            value,
            valueTruncated,
            fields
        );
    }

    private List<MessageHeader> toHeaders(List<KeyValue<String, String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return List.of();
        }
        return headers.stream().map(header -> new MessageHeader(header.getKey(), header.getValue())).toList();
    }

    private static int clampMaxMatches(int maxMatches, int maxAllowed) {
        if (maxMatches < 1) {
            return DEFAULT_MAX_MATCHES;
        }
        return Math.min(maxMatches, maxAllowed);
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
