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

    /* BOM, який додають Excel і Блокнот, не потрапляє в перший рядок. */
    @Test
    void removesByteOrderMark() throws IOException {
        Path file = directory.resolve("bom.csv");
        Files.writeString(file, "﻿UA-1;2.00;Київ;9;true%nUA-2;4.00;Львів;3;false%n".formatted(),
                StandardCharsets.UTF_8);

        assertEquals(List.of("UA-1;2.00;Київ;9;true", "UA-2;4.00;Львів;3;false"), FileReport.readLines(file));
    }

    /* Порожній файл і файл лише з BOM читаються без помилок. */
    @Test
    void readsEmptyFiles() throws IOException {
        Path empty = directory.resolve("empty.csv");
        Path onlyBom = directory.resolve("bom-only.csv");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        Files.writeString(onlyBom, "﻿", StandardCharsets.UTF_8);

        assertEquals(List.of(), FileReport.readLines(empty));
        assertEquals(List.of(""), FileReport.readLines(onlyBom));
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
