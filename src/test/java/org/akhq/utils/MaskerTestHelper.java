package org.akhq.utils;

import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.apache.kafka.clients.admin.TopicDescription;

import java.util.List;

public class MaskerTestHelper {

    static Record sampleRecord(String topicName,
                                  String key,
                                  String value) {
        Record record = new Record();
        record.setTopic(
            new Topic(
                new TopicDescription(topicName, true, List.of()),
                List.of(),
                List.of(),
                true,
                true
            )
        );
        record.setKey(key);
        record.setValue(value);
        return record;
    }
}
