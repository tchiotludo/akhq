package org.akhq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.akhq.AbstractTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeaderAuthControllerTest extends AbstractTest {
    @Inject
    @Client("/")
    protected RxHttpClient client;

    @Test
    void user() {
        AkhqController.AuthUser result = client.toBlocking().retrieve(
            HttpRequest
                .GET("/api/me")
                .header("x-akhq-user", "header-user"),
            AkhqController.AuthUser.class
        );

        assertEquals("header-user", result.getUsername());
        assertEquals(6, result.getRoles().size());
    }

    @Test
    void admin() {
        AkhqController.AuthUser result = client.toBlocking().retrieve(
            HttpRequest
                .GET("/api/me")
                .header("x-akhq-user", "header-admin"),
            AkhqController.AuthUser.class
        );

        assertEquals("header-admin", result.getUsername());
        assertEquals(35, result.getRoles().size());
    }

    @Test
    void userGroup() {
        AkhqController.AuthUser result = client.toBlocking().retrieve(
            HttpRequest
                .GET("/api/me")
                .header("x-akhq-user", "header-user")
                .header("x-akhq-group", "limited,operator"),
            AkhqController.AuthUser.class
        );

        assertEquals("header-user", result.getUsername());
        assertEquals(11, result.getRoles().size());
    }

    @Test
    void invalidUser() {
        AkhqController.AuthUser result = client.toBlocking().retrieve(
            HttpRequest
                .GET("/api/me")
                .header("x-akhq-user", "header-invalid"),
            AkhqController.AuthUser.class
        );

        assertEquals(null, result.getUsername());
        assertEquals(7, result.getRoles().size());
    }

    @MicronautTest(environments = "overridegroups")
    public static class NoUser extends AbstractTest {
        @Inject
        @Client("/")
        protected RxHttpClient client;

        @Test
        void invalidUser() {
            AkhqController.AuthUser result = client.toBlocking().retrieve(
                HttpRequest
                    .GET("/api/me")
                    .header("x-akhq-user", "header-user")
                    .header("x-akhq-group", "limited,extra"),
                AkhqController.AuthUser.class
            );

            assertEquals("header-user", result.getUsername());
            assertEquals(3, result.getRoles().size());
        }
    }

    @MicronautTest(environments = "header-ip-disallow")
    public static class UntrustedIp extends AbstractTest {
        @Inject
        @Client("/")
        protected RxHttpClient client;

        @Test
        void invalidIp() {
            AkhqController.AuthUser result = client.toBlocking().retrieve(
                HttpRequest
                    .GET("/api/me")
                    .header("x-akhq-user", "header-user")
                    .header("x-akhq-group", "limited,extra"),
                AkhqController.AuthUser.class
            );

            assertNull(result.getUsername());
            assertNull(result.getRoles());
        }
    }
}
