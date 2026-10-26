package dk.campusbornholm.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class AppProperties {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke læse config.properties", e);
        }
    }

    private AppProperties() {
    }

    public static String get(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException("Mangler indstillingen '" + key
                + "'. Sæt den i config.properties eller som miljøvariabel."));
    }

    public static Optional<String> find(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return Optional.of(env);
        }
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
