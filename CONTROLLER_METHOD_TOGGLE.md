# Controller Method Toggle Feature

## Overview

This feature allows you to enable or disable specific controller endpoints via configuration without modifying the code. This is useful for restricting access to certain API endpoints in specific environments or deployments.

## Usage

### 1. Annotate Controller Methods

Add the `@ControllerMethodEnabled` annotation to any controller method you want to make toggleable:

```java
@Controller
public class TopicController extends AbstractController {

    @AKHQSecured(resource = Role.Resource.TOPIC, action = Role.Action.CREATE)
    @ControllerMethodEnabled(property = "akhq.controllers.topic.create.enabled", defaultValue = true)
    @Post(value = "api/{cluster}/topic")
    @Operation(tags = {"topic"}, summary = "Create a topic")
    public Topic create(...) {
        // method implementation
    }
}
```

### 2. Configure in application.yml

Control the endpoint availability through your `application.yml` configuration:

```yaml
akhq:
  controllers:
    topic:
      create:
        enabled: false  # Disables the topic creation endpoint
      delete:
        enabled: false  # Disables the topic deletion endpoint
    connect:
      create:
        enabled: true   # Explicitly enable (optional if default is true)
```

## Annotation Parameters

### `@ControllerMethodEnabled`

- **property**: The configuration property path to check (e.g., `"akhq.controllers.topic.create.enabled"`)
- **defaultValue**: Whether the endpoint is enabled by default if no configuration is provided (default: `true`)

## Configuration Examples

### Example 1: Disable Topic Creation and Deletion

```yaml
akhq:
  controllers:
    topic:
      create:
        enabled: false
      delete:
        enabled: false
```

### Example 2: Disable All Consumer Group Operations

```yaml
akhq:
  controllers:
    group:
      delete:
        enabled: false
      update:
        enabled: false
```

### Example 3: Read-Only Mode (Disable All Modifications)

```yaml
akhq:
  controllers:
    topic:
      create:
        enabled: false
      delete:
        enabled: false
      update:
        enabled: false
    connect:
      create:
        enabled: false
      delete:
        enabled: false
      update:
        enabled: false
```

## How It Works

1. The `@ControllerMethodEnabled` annotation is applied to controller methods
2. The annotation uses a custom Micronaut `Condition` (`ControllerMethodEnabledCondition`) to evaluate whether the method should be active
3. At startup, Micronaut checks the configuration property specified in the annotation
4. If the property is set to `false`, the endpoint will not be registered and will return a 404 when accessed
5. If the property is not set, the `defaultValue` from the annotation is used

## Benefits

- **Security**: Easily disable sensitive operations in production environments
- **Flexibility**: Different configurations for different environments without code changes
- **Granular Control**: Enable/disable individual endpoints rather than entire controllers
- **No Code Changes**: Toggle features through configuration only

## When to Use This Feature

- Restricting write operations in read-only environments
- Disabling dangerous operations (like topic deletion) in production
- Creating limited-access deployments for specific user groups
- Testing scenarios where certain endpoints should be unavailable
- Compliance requirements where certain operations must be disabled

## Migration from Standard `@Requires`

The standard Micronaut `@Requires` annotation can also be used at the method level, but `@ControllerMethodEnabled` provides:

- More consistent naming convention for controller toggles
- Built-in support for default values
- Clearer documentation of intent

Both approaches work; `@ControllerMethodEnabled` is recommended for new code.
