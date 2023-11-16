package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
@ConfigurationProperties("akhq.ui-options")
public class UIOptions {
    @ConfigurationBuilder(configurationPrefix = "topic")
    private UiOptionsTopic topic = new UiOptionsTopic();

    @ConfigurationBuilder(configurationPrefix = "topic-data")
    private UiOptionsTopicData topicData = new UiOptionsTopicData();
}
