package org.akhq.security.authentication.mcp;

import com.nimbusds.jwt.JWTClaimsSet;

public interface McpOauthTokenValidator {
    JWTClaimsSet validate(String token) throws Exception;
}
