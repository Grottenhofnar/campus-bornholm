package dk.campusbornholm.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.campusbornholm.dtos.DishDTO;
import dk.campusbornholm.dtos.EventDTO;
import dk.campusbornholm.dtos.external.CampusActivityDTO;
import dk.campusbornholm.dtos.external.CampusMenuDTO;
import dk.campusbornholm.exceptions.ApiException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class CampusApiService {

    public static final String DEFAULT_BASE_URL = "https://campusnews.showcode.dk";

    private final String baseUrl;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public CampusApiService() {
        this(DEFAULT_BASE_URL);
    }

    public CampusApiService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = createMapper();
    }

    public static ObjectMapper createMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    public CampusMenuDTO fetchMenu() {
        return read(get("/menu"), new TypeReference<CampusMenuDTO>() {
        }, "/menu");
    }

    public List<CampusActivityDTO> fetchActivities() {
        return read(get("/activities"), new TypeReference<List<CampusActivityDTO>>() {
        }, "/activities");
    }

    public List<DishDTO> fetchDishes() {
        return toDishes(fetchMenu(), baseUrl + "/menu");
    }

    public List<EventDTO> fetchEvents() {
        return toEvents(fetchActivities());
    }

    public static List<DishDTO> toDishes(CampusMenuDTO menu, String sourceUrl) {
        if (menu == null || menu.items() == null) {
            return List.of();
        }
        return menu.items().stream()
                .filter(item -> item != null && item.date() != null && !isBlank(item.title()))
                .map(item -> new DishDTO(
                        null,
                        item.title().trim(),
                        blankToNull(item.description()),
                        item.priceDkk(),
                        item.date(),
                        sourceUrl))
                .toList();
    }

    public static List<EventDTO> toEvents(List<CampusActivityDTO> activities) {
        if (activities == null) {
            return List.of();
        }
        return activities.stream()
                .filter(a -> a != null && a.startDateTime() != null && !isBlank(a.title()))
                .map(a -> new EventDTO(
                        null,
                        a.title().trim(),
                        null,
                        blankToNull(a.location()),
                        a.startDateTime(),
                        a.endDateTime(),
                        null))
                .toList();
    }

    private String get(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException(502, "Campus-API svarede " + response.statusCode() + " på " + path);
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException(502, "Kunne ikke hente " + path + " fra campus-API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Hentning af " + path + " blev afbrudt", e);
        }
    }

    private <T> T read(String json, TypeReference<T> type, String path) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException(502, "Ugyldig JSON fra campus-API på " + path + ": " + e.getMessage(), e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
