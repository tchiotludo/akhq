package org.akhq.modules.schemaregistry;

import build.buf.bsr.kafka.gen.buf.registry.module.v1.GetFileDescriptorSetRequest;
import build.buf.bsr.kafka.gen.buf.registry.module.v1.GetFileDescriptorSetResponse;
import build.buf.bsr.kafka.gen.buf.registry.module.v1.ResourceRef;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Standalone client for Buf Schema Registry (BSR) that fetches protobuf descriptors.
 *
 * <p>This implementation is adapted from the bsr-kafka-serde library (Apache 2.0 licensed)
 * and uses the BSR HTTP API directly without reflection.
 *
 * @see <a href="https://github.com/bufbuild/buf-kafka-serde-java">buf-kafka-serde-java</a>
 */
@Slf4j
public class BufSchemaRegistryClient {
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONNECT_PROTOCOL_VERSION = "Connect-Protocol-Version";
    private static final String HEADER_CONNECT_TIMEOUT_MS = "Connect-Timeout-Ms";
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String METHOD_GET_FILE_DESCRIPTOR_SET =
            "buf.registry.module.v1.FileDescriptorSetService/GetFileDescriptorSet";
    private static final String USER_AGENT = "akhq/bsr-client";
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String host;
    private final String token;
    private final HttpClient httpClient;
    private final ConcurrentMap<String, Descriptors.Descriptor> descriptorCache = new ConcurrentHashMap<>();

