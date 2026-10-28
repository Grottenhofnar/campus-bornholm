package dk.campusbornholm.controllers;

import dk.campusbornholm.daos.NewsDAO;
import dk.campusbornholm.dtos.NewsDTO;
import dk.campusbornholm.security.SecurityController;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import static dk.campusbornholm.controllers.ControllerUtils.MAX_TEXT;
import static dk.campusbornholm.controllers.ControllerUtils.id;
import static dk.campusbornholm.controllers.ControllerUtils.maxLength;
import static dk.campusbornholm.controllers.ControllerUtils.notBlank;
import static dk.campusbornholm.controllers.ControllerUtils.nullOrNotBlank;

public class NewsController {

    private static final int MAX_LIMIT = 100;

    private final NewsDAO newsDAO;

    public NewsController(NewsDAO newsDAO) {
        this.newsDAO = newsDAO;
    }

    public void getAll(Context ctx) {
        if (ctx.queryParam("limit") == null) {
            ctx.json(newsDAO.getAll());
            return;
        }
        int limit = ctx.queryParamAsClass("limit", Integer.class)
                .check(l -> l >= 1 && l <= MAX_LIMIT, "limit skal være mellem 1 og " + MAX_LIMIT)
                .get();
        ctx.json(newsDAO.getLatest(limit));
    }

    public void getById(Context ctx) {
        ctx.json(newsDAO.getById(id(ctx)));
    }

    public void create(Context ctx) {
        NewsDTO dto = ctx.bodyValidator(NewsDTO.class)
                .check(d -> notBlank(d.title()), "title skal udfyldes")
                .check(d -> maxLength(d.title(), MAX_TEXT), "title må højst være " + MAX_TEXT + " tegn")
                .check(d -> notBlank(d.content()), "content skal udfyldes")
                .get();

        String author = SecurityController.loggedInUsername(ctx);
        NewsDTO toCreate = new NewsDTO(null, dto.title(), dto.content(), null, author);
        ctx.status(HttpStatus.CREATED).json(newsDAO.create(toCreate));
    }

    public void update(Context ctx) {
        Long id = id(ctx);
        NewsDTO dto = ctx.bodyValidator(NewsDTO.class)
                .check(d -> d.title() != null || d.content() != null, "send mindst title eller content")
                .check(d -> nullOrNotBlank(d.title()), "title må ikke være tom")
                .check(d -> maxLength(d.title(), MAX_TEXT), "title må højst være " + MAX_TEXT + " tegn")
                .check(d -> nullOrNotBlank(d.content()), "content må ikke være tom")
                .get();

        ctx.json(newsDAO.update(id, dto));
    }

    public void delete(Context ctx) {
        newsDAO.delete(id(ctx));
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
