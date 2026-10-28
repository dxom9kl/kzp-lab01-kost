package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.CharacterCodingException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        List<String> errors = collect(lines, summary);
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

    /*
     * Додає коректні записи до показників і повертає причини пропуску решти рядків.
     * Хибний рядок не зупиняє обробку, а потрапляє у звіт із номером і причиною.
     */
    private static List<String> collect(List<String> lines, DeliverySummary summary) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> firstLineById = new HashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String[] fields;
            try {
                fields = ParcelParser.parse(lines.get(index));
            } catch (InvalidRecordException exception) {
                errors.add(skipped(lineNumber, exception.getMessage()));
                continue;
            }
            // Ідентифікатор посилки унікальний: повторний запис спотворив би всі показники.
            Integer firstLine = firstLineById.putIfAbsent(fields[ParcelParser.ID], lineNumber);
            if (firstLine != null) {
                errors.add(skipped(lineNumber, "повторний ідентифікатор посилки %s (уперше в рядку %d)"
                        .formatted(fields[ParcelParser.ID], firstLine)));
                continue;
            }
            summary.add(fields);
        }
        return errors;
    }

    private static String skipped(int lineNumber, String reason) {
        return String.format(Locale.ROOT, "Рядок %d: %s", lineNumber, reason);
    }
}
