package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
//import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;

import java.net.URL;

@Getter
@EachProperty("connect")
//@Serdeable
public class Connect {

    String name;
    URL url;
    String basicAuthUsername;
    String basicAuthPassword;
    String sslTrustStore;
    String sslTrustStorePassword;
    String sslKeyStore;
    String sslKeyStorePassword;


}
