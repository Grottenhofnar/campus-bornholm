package dk.campusbornholm.controllers;

import io.javalin.http.Context;

final class ControllerUtils {

    static final int MAX_TEXT = 255;

    private ControllerUtils() {
    }

    static Long id(Context ctx) {
        return ctx.pathParamAsClass("id", Long.class)
                .check(id -> id > 0, "id skal være et positivt tal")
                .get();
    }

    static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    static boolean nullOrNotBlank(String s) {
        return s == null || !s.isBlank();
    }

    static boolean maxLength(String s, int max) {
        return s == null || s.length() <= max;
    }
}
