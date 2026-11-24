package ua.lpnu.kzp.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Читає CSV-файл у форматі {@link CsvExporter} і відновлює записи.
 *
 * <p>Імпортер виконує лише загальні кроки: читає весь файл в UTF-8, розбирає його {@link CsvParser},
 * перевіряє, що заголовок збігається з назвами анотованих полів класу, і передає поля кожного запису
 * предметному {@link RowMapper}. Перетворення тексту на число, enum чи інший тип залежить від класу, тому
 * його виконує сам клас записів.
 */
public final class CsvImporter {
    /* Позначка порядку байтів, яку додають, зокрема, Excel і Блокнот у режимі «UTF-8 з BOM». */
    private static final char BYTE_ORDER_MARK = '﻿';

    /* Забороняє створення екземплярів службового класу. */
    private CsvImporter() {
    }

    /**
     * Перетворює поля одного CSV-запису на об'єкт.
     *
     * @param <T> тип об'єкта
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        /**
         * Створює об'єкт із полів CSV-запису.
         *
         * @param fields поля запису в порядку стовпців заголовка
         * @return відновлений об'єкт, не {@code null}
         * @throws DataStorageException якщо поля не утворюють коректного об'єкта
         */
        T fromFields(List<String> fields) throws DataStorageException;
    }

    /**
     * Читає CSV-файл і відновлює записи.
     *
     * <p>Файл лише із заголовком дає порожній список, а порожній файл без заголовка є помилкою формату. Номер
     * запису в повідомленні враховує заголовок: без багаторядкових полів він збігається з номером рядка файла.
     *
     * @param <T> тип записів
     * @param path шлях до CSV-файла в UTF-8; позначка порядку байтів на початку ігнорується
     * @param type клас записів, анотовані поля якого задають очікуваний заголовок
     * @param mapper предметне перетворення полів запису на об'єкт
     * @return незмінний список записів у порядку файла
     * @throws DataStorageException якщо файл неможливо прочитати або він не в UTF-8, порожній, має інший
     *         заголовок, порушує правила CSV чи містить хибний запис; першопричину повертає {@code getCause()}
     * @throws NullPointerException якщо будь-який аргумент дорівнює {@code null}
     */
    public static <T> List<T> read(Path path, Class<T> type, RowMapper<? extends T> mapper)
            throws DataStorageException {
        Objects.requireNonNull(path, "шлях не може бути null");
        Objects.requireNonNull(mapper, "перетворення запису не може бути null");
        List<String> header = CsvExporter.header(type);
        String text;
        try {
            // Увесь текст читається одразу: запис із полем у лапках може займати кілька рядків файла.
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося прочитати CSV-файл %s".formatted(path), exception);
        }
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            text = text.substring(1);
        }
        List<List<String>> rows;
        try {
            rows = CsvParser.parseDocument(text);
        } catch (DataStorageException exception) {
            throw new DataStorageException("пошкоджений CSV-файл %s: %s".formatted(path, exception.getMessage()),
                    exception);
        }
        if (rows.isEmpty()) {
            throw new DataStorageException("CSV-файл %s порожній: немає рядка заголовка".formatted(path));
        }
        // Поля відновлюються за позицією, тому інший порядок стовпців переплутав би значення.
        if (!rows.get(0).equals(header)) {
            throw new DataStorageException("заголовок CSV-файла %s не відповідає класу %s: очікується «%s», отримано «%s»"
                    .formatted(path, type.getSimpleName(), String.join(",", header),
                            String.join(",", rows.get(0))));
        }
        List<T> records = new ArrayList<>(rows.size() - 1);
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(Objects.requireNonNull(mapper.fromFields(rows.get(index)), "запис не відновлено"));
            } catch (DataStorageException exception) {
                throw new DataStorageException(String.format(Locale.ROOT, "запис %d CSV-файла %s: %s",
                        index + 1, path, exception.getMessage()), exception);
            }
        }
        return List.copyOf(records);
    }
}
