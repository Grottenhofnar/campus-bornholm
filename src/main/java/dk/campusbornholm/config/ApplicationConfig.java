package dk.campusbornholm.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.campusbornholm.exceptions.ApiException;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

public final class ApplicationConfig {
    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    private static Javalin app;

    private ApplicationConfig() {
    }

    public static Javalin startServer(int port) {
        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.router.contextPath = "/api";
            config.router.apiBuilder(Routes.getRoutes());

            config.bundledPlugins.enableRouteOverview("/routes");

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            config.requestLogger.http((ctx, ms) ->
                    log.info("{} {} -> {} ({} ms)", ctx.method(), ctx.path(), ctx.status().getCode(), Math.round(ms)));
        });

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

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Uventet fejl på {}", ctx.path(), e);
            ctx.status(500);
            ctx.json(errorBody(500, "Intern serverfejl"));
        });
    }

    private static Map<String, Object> errorBody(int status, String message) {
        return Map.of(
                "status", status,
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
