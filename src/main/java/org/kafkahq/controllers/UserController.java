package org.kafkahq.controllers;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.views.View;
import org.kafkahq.configs.Role;
import org.kafkahq.repositories.UserRepository;

import javax.inject.Inject;
import java.util.Optional;

@Secured(Role.ROLE_USER_READ)
@Controller("${kafkahq.server.base-path:}/{cluster}/user")
public class UserController extends AbstractController {

    private UserRepository userRepository;

    @Inject
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Get
    @View("userList")
    public HttpResponse list(HttpRequest request, String cluster,
                             Optional<String> search) {
        return this.template(
                request,
                cluster,
                "search", search,
                "users", userRepository.findAll(cluster, search)
        );
    }

    @View("user")
    @Get("{user}")
    public HttpResponse user (HttpRequest request, String cluster, String user){
        return this.template(
                request,
                cluster,
                "tab", "topic",
                "user", userRepository.findByUser(cluster, user, Optional.of("topic"))
        );
    }

    @View("user")
    @Get("{user}/{tab:(group)}")
    public HttpResponse tab (HttpRequest request, String cluster, String user, String tab){
        return this.template(
                request,
                cluster,
                "tab", tab,
                "user", userRepository.findByUser(cluster, user, Optional.of(tab))
        );
    }

}
