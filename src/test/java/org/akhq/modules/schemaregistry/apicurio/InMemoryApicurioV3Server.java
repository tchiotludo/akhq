package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-process HTTP implementation of the Apicurio Registry v3 REST API
 * for deterministic, fast unit and integration testing without requiring Docker.
 */
public class InMemoryApicurioV3Server implements AutoCloseable {
    private static final String BASE_PATH = "/apis/registry/v3";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final AtomicLong globalIdSeq = new AtomicLong(100);
    private final AtomicLong contentIdSeq = new AtomicLong(1);

    private final Map<String, Long> contentHashToContentId = new LinkedHashMap<>();
    private final Map<Long, StoredContent> contentsById = new LinkedHashMap<>();
    private final Map<String, StoredContent> contentsByHash = new LinkedHashMap<>();
    private final Map<String, StoredArtifact> artifacts = new LinkedHashMap<>();
    private final Map<String, String> artifactCompatibilityRules = new HashMap<>();
    private final Map<String, String> groupCompatibilityRules = new HashMap<>();
    private String globalCompatibilityRule;
    private String expectedAuthorizationHeader;
    private boolean rejectCompatibilityDryRun;

    public InMemoryApicurioV3Server() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext(BASE_PATH, this::handle);
        this.server.start();
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String getV3Url() {
        return getBaseUrl() + BASE_PATH;
    }

    public void setExpectedAuthorizationHeader(String expectedAuthorizationHeader) {
        this.expectedAuthorizationHeader = expectedAuthorizationHeader;
    }

    public void setGlobalCompatibilityRule(String level) {
        this.globalCompatibilityRule = level;
    }

    public void setGroupCompatibilityRule(String groupId, String level) {
        this.groupCompatibilityRules.put(groupId, level);
    }

    public void setRejectCompatibilityDryRun(boolean reject) {
        this.rejectCompatibilityDryRun = reject;
    }

