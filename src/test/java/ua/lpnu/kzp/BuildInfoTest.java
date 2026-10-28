package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuildInfoTest {
    /* Перевіряє, що Maven підставив версію та номер збірки, а не залишив шаблони ${...}. */
    @Test
    void versionContainsProjectVersionAndBuildNumber() {
        String version = BuildInfo.version();

        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+\\+\\w+"),
                "Неочікуваний формат версії: %s".formatted(version));
    }
}
