package ua.lpnu.kzp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Надає версію програми та номер збірки, які Maven підставляє з pom.xml. */
public final class BuildInfo {
    private static final String RESOURCE = "/version.properties";

    /* Забороняє створення екземплярів службового класу. */
    private BuildInfo() {
    }

    /**
     * Повертає версію з pom.xml разом із номером збірки.
     *
     * @return рядок у форматі {@code версія+збірка}, наприклад {@code 1.0.0+42}
     * @throws IllegalStateException якщо ресурс відсутній або не містить потрібних ключів
     * @throws UncheckedIOException якщо ресурс не вдалося прочитати
     */
    public static String version() {
        Properties properties = new Properties();
        try (InputStream input = BuildInfo.class.getResourceAsStream(RESOURCE)) {
            // Ресурс відсутній, якщо програму скомпільовано без Maven.
            if (input == null) {
                throw new IllegalStateException("Ресурс %s не знайдено".formatted(RESOURCE));
            }
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        String version = properties.getProperty("version");
        String build = properties.getProperty("build");
        if (version == null || build == null) {
            throw new IllegalStateException("У %s немає version або build".formatted(RESOURCE));
        }
        return "%s+%s".formatted(version, build);
    }
}
