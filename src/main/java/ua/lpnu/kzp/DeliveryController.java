package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ua.lpnu.kzp.delivery.DeliverySummary;
import ua.lpnu.kzp.delivery.InvalidRecordException;
import ua.lpnu.kzp.delivery.ParcelParser;
import ua.lpnu.kzp.report.FileReport;
import ua.lpnu.kzp.report.ReportFormatter;

/**
 * Керує обробкою записів: читає файл, перевіряє рядки, обчислює показники
 * та виводить однаковий звіт у консоль і файл.
 */
public final class DeliveryController {
    private final Path input;
    private final Path output;

    /**
     * Створює контролер для пари файлів.
     *
     * @param input шлях до вхідного CSV-файла
     * @param output шлях до файла звіту
     */
    public DeliveryController(Path input, Path output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Виконує обробку.
     *
     * @param out потік для звіту
     * @param err потік для повідомлень про помилки
     * @return код завершення з {@link ExitCode}
     */
    public int run(PrintStream out, PrintStream err) {
        List<String> lines;
        try {
            lines = FileReport.readLines(input);
        } catch (NoSuchFileException exception) {
            err.printf("Файл не знайдено: %s%n", input);
            return ExitCode.DATA_ERROR;
        } catch (CharacterCodingException exception) {
            err.printf("Файл %s має бути в кодуванні UTF-8%n", input);
            return ExitCode.DATA_ERROR;
        } catch (IOException exception) {
            err.printf("Не вдалося прочитати файл %s: %s%n", input, exception.getMessage());
            return ExitCode.DATA_ERROR;
        }

        DeliverySummary summary = new DeliverySummary();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            try {
                summary.add(ParcelParser.parse(lines.get(index)));
            } catch (InvalidRecordException exception) {
                // Хибний рядок не зупиняє обробку, а потрапляє у звіт із номером і причиною.
                errors.add(String.format(Locale.ROOT, "Рядок %d: %s", index + 1, exception.getMessage()));
            }
        }

        String report = ReportFormatter.format(input, summary, errors);
        out.print(report);
        try {
            FileReport.writeReport(output, report);
        } catch (IOException exception) {
            err.printf("Не вдалося записати звіт у файл %s: %s%n", output, exception.getMessage());
            return ExitCode.DATA_ERROR;
        }
        out.printf("%nЗвіт записано у файл: %s%n", output);
        return summary.isEmpty() ? ExitCode.DATA_ERROR : ExitCode.OK;
    }
}
