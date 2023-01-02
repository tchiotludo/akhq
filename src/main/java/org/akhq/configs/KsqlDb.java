package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import lombok.Getter;

import java.net.URL;

@Getter
@EachProperty("ksqldb")
public class KsqlDb {
    String name;
    URL url;
    boolean useTls = false;
    boolean useAlpn = false;
    boolean verifyHost = true;
    String basicAuthUsername;
    String basicAuthPassword;
}