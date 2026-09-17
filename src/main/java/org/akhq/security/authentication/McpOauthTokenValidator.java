package org.akhq.security.authentication;

import com.nimbusds.jwt.JWTClaimsSet;

public interface McpOauthTokenValidator {
    JWTClaimsSet validate(String token) throws Exception;
}