    public StoredVersion seedArtifactVersion(
        String groupId,
        String artifactId,
        String artifactType,
        String content,
        String state,
        List<StoredRef> references
    ) {
        String key = groupId + "::" + artifactId;
        StoredArtifact artifact = artifacts.computeIfAbsent(key, k -> new StoredArtifact(groupId, artifactId, artifactType));
        String contentHash = sha256(content + "::" + (references != null ? references.toString() : ""));
        long contentId = contentHashToContentId.computeIfAbsent(contentHash, h -> {
            long id = contentIdSeq.getAndIncrement();
            StoredContent sc = new StoredContent(id, contentHash, content, artifactType, references != null ? List.copyOf(references) : List.of());
            contentsById.put(id, sc);
            contentsByHash.put(contentHash, sc);
            return id;
        });
        long globalId = globalIdSeq.getAndIncrement();
        String version = String.valueOf(artifact.versions.size() + 1);
        StoredVersion sv = new StoredVersion(groupId, artifactId, artifactType, version, globalId, contentId, contentHash, state);
        artifact.versions.add(sv);
        return sv;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (expectedAuthorizationHeader != null) {
                String actual = exchange.getRequestHeaders().getFirst("Authorization");
                if (!expectedAuthorizationHeader.equals(actual)) {
                    sendProblem(exchange, 401, "Unauthorized", "Missing or invalid Authorization header");
                    return;
                }
            }

            String fullPath = exchange.getRequestURI().getRawPath();
            String path = fullPath.substring(BASE_PATH.length());
            String method = exchange.getRequestMethod();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

            if (path.equals("/search/artifacts") && method.equals("GET")) {
                handleSearchArtifacts(exchange, query);
            } else if (path.equals("/search/versions") && method.equals("GET")) {
                handleSearchVersionsGet(exchange, query);
            } else if (path.equals("/search/versions") && method.equals("POST")) {
                handleSearchVersionsPost(exchange, query);
            } else if (path.startsWith("/admin/rules/")) {
                String ruleType = decode(path.substring("/admin/rules/".length()));
                handleAdminRule(exchange, method, ruleType);
            } else if (path.startsWith("/ids/contentIds/")) {
                handleIdsByContentId(exchange, path.substring("/ids/contentIds/".length()));
            } else if (path.startsWith("/ids/globalIds/")) {
                handleIdsByGlobalId(exchange, path.substring("/ids/globalIds/".length()));
            } else if (path.startsWith("/ids/contentHashes/")) {
                handleIdsByContentHash(exchange, path.substring("/ids/contentHashes/".length()));
            } else if (path.startsWith("/groups/")) {
                handleGroups(exchange, method, path.substring("/groups/".length()), query);
            } else {
                sendProblem(exchange, 404, "NotFound", "Unhandled endpoint: " + method + " " + path);
            }
        } catch (Exception e) {
            sendProblem(exchange, 500, "InternalError", e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void handleSearchArtifacts(HttpExchange exchange, Map<String, String> query) throws IOException {
        String groupFilter = query.get("groupId");
        int offset = Integer.parseInt(query.getOrDefault("offset", "0"));
        int limit = Integer.parseInt(query.getOrDefault("limit", "20"));

        List<StoredArtifact> matched = artifacts.values().stream()
            .filter(a -> groupFilter == null || groupFilter.equals(a.groupId))
            .sorted(Comparator.comparing((StoredArtifact a) -> a.groupId).thenComparing(a -> a.artifactId))
            .toList();

        int end = Math.min(offset + limit, matched.size());
        List<StoredArtifact> page = offset < matched.size() ? matched.subList(offset, end) : List.of();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("count", matched.size());
        ArrayNode arr = root.putArray("artifacts");
        for (StoredArtifact a : page) {
            ObjectNode node = arr.addObject();
            node.put("groupId", a.groupId);
            node.put("artifactId", a.artifactId);
            node.put("artifactType", a.artifactType);
        }
        sendJson(exchange, 200, root);
    }

    private void handleSearchVersionsGet(HttpExchange exchange, Map<String, String> query) throws IOException {
        Long globalId = query.containsKey("globalId") ? Long.parseLong(query.get("globalId")) : null;
        Long contentId = query.containsKey("contentId") ? Long.parseLong(query.get("contentId")) : null;

        List<StoredVersion> matched = new ArrayList<>();
        for (StoredArtifact a : artifacts.values()) {
            for (StoredVersion v : a.versions) {
                if (globalId != null && v.globalId != globalId) {
                    continue;
                }
                if (contentId != null && v.contentId != contentId) {
                    continue;
                }
                matched.add(v);
            }
        }
        sendVersionsSearchResponse(exchange, matched);
    }

    private void handleSearchVersionsPost(HttpExchange exchange, Map<String, String> query) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String groupId = query.get("groupId");
        String artifactId = query.get("artifactId");

        List<StoredVersion> matched = new ArrayList<>();
        for (StoredArtifact a : artifacts.values()) {
            if (groupId != null && !groupId.equals(a.groupId)) {
                continue;
            }
            if (artifactId != null && !artifactId.equals(a.artifactId)) {
                continue;
            }
            for (StoredVersion v : a.versions) {
                StoredContent sc = contentsById.get(v.contentId);
                if (sc != null && sc.content.equals(body)) {
                    matched.add(v);
                }
            }
        }
        sendVersionsSearchResponse(exchange, matched);
    }

    private void handleAdminRule(HttpExchange exchange, String method, String ruleType) throws IOException {
        if ("GET".equals(method) && "COMPATIBILITY".equals(ruleType)) {
            if (globalCompatibilityRule == null) {
                sendProblem(exchange, 404, "RuleNotFoundException", "Global COMPATIBILITY rule not found");
                return;
            }
            ObjectNode rule = MAPPER.createObjectNode();
            rule.put("ruleType", "COMPATIBILITY");
            rule.put("config", globalCompatibilityRule);
            sendJson(exchange, 200, rule);
            return;
        }
        sendProblem(exchange, 404, "RuleNotFoundException", "Rule not found");
    }

    private void handleIdsByContentId(HttpExchange exchange, String tail) throws IOException {
        if (tail.endsWith("/references")) {
            long contentId = Long.parseLong(tail.substring(0, tail.length() - "/references".length()));
            StoredContent sc = contentsById.get(contentId);
            if (sc == null) {
                sendProblem(exchange, 404, "ContentNotFoundException", "Content " + contentId + " not found");
                return;
            }
            sendReferences(exchange, sc.references);
            return;
        }
        long contentId = Long.parseLong(tail);
        StoredContent sc = contentsById.get(contentId);
        if (sc == null) {
            sendProblem(exchange, 404, "ContentNotFoundException", "Content " + contentId + " not found");
            return;
        }
        sendText(exchange, 200, "PROTOBUF".equals(sc.artifactType) ? "application/x-protobuf" : "application/json", sc.content);
    }

    private void handleIdsByGlobalId(HttpExchange exchange, String tail) throws IOException {
        boolean isRefs = tail.endsWith("/references");
        long globalId = Long.parseLong(isRefs ? tail.substring(0, tail.length() - "/references".length()) : tail);
        StoredVersion found = findVersionByGlobalId(globalId);
        if (found == null || "DISABLED".equals(found.state)) {
            sendProblem(exchange, 404, "ArtifactNotFoundException", "GlobalId " + globalId + " not found");
            return;
        }
        StoredContent sc = contentsById.get(found.contentId);
        if (isRefs) {
            sendReferences(exchange, sc.references);
        } else {
            sendText(exchange, 200, "PROTOBUF".equals(sc.artifactType) ? "application/x-protobuf" : "application/json", sc.content);
        }
    }

    private void handleIdsByContentHash(HttpExchange exchange, String tail) throws IOException {
        boolean isRefs = tail.endsWith("/references");
        String hash = decode(isRefs ? tail.substring(0, tail.length() - "/references".length()) : tail);
        StoredContent sc = contentsByHash.get(hash);
        if (sc == null) {
            sendProblem(exchange, 404, "ContentNotFoundException", "Content hash " + hash + " not found");
            return;
        }
        if (isRefs) {
            sendReferences(exchange, sc.references);
        } else {
            sendText(exchange, 200, "PROTOBUF".equals(sc.artifactType) ? "application/x-protobuf" : "application/json", sc.content);
        }
    }

    private void handleGroups(HttpExchange exchange, String method, String tail, Map<String, String> query) throws IOException {
        String[] parts = tail.split("/");
        String groupId = decode(parts[0]);

        // /groups/{groupId}/rules/{ruleType}
        if (parts.length == 3 && parts[1].equals("rules") && "GET".equals(method)) {
            String ruleType = decode(parts[2]);
            String cfg = "COMPATIBILITY".equals(ruleType) ? groupCompatibilityRules.get(groupId) : null;
            if (cfg == null) {
                sendProblem(exchange, 404, "RuleNotFoundException", "Group rule not found");
                return;
            }
            ObjectNode rule = MAPPER.createObjectNode();
            rule.put("ruleType", ruleType);
            rule.put("config", cfg);
            sendJson(exchange, 200, rule);
            return;
        }

        // POST /groups/{groupId}/artifacts
        if (parts.length == 2 && parts[1].equals("artifacts") && "POST".equals(method)) {
            handleCreateArtifact(exchange, groupId, query);
            return;
        }

        if (parts.length >= 3 && parts[1].equals("artifacts")) {
            String artifactId = decode(parts[2]);
            String artifactKey = groupId + "::" + artifactId;

            // DELETE /groups/{groupId}/artifacts/{artifactId}
            if (parts.length == 3 && "DELETE".equals(method)) {
                if (artifacts.remove(artifactKey) == null) {
                    sendProblem(exchange, 404, "ArtifactNotFoundException", "Artifact not found");
                    return;
                }
                artifactCompatibilityRules.remove(artifactKey);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            // /groups/{groupId}/artifacts/{artifactId}/rules...
            if (parts.length >= 4 && parts[3].equals("rules")) {
                handleArtifactRules(exchange, method, artifactKey, parts);
                return;
            }

            // /groups/{groupId}/artifacts/{artifactId}/versions...
            if (parts.length >= 4 && parts[3].equals("versions")) {
                handleArtifactVersions(exchange, method, artifactKey, parts);
                return;
            }
        }

        sendProblem(exchange, 404, "NotFound", "Unhandled group endpoint: " + method + " /groups/" + tail);
    }

    private void handleCreateArtifact(HttpExchange exchange, String groupId, Map<String, String> query) throws IOException {
        JsonNode body = MAPPER.readTree(exchange.getRequestBody());
        String artifactId = body.get("artifactId").asText();
        String artifactType = body.has("artifactType") ? body.get("artifactType").asText() : "AVRO";
        JsonNode contentNode = body.path("firstVersion").path("content");
        String content = contentNode.path("content").asText();
        List<StoredRef> refs = new ArrayList<>();
        if (contentNode.has("references") && contentNode.get("references").isArray()) {
            for (JsonNode refNode : contentNode.get("references")) {
                refs.add(new StoredRef(
                    refNode.path("groupId").asText("default"),
                    refNode.path("artifactId").asText(),
                    refNode.path("version").asText("1"),
                    refNode.path("name").asText()
                ));
            }
        }

        boolean dryRun = "true".equalsIgnoreCase(query.get("dryRun"));
        if (dryRun && rejectCompatibilityDryRun) {
            ObjectNode problem = MAPPER.createObjectNode();
            problem.put("status", 400);
            problem.put("name", "RuleViolationException");
            problem.put("title", "Compatibility rule violation");
            problem.put("detail", "Schema is not compatible with previous version");
            ArrayNode causes = problem.putArray("causes");
            ObjectNode cause = causes.addObject();
            cause.put("description", "Field removed without default");
            cause.put("context", "/fields/email");
            sendJson(exchange, 400, problem);
            return;
        }

        String key = groupId + "::" + artifactId;
        String ifExists = query.getOrDefault("ifExists", "FAIL");
        StoredArtifact existing = artifacts.get(key);
        if (existing != null && "FIND_OR_CREATE_VERSION".equals(ifExists)) {
            String hash = sha256(content + "::" + refs);
            for (StoredVersion v : existing.versions) {
                if (v.contentHash.equals(hash) && !"DISABLED".equals(v.state)) {
                    sendCreateArtifactResponse(exchange, existing, v);
                    return;
                }
            }
        }

        if (dryRun) {
            StoredVersion fake = new StoredVersion(groupId, artifactId, artifactType, "dry-run", 0L, 0L, "", "ENABLED");
            sendCreateArtifactResponse(exchange, existing != null ? existing : new StoredArtifact(groupId, artifactId, artifactType), fake);
            return;
        }

        StoredVersion created = seedArtifactVersion(groupId, artifactId, artifactType, content, "ENABLED", refs);
        sendCreateArtifactResponse(exchange, artifacts.get(key), created);
    }

    private void handleArtifactRules(HttpExchange exchange, String method, String artifactKey, String[] parts) throws IOException {
        if (parts.length == 4 && "POST".equals(method)) {
            JsonNode body = MAPPER.readTree(exchange.getRequestBody());
            String ruleType = body.path("ruleType").asText();
            String config = body.path("config").asText();
            if ("COMPATIBILITY".equals(ruleType)) {
                artifactCompatibilityRules.put(artifactKey, config);
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (parts.length == 5) {
            String ruleType = decode(parts[4]);
            if ("GET".equals(method)) {
                String cfg = "COMPATIBILITY".equals(ruleType) ? artifactCompatibilityRules.get(artifactKey) : null;
                if (cfg == null) {
                    sendProblem(exchange, 404, "RuleNotFoundException", "Artifact rule not found");
                    return;
                }
                ObjectNode rule = MAPPER.createObjectNode();
                rule.put("ruleType", ruleType);
                rule.put("config", cfg);
                sendJson(exchange, 200, rule);
                return;
            }
            if ("PUT".equals(method)) {
                if (!artifactCompatibilityRules.containsKey(artifactKey)) {
                    sendProblem(exchange, 404, "RuleNotFoundException", "Artifact rule not found");
                    return;
                }
                JsonNode body = MAPPER.readTree(exchange.getRequestBody());
                String cfg = body.path("config").asText();
                artifactCompatibilityRules.put(artifactKey, cfg);
                ObjectNode rule = MAPPER.createObjectNode();
                rule.put("ruleType", ruleType);
                rule.put("config", cfg);
                sendJson(exchange, 200, rule);
                return;
            }
        }
        sendProblem(exchange, 404, "RuleNotFoundException", "Rule not found");
    }

    private void handleArtifactVersions(HttpExchange exchange, String method, String artifactKey, String[] parts) throws IOException {
        StoredArtifact artifact = artifacts.get(artifactKey);
        if (artifact == null) {
            sendProblem(exchange, 404, "ArtifactNotFoundException", "Artifact not found");
            return;
        }

        // GET /groups/{g}/artifacts/{a}/versions
        if (parts.length == 4 && "GET".equals(method)) {
            sendVersionsSearchResponse(exchange, artifact.versions);
            return;
        }

        String versionExpr = decode(parts[4]);
        StoredVersion version = resolveVersionExpression(artifact, versionExpr);
        if (version == null) {
            sendProblem(exchange, 404, "VersionNotFoundException", "Version '" + versionExpr + "' not found");
            return;
        }

        if (parts.length == 5) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, toVersionMetaNode(version));
                return;
            }
            if ("DELETE".equals(method)) {
                artifact.versions.remove(version);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
        }

        if (parts.length == 6 && "GET".equals(method)) {
            StoredContent sc = contentsById.get(version.contentId);
            if (parts[5].equals("content")) {
                if ("DISABLED".equals(version.state)) {
                    sendProblem(exchange, 404, "VersionNotFoundException", "Disabled version");
                    return;
                }
                sendText(exchange, 200, "PROTOBUF".equals(sc.artifactType) ? "application/x-protobuf" : "application/json", sc.content);
                return;
            }
            if (parts[5].equals("references")) {
                sendReferences(exchange, sc.references);
                return;
            }
        }

        sendProblem(exchange, 404, "NotFound", "Unhandled version endpoint");
    }

    private StoredVersion resolveVersionExpression(StoredArtifact artifact, String expr) {
        if ("branch=latest".equals(expr)) {
            for (int i = artifact.versions.size() - 1; i >= 0; i--) {
                StoredVersion candidate = artifact.versions.get(i);
                if (!"DISABLED".equals(candidate.state) && !"DRAFT".equals(candidate.state)) {
                    return candidate;
                }
            }
            return null;
        }
        return artifact.versions.stream()
            .filter(v -> v.version.equals(expr))
            .findFirst()
            .orElse(null);
    }

    private StoredVersion findVersionByGlobalId(long globalId) {
        for (StoredArtifact a : artifacts.values()) {
            for (StoredVersion v : a.versions) {
                if (v.globalId == globalId) {
                    return v;
                }
            }
        }
        return null;
    }

    private void sendCreateArtifactResponse(HttpExchange exchange, StoredArtifact artifact, StoredVersion version) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode artNode = root.putObject("artifact");
        artNode.put("groupId", artifact.groupId);
        artNode.put("artifactId", artifact.artifactId);
        artNode.put("artifactType", artifact.artifactType);
        root.set("version", toVersionMetaNode(version));
        sendJson(exchange, 200, root);
    }

    private void sendVersionsSearchResponse(HttpExchange exchange, List<StoredVersion> versions) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("count", versions.size());
        ArrayNode arr = root.putArray("versions");
        for (StoredVersion v : versions) {
            arr.add(toVersionMetaNode(v));
        }
        sendJson(exchange, 200, root);
    }

