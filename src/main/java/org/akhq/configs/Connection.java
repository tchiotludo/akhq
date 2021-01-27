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
    UiOptions uiOptions = new UiOptions();

    public Connection(@Parameter String name) {
        super(name);
    }

    @Getter
    @ConfigurationProperties("schema-registry")
    public static class SchemaRegistry {
        String url;
        String basicAuthUsername;
        String basicAuthPassword;
        SchemaRegistryType type = SchemaRegistryType.CONFLUENT;

        @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
        Map<String, String> properties;
    }

    @Getter
    @Data
    @ConfigurationProperties("deserialization.protobuf")
    public static class ProtobufDeserializationTopicsMapping {
        String descriptorsFolder;
        List<TopicsMapping> topicsMapping = new ArrayList<>();
    }

    @Data
    @ConfigurationProperties("ui-options")
    public static class UiOptions {
        @ConfigurationBuilder(configurationPrefix = "topic")
        private UiOptionsTopic topic = new UiOptionsTopic();

        @ConfigurationBuilder(configurationPrefix = "topic-data")
        private UiOptionsTopicData topicData = new UiOptionsTopicData();
    }

    public UiOptions mergeOptions(UIOptions defaultOptions) {
        UiOptions options = new UiOptions();

        options.topic = new UiOptionsTopic(
            StringUtils.isNotEmpty(this.uiOptions.topic.getDefaultView()) ? this.uiOptions.topic.getDefaultView() : defaultOptions.getTopic().getDefaultView(),
            (this.uiOptions.topic.getSkipConsumerGroups() != null) ? this.uiOptions.topic.getSkipConsumerGroups() : defaultOptions.getTopic().getSkipConsumerGroups(),
            (this.uiOptions.topic.getSkipLastRecord() != null) ? this.uiOptions.topic.getSkipLastRecord() : defaultOptions.getTopic().getSkipLastRecord()
        );

        options.topicData = new UiOptionsTopicData(
            StringUtils.isNotEmpty(this.uiOptions.topicData.getSort()) ? this.uiOptions.topicData.getSort() : defaultOptions.getTopicData().getSort()
        );

        return options;
    }
}

