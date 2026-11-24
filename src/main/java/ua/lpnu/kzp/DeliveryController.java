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
import ua.lpnu.kzp.delivery.Parcel;
import ua.lpnu.kzp.delivery.ParcelRecord;
import ua.lpnu.kzp.report.FileReport;
import ua.lpnu.kzp.report.ReportFormatter;
import ua.lpnu.kzp.storage.CsvExporter;
import ua.lpnu.kzp.storage.CsvImporter;
import ua.lpnu.kzp.storage.DataStorageException;
import ua.lpnu.kzp.storage.Repository;

/**
 * Керує обробкою записів: читає файл, перевіряє рядки, зберігає коректні посилки в {@link Repository},
 * обчислює показники та виводить однаковий звіт у консоль і файл. За потреби експортує посилки в CSV і
 * перевіряє, що з файла їх можна відновити без змін.
 */
public final class DeliveryController {
    private final Path input;
    private final Path output;
    private final Path csv;

    /**
     * Створює контролер для пари файлів без CSV-експорту.
     *
     * @param input шлях до вхідного CSV-файла
     * @param output шлях до файла звіту
     */
    public DeliveryController(Path input, Path output) {
        this(input, output, null);
    }

    /**
     * Створює контролер, який після звіту може експортувати коректні посилки в CSV.
     *
     * @param input шлях до вхідного CSV-файла
     * @param output шлях до файла звіту
     * @param csv шлях до CSV-файла посилок або {@code null}, якщо експорт не потрібен
     */
    public DeliveryController(Path input, Path output, Path csv) {
        this.input = input;
        this.output = output;
        this.csv = csv;
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

        Repository<Parcel> parcels = new Repository<>();
        List<String> errors = collect(lines, parcels);
        DeliverySummary summary = DeliverySummary.of(parcels.all());
        String report = ReportFormatter.format(input, summary, errors);
        out.print(report);
        try {
            FileReport.writeReport(output, report);
        } catch (IOException exception) {
            err.printf("Не вдалося записати звіт у файл %s: %s%n", output, exception.getMessage());
            return ExitCode.DATA_ERROR;
        }
        out.printf("%nЗвіт записано у файл: %s%n", output);
        if (csv != null && !exportCsv(parcels.all(), out, err)) {
            return ExitCode.DATA_ERROR;
        }
        return summary.isEmpty() ? ExitCode.DATA_ERROR : ExitCode.OK;
    }

    /*
     * Додає коректні посилки до сховища й повертає причини пропуску решти рядків.
     * Хибний рядок не зупиняє обробку, а потрапляє у звіт із номером і причиною.
     */
    private static List<String> collect(List<String> lines, Repository<Parcel> parcels) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> firstLineById = new HashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            Parcel parcel;
            try {
                parcel = Parcel.fromCsv(lines.get(index));
            } catch (IllegalArgumentException exception) {
                errors.add(skipped(lineNumber, exception.getMessage()));
                continue;
            }
            // Ідентифікатор посилки унікальний: повторний запис спотворив би всі показники.
            Integer firstLine = firstLineById.putIfAbsent(parcel.getId(), lineNumber);
            if (firstLine != null) {
                errors.add(skipped(lineNumber, String.format(Locale.ROOT,
                        "повторний ідентифікатор посилки %s (уперше в рядку %d)", parcel.getId(), firstLine)));
                continue;
            }
            parcels.add(parcel);
        }
        return errors;
    }

    private static String skipped(int lineNumber, String reason) {
        return String.format(Locale.ROOT, "Рядок %d: %s", lineNumber, reason);
    }

    /*
     * Записує посилки в CSV, читає файл назад і порівнює відновлені записи з початковими (round-trip).
     * Помилку не поглинаємо: її повідомлення й першопричина потрапляють у потік помилок.
     */
    private boolean exportCsv(List<Parcel> parcels, PrintStream out, PrintStream err) {
        Repository<ParcelRecord> records = new Repository<>();
        parcels.forEach(parcel -> records.add(ParcelRecord.of(parcel)));
        List<ParcelRecord> restored;
        try {
            CsvExporter.write(csv, ParcelRecord.class, records.all());
            restored = CsvImporter.read(csv, ParcelRecord.class, ParcelRecord::fromFields);
        } catch (DataStorageException exception) {
            err.printf("Помилка CSV: %s%n", exception.getMessage());
            Throwable cause = rootCause(exception);
            if (cause != exception) {
                err.printf("  причина: %s%n", cause);
            }
            return false;
        }
        boolean same = restored.equals(records.all());
        out.printf("CSV записано у файл: %s%n", csv);
        out.printf(Locale.ROOT, "Відновлених із CSV записів: %d%n", restored.size());
        out.printf("Round-trip: %s%n", same);
        if (!same) {
            err.printf("Відновлені з CSV записи відрізняються від записаних%n");
        }
        return same;
    }

    /* Найглибша причина в ланцюгу: IOException, NumberFormatException тощо. */
    private static Throwable rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
