package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args,
                new PrintStream(outBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }

    private static boolean hasLine(String text, String regex) {
        return text.lines().anyMatch(line -> line.matches(regex));
    }

    /* --help виводить довідку й завершується успішно. */
    @Test
    void printsHelp() {
        assertEquals(ExitCode.OK, run("--help"));
        assertTrue(out().startsWith("Використання:"), out());
    }

    /* --version виводить версію з pom.xml і номер збірки. */
    @Test
    void printsVersion() {
        assertEquals(ExitCode.OK, run("--version"));
        assertTrue(out().strip().matches("\\d+\\.\\d+\\.\\d+\\+\\w+"), out());
    }

    /* Помилка в аргументах завершується кодом 2 і підказкою про --help. */
    @Test
    void rejectsWrongArguments() {
        assertEquals(ExitCode.USAGE_ERROR, run("--input"));
        assertTrue(err().contains("Опція --input потребує шляху до файла"), err());
        assertTrue(err().contains("--help"), err());
    }

    /* Приклад із репозиторію дає показники, обчислені вручну. */
    @Test
    void processesSampleData(@TempDir Path directory) throws IOException {
        Path output = directory.resolve("out").resolve("report.txt");

        assertEquals(ExitCode.OK, run("--input", "data/input.csv", "--output", output.toString()));

        String report = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(hasLine(report, "Коректних записів\\s+8"), report);
        assertTrue(hasLine(report, "Середня вага\\s+6\\.45 кг"), report);
        assertTrue(hasLine(report, "Найбільший термін доставлення\\s+6 дн\\. \\(UA-1007, Кам'янець-Подільський\\)"),
                report);
        assertTrue(hasLine(report, "Експрес-доставлень\\s+4"), report);
        assertTrue(out().startsWith(report), out());
    }
}
