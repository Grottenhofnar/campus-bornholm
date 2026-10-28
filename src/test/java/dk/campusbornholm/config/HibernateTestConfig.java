package dk.campusbornholm.config;

import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

public final class HibernateTestConfig {
    private static EntityManagerFactory emf;

    private HibernateTestConfig() {
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Properties props = HibernateConfig.baseProperties();
            props.put("hibernate.connection.driver_class", "org.testcontainers.jdbc.ContainerDatabaseDriver");
            props.put("hibernate.connection.url", "jdbc:tc:postgresql:16.2:///test_db");
            props.put("hibernate.connection.username", "postgres");
            props.put("hibernate.connection.password", "postgres");
            props.put("hibernate.hbm2ddl.auto", "create-drop");
            emf = HibernateConfig.build(props);
        }
        return emf;
    }
}
