package org.akhq.utils;

public interface ClaimProvider {
    ClaimResponse generateClaim(ClaimRequest request);

}
