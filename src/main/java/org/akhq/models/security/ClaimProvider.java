package org.akhq.models.security;

public interface ClaimProvider {
    ClaimResponse generateClaim(ClaimRequest request);

}
