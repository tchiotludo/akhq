package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.sse.Event;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.akhq.configs.Role;
import org.akhq.models.Record;
import org.akhq.repositories.RecordRepository;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

@Secured(Role.ROLE_TOPIC_READ)
@Controller
public class TailController extends AbstractController {
    private final RecordRepository recordRepository;

    @Inject
    public TailController(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    @Secured(Role.ROLE_TOPIC_DATA_READ)
    @Get(value = "api/{cluster}/tail/sse", produces = MediaType.TEXT_EVENT_STREAM)
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"topic data"}, summary = "Tail for data on multiple topic")
    public Publisher<Event<TailRecord>> sse(
        String cluster,
        List<String> topics,
        Optional<String> search,
        Optional<List<String>> after
    ) {
        RecordRepository.TailOptions options = new RecordRepository.TailOptions(cluster, topics);
        search.ifPresent(options::setSearch);
        after.ifPresent(options::setAfter);

        return recordRepository
            .tail(cluster, options)
            .map(event -> {
                TailRecord tailRecord = new TailRecord();
                tailRecord.offsets = getOffsets(event);

                if (event.getData().getRecords().size() > 0) {
                    tailRecord.records = event.getData().getRecords();
                }

                return Event
                    .of(tailRecord)
                    .name(event.getName());
            });
    }

    private static List<String> getOffsets(Event<RecordRepository.TailEvent> event) {
        return event.getData()
            .getOffsets()
            .entrySet()
            .stream()
            .flatMap(s -> s.getKey()
                .entrySet()
                .stream()
                .map(r -> {
                    List<String> strings = Arrays.asList(
                        r.getKey(),
                        String.valueOf(r.getValue()),
                        String.valueOf(s.getValue() + 1)
                    );

                    return String.join(
                        ",",
                        strings
                    );
                })
            )
            .collect(Collectors.toList());
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class TailRecord {
        @JsonProperty("records")
        private List<Record> records;

        @JsonProperty("offsets")
        private List<String> offsets = new ArrayList<>();
    }
}
