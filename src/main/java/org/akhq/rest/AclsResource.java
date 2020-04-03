package org.akhq.rest;

import io.micronaut.http.annotation.Controller;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller("${akhq.server.base-path:}/api")
public class AclsResource {
}
