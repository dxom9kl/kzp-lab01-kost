package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportCompatibilityTest {
    /* Вхідний файл і звіт, отримані з тегу v1.0.0 до перебудови програми. */
    private static final Path BASELINE = Path.of("src", "test", "resources", "lab01");

    /* Контрольний звіт зберігається з LF, а Windows завершує рядки CRLF. */
    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static PrintStream discard() {
        return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    }

    /* Звіт на контрольному вході збігається зі звітом лабораторної роботи № 1. */
    @Test
    void reportMatchesLab01Baseline(@TempDir Path directory) throws IOException {
        // Arrange
        Path input = BASELINE.resolve("input.csv");
        Path output = directory.resolve("report.txt");

        // Act
        int code = new DeliveryController(input, output).run(discard(), discard());

        // Assert: заголовок звіту містить шлях, тому підставляємо шлях контрольного запуску.
        assertEquals(ExitCode.OK, code);
        String actual = read(output).replace(input.toString(), "data/input.csv");
        assertEquals(read(BASELINE.resolve("report.txt")), actual);
    }
}
