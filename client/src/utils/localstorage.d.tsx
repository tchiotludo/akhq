type TopicEventHeaders = {
  [key: string]: string;
};

interface TopicEvent {
  partition: number;
  key: string;
  header: TopicEventHeaders;
  keySchemaId: string;
  valueSchemaId: string;
  value: object;
}

declare const popProduceToTopicValues: () => TopicEvent | {};
declare const setProduceToTopicValues: (event: TopicEvent) => void;
