package dk.campusbornholm;

import dk.campusbornholm.config.AppProperties;
import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateConfig;
import dk.campusbornholm.daos.DishDAO;
import dk.campusbornholm.daos.EventDAO;
import dk.campusbornholm.daos.UserDAO;
import dk.campusbornholm.services.CampusApiService;
import dk.campusbornholm.services.CampusImportService;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final long IMPORT_INTERVAL_HOURS = 6;

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        createAdmin(new UserDAO(emf));

        ApplicationConfig.startServer(7070, emf);

        CampusImportService importService = new CampusImportService(
                new CampusApiService(),
                new DishDAO(emf),
                new EventDAO(emf));
        importService.start(IMPORT_INTERVAL_HOURS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Lukker ned ...");
            importService.stop();
            ApplicationConfig.stopServer();
            emf.close();
        }, "shutdown"));
    }

    private static void createAdmin(UserDAO userDAO) {
        var username = AppProperties.find("ADMIN_USERNAME");
        var password = AppProperties.find("ADMIN_PASSWORD");
        if (username.isEmpty() || password.isEmpty()) {
            log.warn("ADMIN_USERNAME / ADMIN_PASSWORD er ikke sat – der oprettes ingen admin");
            return;
        }
        userDAO.createAdminIfMissing(username.get(), password.get());
        log.info("Admin-brugeren '{}' er klar", username.get());
    }
}
