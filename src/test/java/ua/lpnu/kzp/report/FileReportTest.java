package ua.lpnu.kzp.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileReportTest {
    @TempDir
    Path directory;

    /* Український текст записується й читається без втрат незалежно від кодування ОС. */
    @Test
    void keepsUkrainianText() throws IOException {
        Path file = directory.resolve("report.txt");

        FileReport.writeReport(file, "Звіт: доставлення посилок%nКам'янець-Подільський%n".formatted());

        assertEquals(List.of("Звіт: доставлення посилок", "Кам'янець-Подільський"), FileReport.readLines(file));
        assertEquals("Звіт", new String(Files.readAllBytes(file), StandardCharsets.UTF_8).substring(0, 4));
    }

    /* Відсутні каталоги на шляху до звіту створюються автоматично. */
    @Test
    void createsMissingDirectories() throws IOException {
        Path file = directory.resolve("out").resolve("nested").resolve("report.txt");

        FileReport.writeReport(file, "текст");

        assertEquals("текст", Files.readString(file, StandardCharsets.UTF_8));
    }

    /* Відсутній вхідний файл повідомляється окремим типом винятку. */
    @Test
    void reportsMissingFile() {
        assertThrows(NoSuchFileException.class, () -> FileReport.readLines(directory.resolve("nope.csv")));
    }
}
