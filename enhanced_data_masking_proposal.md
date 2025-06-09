# Proposal: Enhanced Data Masking in AKHQ

**Status:** Proposed
**Author:** AI Agent
**Date:** $(date +%Y-%m-%d)

## Abstract

This proposal outlines an enhancement to AKHQ's data masking capabilities. The current regular expression-based masking is useful but limited for complex scenarios. We propose introducing more sophisticated techniques like Faker-based data generation, hashing/encryption, and customizable masking rules to provide improved data privacy, better compliance with regulations, more realistic test data, and increased flexibility.

## Current System

AKHQ currently supports data masking through regular expression replacement. While this is a valuable feature, it has limitations when:

*   Dealing with structured data where context is important for realistic masking.
*   Requiring cryptographic hashing or encryption for specific fields.
*   Needing to apply different masking strategies based on data types or schema information.

## Proposed Enhancements

We propose the following enhancements to the data masking feature in AKHQ:

### 1. Faker-Based Data Generation

*   **Description:** Integrate a library like Java Faker (or a similar robust data generation library) to replace sensitive data with realistic-looking yet entirely fake data.
*   **Use Cases:**
    *   Replacing real names with plausible fake names.
    *   Generating fake addresses, phone numbers, email addresses, etc.
    *   Creating mock financial transaction data (e.g., credit card numbers, bank account numbers).
*   **Benefits:** This provides excellent anonymization while maintaining the verisimilitude of the data, making it highly suitable for testing, development, and demonstration purposes where data structure and realism are important.

### 2. Hashing/Encryption

*   **Description:** Allow users to define specific fields that should be irreversibly hashed or reversibly encrypted.
*   **Hashing:**
    *   **Use Cases:** Masking fields where the original value is not needed post-masking, but uniqueness or the ability to identify duplicates is important (e.g., user IDs, certain types of sensitive identifiers).
    *   **Implementation:** Support for common hashing algorithms like SHA-256, SHA-512, MD5 (with appropriate warnings about collision vulnerabilities for MD5).
*   **Encryption:**
    *   **Use Cases:** Protecting highly sensitive data where there might be a legitimate, controlled need to decrypt it later (e.g., for specific analytical purposes by authorized personnel).
    *   **Implementation:** Support for strong symmetric encryption algorithms like AES.
    *   **Key Management:** Provide options for key management, such as:
        *   Directly embedding a key in the configuration (suitable for simpler setups, with security warnings).
        *   Referencing environment variables for the key.
        *   Potential integration with external key management systems (KMS) in the future.

### 3. Custom Masking Rules

*   **Description:** Empower users to define more granular and context-aware masking rules.
*   **Rule Triggers:**
    *   **Data Type:** Apply specific masking functions based on the inferred or defined data type of a field (e.g., a default number masker, a string truncator).
    *   **Schema Information:** If a schema registry (like Confluent Schema Registry) is integrated with AKHQ, leverage schema field names, types, or custom properties to apply targeted masking. For example, a rule could automatically mask all fields named "creditCardNumber" or any field tagged with a "sensitive" property in the schema.
    *   **User-Defined Functions (Advanced):** Allow users to provide small, sandboxed scripts (e.g., using Groovy, JavaScript, or a simple expression language) for complex or highly specific masking logic not covered by standard functions. This offers maximum flexibility for unique requirements.

## Benefits of Proposed Enhancements

*   **Improved Data Privacy:** Offers significantly stronger and more appropriate protection for sensitive information in non-production environments (development, testing, staging).
*   **Better Compliance:** Helps organizations more effectively meet the requirements of data privacy regulations like GDPR, CCPA, HIPAA, etc., by providing robust anonymization and pseudonymization techniques.
*   **More Realistic Test Data:** Faker integration, in particular, allows for the creation of anonymized datasets that are much more realistic and usable for development and testing teams.
*   **Increased Flexibility:** Custom rules and a wider range of masking techniques provide the adaptability needed to handle diverse data types, structures, and specific organizational masking policies.
*   **Reduced Risk:** Minimizes the risk of sensitive data exposure in pre-production systems.

## Proposed Configuration Changes

The existing `akhq.data-masking.filters` configuration structure could be extended to accommodate these new masking types. We aim for backward compatibility where possible.

