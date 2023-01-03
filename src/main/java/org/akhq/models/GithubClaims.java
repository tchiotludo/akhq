package org.akhq.models;

import io.micronaut.security.token.Claims;

import java.util.HashMap;
import java.util.Set;

public class GithubClaims extends HashMap<String, Object> implements Claims {
    @Override
    public Object get(String name) {
        return super.get(name);
    }

    @Override
    public Set<String> names() {
        return super.keySet();
    }

    @Override
    public boolean contains(String name) {
        return super.containsKey(name);
    }
}