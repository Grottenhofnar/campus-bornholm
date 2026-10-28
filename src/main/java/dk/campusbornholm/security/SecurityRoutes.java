package dk.campusbornholm.security;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class SecurityRoutes {

    public enum Role implements RouteRole { ANYONE, USER, ADMIN }

    private final SecurityController securityController;

    public SecurityRoutes(SecurityController securityController) {
        this.securityController = securityController;
    }

    public EndpointGroup getSecurityRoutes() {
        return () -> {
            path("/auth", () -> {
                get("/healthcheck", ctx -> ctx.json(Map.of("msg", "API is up and running")), Role.ANYONE);
                post("/register", securityController::register, Role.ANYONE);
                post("/login", securityController::login, Role.ANYONE);
                get("/me", securityController::me, Role.USER);
            });
        };
    }
}
