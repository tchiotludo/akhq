# JWT

AKHQ uses signed JWT tokens to perform authentication.

Please generate a secret that is at least 256 bits.

You can use one of the following methods to provide the generated secret to AKHQ.

## Configuration File

Provide the generated secret via the AKHQ `application.yml` via the following directive:

```yaml
micronaut:
  security:
    enabled: true
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: <your secret here>
```

## Environment Variable

Provide the generated secret via [Micronaut Property Value Binding](https://docs.micronaut.io/latest/guide/index.html#_property_value_binding) using the following environment variable for the execution environment of AKHQ:

```bash
MICRONAUT_SECURITY_TOKEN_JWT_SIGNATURES_SECRET_GENERATOR_SECRET="<your secret here>"
```
