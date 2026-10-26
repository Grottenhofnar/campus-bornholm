package dk.campusbornholm.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.campusbornholm.daos.UserDAO;
import dk.campusbornholm.exceptions.ApiException;
import dk.campusbornholm.security.SecurityController;
import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.ValidationException;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public final class ApplicationConfig {
    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    private static Javalin app;

    private ApplicationConfig() {
    }

    public static Javalin startServer(int port, EntityManagerFactory emf) {
        SecurityController securityController = new SecurityController(new UserDAO(emf));
        Routes routes = new Routes(emf, securityController);
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.router.contextPath = "/api";
            config.router.apiBuilder(routes.getRoutes());

            config.bundledPlugins.enableRouteOverview("/routes");
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
            config.bundledPlugins.enableHttpAllowedMethodsOnRoutes();

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            config.requestLogger.http((ctx, ms) ->
                    log.info("{} {} -> {} ({} ms)", ctx.method(), ctx.path(), ctx.status().getCode(), Math.round(ms)));
        });

        app.beforeMatched(securityController::authenticate);
        app.beforeMatched(securityController::authorize);

        registerExceptionHandlers(app);
        app.start(port);
        return app;
    }

    public static void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    private static void registerExceptionHandlers(Javalin app) {
        app.exception(ApiException.class, (e, ctx) -> {
            log.warn("ApiException {} på {}: {}", e.getStatusCode(), ctx.path(), e.getMessage());
            ctx.status(e.getStatusCode());
            ctx.json(errorBody(e.getStatusCode(), e.getMessage()));
        });

        app.exception(HttpResponseException.class, (e, ctx) -> {
            log.warn("HTTP {} på {}: {}", e.getStatus(), ctx.path(), e.getMessage());
            ctx.status(e.getStatus());
            ctx.json(errorBody(e.getStatus(), e.getMessage()));
        });

        app.exception(ValidationException.class, (e, ctx) -> {
            String message = validationMessage(e);
            log.warn("Validering fejlede på {}: {}", ctx.path(), message);
            ctx.status(400);
            ctx.json(errorBody(400, message));
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Uventet fejl på {}", ctx.path(), e);
            ctx.status(500);
            ctx.json(errorBody(500, "Intern serverfejl"));
        });
    }

    private static String validationMessage(ValidationException e) {
        return e.getErrors().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(error -> readable(entry.getKey(), error.getMessage())))
                .collect(Collectors.joining("; "));
    }

    private static String readable(String field, String message) {
        return switch (message) {
            case "DESERIALIZATION_FAILED" -> "Body er ikke gyldig JSON eller har forkerte typer";
            case "TYPE_CONVERSION_FAILED" -> "'" + field + "' har en ugyldig værdi";
            case "NULLCHECK_FAILED" -> "'" + field + "' mangler";
            default -> message;
        };
    }

    private static Map<String, Object> errorBody(int status, String message) {
        return Map.of(
                "status", status,
                "msg", message,
                "timestamp", AppClock.now().toString()
        );
    }
}
