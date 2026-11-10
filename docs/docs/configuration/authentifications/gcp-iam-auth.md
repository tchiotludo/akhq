# Google Cloud Managed Service for Apache Kafka IAM Auth

* The libraries required for IAM authentication have already been loaded.

Configure Google Cloud Managed Service for Apache Kafka connection in AKHQ using Google Cloud IAM and Application Default Credentials (ADC).

```yaml
akhq:
  connections:
    gcp-kafka-cluster:
      properties:
        bootstrap.servers: bootstrap.my-cluster.us-central1.managedkafka.my-project.cloud.goog:9092
        security.protocol: SASL_SSL
        sasl.mechanism: OAUTHBEARER
        sasl.login.callback.handler.class: com.google.cloud.hosted.kafka.auth.GcpLoginCallbackHandler
        sasl.jaas.config: org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
```

Authentication relies on Google Cloud Application Default Credentials (ADC). When running on Google Cloud (e.g. GKE with Workload Identity, Compute Engine, Cloud Run), credentials are automatically retrieved from the attached service account. For local development or outside GCP, set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to your service account key file.

## References
* [Google Cloud Managed Service for Apache Kafka](https://cloud.google.com/managed-service-for-apache-kafka/docs)
* [Google Managed Kafka Java Auth Handler](https://github.com/googleapis/managedkafka/tree/main/kafka-java-auth)
