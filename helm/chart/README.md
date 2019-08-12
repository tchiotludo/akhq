# Helm for KafkaHQ

## Install Chart
To install the KafkaHQ Chart into your Kubernetes cluster :

```bash
helm install --namespace "kafkahq" --name "kafkahq" kafkahq
```

After installation succeeds, you can get a status of Chart

```bash
helm status "kafkahq"
```

If you want to delete your Chart, use this command:

```bash
helm delete  --purge "kafkahq"
```

## Configuration
Application configuration is in `templates/kafkahq-configmap.yaml`

Others are presented in `values.yaml`
