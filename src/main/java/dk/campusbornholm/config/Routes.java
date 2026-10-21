package dk.campusbornholm.config;

import io.javalin.apibuilder.EndpointGroup;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.get;

public final class Routes {
    private Routes() {
    }

    public static EndpointGroup getRoutes() {
        return () -> {
            get("/hello", ctx -> ctx.json(Map.of("msg", "hello fra Campus Bornholm")));
        };
    }
}
