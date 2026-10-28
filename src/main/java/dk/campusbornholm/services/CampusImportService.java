package dk.campusbornholm.services;

import dk.campusbornholm.daos.DishDAO;
import dk.campusbornholm.daos.EventDAO;
import dk.campusbornholm.dtos.DishDTO;
import dk.campusbornholm.dtos.EventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CampusImportService {

    private static final Logger log = LoggerFactory.getLogger(CampusImportService.class);

    private final CampusApiService api;
    private final DishDAO dishDAO;
    private final EventDAO eventDAO;

    private final ExecutorService fetchPool = Executors.newFixedThreadPool(2, namedDaemon("campus-fetch"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(namedDaemon("campus-import"));

    public CampusImportService(CampusApiService api, DishDAO dishDAO, EventDAO eventDAO) {
        this.api = api;
        this.dishDAO = dishDAO;
        this.eventDAO = eventDAO;
    }

    public void start(long intervalHours) {
        scheduler.scheduleAtFixedRate(this::runSafely, 0, intervalHours, TimeUnit.HOURS);
        log.info("Import fra campus-API startet – kører hver {}. time", intervalHours);
    }

    public void stop() {
        shutdown(scheduler, "campus-import");
        shutdown(fetchPool, "campus-fetch");
        log.info("Import fra campus-API stoppet");
    }

    private static void shutdown(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{} nåede ikke at stoppe på 5 sekunder – tvinger stop", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public ImportResult importAll() {
        CompletableFuture<List<DishDTO>> dishes = CompletableFuture.supplyAsync(api::fetchDishes, fetchPool);
        CompletableFuture<List<EventDTO>> events = CompletableFuture.supplyAsync(api::fetchEvents, fetchPool);

        int savedDishes = saveDishes(dishes);
        int savedEvents = saveEvents(events);

        log.info("Import fra campus-API færdig: {} retter, {} events", savedDishes, savedEvents);
        return new ImportResult(savedDishes, savedEvents);
    }

    private int saveDishes(CompletableFuture<List<DishDTO>> future) {
        try {
            List<DishDTO> dishes = future.join();
            if (dishes.isEmpty()) {
                log.warn("Campus-API returnerede ingen retter – den gamle menu beholdes");
                return 0;
            }
            return dishDAO.saveAll(dishes).size();
        } catch (RuntimeException e) {
            log.warn("Menuen kunne ikke importeres – den gamle menu beholdes: {}", rootMessage(e));
            return 0;
        }
    }

    private int saveEvents(CompletableFuture<List<EventDTO>> future) {
        try {
            List<EventDTO> events = future.join();
            if (events.isEmpty()) {
                log.warn("Campus-API returnerede ingen aktiviteter – de gamle events beholdes");
                return 0;
            }
            return eventDAO.replaceImported(events);
        } catch (RuntimeException e) {
            log.warn("Aktiviteterne kunne ikke importeres – de gamle events beholdes: {}", rootMessage(e));
            return 0;
        }
    }

    private void runSafely() {
        try {
            importAll();
        } catch (Exception e) {
            log.error("Uventet fejl under import fra campus-API", e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
        return t.getMessage();
    }

    private static ThreadFactory namedDaemon(String name) {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    public record ImportResult(int dishes, int events) {
    }
}
