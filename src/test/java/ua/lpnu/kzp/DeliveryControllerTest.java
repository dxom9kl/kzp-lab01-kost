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
import java.util.Locale;
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

    private static boolean hasLine(String text, String regex) {
        return text.lines().anyMatch(line -> line.matches(regex));
    }

    private static List<String> metrics(String report) {
        return report.lines()
                .filter(line -> line.matches(
                        "(Коректних записів|Середня вага|Найбільший термін доставлення|Експрес-доставлень)\\s.*"))
                .toList();
    }

    /* Хибні рядки не потрапляють до списку посилок: показники ті самі, що й без цих рядків. */
    @Test
    void invalidLinesDoNotChangeMetrics() throws IOException {
        // Arrange
        String first = "UA-1;2.00;Львів;1;true";
        String second = "UA-2;4.00;Одеса;3;false";
        process(first, second);
        List<String> expected = metrics(report());

        // Act
        process("UA-3;0;Київ;9;true", first, "UA-4;100.00;Київ;9;так", second, "UA-5;-1;Київ;9;true");

        // Assert
        assertEquals(4, expected.size());
        assertEquals(expected, metrics(report()));
    }

    /* Кожен наступний повтор називає рядок першої появи, а не попередній повтор. */
    @Test
    void repeatedDuplicateNamesFirstLine() throws IOException {
        process("UA-1;2.00;Київ;1;true", "UA-1;2.00;Київ;1;true", "UA-1;2.00;Київ;1;true");

        assertTrue(report().contains("Рядок 3: повторний ідентифікатор посилки UA-1 (уперше в рядку 1)"),
                report());
    }

    /* Номер рядка в причині пропуску записується цифрами 0–9 за будь-якої локалі за замовчуванням. */
    @Test
    void duplicateReasonUsesAsciiDigitsRegardlessOfDefaultLocale() throws IOException {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SA-u-nu-arab"));

            process("UA-1;2.00;Київ;1;true", "UA-1;2.00;Київ;1;true");

            assertTrue(report().contains("Рядок 2: повторний ідентифікатор посилки UA-1 (уперше в рядку 1)"),
                    report());
        } finally {
            Locale.setDefault(saved);
        }
    }

    /* Хибний рядок не зупиняє обробку, а потрапляє у звіт із номером і причиною. */
    @Test
    void continuesAfterInvalidLine() throws IOException {
        int code = process("UA-1;2.00;Львів;1;true", "UA-2;abc;Київ;2;false", "UA-3;4.00;Одеса;3;false");

        assertEquals(ExitCode.OK, code);
        assertTrue(report().contains("Рядок 2: вага не є числом: \"abc\""), report());
        assertTrue(hasLine(report(), "Коректних записів\\s+2"), report());
    }

    /* Повторний ідентифікатор не враховується вдруге й називає рядок першої появи. */
    @Test
    void skipsDuplicateParcelId() throws IOException {
        int code = process("UA-1;2.00;Київ;1;true", "UA-2;4.00;Львів;3;false", "UA-1;2.00;Київ;1;true");

        assertEquals(ExitCode.OK, code);
        assertTrue(report().contains("Рядок 3: повторний ідентифікатор посилки UA-1 (уперше в рядку 1)"),
                report());
        assertTrue(hasLine(report(), "Коректних записів\\s+2"), report());
        assertTrue(hasLine(report(), "Експрес-доставлень\\s+1"), report());
    }

    /* Ідентифікатор хибного рядка не займає місця: наступний коректний запис з ним приймається. */
    @Test
    void acceptsIdAfterInvalidLineWithSameId() throws IOException {
        process("UA-1;0;Київ;1;true", "UA-1;2.00;Київ;1;true");

        assertTrue(hasLine(report(), "Коректних записів\\s+1"), report());
        assertTrue(report().contains("Рядок 1: вага має бути додатною: 0"), report());
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

    /* --csv: коректні посилки записуються в CSV, читаються назад і дорівнюють записаним. */
    @Test
    void exportsValidParcelsToCsv() throws IOException {
        Path csv = directory.resolve("csv").resolve("parcels.csv");

        int code = new DeliveryController(Path.of("data", "input.csv"), directory.resolve("report.txt"), csv)
                .run(out, err);

        assertEquals(ExitCode.OK, code);
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(9, lines.size());
        assertEquals("місто,габарити,статус,номер,вага", lines.get(0));
        assertEquals("Кам'янець-Подільський,,IN_TRANSIT,UA-1007,3.1", lines.get(7));
        String console = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(console.endsWith("CSV записано у файл: %s%nВідновлених із CSV записів: 8%nRound-trip: true%n"
                .formatted(csv)), console);
        assertEquals("", errBuffer.toString(StandardCharsets.UTF_8));
    }

    /* Експорт у CSV не змінює звіту: файл звіту такий самий, як без --csv. */
    @Test
    void csvExportKeepsReportUnchanged() throws IOException {
        Path input = Path.of("data", "input.csv");

        new DeliveryController(input, directory.resolve("plain.txt")).run(out, err);
        new DeliveryController(input, directory.resolve("with-csv.txt"), directory.resolve("parcels.csv")).run(out, err);

        assertEquals(Files.readString(directory.resolve("plain.txt"), StandardCharsets.UTF_8),
                Files.readString(directory.resolve("with-csv.txt"), StandardCharsets.UTF_8));
    }

    /* Помилку запису CSV видно в потоці помилок разом із першопричиною, а код завершення – 1. */
    @Test
    void reportsCsvErrorWithCause() throws IOException {
        Path folder = Files.createDirectory(directory.resolve("parcels.csv"));
        Path input = directory.resolve("input.csv");
        Files.write(input, List.of("UA-1;2.00;Львів;1;true"), StandardCharsets.UTF_8);

        int code = new DeliveryController(input, directory.resolve("report.txt"), folder).run(out, err);

        assertEquals(ExitCode.DATA_ERROR, code);
        String errors = errBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(errors.startsWith("Помилка CSV: не вдалося записати CSV-файл %s%n  причина: java.nio.file."
                .formatted(folder)), errors);
        assertTrue(hasLine(report(), "Коректних записів\\s+1"), report());
    }

    /* Без коректних посилок CSV містить лише заголовок, а код завершення лишається 1. */
    @Test
    void noValidParcelsGiveHeaderOnlyCsv() throws IOException {
        Path input = directory.resolve("input.csv");
        Files.write(input, List.of("UA-1;0;Київ;1;false"), StandardCharsets.UTF_8);
        Path csv = directory.resolve("parcels.csv");

        int code = new DeliveryController(input, directory.resolve("report.txt"), csv).run(out, err);

        assertEquals(ExitCode.DATA_ERROR, code);
        assertEquals(List.of("місто,габарити,статус,номер,вага"), Files.readAllLines(csv, StandardCharsets.UTF_8));
        String console = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("Відновлених із CSV записів: 0"), console);
        assertTrue(console.contains("Round-trip: true"), console);
    }
}
