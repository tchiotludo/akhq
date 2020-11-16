package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("akhq")
public class UIOptions {

    @ConfigurationBuilder(configurationPrefix = "topic")
    private Topic topic = new Topic();

    @ConfigurationBuilder(configurationPrefix = "topic-data")
    private TopicData topicData = new TopicData();
}