    private void sendReferences(HttpExchange exchange, List<StoredRef> refs) throws IOException {
        ArrayNode arr = MAPPER.createArrayNode();
        for (StoredRef ref : refs) {
            ObjectNode n = arr.addObject();
            n.put("groupId", ref.groupId);
            n.put("artifactId", ref.artifactId);
            n.put("version", ref.version);
            n.put("name", ref.name);
        }
        sendJson(exchange, 200, arr);
    }

    private ObjectNode toVersionMetaNode(StoredVersion v) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("groupId", v.groupId);
        node.put("artifactId", v.artifactId);
        node.put("artifactType", v.artifactType);
        node.put("version", v.version);
        node.put("globalId", v.globalId);
        node.put("contentId", v.contentId);
        node.put("state", v.state);
        return node;
    }

    private void sendProblem(HttpExchange exchange, int status, String name, String detail) throws IOException {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("status", status);
        node.put("name", name);
        node.put("title", name);
        node.put("detail", detail);
        byte[] bytes = MAPPER.writeValueAsBytes(node);
        exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendJson(HttpExchange exchange, int status, JsonNode node) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(node);
        exchange.getResponseHeaders().set("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return map;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class StoredArtifact {
        final String groupId;
        final String artifactId;
        final String artifactType;
        final List<StoredVersion> versions = new ArrayList<>();

        StoredArtifact(String groupId, String artifactId, String artifactType) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.artifactType = artifactType;
        }
    }

    public record StoredContent(long contentId, String contentHash, String content, String artifactType, List<StoredRef> references) {}

    public record StoredVersion(
        String groupId,
        String artifactId,
        String artifactType,
        String version,
        long globalId,
        long contentId,
        String contentHash,
        String state
    ) {}

    public record StoredRef(String groupId, String artifactId, String version, String name) {}
}
