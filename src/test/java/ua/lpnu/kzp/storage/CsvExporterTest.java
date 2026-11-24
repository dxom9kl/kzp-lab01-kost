package ua.lpnu.kzp.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CsvExporterTest {
    private static final String NL = System.lineSeparator();

    @TempDir
    Path directory;

    /* Клас іншої предметної області; поля оголошено не в алфавітному порядку, note не позначено. */
    static final class Book {
        @CsvColumn("рік")
        private final int year;

        @CsvColumn("назва")
        private final String title;

        private final String note;

        Book(String title, int year, String note) {
            this.title = title;
            this.year = year;
            this.note = note;
        }

        @Override
        public String toString() {
            return "%s (%d): %s".formatted(title, year, note);
        }
    }

    /* Базовий клас з анотованим полем. */
    static class Item {
        @CsvColumn("код")
        private final String code;

        Item(String code) {
            this.code = code;
        }

        String code() {
            return code;
        }
    }

    /* Підклас додає власний стовпець до стовпця суперкласу. */
    static final class PricedItem extends Item {
        @CsvColumn("ціна")
        private final double price;

        PricedItem(String code, double price) {
            super(code);
            this.price = price;
        }

        double price() {
            return price;
        }
    }

    /* Record: анотації компонентів компілятор переносить на поля. */
    record Point(@CsvColumn("y") int y, @CsvColumn("x") int x) {
    }

    /* Значення може бути відсутнім. */
    record Note(@CsvColumn("текст") String text) {
    }

    /* Клас без анотованих полів. */
    record Plain(String name) {
    }

    /* Два поля мають однакову назву стовпця. */
    record Duplicated(@CsvColumn("назва") String first, @CsvColumn("назва") String second) {
    }

    /* Статичне поле не належить окремому запису. */
    static final class Counter {
        @CsvColumn("лічильник")
        private static int total;

        static int total() {
            return total;
        }
    }

    /* Порядок стовпців задають Java-імена полів, а не порядок оголошення; назви – з анотацій. */
    @Test
    void ordersColumnsBySortedJavaFieldNames() throws DataStorageException {
        assertEquals(List.of("назва", "рік"), CsvExporter.header(Book.class));
        assertEquals(List.of("x", "y"), CsvExporter.header(Point.class));
    }

    /* Експортер працює з довільним класом без змін свого коду; неанотоване поле не експортується. */
    @Test
    void exportsAnyAnnotatedClass() throws DataStorageException {
        String csv = CsvExporter.toCsv(Book.class, List.of(new Book("Кобзар", 1840, "не експортується")));

        assertEquals("назва,рік" + NL + "Кобзар,1840" + NL, csv);
    }

    /* Поля суперкласу теж стають стовпцями. */
    @Test
    void includesSuperclassFields() throws DataStorageException {
        assertEquals("код,ціна" + NL + "A-01,99.5" + NL,
                CsvExporter.toCsv(PricedItem.class, List.of(new PricedItem("A-01", 99.5))));
    }

    /* Значення з комою, лапкою, CR чи LF береться в лапки, а внутрішня лапка подвоюється. */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource
    void escapesSpecialCharacters(String value, String expected) {
        assertEquals(expected, CsvExporter.escape(value));
    }

    static Stream<Arguments> escapesSpecialCharacters() {
        return Stream.of(
                arguments("Львів", "Львів"),
                arguments("", ""),
                arguments("Кам'янець-Подільський", "Кам'янець-Подільський"),
                arguments("Біла Церква, Київська обл.", "\"Біла Церква, Київська обл.\""),
                arguments("відділення \"Центральне\"", "\"відділення \"\"Центральне\"\"\""),
                arguments("Ужгород\nвул. Корзо", "\"Ужгород\nвул. Корзо\""),
                arguments("рядок\rрядок", "\"рядок\rрядок\""));
    }

    /* Відсутнє значення (null) дає порожнє поле, а не текст «null». */
    @Test
    void nullValueBecomesEmptyField() throws DataStorageException {
        assertEquals("текст" + NL + NL + "\"a,b\"" + NL,
                CsvExporter.toCsv(Note.class, Arrays.asList(new Note(null), new Note("a,b"))));
    }

    /* Порожній список дає лише заголовок: тому клас записів передається окремим параметром. */
    @Test
    void emptyListGivesHeaderOnly() throws DataStorageException {
        assertEquals("назва,рік" + NL, CsvExporter.toCsv(Book.class, List.of()));
    }

    /* Клас без анотованих полів не можна експортувати. */
    @Test
    void rejectsClassWithoutAnnotatedFields() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Plain.class, List.of(new Plain("a"))));

        assertEquals("клас %s не має полів з анотацією @CsvColumn".formatted(Plain.class.getName()),
                exception.getMessage());
    }

    /* Повторна назва стовпця зробила б заголовок неоднозначним. */
    @Test
    void rejectsDuplicateColumnNames() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.header(Duplicated.class));

        assertEquals("повторна назва CSV-стовпця \"назва\" в полі %s.second".formatted(Duplicated.class.getName()),
                exception.getMessage());
    }

    /* Статичне поле не може бути стовпцем. */
    @Test
    void rejectsStaticField() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.header(Counter.class));

        assertEquals("статичне поле %s.total не може бути CSV-стовпцем".formatted(Counter.class.getName()),
                exception.getMessage());
    }

    /* null у списку записів є помилкою з номером запису. */
    @Test
    void rejectsNullRecord() {
        List<Book> books = Arrays.asList(new Book("Кобзар", 1840, ""), null);

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Book.class, books));

        assertEquals("запис 2 для CSV дорівнює null", exception.getMessage());
    }

    /* Файл записується в UTF-8 з роздільником рядка поточної ОС, а перенесення в полі лишається LF. */
    @Test
    void writesUtf8WithSystemLineSeparator() throws Exception {
        Path path = directory.resolve("notes.csv");

        CsvExporter.write(path, Note.class, List.of(new Note("Ужгород\nвул. Корзо")));

        byte[] expected = ("текст" + NL + "\"Ужгород\nвул. Корзо\"" + NL).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, Files.readAllBytes(path));
    }

    /* Новий коротший експорт не лишає рядків попереднього. */
    @Test
    void truncatesPreviousContent() throws Exception {
        Path path = directory.resolve("books.csv");
        CsvExporter.write(path, Book.class, List.of(new Book("А", 1, ""), new Book("Б", 2, ""), new Book("В", 3, "")));

        CsvExporter.write(path, Book.class, List.of(new Book("Г", 4, "")));

        assertEquals("назва,рік" + NL + "Г,4" + NL, Files.readString(path, StandardCharsets.UTF_8));
    }

    /* Відсутні батьківські каталоги створюються. */
    @Test
    void createsMissingDirectories() throws Exception {
        Path path = directory.resolve("out").resolve("csv").resolve("books.csv");

        CsvExporter.write(path, Book.class, List.of());

        assertTrue(Files.isRegularFile(path));
    }

    /* Помилка файлової системи загортається, а IOException лишається першопричиною. */
    @Test
    void wrapsFileErrorWithCause() throws Exception {
        Path folder = Files.createDirectory(directory.resolve("books.csv"));

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(folder, Book.class, List.of()));

        assertEquals("не вдалося записати CSV-файл %s".formatted(folder), exception.getMessage());
        assertInstanceOf(IOException.class, exception.getCause());
    }
}
