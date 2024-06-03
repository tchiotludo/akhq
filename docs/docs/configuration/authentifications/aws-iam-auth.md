# AWS MSK IAM Auth

* The libraries required for IAM authentication have already been loaded.

Configure aws-msk-iam-auth connection in AKHQ

```yaml
akhq:
  connections:
    docker-kafka-server:
      properties:
        bootstrap.servers: msk-broker:9098
        security.protocol: SASL_SSL
        sasl.mechanism: AWS_MSK_IAM
        sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required awsDebugCreds=true;
        sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
        ssl.truststore.location: ${JAVA_HOME}/lib/security/cacerts
        ssl.truststore.password: changeit
```

## References
[https://docs.aws.amazon.com/msk/latest/developerguide/iam-access-control.html](https://docs.aws.amazon.com/msk/latest/developerguide/iam-access-control.html)
[https://github.com/aws/aws-msk-iam-auth/blob/main/README.md](https://github.com/aws/aws-msk-iam-auth/blob/main/README.md)
