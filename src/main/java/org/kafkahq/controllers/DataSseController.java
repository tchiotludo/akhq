package org.kafkahq.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Sse;
import org.kafkahq.App;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.ConfigRepository;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.repositories.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DataSseController implements Sse.Handler {
    private static Logger logger = LoggerFactory.getLogger(DataSseController.class);

    @Override
    public void handle(Request request, Sse sse) throws Exception {
        TopicRepository topicRepository = sse.require(TopicRepository.class);
        RecordRepository recordRepository = sse.require(RecordRepository.class);
        ConfigRepository configRepository = sse.require(ConfigRepository.class);
        Config config = sse.require(Config.class);

        Topic topic = topicRepository.findByName(request.param("topic").value());
        RecordRepository.Options options = RequestHelper.buildRecordRepositoryOptions(
            request,
            request.param("cluster").value(),
            request.param("topic").value()
        );

        Map<String, Object> datas = new HashMap<>();
        datas.put("topic", topic);
        datas.put("canDeleteRecords", topic.canDeleteRecords(configRepository));
        datas.put("clusterId", request.param("cluster").value());
        datas.put("basePath", App.getBasePath(config));

        AtomicInteger next = new AtomicInteger(0);
        RecordRepository.SearchConsumer searchConsumer = new RecordRepository.SearchConsumer() {
            @Override
            public void accept(RecordRepository.SearchEvent searchEvent) {
                datas.put("datas", searchEvent.getRecords());
                try {
                    sse
                        .event(new SearchBody(searchEvent.getOffsets(), searchEvent.getProgress(), render(sse, datas)))
                        .id(next.incrementAndGet())
                        .name("searchBody")
                        .type(MediaType.json)
                        .send();
                } catch (IOException | TemplateException e) {
                    logger.error("Error on sse send", e);
                }
            }
        };
        sse.onClose(searchConsumer::close);

        RecordRepository.SearchEnd end = recordRepository.search(options, searchConsumer);
        sse
            .event(end)
            .id(next.incrementAndGet())
            .name("searchEnd")
            .type(MediaType.json)
            .send();
        sse.close();
    }

    private String render(Sse sse, Map<String, Object> datas) throws IOException, TemplateException {
        Configuration configuration = sse.require(Configuration.class);
        Template template = configuration.getTemplate("topicSearch.ftl");

        Writer stringWriterr = new StringWriter();
        template.process(datas, stringWriterr);

        return stringWriterr.toString();
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    @AllArgsConstructor
    public static class SearchBody {
        @JsonProperty("offsets")
        private Map<Integer, RecordRepository.SearchEvent.Offset> offsets = new HashMap<>();

        @JsonProperty("progress")
        private Map<Integer, Long> progress = new HashMap<>();

        @JsonProperty("body")
        private String body;
    }
}
