package org.kafkahq.models;

import lombok.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
public class User {

    private String name;
    private String encodedName;

    private Map<String, Map<HostResource,List<String>>> acls;

    public static String encodeUsername(String username){
        return Base64.getEncoder().encodeToString(username.getBytes());
    }

    public static String decodeUsername(String username){
        return new String(Base64.getDecoder().decode(username));
    }

    public User(String name){
        this.name = name;
        this.encodedName = encodeUsername(name);
    }

    public User(String name, Map<String, Map<HostResource,List<String>>> acls){
        this(name);
        this.acls = acls;
    }

    @Data
    @AllArgsConstructor
    public static class HostResource{
        private String host;
        private String resource;
    }

}
