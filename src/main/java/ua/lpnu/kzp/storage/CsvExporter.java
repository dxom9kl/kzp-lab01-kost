package ua.lpnu.kzp.storage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Експортує в CSV поля об'єктів, позначені анотацією {@link CsvColumn}.
 *
 * <p>Експортер не знає жодного класу предметної області: стовпці він отримує рефлексією з класу записів, тому
 * працює з будь-яким плоским класом або record, поля якого позначено {@code @CsvColumn}, зокрема поля
 * суперкласів. Порядок стовпців визначає сортування Java-імен полів, а назви стовпців беруться з анотації.
 *
 * <p>Значення перетворюється на текст через {@link String#valueOf(Object)}, константа enum – через
 * {@link Enum#name()}, а {@code null} дає порожнє поле. Значення з комою, лапкою, CR чи LF береться в лапки, а
 * внутрішня лапка подвоюється. Кожен запис завершує роздільник рядка поточної ОС, а файл записується в UTF-8.
 */
public final class CsvExporter {
    private static final String SEPARATOR = ",";
    private static final String QUOTE = "\"";

    /* Забороняє створення екземплярів службового класу. */
    private CsvExporter() {
    }

    /**
     * Записує CSV-файл: рядок заголовка й по одному запису на кожен об'єкт списку.
     *
     * @param <T> тип записів
     * @param path шлях до CSV-файла; відсутні батьківські каталоги створюються, наявний файл перезаписується
     * @param type клас записів, з якого беруться стовпці; він потрібен і для порожнього списку
     * @param records записи для експорту
     * @throws DataStorageException якщо клас не має придатних CSV-полів, список містить {@code null}, поле
     *         неможливо прочитати або файл неможливо записати
     * @throws NullPointerException якщо будь-який аргумент дорівнює {@code null}
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records) throws DataStorageException {
        Objects.requireNonNull(path, "шлях не може бути null");
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // TRUNCATE_EXISTING не лишає у файлі рядків попереднього, довшого експорту.
            Files.writeString(path, csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException("не вдалося записати CSV-файл %s".formatted(path), exception);
        }
    }

    /**
     * Формує текст CSV-документа без запису у файл.
     *
     * @param <T> тип записів
     * @param type клас записів, з якого беруться стовпці
     * @param records записи для експорту
     * @return заголовок і записи; кожен рядок завершує роздільник рядка поточної ОС
     * @throws DataStorageException якщо клас не має придатних CSV-полів, список містить {@code null} або поле
     *         неможливо прочитати
     * @throws NullPointerException якщо {@code type} або {@code records} дорівнює {@code null}
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records) throws DataStorageException {
        Objects.requireNonNull(records, "список записів не може бути null");
        // Один набір полів задає і заголовок, і порядок значень у кожному записі.
        List<Field> fields = columns(type);
        StringBuilder csv = new StringBuilder();
        appendRecord(csv, names(fields));
        for (int index = 0; index < records.size(); index++) {
            T record = records.get(index);
            if (record == null) {
                throw new DataStorageException(String.format(Locale.ROOT,
                        "запис %d для CSV дорівнює null", index + 1));
            }
            List<String> values = new ArrayList<>(fields.size());
            for (Field field : fields) {
                values.add(text(read(field, record)));
            }
            appendRecord(csv, values);
        }
        return csv.toString();
    }

    /**
     * Повертає назви CSV-стовпців класу в порядку експорту.
     *
     * @param type клас записів
     * @return незмінний список назв із {@link CsvColumn}, упорядкований за Java-іменами полів
     * @throws DataStorageException якщо клас не має придатних CSV-полів
     * @throws NullPointerException якщо {@code type} дорівнює {@code null}
     */
    public static List<String> header(Class<?> type) throws DataStorageException {
        return names(columns(type));
    }

    /*
     * Enum.valueOf відновлює константу за назвою, а toString можна перевизначити, наприклад підписом
     * «у дорозі». Тому константа enum записується назвою, інші значення – через String.valueOf.
     */
    private static String text(Object value) {
        if (value == null) {
            return "";
        }
        return value instanceof Enum<?> constant ? constant.name() : String.valueOf(value);
    }

    /* Бере значення в лапки, якщо воно містить кому, лапку або перенесення рядка. */
    static String escape(String value) {
        boolean special = value.contains(SEPARATOR) || value.contains(QUOTE)
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
        return special ? QUOTE + value.replace(QUOTE, QUOTE + QUOTE) + QUOTE : value;
    }

    private static void appendRecord(StringBuilder csv, List<String> values) {
        csv.append(values.stream()
                        .map(CsvExporter::escape)
                        .collect(Collectors.joining(SEPARATOR)))
                .append(System.lineSeparator());
    }

    private static List<String> names(List<Field> fields) {
        return fields.stream()
                .map(field -> field.getAnnotation(CsvColumn.class).value())
                .toList();
    }

    /*
     * Збирає анотовані поля класу та його суперкласів до Object і впорядковує їх за Java-іменем, бо
     * getDeclaredFields не гарантує порядку. Однакові імена полів різних класів розрізняє назва класу.
     */
    private static List<Field> columns(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "клас записів не може бути null");
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(CsvColumn.class)) {
                    fields.add(field);
                }
            }
        }
        if (fields.isEmpty()) {
            throw new DataStorageException("клас %s не має полів з анотацією @CsvColumn".formatted(type.getName()));
        }
        fields.sort(Comparator.comparing(Field::getName)
                .thenComparing(field -> field.getDeclaringClass().getName()));
        Set<String> names = new HashSet<>();
        for (Field field : fields) {
            String place = "%s.%s".formatted(field.getDeclaringClass().getName(), field.getName());
            String name = field.getAnnotation(CsvColumn.class).value();
            if (name.isBlank()) {
                throw new DataStorageException("порожня назва CSV-стовпця в полі %s".formatted(place));
            }
            if (!names.add(name)) {
                throw new DataStorageException("повторна назва CSV-стовпця \"%s\" в полі %s".formatted(name, place));
            }
            // Статичне поле не належить окремому запису, тому стовпцем бути не може.
            if (Modifier.isStatic(field.getModifiers())) {
                throw new DataStorageException("статичне поле %s не може бути CSV-стовпцем".formatted(place));
            }
            // Приватне поле відкривається свідомо; модулі Java можуть заборонити доступ, тоді експорт неможливий.
            if (!field.trySetAccessible()) {
                throw new DataStorageException("немає доступу до поля %s".formatted(place));
            }
        }
        return List.copyOf(fields);
    }

    private static Object read(Field field, Object record) throws DataStorageException {
        try {
            return field.get(record);
        } catch (IllegalAccessException exception) {
            throw new DataStorageException("не вдалося прочитати поле %s.%s"
                    .formatted(field.getDeclaringClass().getName(), field.getName()), exception);
        }
    }
}
