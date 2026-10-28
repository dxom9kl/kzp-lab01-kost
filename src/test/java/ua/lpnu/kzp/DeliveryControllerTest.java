package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeliveryControllerTest {
    @TempDir
    Path directory;

    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

    private int process(String... lines) throws IOException {
        Path input = directory.resolve("input.csv");
        Files.write(input, List.of(lines), StandardCharsets.UTF_8);
        return new DeliveryController(input, directory.resolve("report.txt")).run(out, err);
    }

    private String report() throws IOException {
        return Files.readString(directory.resolve("report.txt"), StandardCharsets.UTF_8);
    }

    /* Хибний рядок не зупиняє обробку, а потрапляє у звіт із номером і причиною. */
    @Test
    void continuesAfterInvalidLine() throws IOException {
        int code = process("UA-1;2.00;Львів;1;true", "UA-2;abc;Київ;2;false", "UA-3;4.00;Одеса;3;false");

        assertEquals(ExitCode.OK, code);
        assertTrue(report().contains("Рядок 2: вага не є числом: \"abc\""), report());
        assertTrue(report().lines().anyMatch(line -> line.matches("Коректних записів\\s+2")), report());
    }

    /* Консоль отримує той самий текст, що й файл, плюс повідомлення про запис. */
    @Test
    void printsSameReportToConsoleAndFile() throws IOException {
        process("UA-1;2.00;Львів;1;true");

        String console = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(console.startsWith(report()), console);
        assertTrue(console.endsWith("Звіт записано у файл: %s%n".formatted(directory.resolve("report.txt"))),
                console);
    }

    /* Файл без жодного коректного запису завершується кодом помилки даних, але звіт є. */
    @Test
    void failsWhenNoValidRecords() throws IOException {
        int code = process("", "UA-1;0;Київ;1;false");

        assertEquals(ExitCode.DATA_ERROR, code);
        assertTrue(report().contains("Коректних записів немає"), report());
    }

    /* Відсутній вхідний файл повідомляється в потік помилок. */
    @Test
    void reportsMissingInputFile() {
        Path missing = directory.resolve("nope.csv");

        int code = new DeliveryController(missing, directory.resolve("report.txt")).run(out, err);

        assertEquals(ExitCode.DATA_ERROR, code);
        assertEquals("Файл не знайдено: %s%n".formatted(missing), errBuffer.toString(StandardCharsets.UTF_8));
    }

    /* Файл не в UTF-8 не читається з помилковими символами, а відкидається з поясненням. */
    @Test
    void rejectsNonUtf8File() throws IOException {
        Path input = directory.resolve("cp1251.csv");
        Files.write(input, "UA-1;1.00;Київ;1;true".getBytes(Charset.forName("windows-1251")));

        int code = new DeliveryController(input, directory.resolve("report.txt")).run(out, err);

        assertEquals(ExitCode.DATA_ERROR, code);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("має бути в кодуванні UTF-8"));
    }
}
