package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EachProperty("akhq.connections")
@Getter
public class Connection extends AbstractProperties {
    SchemaRegistry schemaRegistry;
    List<Connect> connect;
    ProtobufDeserializationTopicsMapping deserialization;
    Options options = new Options();

    public Connection(@Parameter String name) {
        super(name);
    }

    @Getter
    @ConfigurationProperties("schema-registry")
    public static class SchemaRegistry {
        String url;
        String basicAuthUsername;
        String basicAuthPassword;

        @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
        Map<String, String> properties;
    }

    @Getter
    @Data
    @ConfigurationProperties("deserialization.protobuf")
    public static class ProtobufDeserializationTopicsMapping {
        List<TopicsMapping> topicsMapping = new ArrayList<>();
    }

    @Data
    @ConfigurationProperties("options")
    public static class Options {

        @ConfigurationBuilder(configurationPrefix = "topic")
        private Topic topic = new Topic();

        @ConfigurationBuilder(configurationPrefix = "topic-data")
        private TopicData topicData = new TopicData();

    }

    public Connection.Options mergeOptions(UIOptions defaultOptions) {

        Options options = new Options();
        options.topic = new Topic(
                StringUtils.isNotEmpty(this.options.topic.getDefaultView())? this.options.topic.getDefaultView(): defaultOptions.getTopic().getDefaultView(),
                (this.options.topic.getSkipConsumerGroups() != null)? this.options.topic.getSkipConsumerGroups() : defaultOptions.getTopic().getSkipConsumerGroups());
        options.topicData = new TopicData(
                StringUtils.isNotEmpty(this.options.topicData.getSort())? this.options.topicData.getSort(): defaultOptions.getTopicData().getSort());
        return options;
    }

}