```yaml
akhq:
  data-masking:
    # Optional: Default masking-type if not specified in a filter
    # default-masking-type: REGEX_REPLACE
    # Optional: Default replacement for REGEX_REPLACE if not specified
    # default-regex-replacement: "***"

    filters:
      # Existing Regex Example (for comparison)
      - description: "Masks account numbers with regex"
        topic-regex: ".*accounts.*"
        key-regex: ".*" # or specific key pattern
        value-regex: "(?<=\"accountNumber\":\")[^\"]+(?=\")"
        replacement: "HIDDEN_ACCOUNT"
        # masking-type: REGEX_REPLACE # Could be explicit or default

      # New Faker Example
      - description: "Masks credit card numbers with Faker"
        topic-regex: ".*transactions.*"
        field-name: "creditCardNumber" # Used for structured data (JSON, Avro)
        masking-type: FAKER
        faker-expression: "#{finance.creditCard}" # Standard Faker expression
        # Optional: locale for Faker
        # faker-locale: "en-US"

      # New Hashing Example
      - description: "Masks email addresses by hashing"
        topic-regex: ".*users.*"
        field-name: "email"
        masking-type: HASH
        hash-algorithm: "SHA-256"
        # Optional: Add a salt (from config, env var, or generated)
        # hash-salt: "your-static-salt" or { "env": "HASH_SALT_VAR" }

      # New Encryption Example
      - description: "Encrypts social security numbers"
        topic-regex: ".*customers.*"
        field-name: "ssn"
        masking-type: ENCRYPT
        encryption-algorithm: "AES/GCM/NoPadding" # Example algorithm
        encryption-key-env: "SSN_ENCRYPTION_KEY" # Key sourced from an environment variable
        # Or direct key (less secure, for dev/test only):
        # encryption-key: "your-super-secret-key-for-dev"

      # Custom Rule based on Schema (Conceptual)
      - description: "Mask all fields tagged as PII in Avro schema"
        topic-regex: ".*" # Apply to all topics or specific ones
        masking-type: SCHEMA_BASED
        schema-field-property: "sensitivity" # Custom property in Avro schema
        schema-property-value: "PII"
        # Defines what to do if "sensitivity=PII" is found
        mask-with: FAKER # Use Faker for these fields
        faker-expression: "#{lorem.word}" # Generic masking

      # User-Defined Function Example (Conceptual)
      # - description: "Custom logic for complex field"
      #   topic-regex: ".*events.*"
      #   field-name: "customPayload"
      #   masking-type: CUSTOM_SCRIPT
      #   script-language: "groovy" # or "javascript"
      #   script: |
      #     // value is the original field value
      #     // helper is a utility object (e.g., for hashing, faker access)
      #     if (value.type == "A") {
      #       return helper.faker("address.streetAddress");
      #     } else {
      #       return helper.hash(value.id, "SHA-256");
      #     }

# Field Discovery for Structured Data (JSON/Avro)
# For field-name based masking to work effectively with JSON or Avro data,
# AKHQ would need to parse the message content.
# - For JSON: Standard JSON parsing.
# - For Avro: Integration with a schema registry is essential to interpret the binary data.
# The `field-name` could support dot notation for nested fields, e.g., "user.profile.email".

## Implementation Considerations

*   **Performance:** Masking, especially parsing and complex transformations, can add overhead. Performance testing will be crucial. Consider options for sampling or applying masking asynchronously if direct Kafka stream interception is too slow.
*   **Library Choices:** Carefully evaluate and select robust, well-maintained libraries for Faker, hashing, and encryption.
*   **Security:**
    *   Key management for encryption is critical. Avoid storing raw keys in version control.
    *   Sandboxing for custom scripts is essential to prevent security vulnerabilities.
*   **Error Handling:** How are errors during masking (e.g., unparseable data, misconfiguration) handled? Should the message be skipped, masked with a default, or should an error be logged prominently?
*   **Usability:** The configuration should be as intuitive as possible, with clear documentation and examples.
*   **Schema Registry Integration:** Deeper integration with schema registries will significantly enhance the power of schema-aware masking.

## Future Possibilities

*   **Conditional Masking:** Masking a field based on the value of another field.
*   **Integration with External Data Classification Tools:** Dynamically apply masking based on classifications from external tools.
*   **UI for Masking Rule Configuration:** A user interface within AKHQ to manage masking rules could improve usability for less technical users.

## Conclusion

Enhancing AKHQ's data masking capabilities as proposed will provide users with a powerful and flexible toolset to protect sensitive data, meet compliance requirements, and create high-quality anonymized data for non-production use. This feature would be a significant value-add for the AKHQ platform.
