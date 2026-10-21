package dk.campusbornholm;

import dk.campusbornholm.config.ApplicationConfig;
import dk.campusbornholm.config.HibernateConfig;

public class Main {
    public static void main(String[] args) {
        HibernateConfig.getEntityManagerFactory();

        ApplicationConfig.startServer(7070);
    }
}
