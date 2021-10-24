# JWT

AKHQ uses JWT tokens to perform authentication.
Please generate a secret that is at least 256 bits and change the config like this:

```yaml
micronaut:
  security:
    enabled: true
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: <Your secret here>
```

