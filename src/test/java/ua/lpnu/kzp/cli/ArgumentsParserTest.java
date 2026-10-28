package ua.lpnu.kzp.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArgumentsParserTest {
    /* Без аргументів програма обробляє файли за замовчуванням. */
    @Test
    void usesDefaultsWithoutArguments() throws UsageException {
        Arguments arguments = ArgumentsParser.parse(new String[0]);

        assertEquals(Arguments.Mode.PROCESS, arguments.mode());
        assertEquals(Path.of("data", "input.csv"), arguments.input());
        assertEquals(Path.of("out", "report.txt"), arguments.output());
    }

    /* Шляхи до файлів можна задати в будь-якому порядку. */
    @Test
    void readsPathsInAnyOrder() throws UsageException {
        Arguments arguments = ArgumentsParser.parse(
                new String[] {"--output", "звіти/звіт.txt", "--input", "дані.csv"});

        assertEquals(Arguments.Mode.PROCESS, arguments.mode());
        assertEquals(Path.of("дані.csv"), arguments.input());
        assertEquals(Path.of("звіти", "звіт.txt"), arguments.output());
    }

    /* --help і --version перемикають режим роботи. */
    @Test
    void recognisesHelpAndVersion() throws UsageException {
        assertEquals(Arguments.Mode.HELP, ArgumentsParser.parse(new String[] {"--help"}).mode());
        assertEquals(Arguments.Mode.VERSION, ArgumentsParser.parse(new String[] {"--version"}).mode());
    }

    /* Помилки в аргументах повідомляються зрозумілим текстом. */
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            --input                     | Опція --input потребує шляху до файла
            --output                    | Опція --output потребує шляху до файла
            --input --output report.txt | Опція --input потребує шляху до файла
            --verbose                   | Невідомий аргумент: --verbose
            data/input.csv              | Невідомий аргумент: data/input.csv
            --help --input a.csv        | Опцію --help не можна поєднувати з іншими аргументами
            --input a.csv --version     | Опцію --version не можна поєднувати з іншими аргументами
            """)
    void rejectsWrongArguments(String commandLine, String message) {
        UsageException exception = assertThrows(UsageException.class,
                () -> ArgumentsParser.parse(commandLine.split(" ")));

        assertEquals(message, exception.getMessage());
    }

    /* Довідка описує всі опції й завершує рядки роздільником поточної ОС. */
    @Test
    void usageDescribesAllOptions() {
        String usage = ArgumentsParser.usage();

        for (String option : new String[] {"--input <файл>", "--output <файл>", "--help", "--version"}) {
            assertTrue(usage.contains(option), option);
        }
        assertTrue(usage.endsWith(System.lineSeparator()));
    }
}
