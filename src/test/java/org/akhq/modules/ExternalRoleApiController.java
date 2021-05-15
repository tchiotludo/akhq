package org.akhq.modules;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import org.akhq.utils.ClaimProvider;

@Controller("/external-role-api")
public class ExternalRoleApiController {

    @Post
    public String computeRolesAndAttributes(@Body ClaimProvider.AKHQClaimRequest claimRequest){
        return  "{\n" +
                "  \"roles\": [\"topic/read\", \"topic/write\"],\n" +
                "  \"attributes\": \n" +
                "  {\n" +
                "    \"topics-filter-regexp\": [\".*\"],\n" +
                "    \"connects-filter-regexp\": [\".*\"],\n" +
                "    \"consumer-groups-filter-regexp\": [\".*\"]\n" +
                "  }\n" +
                "}";
    }
}
