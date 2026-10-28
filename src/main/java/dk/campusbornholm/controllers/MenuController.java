package dk.campusbornholm.controllers;

import dk.campusbornholm.config.AppClock;
import dk.campusbornholm.daos.DishDAO;
import dk.campusbornholm.dtos.MenuWeekDTO;
import io.javalin.http.Context;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

public class MenuController {

    private final DishDAO dishDAO;
    private final Clock clock;

    public MenuController(DishDAO dishDAO) {
        this(dishDAO, Clock.system(AppClock.ZONE));
    }

    public MenuController(DishDAO dishDAO, Clock clock) {
        this.dishDAO = dishDAO;
        this.clock = clock;
    }

    public void today(Context ctx) {
        ctx.json(dishDAO.getByDate(LocalDate.now(clock)));
    }

    public void week(Context ctx) {
        LocalDate today = LocalDate.now(clock);
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);
        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());

        ctx.json(new MenuWeekDTO(weekNumber, monday, friday, dishDAO.getByDateRange(monday, friday)));
    }
}