    /**
     * Creates a new BSR client.
     *
     * @param host  BSR hostname (e.g., "buf.build", "bufbuild.internal")
     * @param token API token for authentication (optional for public modules)
     */
    public BufSchemaRegistryClient(String host, String token) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("BSR host cannot be null or empty");
        }
        this.host = host;
        this.token = token;
        this.httpClient = HttpClient.newBuilder().build();

        log.info("Initialized standalone BSR client for host: {}", host);
    }

    /**
     * Get a message descriptor from BSR by commit ID and message fully-qualified name.
     * Results are cached locally.
     *
     * @param commitId   BSR commit ID (e.g., "a1b2c3d4...")
     * @param messageFQN Fully-qualified protobuf message name (e.g., "com.myorg.Order")
     * @return Protobuf descriptor for the message
     * @throws SerializationException if the descriptor cannot be retrieved
     */
    public Descriptors.Descriptor getMessageDescriptor(String commitId, String messageFQN)
            throws SerializationException {
        if (commitId == null || commitId.isEmpty()) {
            throw new IllegalArgumentException("Commit ID cannot be null or empty");
        }
        if (messageFQN == null || messageFQN.isEmpty()) {
            throw new IllegalArgumentException("Message FQN cannot be null or empty");
        }

        String cacheKey = commitId + ":" + messageFQN;

        // Check cache first
        Descriptors.Descriptor cached = descriptorCache.get(cacheKey);
        if (cached != null) {
            log.debug("Using cached descriptor for {} (commit: {})", messageFQN, commitId);
            return cached;
        }

        log.debug("Fetching descriptor from BSR - commit: {}, message: {}", commitId, messageFQN);

        try {
            Descriptors.Descriptor descriptor = fetchDescriptorFromBSR(commitId, messageFQN);
            descriptorCache.put(cacheKey, descriptor);
            log.debug("Successfully retrieved and cached descriptor for {}", messageFQN);
            return descriptor;
        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to retrieve descriptor from BSR - commit: %s, message: %s",
                commitId, messageFQN);
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    private Descriptors.Descriptor fetchDescriptorFromBSR(String commitId, String messageFQN)
            throws IOException, InterruptedException {
        // Build the BSR API request
        GetFileDescriptorSetRequest request = GetFileDescriptorSetRequest.newBuilder()
                .setResourceRef(ResourceRef.newBuilder().setId(commitId).build())
                .addIncludeTypes(messageFQN)
                .build();

        // Build HTTP request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("https://" + host + "/" + METHOD_GET_FILE_DESCRIPTOR_SET))
                .header(HEADER_CONTENT_TYPE, "application/proto")
                .header(HEADER_CONNECT_PROTOCOL_VERSION, "1")
                .header(HEADER_ACCEPT_ENCODING, "identity")
                .header(HEADER_USER_AGENT, USER_AGENT)
                .header(HEADER_CONNECT_TIMEOUT_MS, "30000")
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(request.toByteArray()));

        if (token != null && !token.isEmpty()) {
            requestBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + token);
        }

        HttpRequest httpRequest = requestBuilder.build();

        // Send request
        HttpResponse<byte[]> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException(String.format(
                    "BSR API request failed: HTTP %d - %s",
                    response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8)));
        }

        // Parse response
        GetFileDescriptorSetResponse bsrResponse = GetFileDescriptorSetResponse.parseFrom(response.body());

        // Extract the descriptor
        Descriptors.Descriptor descriptor = findMessageDescriptor(
                bsrResponse.getFileDescriptorSet(),
                messageFQN);

        if (descriptor == null) {
            throw new IOException("Failed to find message descriptor for " + messageFQN);
        }

        return descriptor;
    }

    /**
     * Finds a message descriptor within a FileDescriptorSet.
     * Adapted from bsr-kafka-serde library.
     */
    private static Descriptors.Descriptor findMessageDescriptor(
            DescriptorProtos.FileDescriptorSet fds,
            String messageFQN) {
        Map<String, Descriptors.FileDescriptor> descriptorsByName = new HashMap<>(fds.getFileCount());

        // Build all file descriptors
        for (DescriptorProtos.FileDescriptorProto fd : fds.getFileList()) {
            try {
                buildFileDescriptor(fd, fds, descriptorsByName);
            } catch (Descriptors.DescriptorValidationException e) {
                throw new SerializationException(
                        "Failed to build file descriptor for " + fd.getName(), e);
            }
        }

        // Parse message FQN into package and message name
        final String packageName, messageName;
        int lastDot = messageFQN.lastIndexOf('.');
        if (lastDot != -1) {
            packageName = messageFQN.substring(0, lastDot);
            messageName = messageFQN.substring(lastDot + 1);
        } else {
            packageName = "";
            messageName = messageFQN;
        }

        // Find the message descriptor
        for (Descriptors.FileDescriptor fd : descriptorsByName.values()) {
            if (!fd.getPackage().equals(packageName)) {
                continue;
            }
            Descriptors.Descriptor md = fd.findMessageTypeByName(messageName);
            if (md != null) {
                return md;
            }
        }

        return null;
    }

    /**
     * Recursively builds a FileDescriptor and its dependencies.
     * Adapted from bsr-kafka-serde library.
     */
    private static Descriptors.FileDescriptor buildFileDescriptor(
            DescriptorProtos.FileDescriptorProto fdp,
            DescriptorProtos.FileDescriptorSet fds,
            Map<String, Descriptors.FileDescriptor> fileDescriptorsByName)
            throws Descriptors.DescriptorValidationException {

        if (fileDescriptorsByName.containsKey(fdp.getName())) {
            return fileDescriptorsByName.get(fdp.getName());
        }

        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>(fdp.getDependencyCount());
        for (String depName : fdp.getDependencyList()) {
            Descriptors.FileDescriptor dependency = fileDescriptorsByName.get(depName);
            if (dependency != null) {
                dependencies.add(dependency);
                continue;
            }
            DescriptorProtos.FileDescriptorProto depProto = fds.getFileList().stream()
                    .filter(f -> f.getName().equals(depName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Dependency not found: " + depName));
            dependencies.add(buildFileDescriptor(depProto, fds, fileDescriptorsByName));
        }

        Descriptors.FileDescriptor fd = Descriptors.FileDescriptor.buildFrom(
                fdp,
                dependencies.toArray(new Descriptors.FileDescriptor[0]));
        fileDescriptorsByName.put(fdp.getName(), fd);
        return fd;
    }
}
