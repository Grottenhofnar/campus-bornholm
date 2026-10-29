package dk.campusbornholm.services;

import dk.campusbornholm.dtos.DishDTO;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.dtos.external.CampusActivityDTO;
import dk.campusbornholm.dtos.external.CampusMenuDTO;
import dk.campusbornholm.dtos.external.CampusMenuItemDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class CampusApiServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    @Test
    @DisplayName("toDishes mapper en ret fra campus-API'et til DishDTO og trimmer titlen")
    void toDishes() {
        CampusMenuDTO menu = new CampusMenuDTO(44, 2026, List.of(
                new CampusMenuItemDTO(MONDAY, "  Cajun pasta  ", "med kylling", 40.0)));

        List<DishDTO> dishes = CampusApiService.toDishes(menu, "https://campus/menu");

        assertThat(dishes, hasSize(1));
        DishDTO dish = dishes.get(0);
        assertThat(dish.name(), is("Cajun pasta"));
        assertThat(dish.price(), is(40.0));
        assertThat(dish.date(), is(MONDAY));
        assertThat(dish.id(), is(nullValue()));
    }

    @Test
    @DisplayName("Retter uden titel og aktiviteter uden starttid bliver sprunget over")
    void skipsInvalidRows() {
        CampusMenuDTO menu = new CampusMenuDTO(44, 2026, List.of(
                new CampusMenuItemDTO(MONDAY, "", "Lukket", null)));
        List<CampusActivityDTO> activities = List.of(
                new CampusActivityDTO("Uden starttid", null, null, "Aulaen", null),
                new CampusActivityDTO("AMU Kursus Kran", LocalDateTime.of(2026, 10, 26, 8, 0), null, "", null));

        assertThat(CampusApiService.toDishes(menu, "x"), is(empty()));

        List<EventDTO> events = CampusApiService.toEvents(activities);
        assertThat(events, hasSize(1));
        assertThat(events.get(0).title(), is("AMU Kursus Kran"));
        assertThat(events.get(0).location(), is(nullValue()));
        assertThat(events.get(0).createdBy(), is(nullValue()));
    }

    @Test
    @DisplayName("null fra API'et giver tomme lister i stedet for en fejl")
    void nullSafe() {
        assertThat(CampusApiService.toDishes(null, "x"), is(empty()));
        assertThat(CampusApiService.toEvents(null), is(empty()));
    }
}
