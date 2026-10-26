package dk.campusbornholm.config;

import dk.campusbornholm.entities.Dish;
import dk.campusbornholm.entities.Event;
import dk.campusbornholm.entities.Guess;
import dk.campusbornholm.entities.News;
import dk.campusbornholm.entities.Role;
import dk.campusbornholm.entities.User;
import dk.campusbornholm.entities.Word;
import dk.campusbornholm.entities.WordleGame;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public final class HibernateConfig {
    private static EntityManagerFactory emf;

    private HibernateConfig() {
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Properties props = baseProperties();
            props.put("hibernate.connection.url", AppProperties.get("CONNECTION_STR") + AppProperties.get("DB_NAME"));
            props.put("hibernate.connection.username", AppProperties.get("DB_USERNAME"));
            props.put("hibernate.connection.password", AppProperties.get("DB_PASSWORD"));
            props.put("hibernate.hbm2ddl.auto", "update");
            emf = build(props);
        }
        return emf;
    }

    static Properties baseProperties() {
        Properties props = new Properties();
        props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        props.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        props.put("hibernate.hikari.maximumPoolSize", "10");
        props.put("hibernate.hikari.minimumIdle", "2");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "true");
        props.put("hibernate.current_session_context_class", "thread");
        return props;
    }

    static EntityManagerFactory build(Properties props) {
        Configuration configuration = new Configuration();
        configuration.setProperties(props);
        registerEntities(configuration);
        return configuration.buildSessionFactory().unwrap(EntityManagerFactory.class);
    }

    private static void registerEntities(Configuration configuration) {
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Role.class);
        configuration.addAnnotatedClass(News.class);
        configuration.addAnnotatedClass(Event.class);
        configuration.addAnnotatedClass(Dish.class);
        configuration.addAnnotatedClass(Word.class);
        configuration.addAnnotatedClass(WordleGame.class);
        configuration.addAnnotatedClass(Guess.class);
    }
}
