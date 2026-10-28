package dk.campusbornholm.controllers;

import dk.campusbornholm.daos.EventDAO;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.security.SecurityController;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import static dk.campusbornholm.controllers.ControllerUtils.MAX_TEXT;
import static dk.campusbornholm.controllers.ControllerUtils.id;
import static dk.campusbornholm.controllers.ControllerUtils.maxLength;
import static dk.campusbornholm.controllers.ControllerUtils.notBlank;
import static dk.campusbornholm.controllers.ControllerUtils.nullOrNotBlank;

public class EventController {

    public static final int DEFAULT_DAYS = 14;
    private static final int MAX_DAYS = 365;

    private final EventDAO eventDAO;

    public EventController(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    public void getUpcoming(Context ctx) {
        int days = DEFAULT_DAYS;
        if (ctx.queryParam("days") != null) {
            days = ctx.queryParamAsClass("days", Integer.class)
                    .check(d -> d >= 1 && d <= MAX_DAYS, "days skal være mellem 1 og " + MAX_DAYS)
                    .get();
        }
        ctx.json(eventDAO.getUpcoming(days));
    }

    public void getById(Context ctx) {
        ctx.json(eventDAO.getById(id(ctx)));
    }

    public void create(Context ctx) {
        EventDTO dto = ctx.bodyValidator(EventDTO.class)
                .check(d -> notBlank(d.title()), "title skal udfyldes")
                .check(d -> maxLength(d.title(), MAX_TEXT), "title må højst være " + MAX_TEXT + " tegn")
                .check(d -> maxLength(d.location(), MAX_TEXT), "location må højst være " + MAX_TEXT + " tegn")
                .check(d -> d.startTime() != null, "startTime skal udfyldes")
                .check(d -> endAfterStart(d), "endTime skal være efter startTime")
                .get();

        String creator = SecurityController.loggedInUsername(ctx);
        EventDTO toCreate = new EventDTO(null, dto.title(), dto.description(), dto.location(),
                dto.startTime(), dto.endTime(), creator);
        ctx.status(HttpStatus.CREATED).json(eventDAO.create(toCreate));
    }

    public void update(Context ctx) {
        Long id = id(ctx);
        EventDTO dto = ctx.bodyValidator(EventDTO.class)
                .check(d -> nullOrNotBlank(d.title()), "title må ikke være tom")
                .check(d -> maxLength(d.title(), MAX_TEXT), "title må højst være " + MAX_TEXT + " tegn")
                .check(d -> maxLength(d.location(), MAX_TEXT), "location må højst være " + MAX_TEXT + " tegn")
                .check(d -> endAfterStart(d), "endTime skal være efter startTime")
                .get();

        ctx.json(eventDAO.update(id, dto));
    }

    public void delete(Context ctx) {
        eventDAO.delete(id(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private static boolean endAfterStart(EventDTO d) {
        return d.startTime() == null || d.endTime() == null || d.endTime().isAfter(d.startTime());
    }
}
