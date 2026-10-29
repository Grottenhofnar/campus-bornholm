package dk.campusbornholm.config;

import dk.campusbornholm.controllers.EventController;
import dk.campusbornholm.controllers.MenuController;
import dk.campusbornholm.daos.DishDAO;
import dk.campusbornholm.daos.EventDAO;
import dk.campusbornholm.security.SecurityController;
import dk.campusbornholm.security.SecurityRoutes;
import dk.campusbornholm.security.SecurityRoutes.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Routes {

    private final SecurityRoutes securityRoutes;
    private final EventController eventController;
    private final MenuController menuController;

    public Routes(EntityManagerFactory emf, SecurityController securityController) {
        this.securityRoutes = new SecurityRoutes(securityController);
        this.eventController = new EventController(new EventDAO(emf));
        this.menuController = new MenuController(new DishDAO(emf));
    }

    public EndpointGroup getRoutes() {
        return () -> {
            get("/hello", ctx -> ctx.json(Map.of("msg", "hello fra Campus Bornholm")), Role.ANYONE);

            securityRoutes.getSecurityRoutes().addEndpoints();

            path("/events", () -> {
                get(eventController::getUpcoming, Role.ANYONE);
                post(eventController::create, Role.ADMIN);
                path("/{id}", () -> {
                    get(eventController::getById, Role.ANYONE);
                    patch(eventController::update, Role.ADMIN);
                    delete(eventController::delete, Role.ADMIN);
                });
            });

            path("/menu", () -> {
                get("/today", menuController::today, Role.ANYONE);
                get("/week", menuController::week, Role.ANYONE);
            });
        };
    }
}
