package org.akhq.azure;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import reactor.core.publisher.Mono;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

public class KafkaOAuth2AuthenticateCallbackHandler implements AuthenticateCallbackHandler {
    private static final Duration ACCESS_TOKEN_REQUEST_BLOCK_TIME = Duration.ofSeconds(30);
    private static final String TOKEN_AUDIENCE_FORMAT = "%s://%s/.default";

    private Function<TokenCredential, Mono<OAuthBearerTokenImpl>> resolveToken;
    private final TokenCredential credential = new DefaultAzureCredentialBuilder().build();

    @Override
    public void configure(Map<String, ?> configs, String mechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        TokenRequestContext request = buildTokenRequestContext(configs);
        this.resolveToken = tokenCredential -> tokenCredential.getToken(request).map(OAuthBearerTokenImpl::new);
    }

    private TokenRequestContext buildTokenRequestContext(Map<String, ?> configs) {
        URI uri = buildEventHubsServerUri(configs);
        String tokenAudience = buildTokenAudience(uri);
        TokenRequestContext request = new TokenRequestContext();
        request.addScopes(tokenAudience);
        return request;
    }

    private URI buildEventHubsServerUri(Map<String, ?> configs) {
        String bootstrapServer = Arrays.asList(configs.get(BOOTSTRAP_SERVERS_CONFIG)).get(0).toString();
        bootstrapServer = bootstrapServer.replaceAll("\\[|\\]", "");
        return URI.create("https://" + bootstrapServer);
    }

    private String buildTokenAudience(URI uri) {
        return TOKEN_AUDIENCE_FORMAT.formatted(uri.getScheme(), uri.getHost());
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback oauthCallback) {
                this.resolveToken
                        .apply(credential)
                        .doOnNext(oauthCallback::token)
                        .doOnError(throwable -> oauthCallback.error("invalid_grant", throwable.getMessage(), null))
                        .block(ACCESS_TOKEN_REQUEST_BLOCK_TIME);
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    @Override
    public void close() {
        //Pas besoin de modifier cette méthode
    }
}

